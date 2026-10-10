package com.teermux.tchat

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class InternetP2PManager(
    private val port: Int = 8888,
    private val onStatusUpdate: (String) -> Unit,
    private val onMessageReceived: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writer: PrintWriter? = null

    fun startInternetHost() {
        thread {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(port)
                onStatusUpdate("Waiting for Internet connection on port $port...")
                
                val socket = serverSocket!!.accept()
                setupConnection(socket, "Peer connected over Internet")
            } catch (e: Exception) {
                onStatusUpdate("Host Error: ${e.message}")
            }
        }
    }

    fun connectToInternetPeer(publicIp: String) {
        thread {
            try {
                onStatusUpdate("Connecting to $publicIp:$port...")
                val socket = Socket()
                socket.connect(InetSocketAddress(publicIp, port), 10000) // 10s timeout
                setupConnection(socket, "Connected to remote peer")
            } catch (e: Exception) {
                onStatusUpdate("Internet Connection Failed: ${e.message}")
            }
        }
    }

    private fun setupConnection(socket: Socket, statusMsg: String) {
        clientSocket = socket
        writer = PrintWriter(socket.getOutputStream(), true)
        onStatusUpdate(statusMsg)

        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                onMessageReceived("Remote: $line")
            }
        } catch (_: Exception) {
            onStatusUpdate("Internet peer disconnected")
        }
    }

    fun sendMessage(message: String) {
        if (writer != null) {
            thread { writer?.println(message) }
        }
    }

    fun disconnect() {
        try {
            clientSocket?.close()
            serverSocket?.close()
        } catch (_: Exception) {}
    }
}
