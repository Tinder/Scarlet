/*
 * © 2018 Match Group, LLC.
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
