/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection

import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.retry.BackoffStrategy
import io.reactivex.Scheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ConnectionFactoryTest {
    private val lifecycle = mock<Lifecycle>()
    private val webSocketFactory = mock<WebSocket.Factory>()
    private val retryStrategy = mock<BackoffStrategy>()
    private val scheduler = mock<Scheduler>()
    private val connectionFactory = Connection.Factory(lifecycle, webSocketFactory, retryStrategy, scheduler)

    @Test
    fun create_shouldUseTheSameLifecycleRegistry() {
        // When
        val connection1 = connectionFactory.create()
        val connection2 = connectionFactory.create()

        // Then
        assertThat(connection1.stateManager.lifecycle).isEqualTo(connection2.stateManager.lifecycle)
    }
}
