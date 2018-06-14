/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.websocket.okhttp.request

import okhttp3.Request

/**
 * A [RequestFactory] that creates requests with a static URL.
 */
internal class StaticUrlRequestFactory(
    private val url: String
) : RequestFactory {

    override fun createRequest(): Request = Request.Builder()
        .url(url)
        .build()

}
