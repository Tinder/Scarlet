/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.lifecycle.LifecycleRegistry

internal class ConnectivityOnLifecycle(
    applicationContext: Context,
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()
) : Lifecycle by lifecycleRegistry {

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
        val intentFilter = IntentFilter()
            .apply { addAction(ConnectivityManager.CONNECTIVITY_ACTION) }
        applicationContext.registerReceiver(ConnectivityChangeBroadcastReceiver(), intentFilter)
    }

    private fun ConnectivityManager.isConnected(): Boolean {
        val activeNetworkInfo = activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    private fun toLifecycleState(isConnected: Boolean): Lifecycle.State = if (isConnected) {
        Lifecycle.State.Started
    } else {
        Lifecycle.State.Stopped.AndAborted
    }

    private inner class ConnectivityChangeBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras ?: return
            val isConnected = !extras.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY)
            lifecycleRegistry.onNext(toLifecycleState(isConnected))
        }
    }
}
