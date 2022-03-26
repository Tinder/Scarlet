package com.carpa.simple_scarlet_websocket_kotlin.main

import android.annotation.SuppressLint
import android.widget.EditText
import androidx.lifecycle.*
import com.carpa.simple_scarlet_websocket_kotlin.network.Msg
import com.carpa.simple_scarlet_websocket_kotlin.network.SocketService
import com.tinder.scarlet.WebSocket
import timber.log.Timber

class MainViewModel(private val socketService: SocketService) : ViewModel() {
    // We use data binding to connect LiveData/onClick methods to views (look at layout files)
    fun onSendMsg(text: EditText){
        socketService.sendText(
            Msg(
                text.text.toString()
            )
        )
    }

    val msgRec: LiveData<Msg> =
        LiveDataReactiveStreams.fromPublisher<Msg> (
            socketService.observeText()
        )

    val isConnected: LiveData<Boolean> =
        LiveDataReactiveStreams.fromPublisher (
            socketService.observeWebSocketEvent().map{
                when(it){
                    is WebSocket.Event.OnConnectionOpened<*> -> Timber.d("Connection opened")
                    is WebSocket.Event.OnMessageReceived -> Timber.d("Message received")
                    is WebSocket.Event.OnConnectionClosing -> Timber.d("Connection closing")
                    is WebSocket.Event.OnConnectionClosed -> Timber.d("Connection closed")
                    is WebSocket.Event.OnConnectionFailed -> Timber.d("Connection failed")
                }
                (it is WebSocket.Event.OnConnectionOpened<*> || it is WebSocket.Event.OnMessageReceived)
            }
        )

    /**
     * Example: another way (we already have [isConnected]) to see the status of the connection :)
     */
    @SuppressLint("CheckResult")
    fun observeWebSocketEvent(){
        socketService.observeWebSocketEvent()
            .subscribe {
                when(it){
                    is WebSocket.Event.OnConnectionOpened<*> -> Timber.d("Connection opened")
                    is WebSocket.Event.OnMessageReceived -> Timber.d("Message received")
                    is WebSocket.Event.OnConnectionClosing -> Timber.d("Connection closing")
                    is WebSocket.Event.OnConnectionClosed -> Timber.d("Connection closed")
                    is WebSocket.Event.OnConnectionFailed -> Timber.d("Connection failed")
                }
            }
    }
}