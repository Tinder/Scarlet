/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import okhttp3.WebSocket
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class OkHttpWebSocketHolderTest {

    private val webSocket = mock<WebSocket> {
        on { send(any<String>()) } doReturn true
        on { send(any<ByteString>()) } doReturn true
        on { close(any(), any()) } doReturn true
    }
    private val webSocketHolder = OkHttpWebSocketHolder()

    @Test
    fun sendText_givenIsInitiated_shouldDelegateToWebSocket() {
        // Given
        webSocketHolder.initiate(webSocket)
        val text = "Hello"

        // When
        val isSuccessful = webSocketHolder.send(text)

        // Then
        assertThat(isSuccessful).isTrue()
        then(webSocket).should().send(text)
    }

    @Test
    fun sendText_givenIsNotInitiated_shouldReturnFalse() {
        // Given
        val text = "Hello"

        // When
        val isSuccessful = webSocketHolder.send(text)

        // Then
        assertThat(isSuccessful).isFalse()
    }

    @Test
    fun sendBytes_givenIsInitiated_shouldDelegateToWebSocket() {
        // Given
        webSocketHolder.initiate(webSocket)
        val bytes = "Hey".toByteArray()
        val byteString = ByteString.of(*bytes)

        // When
        val isSuccessful = webSocketHolder.send(byteString)

        // Then
        then(webSocket).should().send(byteString)
        assertThat(isSuccessful).isTrue()
    }

    @Test
    fun sendBytes_givenIsNotInitiated_shouldReturnFalse() {
        // Given
        val bytes = "Hey".toByteArray()
        val byteString = ByteString.of(*bytes)

        // When
        val isSuccessful = webSocketHolder.send(byteString)

        // Then
        assertThat(isSuccessful).isFalse()
    }

    @Test
    fun close_givenIsInitiated_shouldDelegateToWebSocket() {
        // Given
        webSocketHolder.initiate(webSocket)
        val code = 1001
        val reason = "Some reason"

        // When
        val isSuccessful = webSocketHolder.close(code, reason)

        // Then
        then(webSocket).should().close(code, reason)
        assertThat(isSuccessful).isTrue()
    }

    @Test
    fun close_givenIsNotInitiated_shouldReturnFalse() {
        // Given
        val code = 1001
        val reason = "Some reason"

        // When
        val isSuccessful = webSocketHolder.close(code, reason)

        // Then
        assertThat(isSuccessful).isFalse()
    }

    @Test
    fun cancel_givenIsInitiated_shouldDelegateToWebSocket() {
        // Given
        webSocketHolder.initiate(webSocket)

        // When
        webSocketHolder.cancel()

        // Then
        then(webSocket).should().cancel()
    }
}
