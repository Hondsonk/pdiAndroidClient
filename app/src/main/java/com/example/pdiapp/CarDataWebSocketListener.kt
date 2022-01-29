package com.example.pdiapp

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

internal class CarDataWebSocketListener(
    val output: (Double) -> Unit,
    val ping: (String) -> Unit,
    val closing: () -> Unit
) : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        ping("Connected")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        // Get data from JSON string
        // then plot it in the UI thread

        //output("$text")
        val carDataJSON = JSONObject(text)
        output(carDataJSON.getDouble("engineForce"))
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        //output("Receiving bytes : " + bytes.hex())
        ping("Got ByteString from WS")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        ping("Closing : $code / $reason")
        closing()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        ping("Error : " + t.message)
        closing()
    }

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
    }
}