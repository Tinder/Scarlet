/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle.android

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.lifecycle.LifecycleRegistry

internal class ConnectivityOnLifecycle(
        private val applicationContext: Context,
        private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()
) : Lifecycle by lifecycleRegistry {

    private val connectivityChangeBroadcastReceiver = ConnectivityChangeBroadcastReceiver()

    init {
        emitCurrentConnectivity(applicationContext)
        subscribeToConnectivityChange(applicationContext)
    }

    private fun emitCurrentConnectivity(applicationContext: Context) {
        val connectivityManager =
                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        lifecycleRegistry.onNext(toLifecycleState(connectivityManager.isConnected()))
    }

    private fun subscribeToConnectivityChange(applicationContext: Context) {
        val intentFilter = IntentFilter().apply { addAction(ConnectivityManager.CONNECTIVITY_ACTION) }
        applicationContext.registerReceiver(connectivityChangeBroadcastReceiver, intentFilter)
    }

    @SuppressLint("MissingPermission")
    private fun ConnectivityManager.isConnected(): Boolean {
        val activeNetworkInfo = activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    private fun toLifecycleState(isConnected: Boolean): LifecycleState = if (isConnected) {
        LifecycleState.Started
    } else {
        LifecycleState.Stopped
    }

    fun unregisterReceiver() {
        applicationContext.unregisterReceiver(connectivityChangeBroadcastReceiver)
    }

    private inner class ConnectivityChangeBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras ?: return
            val isConnected = !extras.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY)
            lifecycleRegistry.onNext(toLifecycleState(isConnected))
        }
    }
}

