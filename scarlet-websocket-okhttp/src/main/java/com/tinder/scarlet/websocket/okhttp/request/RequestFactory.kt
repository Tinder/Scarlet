/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.websocket.okhttp.request

import okhttp3.Request

/**
 * Used to customize the [OkHttp Request](http://square.github.io/okhttp/) to start a WebSocket connection.
 */
interface RequestFactory {
    /**
     * Returns an [OkHttp Request](http://square.github.io/okhttp/).
     */
    fun createRequest(): Request
}
