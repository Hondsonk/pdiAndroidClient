package com.example.pdiapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import okhttp3.*
import kotlinx.coroutines.flow.Flow

class SocketViewModel : ViewModel() {
    private lateinit var client: OkHttpClient
    private lateinit var socket: WebSocket

    private fun attachWebSocketListener(webListener: WebSocketListener) {
        client = OkHttpClient()
        val request = Request.Builder().url("ws://10.0.0.203:8082/").build()
        socket = client.newWebSocket(request, webListener)
    }

    suspend fun socketEventsFlow(): Flow<SocketEvent?> = callbackFlow {
        val socketListener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                trySendBlocking(SocketEvent.StringMessage(text))
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                trySendBlocking(SocketEvent.OpenEvent)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSURE_STATUS, null)
                trySendBlocking(SocketEvent.CloseEvent(code, reason))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySendBlocking(SocketEvent.Error(t))
                cancel("",t)
            }
        }
        attachWebSocketListener(socketListener)
        awaitClose { socket.cancel() }
    }.flowOn(Dispatchers.IO).shareIn(viewModelScope, SharingStarted.Lazily)

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
    }

    override fun onCleared() {
        super.onCleared()
        socket?.close(NORMAL_CLOSURE_STATUS, null)
    }
}