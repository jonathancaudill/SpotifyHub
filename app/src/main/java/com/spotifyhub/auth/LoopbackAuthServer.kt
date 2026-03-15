package com.spotifyhub.auth

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class LoopbackAuthResult(val rawPath: String)

class LoopbackAuthServer private constructor(
    private val serverSocket: ServerSocket,
) {
    val redirectUri: String = "http://127.0.0.1:${CALLBACK_PORT}/callback"

    suspend fun awaitCallback(timeoutMs: Long = 90_000L): LoopbackAuthResult {
        return withTimeout(timeoutMs) {
            withContext(Dispatchers.IO) {
                serverSocket.accept().use(::readResult)
            }
        }
    }

    fun close() {
        runCatching { serverSocket.close() }
    }

    private fun readResult(socket: Socket): LoopbackAuthResult {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val requestLine = reader.readLine().orEmpty()
        val path = requestLine.split(" ").getOrNull(1).orEmpty()
        val response = """
            HTTP/1.1 200 OK
            Content-Type: text/html; charset=utf-8

            <html><body style="background:#0A0E14;color:#EAF7F3;font-family:sans-serif;padding:24px;">
            <h2>SpotifyHub connected</h2>
            <p>You can return to the app now.</p>
            </body></html>
        """.trimIndent().replace("\n", "\r\n")
        socket.getOutputStream().write(response.toByteArray())
        socket.getOutputStream().flush()
        return LoopbackAuthResult(rawPath = path)
    }

    companion object {
        const val CALLBACK_PORT: Int = 43821

        fun bind(): LoopbackAuthServer {
            val socket = ServerSocket(CALLBACK_PORT, 0, InetAddress.getByName("127.0.0.1"))
            return LoopbackAuthServer(socket)
        }
    }
}
