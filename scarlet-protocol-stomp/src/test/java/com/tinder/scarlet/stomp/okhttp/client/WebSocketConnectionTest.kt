package com.tinder.scarlet.stomp.okhttp.client

import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.stomp.okhttp.core.Connection
import okhttp3.WebSocket
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class WebSocketConnectionTest {

    companion object {
        private const val TEST_DURATION = 100L
        private const val TEST_LONG_DURATION = 10_000L
    }

    private lateinit var connection: Connection
    private lateinit var scheduledExecutorService: ScheduledExecutorService
    private lateinit var webSocket: WebSocket

    @Before
    fun setUp() {
        webSocket = mock()
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        connection = WebSocketConnection(webSocket, scheduledExecutorService)
    }

    @Test(expected = IllegalStateException::class)
    fun `cannot set receive callback with 0 duration`() {
        connection.onReceiveInactivity(0) {}
    }

    @Test(expected = IllegalStateException::class)
    fun `cannot set write callback with 0 duration`() {
        connection.onWriteInactivity(0) {}
    }

    @Test
    fun `check that receive callback will be invoked on time`() {
        var wasInvoked = false
        connection.onReceiveInactivity(TEST_DURATION) {
            wasInvoked = true
        }

        scheduledExecutorService.awaitTermination(TEST_DURATION * 2, TimeUnit.MILLISECONDS)
        assertTrue(wasInvoked)
    }

    @Test
    fun `check that receive callback won't be invoked if time did't come`() {
        var wasInvoked = false
        connection.onReceiveInactivity(TEST_LONG_DURATION) {
            wasInvoked = true
        }

        scheduledExecutorService.awaitTermination(TEST_DURATION * 2, TimeUnit.MILLISECONDS)
        assertFalse(wasInvoked)
    }

    @Test
    fun `check that write callback will be invoked on time`() {
        var wasInvoked = false
        connection.onWriteInactivity(TEST_DURATION) {
            wasInvoked = true
        }

        scheduledExecutorService.awaitTermination(TEST_DURATION * 2, TimeUnit.MILLISECONDS)
        assertTrue(wasInvoked)
    }

    @Test
    fun `check that write callback won't be invoked if time did't come`() {
        var wasInvoked = false
        connection.onWriteInactivity(TEST_LONG_DURATION) {
            wasInvoked = true
        }

        scheduledExecutorService.awaitTermination(TEST_DURATION * 2, TimeUnit.MILLISECONDS)
        assertFalse(wasInvoked)
    }
}