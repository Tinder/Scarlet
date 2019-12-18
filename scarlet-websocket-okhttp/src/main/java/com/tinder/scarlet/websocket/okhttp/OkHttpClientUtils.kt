/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("OkHttpClientUtils")

package com.tinder.scarlet.websocket.okhttp

import android.os.Build
import android.security.NetworkSecurityPolicy
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.websocket.okhttp.request.RequestFactory
import com.tinder.scarlet.websocket.okhttp.request.StaticUrlRequestFactory
import okhttp3.OkHttpClient
import java.net.URL
import java.net.UnknownServiceException

fun OkHttpClient.newWebSocketFactory(requestFactory: RequestFactory): WebSocket.Factory {
    return OkHttpWebSocket.Factory(OkHttpClientWebSocketConnectionEstablisher(this, requestFactory))
}

fun OkHttpClient.newWebSocketFactory(url: String): WebSocket.Factory {
    if (url.startsWith("ws:", ignoreCase = true)) {
        try {
            if ((Build.VERSION.SDK_INT > 23 &&
                            !NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(
                                    URL("http:${url.substring(3)}").host)) ||
                    (Build.VERSION.SDK_INT == 23 &&
                            !NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted)) {
                throw UnknownServiceException(
                        "CLEARTEXT communication to $url not permitted by network security policy")
            }
        } catch (_: ClassNotFoundException) {
            // Not running on Android
        }
    }

    return newWebSocketFactory(StaticUrlRequestFactory(url))
}
