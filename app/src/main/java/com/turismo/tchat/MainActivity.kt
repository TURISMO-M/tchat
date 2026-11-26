package com.turismo.tchat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.max
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "tchat_prefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_THEME = "theme_mode"
        private const val TCP_PORT = 8888
        private val BT_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
        private const val BT_APP_NAME = "TChatBT"
    }

    private enum class Tab { HOME, CONNECTIONS, CHAT, SETTINGS }

    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Core UI
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mainRootLayout: View
    private lateinit var headerBar: View
    private lateinit var drawerMenu: View
    private lateinit var bottomFloatingDock: View
    private lateinit var btnOpenDrawer: ImageView
    private lateinit var btnModeMenu: ImageView
    private lateinit var cardInAppNotification: CardView
    private lateinit var tvBannerSender: TextView
    private lateinit var tvBannerMessage: TextView

    // Tab Containers
    private lateinit var layoutHomeTab: View
    private lateinit var layoutConnectionsTab: ScrollView
    private lateinit var layoutChatTab: View
    private lateinit var layoutSettingsTab: ScrollView

    // Home Tab
    private lateinit var rvGlobalChats: LinearLayout
    private lateinit var fabNewChat: FloatingActionButton

    // Connections Tab
    private lateinit var tvStatus: TextView
    private lateinit var btnRefreshIp: ImageView
    private lateinit var tvMyIp: TextView
    private lateinit var headerExpandWifi: View
    private lateinit var arrowWifiExpand: ImageView
    private lateinit var btnScanLocalNetwork: Button
    private lateinit var containerDiscoveredWifi: LinearLayout
    private lateinit var headerExpandBt: View
    private lateinit var arrowBtExpand: ImageView
    private lateinit var containerBtContent: LinearLayout
    private lateinit var btnBtServer: Button
    private lateinit var btnScanBt: Button
    private lateinit var containerBtDevices: LinearLayout
    private lateinit var etTargetIp: EditText
    private lateinit var btnHostTcp: Button
    private lateinit var btnConnectTcp: Button

    // Chat Tab
    private lateinit var layoutReplyPreview: View
    private lateinit var tvReplyPreviewText: TextView
    private lateinit var btnCloseReply: ImageView
    private lateinit var rvMessages: RecyclerView
    private lateinit var btnAttach: ImageView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView

    // Settings Tab
    private lateinit var btnGoogleSignIn: Button
    private lateinit var layoutUsernameView: View
    private lateinit var tvUsername: TextView
    private lateinit var btnEditUsername: Button
    private lateinit var layoutUsernameEdit: View
    private lateinit var etUsername: EditText
    private lateinit var btnSaveUsername: Button
    private lateinit var btnCancelEdit: Button
    private lateinit var btnSaveProfile: Button
    private lateinit var btnThemeSystem: Button
    private lateinit var btnThemeDark: Button
    private lateinit var btnThemeLight: Button

    // Bottom Dock & Drawer
    private lateinit var navHomeBtn: LinearLayout
    private lateinit var navConnectionsBtn: LinearLayout
    private lateinit var navSettingsBtn: LinearLayout
    private lateinit var tvDrawerUser: TextView
    private lateinit var drawerItemHome: TextView
    private lateinit var drawerItemConnect: TextView
    private lateinit var drawerItemSettings: TextView
    private lateinit var btnDrawerLogout: Button

    // App State
    private var currentTab = Tab.HOME
    private var username = "User"
    private var googleSignedIn = false
    private val threads = mutableListOf<ChatThread>()
    private var activeThread: ChatThread? = null
    private var replyingTo: ChatMessage? = null
    private lateinit var messageAdapter: ChatMessageAdapter

    private var wifiExpanded = true
    private var btExpanded = true

    // Networking State
    @Volatile
    private var tcpServerSocket: ServerSocket? = null
    @Volatile
    private var activeTcpSocket: Socket? = null
    @Volatile
    private var activeTcpWriter: PrintWriter? = null

    @Volatile
    private var btServerSocket: BluetoothServerSocket? = null
    @Volatile
    private var activeBtSocket: BluetoothSocket? = null
    @Volatile
    private var activeBtWriter: PrintWriter? = null

    private var bannerHideRunnable: Runnable? = null

    private val attachmentPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
            sendMessageText("[Attachment: $fileName]")
        }
    }

    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            showBanner("Bluetooth", "Permissions granted — ready to scan or host")
        } else {
            showBanner("Bluetooth", "Bluetooth permission is required for BT features")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applySavedTheme(prefs.getString(KEY_THEME, "dark") ?: "dark")
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        username = prefs.getString(KEY_USERNAME, "User") ?: "User"

        bindViews()
        setupSystemInsets()
        seedInitialThreads()
        setupListeners()
        setupBackNavigation()
        refreshIpDisplay()
        renderChatsList()
        updateUserDisplay()
        switchTab(Tab.HOME)
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        mainRootLayout = findViewById(R.id.mainRootLayout)
        headerBar = findViewById(R.id.headerBar)
        drawerMenu = findViewById(R.id.drawerMenu)
        bottomFloatingDock = findViewById(R.id.bottomFloatingDock)
        btnOpenDrawer = findViewById(R.id.btnOpenDrawer)
        btnModeMenu = findViewById(R.id.btnModeMenu)
        cardInAppNotification = findViewById(R.id.cardInAppNotification)
        tvBannerSender = findViewById(R.id.tvBannerSender)
        tvBannerMessage = findViewById(R.id.tvBannerMessage)

        layoutHomeTab = findViewById(R.id.layoutHomeTab)
        layoutConnectionsTab = findViewById(R.id.layoutConnectionsTab)
        layoutChatTab = findViewById(R.id.layoutChatTab)
        layoutSettingsTab = findViewById(R.id.layoutSettingsTab)

        rvGlobalChats = findViewById(R.id.rvGlobalChats)
        fabNewChat = findViewById(R.id.fabNewChat)

        tvStatus = findViewById(R.id.tvStatus)
        btnRefreshIp = findViewById(R.id.btnRefreshIp)
        tvMyIp = findViewById(R.id.tvMyIp)
        headerExpandWifi = findViewById(R.id.headerExpandWifi)
        arrowWifiExpand = findViewById(R.id.arrowWifiExpand)
        btnScanLocalNetwork = findViewById(R.id.btnScanLocalNetwork)
        containerDiscoveredWifi = findViewById(R.id.containerDiscoveredWifi)
        headerExpandBt = findViewById(R.id.headerExpandBt)
        arrowBtExpand = findViewById(R.id.arrowBtExpand)
        containerBtContent = findViewById(R.id.containerBtContent)
        btnBtServer = findViewById(R.id.btnBtServer)
        btnScanBt = findViewById(R.id.btnScanBt)
        containerBtDevices = findViewById(R.id.containerBtDevices)
        etTargetIp = findViewById(R.id.etTargetIp)
        btnHostTcp = findViewById(R.id.btnHostTcp)
        btnConnectTcp = findViewById(R.id.btnConnectTcp)

        layoutReplyPreview = findViewById(R.id.layoutReplyPreview)
        tvReplyPreviewText = findViewById(R.id.tvReplyPreviewText)
        btnCloseReply = findViewById(R.id.btnCloseReply)
        rvMessages = findViewById(R.id.rvMessages)
        btnAttach = findViewById(R.id.btnAttach)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        layoutUsernameView = findViewById(R.id.layoutUsernameView)
        tvUsername = findViewById(R.id.tvUsername)
        btnEditUsername = findViewById(R.id.btnEditUsername)
        layoutUsernameEdit = findViewById(R.id.layoutUsernameEdit)
        etUsername = findViewById(R.id.etUsername)
        btnSaveUsername = findViewById(R.id.btnSaveUsername)
        btnCancelEdit = findViewById(R.id.btnCancelEdit)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnThemeSystem = findViewById(R.id.btnThemeSystem)
        btnThemeDark = findViewById(R.id.btnThemeDark)
        btnThemeLight = findViewById(R.id.btnThemeLight)

        navHomeBtn = findViewById(R.id.navHomeBtn)
        navConnectionsBtn = findViewById(R.id.navConnectionsBtn)
        navSettingsBtn = findViewById(R.id.navSettingsBtn)
        tvDrawerUser = findViewById(R.id.tvDrawerUser)
        drawerItemHome = findViewById(R.id.drawerItemHome)
        drawerItemConnect = findViewById(R.id.drawerItemConnect)
        drawerItemSettings = findViewById(R.id.drawerItemSettings)
        btnDrawerLogout = findViewById(R.id.btnDrawerLogout)

        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messageAdapter = ChatMessageAdapter(mutableListOf()) { msg ->
            replyingTo = msg
            layoutReplyPreview.visibility = View.VISIBLE
            tvReplyPreviewText.text = "Replying to ${msg.sender}: ${msg.text}"
        }
        rvMessages.adapter = messageAdapter
    }

    private fun setupSystemInsets() {
        val baseHeaderHeight = dp(56)
        val dp12 = dp(12)
        val dp16 = dp(16)

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, windowInsets ->
            val sysBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = max(sysBars.bottom, ime.bottom)

            // Keep headerBar below notification/status bar while matching its color underneath
            headerBar.layoutParams = headerBar.layoutParams.apply {
                height = baseHeaderHeight + sysBars.top
            }
            headerBar.setPadding(dp12, sysBars.top, dp12, 0)

            // Keep bottom navigation dock & chat input above phone navigation bar / keyboard
            mainRootLayout.setPadding(sysBars.left, 0, sysBars.right, bottomInset)
            bottomFloatingDock.visibility =
                if (ime.bottom > sysBars.bottom && currentTab == Tab.CHAT) View.GONE else View.VISIBLE

            // Keep drawer contents inside safe system bar bounds
            drawerMenu.setPadding(dp16 + sysBars.left, dp16 + sysBars.top, dp16, dp16 + sysBars.bottom)

            windowInsets
        }
        ViewCompat.requestApplyInsets(drawerLayout)
    }

    private fun seedInitialThreads() {
        threads.clear()
    }

    private fun setupListeners() {
        btnOpenDrawer.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnModeMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Space")
            popup.menu.add("Local Wi-Fi")
            popup.menu.add("Bluetooth")
            popup.menu.add("TCP IP")
            popup.setOnMenuItemClickListener { item ->
                activeThread?.let {
                    it.mode = item.title.toString()
                    renderChatsList()
                    showBanner("Mode Selector", "${it.title} set to ${it.mode}")
                }
                true
            }
            popup.show()
        }

        cardInAppNotification.setOnClickListener {
            cardInAppNotification.visibility = View.GONE
        }

        // Bottom Dock
        navHomeBtn.setOnClickListener { switchTab(Tab.HOME) }
        navConnectionsBtn.setOnClickListener { switchTab(Tab.CONNECTIONS) }
        navSettingsBtn.setOnClickListener { switchTab(Tab.SETTINGS) }

        // Drawer Items
        drawerItemHome.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            switchTab(Tab.HOME)
        }
        drawerItemConnect.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            switchTab(Tab.CONNECTIONS)
        }
        drawerItemSettings.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            switchTab(Tab.SETTINGS)
        }
        btnDrawerLogout.setOnClickListener {
            username = "User"
            googleSignedIn = false
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USERNAME, username)
                .apply()
            updateUserDisplay()
            drawerLayout.closeDrawer(GravityCompat.START)
            showBanner("TChat", "Signed out to Guest User")
        }

        // Home Tab
        fabNewChat.setOnClickListener { showCreateChatDialog() }

        // Connections Tab
        btnRefreshIp.setOnClickListener {
            refreshIpDisplay()
            showBanner("Network", "Refreshed local IP address")
        }

        headerExpandWifi.setOnClickListener { toggleWifiSection() }
        arrowWifiExpand.setOnClickListener { toggleWifiSection() }
        btnScanLocalNetwork.setOnClickListener { scanLocalNetwork() }

        headerExpandBt.setOnClickListener { toggleBtSection() }
        arrowBtExpand.setOnClickListener { toggleBtSection() }
        btnBtServer.setOnClickListener { toggleBluetoothHost() }
        btnScanBt.setOnClickListener { scanBluetoothDevices() }

        btnHostTcp.setOnClickListener { toggleTcpHost() }
        btnConnectTcp.setOnClickListener {
            val ip = etTargetIp.text.toString().trim()
            if (ip.isEmpty()) {
                showBanner("TCP Connection", "Enter a Target Host IP Address")
            } else {
                connectToTcpHost(ip)
            }
        }

        // Chat Tab
        btnCloseReply.setOnClickListener {
            replyingTo = null
            layoutReplyPreview.visibility = View.GONE
        }
        btnAttach.setOnClickListener {
            attachmentPicker.launch("*/*")
        }
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessageText(text)
                etMessage.setText("")
            }
        }

        // Settings Tab
        btnGoogleSignIn.setOnClickListener {
            if (googleSignedIn) {
                googleSignedIn = false
                btnGoogleSignIn.text = "Sign In with Google"
                showBanner("Account", "Disconnected local Google profile")
            } else {
                val input = EditText(this).apply {
                    hint = "Display Name"
                    setText(if (username == "User") "" else username)
                }
                AlertDialog.Builder(this)
                    .setTitle("Google Profile Name")
                    .setView(input)
                    .setPositiveButton("Continue") { _, _ ->
                        val name = input.text.toString().trim().ifEmpty { "TChat User" }
                        username = name
                        googleSignedIn = true
                        btnGoogleSignIn.text = "Signed In: $name"
                        updateUserDisplay()
                        saveProfilePrefs()
                        showBanner("Account", "Signed in as $name")
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        btnEditUsername.setOnClickListener {
            etUsername.setText(username)
            layoutUsernameView.visibility = View.GONE
            layoutUsernameEdit.visibility = View.VISIBLE
        }
        btnSaveUsername.setOnClickListener {
            val updated = etUsername.text.toString().trim()
            if (updated.isNotEmpty()) {
                username = updated
                updateUserDisplay()
                saveProfilePrefs()
                showBanner("Profile", "Username updated to $username")
            }
            layoutUsernameEdit.visibility = View.GONE
            layoutUsernameView.visibility = View.VISIBLE
        }
        btnCancelEdit.setOnClickListener {
            layoutUsernameEdit.visibility = View.GONE
            layoutUsernameView.visibility = View.VISIBLE
        }
        btnSaveProfile.setOnClickListener {
            saveProfilePrefs()
            showBanner("Profile", "Profile saved")
        }

        btnThemeSystem.setOnClickListener { setAndSaveTheme("system") }
        btnThemeDark.setOnClickListener { setAndSaveTheme("dark") }
        btnThemeLight.setOnClickListener { setAndSaveTheme("light") }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    currentTab == Tab.CHAT -> {
                        switchTab(Tab.HOME)
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        layoutHomeTab.visibility = if (tab == Tab.HOME) View.VISIBLE else View.GONE
        layoutConnectionsTab.visibility = if (tab == Tab.CONNECTIONS) View.VISIBLE else View.GONE
        layoutChatTab.visibility = if (tab == Tab.CHAT) View.VISIBLE else View.GONE
        layoutSettingsTab.visibility = if (tab == Tab.SETTINGS) View.VISIBLE else View.GONE
        btnModeMenu.visibility = if (tab == Tab.CHAT) View.VISIBLE else View.GONE

        navHomeBtn.setBackgroundResource(
            if (tab == Tab.HOME || tab == Tab.CHAT) R.drawable.bg_nav_active_pill else 0
        )
        navConnectionsBtn.setBackgroundResource(
            if (tab == Tab.CONNECTIONS) R.drawable.bg_nav_active_pill else 0
        )
        navSettingsBtn.setBackgroundResource(
            if (tab == Tab.SETTINGS) R.drawable.bg_nav_active_pill else 0
        )
    }

    private fun openThread(thread: ChatThread) {
        activeThread = thread
        replyingTo = null
        layoutReplyPreview.visibility = View.GONE
        messageAdapter = ChatMessageAdapter(thread.messages) { msg ->
            replyingTo = msg
            layoutReplyPreview.visibility = View.VISIBLE
            tvReplyPreviewText.text = "Replying to ${msg.sender}: ${msg.text}"
        }
        rvMessages.adapter = messageAdapter
        if (thread.messages.isNotEmpty()) {
            rvMessages.scrollToPosition(thread.messages.size - 1)
        }
        switchTab(Tab.CHAT)
    }

    private fun renderChatsList() {
        rvGlobalChats.removeAllViews()
        val dp8 = dp(8)
        val dp14 = dp(14)

        if (threads.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No chats yet.\nTap + to create a Space or connect in Connections."
                setTextColor(Color.parseColor("#888899"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(48), dp(24), dp(24))
            }
            rvGlobalChats.addView(emptyTv)
            return
        }

        for (thread in threads) {
            val card = CardView(this).apply {
                radius = dp(16).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.parseColor("#1D1E30"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(12), dp(6), dp(12), dp(6))
                }
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp14, dp14, dp14, dp14)
            }

            val titleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvTitle = TextView(this).apply {
                text = thread.title
                setTextColor(Color.WHITE)
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvMode = TextView(this).apply {
                text = thread.mode
                setTextColor(Color.parseColor("#4A90E2"))
                textSize = 12f
            }

            titleRow.addView(tvTitle)
            titleRow.addView(tvMode)

            val lastMsg = thread.messages.lastOrNull()
            val tvPreview = TextView(this).apply {
                text = if (lastMsg != null) "${lastMsg.sender}: ${lastMsg.text}" else "No messages yet"
                setTextColor(Color.parseColor("#CCCCCC"))
                textSize = 13f
                maxLines = 1
                setPadding(0, dp8 / 2, 0, 0)
            }

            row.addView(titleRow)
            row.addView(tvPreview)
            card.addView(row)
            card.setOnClickListener { openThread(thread) }
            rvGlobalChats.addView(card)
        }
    }

    private fun showCreateChatDialog() {
        val input = EditText(this).apply {
            hint = "Space or contact name (e.g. #team)"
        }
        AlertDialog.Builder(this)
            .setTitle("New Chat or Space")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val raw = input.text.toString().trim()
                if (raw.isNotEmpty()) {
                    val created = ChatThread(
                        id = "thread_${System.currentTimeMillis()}",
                        title = raw,
                        mode = if (raw.startsWith("#")) "Space" else "Local Wi-Fi"
                    )
                    threads.add(0, created)
                    renderChatsList()
                    openThread(created)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendMessageText(text: String) {
        val thread = activeThread ?: threads.firstOrNull() ?: return
        val replyStr = replyingTo?.let { "${it.sender}: ${it.text}" }
        val msg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            sender = username,
            text = text,
            timestamp = currentTime(),
            isOwn = true,
            replyPreview = replyStr
        )
        thread.messages.add(msg)
        messageAdapter.notifyItemInserted(thread.messages.size - 1)
        rvMessages.scrollToPosition(thread.messages.size - 1)
        replyingTo = null
        layoutReplyPreview.visibility = View.GONE
        renderChatsList()

        // Forward over active TCP or Bluetooth socket if connected
        val payload = if (replyStr != null) "[$username | Reply: $replyStr] $text" else "$username: $text"
        executor.execute {
            try {
                activeTcpWriter?.println(payload)
                activeBtWriter?.println(payload)
            } catch (_: Exception) {
            }
        }
    }

    private fun appendIncomingMessage(sender: String, body: String, mode: String, endpoint: String? = null) {
        mainHandler.post {
            val target = threads.find {
                (endpoint != null && it.endpoint == endpoint) || it.title == sender
            } ?: ChatThread(
                id = "peer_${System.currentTimeMillis()}",
                title = sender,
                mode = mode,
                endpoint = endpoint
            ).also { threads.add(0, it) }

            val msg = ChatMessage(
                id = "in_${System.currentTimeMillis()}",
                sender = sender,
                text = body,
                timestamp = currentTime(),
                isOwn = false
            )
            target.messages.add(msg)
            renderChatsList()

            if (activeThread?.id == target.id && currentTab == Tab.CHAT) {
                messageAdapter.notifyItemInserted(target.messages.size - 1)
                rvMessages.scrollToPosition(target.messages.size - 1)
            } else {
                showBanner(sender, body)
            }
        }
    }

    private fun refreshIpDisplay() {
        val ip = getLocalIpv4Address() ?: "127.0.0.1"
        tvMyIp.text = "Your IP: $ip"
    }

    private fun getLocalIpv4Address(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun toggleWifiSection() {
        wifiExpanded = !wifiExpanded
        val vis = if (wifiExpanded) View.VISIBLE else View.GONE
        btnScanLocalNetwork.visibility = vis
        containerDiscoveredWifi.visibility = vis
        arrowWifiExpand.rotation = if (wifiExpanded) 0f else 180f
    }

    private fun toggleBtSection() {
        btExpanded = !btExpanded
        containerBtContent.visibility = if (btExpanded) View.VISIBLE else View.GONE
        arrowBtExpand.rotation = if (btExpanded) 0f else 180f
    }

    private fun scanLocalNetwork() {
        val localIp = getLocalIpv4Address()
        if (localIp == null || !localIp.contains('.')) {
            showBanner("Local Network", "Connect to Wi-Fi to scan subnet")
            return
        }
        val prefix = localIp.substringBeforeLast('.')
        btnScanLocalNetwork.isEnabled = false
        btnScanLocalNetwork.text = "Scanning $prefix.x..."
        containerDiscoveredWifi.removeAllViews()

        executor.execute {
            val found = mutableListOf<String>()
            val scanPool = Executors.newFixedThreadPool(24)
            val tasks = (1..254).map { host ->
                scanPool.submit {
                    val candidate = "$prefix.$host"
                    if (candidate != localIp) {
                        try {
                            Socket().use { s ->
                                s.connect(InetSocketAddress(candidate, TCP_PORT), 180)
                                synchronized(found) { found.add(candidate) }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            tasks.forEach { try { it.get() } catch (_: Exception) {} }
            scanPool.shutdown()

            mainHandler.post {
                btnScanLocalNetwork.isEnabled = true
                btnScanLocalNetwork.text = "Scan Local Network"
                if (found.isEmpty()) {
                    addDiscoveryMessage(containerDiscoveredWifi, "No TChat hosts found on $prefix.x:$TCP_PORT")
                } else {
                    for (ip in found) {
                        addPeerRow(containerDiscoveredWifi, "TChat Peer ($ip)", ip) {
                            connectToTcpHost(ip)
                        }
                    }
                }
            }
        }
    }

    private fun toggleTcpHost() {
        if (tcpServerSocket != null) {
            try { tcpServerSocket?.close() } catch (_: Exception) {}
            tcpServerSocket = null
            btnHostTcp.text = "Host TCP"
            tvStatus.text = "Status: Disconnected"
            showBanner("TCP Host", "Stopped listening")
            return
        }

        btnHostTcp.text = "Stop TCP Host"
        val ip = getLocalIpv4Address() ?: "0.0.0.0"
        tvStatus.text = "Status: Hosting TCP on $ip:$TCP_PORT"
        showBanner("TCP Host", "Listening on $ip:$TCP_PORT")

        executor.execute {
            try {
                val server = ServerSocket(TCP_PORT)
                tcpServerSocket = server
                while (!server.isClosed) {
                    val client = server.accept()
                    val remoteIp = client.inetAddress.hostAddress ?: "Peer"
                    handleTcpSession(client, remoteIp)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun connectToTcpHost(ip: String) {
        tvStatus.text = "Status: Connecting to $ip..."
        executor.execute {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, TCP_PORT), 3500)
                handleTcpSession(socket, ip)
            } catch (e: Exception) {
                mainHandler.post {
                    tvStatus.text = "Status: Disconnected"
                    showBanner("TCP Error", "Could not connect to $ip:$TCP_PORT")
                }
            }
        }
    }

    private fun handleTcpSession(socket: Socket, remoteIp: String) {
        activeTcpSocket = socket
        activeTcpWriter = PrintWriter(socket.getOutputStream(), true)

        mainHandler.post {
            tvStatus.text = "Status: Connected to TCP $remoteIp"
            showBanner("TCP Connected", "Linked to $remoteIp")
            val existing = threads.find { it.endpoint == remoteIp } ?: ChatThread(
                id = "tcp_$remoteIp",
                title = "TCP $remoteIp",
                mode = "TCP IP",
                endpoint = remoteIp
            ).also {
                threads.add(0, it)
                renderChatsList()
            }
            openThread(existing)
        }

        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val text = line ?: continue
                appendIncomingMessage("TCP $remoteIp", text, "TCP IP", remoteIp)
            }
        } catch (_: Exception) {
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        btPermissionLauncher.launch(perms)
    }

    private fun getBtAdapter(): BluetoothAdapter? {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    @SuppressLint("MissingPermission")
    private fun toggleBluetoothHost() {
        if (btServerSocket != null) {
            try { btServerSocket?.close() } catch (_: Exception) {}
            btServerSocket = null
            btnBtServer.text = "START BT HOST"
            tvStatus.text = "Status: Disconnected"
            return
        }

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        val adapter = getBtAdapter()
        if (adapter == null || !adapter.isEnabled) {
            showBanner("Bluetooth", "Enable Bluetooth on your device first")
            return
        }

        btnBtServer.text = "STOP BT HOST"
        tvStatus.text = "Status: Bluetooth Host Listening"
        showBanner("Bluetooth", "BT Host server started")

        executor.execute {
            try {
                val server = adapter.listenUsingRfcommWithServiceRecord(BT_APP_NAME, BT_UUID)
                btServerSocket = server
                val socket = server.accept()
                handleBtSession(socket, socket.remoteDevice.name ?: socket.remoteDevice.address)
            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanBluetoothDevices() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        val adapter = getBtAdapter()
        if (adapter == null || !adapter.isEnabled) {
            showBanner("Bluetooth", "Enable Bluetooth on your device first")
            return
        }

        containerBtDevices.removeAllViews()
        val paired: Set<BluetoothDevice> = try { adapter.bondedDevices ?: emptySet() } catch (_: Exception) { emptySet() }
        if (paired.isEmpty()) {
            addDiscoveryMessage(containerBtDevices, "No paired Bluetooth devices found")
        } else {
            for (device in paired) {
                val name = device.name ?: "Unknown BT Device"
                addPeerRow(containerBtDevices, name, device.address) {
                    connectToBtDevice(device, name)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToBtDevice(device: BluetoothDevice, displayName: String) {
        tvStatus.text = "Status: Connecting BT to $displayName..."
        executor.execute {
            try {
                val socket = device.createRfcommSocketToServiceRecord(BT_UUID)
                socket.connect()
                handleBtSession(socket, displayName)
            } catch (_: Exception) {
                mainHandler.post {
                    tvStatus.text = "Status: Disconnected"
                    showBanner("Bluetooth", "Could not connect to $displayName (ensure TChat BT Host is running)")
                }
            }
        }
    }

    private fun handleBtSession(socket: BluetoothSocket, peerName: String) {
        activeBtSocket = socket
        activeBtWriter = PrintWriter(socket.outputStream, true)

        mainHandler.post {
            tvStatus.text = "Status: Connected BT ($peerName)"
            showBanner("Bluetooth", "Connected to $peerName")
            val existing = threads.find { it.title == peerName } ?: ChatThread(
                id = "bt_${System.currentTimeMillis()}",
                title = peerName,
                mode = "Bluetooth",
                endpoint = peerName
            ).also {
                threads.add(0, it)
                renderChatsList()
            }
            openThread(existing)
        }

        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val text = line ?: continue
                appendIncomingMessage(peerName, text, "Bluetooth", peerName)
            }
        } catch (_: Exception) {
        }
    }

    private fun addDiscoveryMessage(container: LinearLayout, message: String) {
        val tv = TextView(this).apply {
            text = message
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 13f
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        container.addView(tv)
    }

    private fun addPeerRow(
        container: LinearLayout,
        title: String,
        subtitle: String,
        onConnect: () -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvName = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val tvSub = TextView(this).apply {
            text = subtitle
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 12f
        }
        val btn = Button(this).apply {
            text = "Connect"
            setOnClickListener { onConnect() }
        }
        textCol.addView(tvName)
        textCol.addView(tvSub)
        row.addView(textCol)
        row.addView(btn)
        container.addView(row)
    }

    private fun updateUserDisplay() {
        tvUsername.text = username
        tvDrawerUser.text = "Logged in as: $username"
    }

    private fun saveProfilePrefs() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME, username)
            .apply()
    }

    private fun setAndSaveTheme(mode: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode)
            .apply()
        applySavedTheme(mode)
    }

    private fun applySavedTheme(mode: String) {
        when (mode) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun showBanner(sender: String, message: String) {
        tvBannerSender.text = sender
        tvBannerMessage.text = message
        cardInAppNotification.visibility = View.VISIBLE

        bannerHideRunnable?.let { mainHandler.removeCallbacks(it) }
        val hideTask = Runnable {
            cardInAppNotification.visibility = View.GONE
        }
        bannerHideRunnable = hideTask
        mainHandler.postDelayed(hideTask, 3500)
    }

    private fun currentTime(): String = timeFormat.format(Date())

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        try { tcpServerSocket?.close() } catch (_: Exception) {}
        try { activeTcpSocket?.close() } catch (_: Exception) {}
        try { btServerSocket?.close() } catch (_: Exception) {}
        try { activeBtSocket?.close() } catch (_: Exception) {}
        executor.shutdownNow()
    }
}
