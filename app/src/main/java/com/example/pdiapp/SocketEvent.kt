package com.example.pdiapp

sealed class SocketEvent {
    object OpenEvent : SocketEvent()
    data class CloseEvent(val code: Int, val reason: String) : SocketEvent()
    data class Error(val error: Throwable) : SocketEvent()
    data class StringMessage(val content: String) : SocketEvent()
}
