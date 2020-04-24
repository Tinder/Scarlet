package com.carpa.simple_scarlet_websocket_kotlin.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.carpa.simple_scarlet_websocket_kotlin.network.SocketService

class MainViewModelFactory(private val socketService: SocketService
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(socketService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}