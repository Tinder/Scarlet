/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.websocket.okhttp.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class StaticUrlRequestFactoryTest {

    private val url = "wss://example.com"
    private val staticUrlRequestFactory = StaticUrlRequestFactory(url)

    @Test
    fun create_shouldCreateRequestsWithTheSameUrl() {
        // When
        val request1 = staticUrlRequestFactory.createRequest()
        val request2 = staticUrlRequestFactory.createRequest()

        // Then
        assertThat(request1.url()).isEqualTo(request2.url())
    }

}
