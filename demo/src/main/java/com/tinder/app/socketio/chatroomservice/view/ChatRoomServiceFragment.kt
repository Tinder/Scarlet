/*
 * © 2019 Match Group, LLC.
 */

package com.tinder.app.socketio.chatroomservice.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Messenger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tinder.R
import com.tinder.service.ChatRoomSocketIoService
import timber.log.Timber

class ChatRoomServiceFragment : Fragment() {

    private var messenger: Messenger? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messenger = Messenger(service)
            isBound = true

            Timber.d("chat onServiceConnected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messenger = null
            isBound = false

            Timber.d("chat onServiceDisconnected")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatroom_service, container, false) as View
        return view
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService()
    }

    private fun bindService() {
        val intent = Intent(activity, ChatRoomSocketIoService::class.java)
        activity?.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        activity?.unbindService(serviceConnection)
        isBound = false
    }
}