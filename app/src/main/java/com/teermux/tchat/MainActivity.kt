package com.teermux.tchat

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private var drawerLayout: DrawerLayout? = null
    private var btnOpenDrawer: ImageView? = null
    private var btnModeMenu: ImageView? = null

    private var btnThemeSystem: Button? = null
    private var btnThemeDark: Button? = null
    private var btnThemeLight: Button? = null
    private var btnGoogleSignIn: Button? = null

    private var tvStatus: TextView? = null
    private var tvMyIp: TextView? = null
    
    private var rvChatMessages: RecyclerView? = null
    private val chatMessageList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private var etMessage: EditText? = null
    private var etTargetIp: EditText? = null
    private var etUsername: EditText? = null
    private var btnHostTcp: Button? = null
    private var btnConnectTcp: Button? = null
    private var btnSend: ImageView? = null
    private var btnAttach: ImageView? = null
    private var btnBtServer: Button? = null
    private var btnScanBt: Button? = null
    private var btnScanLocalNetwork: View? = null
    private var btnRefreshIp: ImageView? = null
    private var btnSaveProfile: Button? = null
    private var fabNewChat: FloatingActionButton? = null

    private var layoutReplyPreview: LinearLayout? = null
    private var tvReplyPreviewText: TextView? = null
    private var btnCloseReply: ImageView? = null
    private var replyingToSender: String? = null
    private var replyingToText: String? = null

    private var tvDrawerUser: TextView? = null
    private var drawerItemHome: TextView? = null
    private var drawerItemConnect: TextView? = null
    private var drawerItemSettings: TextView? = null
    private var btnDrawerLogout: Button? = null

    private var containerGlobalChats: LinearLayout? = null

    private var navHomeBtn: View? = null
    private var navConnectionsBtn: View? = null
    private var navSettingsBtn: View? = null

    private var panelBluetooth: CardView? = null
    private var panelIpConnection: CardView? = null

    private var layoutHomeTab: View? = null
    private var layoutConnectionsTab: View? = null
    private var layoutChatTab: View? = null
    private var layoutSettingsTab: View? = null

    private var headerExpandWifi: View? = null
    private var headerExpandBt: View? = null
    private var arrowWifiExpand: ImageView? = null
    private var arrowBtExpand: ImageView? = null
    private var containerDiscoveredWifi: LinearLayout? = null
    private var containerBtContent: LinearLayout? = null
    private var containerBtDevices: LinearLayout? = null
    private var tvWifiDiscoveryStatus: TextView? = null

    private var cardInAppNotification: CardView? = null
    private var tvBannerSender: TextView? = null
    private var tvBannerMessage: TextView? = null

    private lateinit var sharedPrefs: SharedPreferences
    private var username: String = "User"

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothChatManager: BluetoothChatManager? = null

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var writer: PrintWriter? = null
    private val port = 8888
    private var isConnected = false
    private var isOnlineMode = false

    private var firestore: FirebaseFirestore? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var firestoreListener: ListenerRegistration? = null

    private var currentChatId: String? = null
    private var currentChatName: String? = null
    private var currentChatCode: String? = null
    private var currentChatOwner: String? = null
    private var chatMessagesListener: ListenerRegistration? = null
    private var groupsListener: ListenerRegistration? = null
    private var isInitialMessageLoad = true

    private var isAppInForeground = false
    private var activeTabIndex = 0 

    private var temporaryLocalChatName: String? = null

    private val serviceType = "_tchat._tcp."
    private var serviceName = "TChat_" + Build.MODEL.replace(" ", "_")
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var isWifiExpanded = true
    private var isBtExpanded = true

    private val NOTIFICATION_CHANNEL_ID = "tchat_messages_channel"

    private val attachmentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedAttachment(it) }
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth turned on!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth enablement cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPrefs = getSharedPreferences("TChatPrefs", Context.MODE_PRIVATE)

        try {
            val themeMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppCompatDelegate.setDefaultNightMode(themeMode)
        } catch (e: Exception) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            FirebaseApp.initializeApp(this)
            firebaseAuth = FirebaseAuth.getInstance()
        } catch (_: Exception) {}

        initViews()
        setupRecyclerView()
        createNotificationChannel()
        setupBackPressedDispatcher()

        btnOpenDrawer?.setOnClickListener { drawerLayout?.openDrawer(GravityCompat.START) }
        btnModeMenu?.setOnClickListener { view -> showModePopupMenu(view) }

        val rootView = findViewById<View>(R.id.drawerLayout)
        rootView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { view, insets ->
                val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                val navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                view.setPadding(0, statusBarHeight, 0, navigationBarHeight)
                insets
            }
        }

        try {
            val authUser = firebaseAuth?.currentUser
            username = authUser?.displayName ?: sharedPrefs.getString("username", "User_" + (1000..9999).random()) ?: "User"
            currentChatId = sharedPrefs.getString("current_chat_id", null)
            currentChatName = sharedPrefs.getString("current_chat_name", null)
            currentChatCode = sharedPrefs.getString("current_chat_code", null)
            currentChatOwner = sharedPrefs.getString("current_chat_owner", null)
        } catch (e: Exception) {
            username = "User"
        }
        
        serviceName = "TChat_$username"
        tvDrawerUser?.text = "Logged in as: $username"

        requestAppPermissions()
        setupBottomNavigation()
        setupDrawerNavigation()
        setupExpandableSections()
        setupNetworkAutoRefresh()
        displayLocalIpAddress()

        etUsername?.setText(username)

        btnThemeSystem?.setOnClickListener { applyTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        btnThemeDark?.setOnClickListener { applyTheme(AppCompatDelegate.MODE_NIGHT_YES) }
        btnThemeLight?.setOnClickListener { applyTheme(AppCompatDelegate.MODE_NIGHT_NO) }
        btnGoogleSignIn?.setOnClickListener { performGoogleSignIn() }
        fabNewChat?.setOnClickListener { showNewChatOptionsDialog() }
        btnAttach?.setOnClickListener { attachmentLauncher.launch("*/*") }
        btnCloseReply?.setOnClickListener { clearReplyPreview() }

        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("TChatMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {}

        nsdManager = getSystemService(Context.NSD_SERVICE) as? NsdManager
        try {
            registerNsdService()
        } catch (_: Exception) {}

        try {
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = btManager?.adapter
            if (bluetoothAdapter != null) {
                bluetoothChatManager = BluetoothChatManager(
                    bluetoothAdapter,
                    onStatusUpdate = { status ->
                        runOnUiThread {
                            val nowConnected = status.contains("Connected")
                            isConnected = nowConnected
                            updateStatus(status)
                            if (nowConnected) {
                                temporaryLocalChatName = "Bluetooth P2P Chat"
                                currentChatId = null 
                                switchToChatTab()
                                loadChats()
                            } else if (temporaryLocalChatName != null) {
                                temporaryLocalChatName = null
                                loadChats()
                            }
                        }
                    },
                    onMessageReceived = { rawMsg ->
                        handleIncomingP2pMessage("Bluetooth Direct", rawMsg)
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        btnHostTcp?.setOnClickListener { startTcpServer() }
        btnConnectTcp?.setOnClickListener {
            val ip = etTargetIp?.text.toString().trim()
            if (ip.isNotEmpty()) connectToTcpServer(ip)
            else Toast.makeText(this, "Enter host IP", Toast.LENGTH_SHORT).show()
        }
        btnSend?.setOnClickListener { sendMessage() }
        btnBtServer?.setOnClickListener {
            if (ensureBluetoothEnabled()) {
                bluetoothChatManager?.startServer()
            }
        }
        btnScanBt?.setOnClickListener {
            if (ensureBluetoothEnabled()) {
                scanBluetoothDevices()
            }
        }
        btnScanLocalNetwork?.setOnClickListener { discoverNsdServices() }
        btnRefreshIp?.setOnClickListener { displayLocalIpAddress() }
        btnSaveProfile?.setOnClickListener { saveProfile() }

        if (currentChatId != null && temporaryLocalChatName == null) {
            startChatMessagesListener()
        }
        loadChats()
    }

    private fun setupRecyclerView() {
        val existingView = findViewById<View>(R.id.rvMessages)
        if (existingView != null && rvChatMessages == null) {
            if (existingView is RecyclerView) {
                rvChatMessages = existingView
            } else {
                val parent = existingView.parent as? LinearLayout
                if (parent != null) {
                    val index = parent.indexOfChild(existingView)
                    parent.removeView(existingView)
                    val rv = RecyclerView(this).apply {
                        id = R.id.rvMessages
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            0,
                            1.0f
                        )
                    }
                    parent.addView(rv, index)
                    rvChatMessages = rv
                }
            }
        } else if (rvChatMessages == null) {
            rvChatMessages = findViewById(R.id.rvMessages)
        }
        
        chatAdapter = ChatAdapter(chatMessageList)
        rvChatMessages?.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChatMessages?.adapter = chatAdapter
    }

    private fun setupBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
                    drawerLayout?.closeDrawer(GravityCompat.START)
                } else if (layoutChatTab?.visibility == View.VISIBLE) {
                    updateActivePill(navHomeBtn)
                    animateTabChange(layoutHomeTab, targetIndex = 0)
                } else if (activeTabIndex != 0) {
                    updateActivePill(navHomeBtn)
                    animateTabChange(layoutHomeTab, targetIndex = 0)
                } else {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
    }

    private fun handleIncomingNotification(sender: String, messageText: String) {
        if (isAppInForeground) {
            if (layoutChatTab?.visibility != View.VISIBLE) {
                showInAppBanner(sender, messageText)
            }
        } else {
            sendSystemNotification(sender, messageText)
        }
    }

    private fun showInAppBanner(sender: String, message: String) {
        runOnUiThread {
            tvBannerSender?.text = sender
            tvBannerMessage?.text = message

            val banner = cardInAppNotification ?: return@runOnUiThread
            val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
            val slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)

            banner.visibility = View.VISIBLE
            banner.startAnimation(slideIn)

            Handler(Looper.getMainLooper()).postDelayed({
                banner.startAnimation(slideOut)
                banner.visibility = View.GONE
            }, 2000)
        }
    }

    private fun performGoogleSignIn() {
        val credentialManager = CredentialManager.create(this)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com")
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        thread {
            try {
                val executor = ContextCompat.getMainExecutor(this@MainActivity)
                credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    executor,
                    object : androidx.credentials.CredentialManagerCallback<androidx.credentials.GetCredentialResponse, GetCredentialException> {
                        override fun onResult(result: androidx.credentials.GetCredentialResponse) {
                            try {
                                val credential = result.credential
                                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                                val firebaseCredential = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                                firebaseAuth?.signInWithCredential(firebaseCredential)
                                    ?.addOnSuccessListener { authResult ->
                                        val name = authResult.user?.displayName ?: "Google User"
                                        username = name
                                        etUsername?.setText(username)
                                        tvDrawerUser?.text = "Logged in as: $username"
                                        sharedPrefs.edit().putString("username", username).apply()
                                        Toast.makeText(this@MainActivity, "Signed in as $username", Toast.LENGTH_SHORT).show()
                                    }
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Sign-in parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onError(e: GetCredentialException) {
                            Toast.makeText(this@MainActivity, "Google Sign-In prompt unavailable: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error initializing Sign-In: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        navHomeBtn?.setOnClickListener {
            updateActivePill(navHomeBtn)
            animateTabChange(layoutHomeTab, targetIndex = 0)
            btnModeMenu?.visibility = View.GONE
        }
        navConnectionsBtn?.setOnClickListener {
            updateActivePill(navConnectionsBtn)
            animateTabChange(layoutConnectionsTab, targetIndex = 1)
            btnModeMenu?.visibility = View.VISIBLE
        }
        navSettingsBtn?.setOnClickListener {
            updateActivePill(navSettingsBtn)
            animateTabChange(layoutSettingsTab, targetIndex = 2)
            btnModeMenu?.visibility = View.GONE
        }
        updateActivePill(navHomeBtn)
    }

    private fun updateActivePill(activeView: View?) {
        navHomeBtn?.setBackgroundResource(if (activeView == navHomeBtn) R.drawable.bg_nav_active_pill else 0)
        navConnectionsBtn?.setBackgroundResource(if (activeView == navConnectionsBtn) R.drawable.bg_nav_active_pill else 0)
        navSettingsBtn?.setBackgroundResource(if (activeView == navSettingsBtn) R.drawable.bg_nav_active_pill else 0)
    }

    private fun handleSelectedAttachment(uri: Uri) {
        val fileName = uri.lastPathSegment ?: "file"
        etMessage?.append("[Attachment: $fileName] ")
        Toast.makeText(this, "Attachment added: $fileName", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "TChat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming chat notifications"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun sendSystemNotification(sender: String, messageText: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle(sender)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun setupDrawerNavigation() {
        drawerItemHome?.setOnClickListener {
            updateActivePill(navHomeBtn)
            animateTabChange(layoutHomeTab, targetIndex = 0)
            drawerLayout?.closeDrawer(GravityCompat.START)
        }
        drawerItemConnect?.setOnClickListener {
            updateActivePill(navConnectionsBtn)
            animateTabChange(layoutConnectionsTab, targetIndex = 1)
            drawerLayout?.closeDrawer(GravityCompat.START)
        }
        drawerItemSettings?.setOnClickListener {
            updateActivePill(navSettingsBtn)
            animateTabChange(layoutSettingsTab, targetIndex = 2)
            drawerLayout?.closeDrawer(GravityCompat.START)
        }
        btnDrawerLogout?.setOnClickListener {
            firebaseAuth?.signOut()
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
            drawerLayout?.closeDrawer(GravityCompat.START)
        }
    }

    private fun showNewChatOptionsDialog() {
        val options = arrayOf("Create New Space", "Join Space via Code", "Start Direct Message")
        AlertDialog.Builder(this)
            .setTitle("New Conversation")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> promptCreateChat()
                    1 -> promptJoinChat()
                    2 -> promptDirectMessage()
                }
            }
            .show()
    }

    private fun promptDirectMessage() {
        val input = EditText(this).apply {
            hint = "Recipient Username"
            textSize = 16f
        }
        AlertDialog.Builder(this)
            .setTitle("Direct Message")
            .setMessage("Enter the exact username of your friend.")
            .setView(input)
            .setPositiveButton("Start Chat") { _, _ ->
                val targetUser = input.text.toString().trim()
                if (targetUser.isNotEmpty()) {
                    val dmId = if (username < targetUser) "dm_${username}_$targetUser" else "dm_${targetUser}_$username"
                    createOrOpenDm(dmId, targetUser)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createOrOpenDm(dmId: String, targetUser: String) {
        val db = ensureFirestore() ?: return
        val data = hashMapOf(
            "name" to "@$targetUser",
            "type" to "DM",
            "createdAt" to System.currentTimeMillis(),
            "members" to listOf(username, targetUser)
        )
        db.collection("groups").document(dmId).set(data)
            .addOnSuccessListener {
                openChat(dmId, "@$targetUser", "DM", username)
                loadChats()
            }
    }

    private fun setupExpandableSections() {
        arrowWifiExpand?.setOnClickListener {
            isWifiExpanded = !isWifiExpanded
            containerDiscoveredWifi?.visibility = if (isWifiExpanded) View.VISIBLE else View.GONE
            arrowWifiExpand?.animate()?.rotation(if (isWifiExpanded) 0f else 180f)?.setDuration(200)?.start()
        }

        arrowBtExpand?.setOnClickListener {
            isBtExpanded = !isBtExpanded
            containerBtDevices?.visibility = if (isBtExpanded) View.VISIBLE else View.GONE
            arrowBtExpand?.animate()?.rotation(if (isBtExpanded) 0f else 180f)?.setDuration(200)?.start()
        }
    }

    private fun ensureBluetoothEnabled(): Boolean {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!bluetoothAdapter!!.isEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 102)
                return false
            }
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
            return false
        }
        return true
    }

    private fun applyTheme(mode: Int) {
        sharedPrefs.edit().putInt("theme_mode", mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
        drawerLayout?.closeDrawer(GravityCompat.START)
    }

    private fun showModePopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(Menu.NONE, 1, 1, "Bluetooth Mode")
        popup.menu.add(Menu.NONE, 2, 2, "Local Router Mode")
        popup.menu.add(Menu.NONE, 3, 3, "AP Hotspot Mode")
        popup.menu.add(Menu.NONE, 4, 4, "Online Cloud Mode")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    isOnlineMode = false
                    panelBluetooth?.visibility = View.VISIBLE
                    panelIpConnection?.visibility = View.GONE
                    Toast.makeText(this, "Switched to Bluetooth Mode", Toast.LENGTH_SHORT).show()
                    true
                }
                2 -> {
                    isOnlineMode = false
                    panelBluetooth?.visibility = View.GONE
                    panelIpConnection?.visibility = View.VISIBLE
                    discoverNsdServices()
                    Toast.makeText(this, "Switched to Local Router Mode", Toast.LENGTH_SHORT).show()
                    true
                }
                3 -> {
                    isOnlineMode = false
                    panelBluetooth?.visibility = View.GONE
                    panelIpConnection?.visibility = View.VISIBLE
                    val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        @Suppress("DEPRECATION")
                        wifiManager?.isWifiEnabled = false
                    }
                    displayLocalIpAddress()
                    Toast.makeText(this, "Switched to AP Hotspot Mode (Wi-Fi Disabled)", Toast.LENGTH_SHORT).show()
                    true
                }
                4 -> {
                    enableOnlineCloudChat()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        btnOpenDrawer = findViewById(R.id.btnOpenDrawer)
        btnModeMenu = findViewById(R.id.btnModeMenu)

        btnThemeSystem = findViewById(R.id.btnThemeSystem)
        btnThemeDark = findViewById(R.id.btnThemeDark)
        btnThemeLight = findViewById(R.id.btnThemeLight)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        fabNewChat = findViewById(R.id.fabNewChat)
        containerGlobalChats = findViewById(R.id.rvGlobalChats)

        layoutReplyPreview = findViewById(R.id.layoutReplyPreview)
        tvReplyPreviewText = findViewById(R.id.tvReplyPreviewText)
        btnCloseReply = findViewById(R.id.btnCloseReply)

        tvDrawerUser = findViewById(R.id.tvDrawerUser)
        drawerItemHome = findViewById(R.id.drawerItemHome)
        drawerItemConnect = findViewById(R.id.drawerItemConnect)
        drawerItemSettings = findViewById(R.id.drawerItemSettings)
        btnDrawerLogout = findViewById(R.id.btnDrawerLogout)

        tvStatus = findViewById(R.id.tvStatus)
        tvMyIp = findViewById(R.id.tvMyIp)
        etMessage = findViewById(R.id.etMessage)
        etTargetIp = findViewById(R.id.etTargetIp)
        etUsername = findViewById(R.id.etUsername)
        btnHostTcp = findViewById(R.id.btnHostTcp)
        btnConnectTcp = findViewById(R.id.btnConnectTcp)
        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)
        btnBtServer = findViewById(R.id.btnBtServer)
        btnScanBt = findViewById(R.id.btnScanBt)
        btnScanLocalNetwork = findViewById(R.id.btnScanLocalNetwork)
        btnRefreshIp = findViewById(R.id.btnRefreshIp)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        navHomeBtn = findViewById(R.id.navHomeBtn)
        navConnectionsBtn = findViewById(R.id.navConnectionsBtn)
        navSettingsBtn = findViewById(R.id.navSettingsBtn)

        panelBluetooth = findViewById(R.id.panelBluetooth)
        panelIpConnection = findViewById(R.id.panelIpConnection)

        layoutHomeTab = findViewById(R.id.layoutHomeTab)
        layoutConnectionsTab = findViewById(R.id.layoutConnectionsTab)
        layoutChatTab = findViewById(R.id.layoutChatTab)
        layoutSettingsTab = findViewById(R.id.layoutSettingsTab)

        headerExpandWifi = findViewById(R.id.headerExpandWifi)
        headerExpandBt = findViewById(R.id.headerExpandBt)
        arrowWifiExpand = findViewById(R.id.arrowWifiExpand)
        arrowBtExpand = findViewById(R.id.arrowBtExpand)
        containerDiscoveredWifi = findViewById(R.id.containerDiscoveredWifi)
        containerBtContent = findViewById(R.id.containerBtContent)
        containerBtDevices = findViewById(R.id.containerBtDevices)
        tvWifiDiscoveryStatus = findViewById(R.id.tvWifiDiscoveryStatus)

        cardInAppNotification = findViewById(R.id.cardInAppNotification)
        tvBannerSender = findViewById(R.id.tvBannerSender)
        tvBannerMessage = findViewById(R.id.tvBannerMessage)
    }

    private fun enableOnlineCloudChat() {
        isOnlineMode = true
        temporaryLocalChatName = null
        updateStatus("Connected to Online Cloud 🌐")
        Toast.makeText(this, "Switched to Online Cloud Mode!", Toast.LENGTH_SHORT).show()
        animateTabChange(layoutHomeTab, targetIndex = 0)

        try {
            firestore = FirebaseFirestore.getInstance()
            stopChatMessagesListener()
            firestoreListener?.remove()

            firestoreListener = firestore?.collection("global_chat")
                ?.orderBy("timestamp", Query.Direction.ASCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(this, "Firestore Error: ${error.message}", Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        chatMessageList.clear()
                        for (doc in snapshot.documents) {
                            val user = doc.getString("username") ?: "Unknown"
                            val text = doc.getString("message") ?: ""
                            val rSender = doc.getString("replyToSender")
                            val rText = doc.getString("replyToText")
                            val timeMs = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
                            
                            chatMessageList.add(
                                ChatMessage(
                                    messageId = doc.id,
                                    senderName = user,
                                    text = text,
                                    timestamp = timeStr,
                                    replyToSender = rSender,
                                    replyToText = rText
                                )
                            )
                        }
                        chatAdapter.notifyDataSetChanged()
                        if (chatMessageList.isNotEmpty()) {
                            rvChatMessages?.scrollToPosition(chatMessageList.size - 1)
                        }
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Cloud Init Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun ensureFirestore(): FirebaseFirestore? {
        try {
            if (firestore == null) firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Firebase init error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            return null
        }
        return firestore
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun promptCreateChat() {
        if (ensureFirestore() == null) return
        val input = EditText(this).apply {
            hint = "Space or Chat Name"
            textSize = 16f
        }
        AlertDialog.Builder(this)
            .setTitle("Create a Space")
            .setMessage("Give it a name, then share the invite code to add members.")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Enter a chat name", Toast.LENGTH_SHORT).show()
                } else {
                    createChat(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createChat(name: String) {
        val db = ensureFirestore() ?: return
        val inviteCode = generateInviteCode()
        val data = hashMapOf(
            "name" to name,
            "inviteCode" to inviteCode,
            "createdBy" to username,
            "type" to "GROUP",
            "createdAt" to System.currentTimeMillis(),
            "members" to listOf(username)
        )
        db.collection("groups").add(data)
            .addOnSuccessListener { docRef ->
                temporaryLocalChatName = null
                currentChatId = docRef.id
                currentChatName = name
                currentChatCode = inviteCode
                currentChatOwner = username
                sharedPrefs.edit()
                    .putString("current_chat_id", docRef.id)
                    .putString("current_chat_name", name)
                    .putString("current_chat_code", inviteCode)
                    .putString("current_chat_owner", username)
                    .apply()
                startChatMessagesListener()
                loadChats()
                updateStatus("In space: $name")
                Toast.makeText(this, "Space \"$name\" created!", Toast.LENGTH_SHORT).show()
                switchToChatTab()
                showInviteDialog(name, inviteCode)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to create space: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showInviteDialog(name: String, code: String) {
        AlertDialog.Builder(this)
            .setTitle("Invite Friends to $name")
            .setMessage("Invite code: $code\n\nShare it so your friends can join this space.")
            .setPositiveButton("Share") { _, _ ->
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Join my TChat space \"$name\" with invite code: $code")
                }
                startActivity(Intent.createChooser(share, "Invite friends to $name"))
            }
            .setNeutralButton("Copy Code") { _, _ ->
                (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { cm ->
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("TChat invite", code))
                    Toast.makeText(this, "Code copied: $code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Done", null)
            .show()
    }

    private fun promptJoinChat() {
        if (ensureFirestore() == null) return
        val input = EditText(this).apply {
            hint = "Invite code (e.g. ABC123)"
            textSize = 16f
        }
        AlertDialog.Builder(this)
            .setTitle("Join a Space")
            .setMessage("Enter the invite code your friend shared.")
            .setView(input)
            .setPositiveButton("Join") { _, _ ->
                val code = input.text.toString().trim().uppercase()
                if (code.isEmpty()) {
                    Toast.makeText(this, "Enter an invite code", Toast.LENGTH_SHORT).show()
                } else {
                    joinChatByCode(code)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun joinChatByCode(code: String) {
        val db = ensureFirestore() ?: return
        db.collection("groups")
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    Toast.makeText(this, "No space found with code \"$code\"", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val doc = snap.documents[0]
                val name = doc.getString("name") ?: "Chat"
                val owner = doc.getString("createdBy") ?: ""
                doc.reference.update("members", FieldValue.arrayUnion(username))
                    .addOnSuccessListener {
                        openChat(doc.id, name, code, owner)
                        Toast.makeText(this, "Joined \"$name\"!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to join: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Join error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openChat(id: String, name: String, code: String, owner: String) {
        temporaryLocalChatName = null
        currentChatId = id
        currentChatName = name
        currentChatCode = code
        currentChatOwner = owner
        sharedPrefs.edit()
            .putString("current_chat_id", id)
            .putString("current_chat_name", name)
            .putString("current_chat_code", code)
            .putString("current_chat_owner", owner)
            .apply()
        startChatMessagesListener()
        updateStatus("In chat: $name")
        switchToChatTab()
    }

    private fun startChatMessagesListener() {
        stopChatMessagesListener()
        val id = currentChatId ?: return
        val db = ensureFirestore() ?: return
        firestoreListener?.remove()
        firestoreListener = null
        chatMessageList.clear()
        chatAdapter.notifyDataSetChanged()
        isInitialMessageLoad = true

        chatMessagesListener = db.collection("groups")
            .document(id)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Toast.makeText(this, "Chat Error: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                if (snap != null) {
                    chatMessageList.clear()
                    for (doc in snap.documents) {
                        val user = doc.getString("username") ?: "Unknown"
                        val text = doc.getString("message") ?: ""
                        val rSender = doc.getString("replyToSender")
                        val rText = doc.getString("replyToText")
                        val timeMs = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))

                        chatMessageList.add(
                            ChatMessage(
                                messageId = doc.id,
                                senderName = user,
                                text = text,
                                timestamp = timeStr,
                                replyToSender = rSender,
                                replyToText = rText
                            )
                        )
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (chatMessageList.isNotEmpty()) {
                        rvChatMessages?.scrollToPosition(chatMessageList.size - 1)
                    }

                    if (!isInitialMessageLoad && snap.documentChanges.any { it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED }) {
                        val lastDoc = snap.documents.lastOrNull()
                        val author = lastDoc?.getString("username") ?: "Someone"
                        val msgText = lastDoc?.getString("message") ?: ""
                        if (author != username) {
                            handleIncomingNotification(author, msgText)
                        }
                    }
                    isInitialMessageLoad = false
                }
            }
    }

    private fun stopChatMessagesListener() {
        chatMessagesListener?.remove()
        chatMessagesListener = null
    }

    private fun loadChats() {
        runOnUiThread {
            val db = ensureFirestore() ?: return@runOnUiThread
            groupsListener?.remove()
            containerGlobalChats?.removeAllViews()

            val inflater = LayoutInflater.from(this)

            temporaryLocalChatName?.let { localName ->
                val tempView = inflater.inflate(R.layout.item_chat_space, containerGlobalChats, false)
                val tvAvatar = tempView.findViewById<TextView>(R.id.tvAvatarIcon)
                val tvName = tempView.findViewById<TextView>(R.id.tvSpaceName)
                val tvDetails = tempView.findViewById<TextView>(R.id.tvSpaceDetails)

                tvAvatar.text = "⚡"
                tvName.text = localName
                tvDetails.text = "Temporary Direct Connection"

                tempView.setOnClickListener { 
                    currentChatId = null 
                    switchToChatTab() 
                }
                containerGlobalChats?.addView(tempView)
            }

            groupsListener = db.collection("groups")
                .whereArrayContains("members", username)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Toast.makeText(this, "Chats Error: ${error.message}", Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }
                    if (snap == null) return@addSnapshotListener

                    val viewCount = containerGlobalChats?.childCount ?: 0
                    if (viewCount > 0 && temporaryLocalChatName != null) {
                        containerGlobalChats?.removeViews(1, viewCount - 1)
                    } else {
                        containerGlobalChats?.removeAllViews()
                    }

                    if (snap.isEmpty && temporaryLocalChatName == null) {
                        val empty = TextView(this).apply {
                            text = "No spaces or direct messages yet.\nTap '+' below to create one."
                            setTextColor(Color.parseColor("#999999"))
                            textSize = 14f
                            setPadding(32, 48, 32, 48)
                        }
                        containerGlobalChats?.addView(empty)
                        return@addSnapshotListener
                    }

                    val docs = snap.documents.sortedByDescending { it.getLong("createdAt") ?: 0L }
                    for (doc in docs) {
                        val rawName = doc.getString("name") ?: "Unnamed Chat"
                        val code = doc.getString("inviteCode") ?: "DM"
                        val owner = doc.getString("createdBy") ?: ""
                        val type = doc.getString("type") ?: "GROUP"
                        val membersCount = (doc.get("members") as? List<*>)?.size ?: 0

                        val displayName = if (type == "DM") {
                            val members = doc.get("members") as? List<*>
                            val other = members?.firstOrNull { it != username }?.toString() ?: rawName
                            if (other.startsWith("@")) other else "@$other"
                        } else rawName

                        val itemView = inflater.inflate(R.layout.item_chat_space, containerGlobalChats, false)
                        val tvAvatar = itemView.findViewById<TextView>(R.id.tvAvatarIcon)
                        val tvName = itemView.findViewById<TextView>(R.id.tvSpaceName)
                        val tvDetails = itemView.findViewById<TextView>(R.id.tvSpaceDetails)

                        tvAvatar.text = displayName.removePrefix("@").firstOrNull()?.toString()?.uppercase() ?: "#"
                        tvName.text = displayName
                        tvDetails.text = if (type == "DM") "Direct Message" else "Code: $code  •  $membersCount members"

                        itemView.setOnClickListener { openChat(doc.id, displayName, code, owner) }
                        itemView.setOnLongClickListener {
                            showSpaceOptionsMenu(doc.id, displayName, owner)
                            true
                        }
                        containerGlobalChats?.addView(itemView)
                    }
                }
        }
    }

    private fun showSpaceOptionsMenu(spaceId: String, spaceName: String, owner: String) {
        val options = if (owner == username) {
            arrayOf("Share Invite Code", "Delete Space (Owner)")
        } else {
            arrayOf("Share Invite Code", "Leave Space")
        }

        AlertDialog.Builder(this)
            .setTitle(spaceName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showInviteDialog(spaceName, currentChatCode ?: "")
                    1 -> {
                        if (owner == username) {
                            deleteSpace(spaceId)
                        } else {
                            leaveSpace(spaceId)
                        }
                    }
                }
            }
            .show()
    }

    private fun deleteSpace(spaceId: String) {
        ensureFirestore()?.collection("groups")
            ?.document(spaceId)
            ?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show()
                loadChats()
            }
    }

    private fun leaveSpace(spaceId: String) {
        ensureFirestore()?.collection("groups")
            ?.document(spaceId)
            ?.update("members", FieldValue.arrayRemove(username))
            ?.addOnSuccessListener {
                Toast.makeText(this, "Left conversation", Toast.LENGTH_SHORT).show()
                loadChats()
            }
    }

    private fun animateTabChange(targetView: View?, targetIndex: Int = 3) {
        val views = listOf(layoutHomeTab, layoutConnectionsTab, layoutChatTab, layoutSettingsTab)
        val isSlideRight = targetIndex > activeTabIndex

        views.forEach { it?.visibility = View.GONE }
        targetView?.visibility = View.VISIBLE

        val animSet = AnimationSet(true).apply {
            addAnimation(AlphaAnimation(0.2f, 1.0f).apply { duration = 220 })
            val fromX = if (isSlideRight) 80f else -80f
            addAnimation(TranslateAnimation(fromX, 0f, 0f, 0f).apply { duration = 220 })
        }
        targetView?.startAnimation(animSet)
        activeTabIndex = targetIndex
    }

    private fun switchToChatTab() {
        runOnUiThread { animateTabChange(layoutChatTab, targetIndex = 3) }
    }

    private fun saveProfile() {
        val newName = etUsername?.text.toString().trim()
        if (newName.isNotEmpty()) {
            username = newName
            tvDrawerUser?.text = "Logged in as: $username"
            sharedPrefs.edit().putString("username", username).apply()
            loadChats()
            Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNetworkAutoRefresh() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runOnUiThread { displayLocalIpAddress() }
                }
                override fun onLost(network: Network) {
                    runOnUiThread { displayLocalIpAddress() }
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (_: Exception) {}
    }

    private fun displayLocalIpAddress() {
        val ip = getDeviceLocalIp()
        tvMyIp?.text = "Your IP: $ip"
    }

    private fun getDeviceLocalIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun registerNsdService() {
        val info = NsdServiceInfo().apply {
            serviceName = this@MainActivity.serviceName
            serviceType = this@MainActivity.serviceType
            port = this@MainActivity.port
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(sInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(sInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(sInfo: NsdServiceInfo) {}
            override fun onUnregistrationFailed(sInfo: NsdServiceInfo, errorCode: Int) {}
        }
        try {
            nsdManager?.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (_: Exception) {}
    }

    private fun discoverNsdServices() {
        containerDiscoveredWifi?.removeAllViews()
        tvWifiDiscoveryStatus?.text = "Discovered Local Devices:"

        scanSubnetRange()

        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("tchat") && service.serviceName != serviceName) {
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val hostIp = serviceInfo.host?.hostAddress ?: return
                            val name = serviceInfo.serviceName.replace("TChat_", "")
                            runOnUiThread { addDiscoveredWifiDevice(name, hostIp) }
                        }
                    })
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        try {
            nsdManager?.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (_: Exception) {}
    }

    private fun scanSubnetRange() {
        thread {
            val myIp = getDeviceLocalIp()
            if (myIp == "127.0.0.1") return@thread
            val subnetPrefix = myIp.substringBeforeLast(".") + "."

            for (i in 1..254) {
                val testIp = "$subnetPrefix$i"
                if (testIp == myIp) continue

                thread {
                    try {
                        val address = InetAddress.getByName(testIp)
                        if (address.isReachable(300)) {
                            runOnUiThread { addDiscoveredWifiDevice("Network Host", testIp) }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun addDiscoveredWifiDevice(name: String, ip: String) {
        val btn = Button(this).apply {
            text = "Connect to $name ($ip)"
            textSize = 12f
            setOnClickListener { connectToTcpServer(ip) }
        }
        containerDiscoveredWifi?.addView(btn)
    }

    private fun scanBluetoothDevices() {
        containerBtDevices?.removeAllViews()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth Connect permission required", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val paired = bluetoothAdapter?.bondedDevices
            if (!paired.isNullOrEmpty()) {
                val title = TextView(this).apply {
                    text = "Paired Devices:"
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                }
                containerBtDevices?.addView(title)
                for (device in paired) {
                    val btn = Button(this).apply {
                        text = "${device.name ?: "Unknown"} (${device.address})"
                        textSize = 12f
                        setOnClickListener {
                            Toast.makeText(this@MainActivity, "Connecting to ${device.name}...", Toast.LENGTH_SHORT).show()
                            bluetoothChatManager?.connectToDevice(device)
                        }
                    }
                    containerBtDevices?.addView(btn)
                }
            }

            if (bluetoothAdapter?.isDiscovering == true) bluetoothAdapter?.cancelDiscovery()
            bluetoothAdapter?.startDiscovery()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        try {
            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (BluetoothDevice.ACTION_FOUND == intent?.action) {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
                        }
                        device?.let { btDev ->
                            val btn = Button(this@MainActivity).apply {
                                text = "Found: ${btDev.name ?: "Unknown"} (${btDev.address})"
                                textSize = 12f
                                setOnClickListener {
                                    Toast.makeText(this@MainActivity, "Connecting to ${btDev.name}...", Toast.LENGTH_SHORT).show()
                                    bluetoothChatManager?.connectToDevice(btDev)
                                }
                            }
                            containerBtDevices?.addView(btn)
                        }
                    }
                }
            }, filter)
        } catch (_: Exception) {}
    }

    private fun startTcpServer() {
        thread {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(port)
                updateStatus("Hosting... Waiting on port $port")
                val client = serverSocket!!.accept()
                isConnected = true
                temporaryLocalChatName = "Local Network TCP Host"
                currentChatId = null 
                setupSocketConnection(client, "Connected to Client")
            } catch (e: Exception) {
                isConnected = false
                temporaryLocalChatName = null
                updateStatus("Server Error: ${e.message}")
            }
        }
    }

    private fun connectToTcpServer(hostIp: String) {
        thread {
            try {
                updateStatus("Connecting to $hostIp:$port...")
                val socket = Socket(hostIp, port)
                isConnected = true
                temporaryLocalChatName = "TCP Direct Chat ($hostIp)"
                currentChatId = null 
                setupSocketConnection(socket, "Connected to Host")
            } catch (e: Exception) {
                isConnected = false
                temporaryLocalChatName = null
                updateStatus("Connection Failed: ${e.message}")
            }
        }
    }

    private fun setupSocketConnection(socket: Socket, statusMessage: String) {
        activeSocket = socket
        writer = PrintWriter(socket.getOutputStream(), true)
        updateStatus(statusMessage)
        switchToChatTab()
        runOnUiThread { loadChats() }

        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        try {
            var incomingText: String?
            while (reader.readLine().also { incomingText = it } != null) {
                incomingText?.let { handleIncomingP2pMessage("P2P Client", it) }
            }
        } catch (_: Exception) {
            isConnected = false
            temporaryLocalChatName = null
            updateStatus("Disconnected")
            runOnUiThread { loadChats() }
        }
    }

    private fun handleIncomingP2pMessage(defaultSender: String, rawText: String) {
        val parts = rawText.split("||", limit = 3)
        val sender = if (parts.size >= 2) parts[0] else defaultSender
        val text = if (parts.size >= 2) parts[1] else rawText
        val replyInfo = if (parts.size == 3) parts[2] else null
        
        var rSender: String? = null
        var rText: String? = null
        if (replyInfo != null && replyInfo.contains("::")) {
            val rParts = replyInfo.split("::", limit = 2)
            rSender = rParts[0]
            rText = rParts[1]
        }

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val msg = ChatMessage(
            senderName = sender,
            text = text,
            timestamp = timeStr,
            replyToSender = rSender,
            replyToText = rText,
            isLocalOnly = true
        )

        runOnUiThread {
            chatMessageList.add(msg)
            chatAdapter.notifyItemInserted(chatMessageList.size - 1)
            if (chatMessageList.isNotEmpty()) {
                rvChatMessages?.scrollToPosition(chatMessageList.size - 1)
            }
            handleIncomingNotification(sender, text)
        }
    }

    private fun sendMessage() {
        val msgText = etMessage?.text.toString().trim()
        if (msgText.isEmpty()) return

        val rSender = replyingToSender
        val rText = replyingToText
        clearReplyPreview()

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        if (temporaryLocalChatName != null || (!isOnlineMode && currentChatId == null)) {
            val p2pMessagePayload = if (rSender != null && rText != null) {
                "$username||$msgText||$rSender::$rText"
            } else {
                "$username||$msgText"
            }

            val sentByBt = bluetoothChatManager?.sendMessage(p2pMessagePayload) == true
            val sentByTcp = writer != null && isConnected

            if (sentByTcp) {
                thread { writer?.println(p2pMessagePayload) }
            }

            val localMsg = ChatMessage(
                senderName = username,
                text = msgText,
                timestamp = timeStr,
                replyToSender = rSender,
                replyToText = rText,
                isLocalOnly = true
            )
            chatMessageList.add(localMsg)
            chatAdapter.notifyItemInserted(chatMessageList.size - 1)
            if (chatMessageList.isNotEmpty()) {
                rvChatMessages?.scrollToPosition(chatMessageList.size - 1)
            }
            etMessage?.setText("")

        } else {
            val activeChatId = currentChatId
            val msgData = hashMapOf<String, Any>(
                "username" to username,
                "message" to msgText,
                "timestamp" to System.currentTimeMillis()
            )
            if (rSender != null && rText != null) {
                msgData["replyToSender"] = rSender
                msgData["replyToText"] = rText
            }

            if (activeChatId != null) {
                ensureFirestore()?.collection("groups")
                    ?.document(activeChatId)
                    ?.collection("messages")
                    ?.add(msgData)
                    ?.addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else if (isOnlineMode) {
                firestore?.collection("global_chat")
                    ?.add(msgData)
                    ?.addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            etMessage?.setText("")
        }
    }

    private fun clearReplyPreview() {
        replyingToSender = null
        replyingToText = null
        layoutReplyPreview?.visibility = View.GONE
    }

    private fun updateStatus(text: String) {
        runOnUiThread {
            tvStatus?.text = "Status: $text"
            if (isConnected || text.contains("Connected")) {
                tvStatus?.setBackgroundResource(R.drawable.bg_status_connected)
                tvStatus?.setTextColor(Color.WHITE)
            } else {
                tvStatus?.setBackgroundResource(R.drawable.bg_status_disconnected)
                tvStatus?.setTextColor(Color.parseColor("#CCCCCC"))
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            multicastLock?.release()
            firestoreListener?.remove()
            stopChatMessagesListener()
            groupsListener?.remove()
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            nsdManager?.unregisterService(registrationListener)
            nsdManager?.stopServiceDiscovery(discoveryListener)
            bluetoothChatManager?.disconnect()
            activeSocket?.close()
            serverSocket?.close()
        } catch (_: Exception) {}
    }
}
