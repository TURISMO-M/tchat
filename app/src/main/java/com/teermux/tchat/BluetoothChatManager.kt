package com.teermux.tchat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Manages Bluetooth RFCOMM connections for peer-to-peer messaging without internet.
 */
class BluetoothChatManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val onStatusUpdate: (String) -> Unit,
    private val onMessageReceived: (String) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

    companion object {
        private const val APP_NAME = "TChat"
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    @SuppressLint("MissingPermission")
    fun startServer() {
        stopThreads()
        onStatusUpdate("Bluetooth: Listening for incoming connections...")
        acceptThread = AcceptThread().apply { start() }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        stopThreads()
        onStatusUpdate("Bluetooth: Connecting to ${device.name ?: device.address}...")
        connectThread = ConnectThread(device).apply { start() }
    }

    fun sendMessage(message: String): Boolean {
        return connectedThread?.write(message.toByteArray(Charsets.UTF_8)) ?: false
    }

    private fun manageConnectedSocket(socket: BluetoothSocket, deviceName: String) {
        stopThreadsExceptConnected()
        onStatusUpdate("Connected via Bluetooth to $deviceName")
        connectedThread = ConnectedThread(socket).apply { start() }
    }

    fun disconnect() {
        stopThreads()
        onStatusUpdate("Bluetooth Disconnected")
    }

    private fun stopThreads() {
        acceptThread?.cancel()
        acceptThread = null
        connectThread?.cancel()
        connectThread = null
        stopThreadsExceptConnected()
    }

    private fun stopThreadsExceptConnected() {
        connectedThread?.cancel()
        connectedThread = null
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    mainHandler.post { onStatusUpdate("Bluetooth Accept Error: ${e.localizedMessage}") }
                    shouldLoop = false
                    null
                }

                socket?.let {
                    val deviceName = try { it.remoteDevice.name ?: "Peer" } catch (_: Exception) { "Peer" }
                    mainHandler.post { manageConnectedSocket(it, deviceName) }
                    try { serverSocket?.close() } catch (_: IOException) {}
                    shouldLoop = false
                }
            }
        }

        fun cancel() {
            try { serverSocket?.close() } catch (_: IOException) {}
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            try {
                socket?.let {
                    it.connect()
                    val devName = device.name ?: device.address
                    mainHandler.post { manageConnectedSocket(it, devName) }
                }
            } catch (e: IOException) {
                mainHandler.post { onStatusUpdate("Bluetooth Connection Failed") }
                try { socket?.close() } catch (_: IOException) {}
            }
        }

        fun cancel() {
            try { socket?.close() } catch (_: IOException) {}
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private val buffer = ByteArray(1024)

        override fun run() {
            while (true) {
                try {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes, Charsets.UTF_8)
                        mainHandler.post { onMessageReceived(message) }
                    }
                } catch (e: IOException) {
                    mainHandler.post { onStatusUpdate("Bluetooth Connection Lost") }
                    break
                }
            }
        }

        fun write(bytes: ByteArray): Boolean {
            return try {
                outputStream.write(bytes)
                true
            } catch (e: IOException) {
                mainHandler.post { onStatusUpdate("Failed to send Bluetooth message") }
                false
            }
        }

        fun cancel() {
            try { socket.close() } catch (_: IOException) {}
        }
    }
}
