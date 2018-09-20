/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection

import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Event
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.State
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.FlowableLifecycle
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.testutils.withLifecycleState
import com.tinder.scarlet.testutils.withState
import com.tinder.scarlet.testutils.withWebSocketEvent
import com.tinder.scarlet.utils.toStream
import io.reactivex.Flowable
import io.reactivex.processors.ReplayProcessor
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
internal class ConnectionStateManagerTest {

    private val webSocketFactory = mock<WebSocket.Factory>()
    private val scheduler = TestScheduler()
    private val lifecycleSubject = ReplayProcessor.create<Lifecycle.State>()
    private val lifecycle = FlowableLifecycle(lifecycleSubject, scheduler)
    private val backoffStrategy = LinearBackoffStrategy(RETRY_INTERVAL_MILLIS)

    private val stateManager = Connection.StateManager(lifecycle, webSocketFactory, backoffStrategy, scheduler)

    @Test
    fun subscribe_givenStartEvent_shouldOpenWebSocketClient() {
        // Given
        lifecycleSubject.onNext(Lifecycle.State.Started)
        var subscriptionCount = 0
        val delayMillis = 100L
        val webSocket = mock<WebSocket>()
            .apply {
                given(open()).willReturn(
                    Flowable.empty<WebSocket.Event>()
                        .delay(delayMillis, TimeUnit.MILLISECONDS, scheduler)
                        .startWith(mock<WebSocket.Event.OnConnectionOpened<*>>())
                        .doOnSubscribe { subscriptionCount += 1 }.toStream()
                )
            }
        given(webSocketFactory.create()).willReturn(webSocket)
        val events = stateManager.observeEvent().toStream().test()

        // When
        stateManager.subscribe()
        scheduler.triggerActions()

        // Then
        assertThat(subscriptionCount).isEqualTo(1)
        events.awaitValues(
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Started>(),
            any<Event.OnStateChange<*>>().withState<State.Connecting>(),
            any<Event.OnWebSocket.Event<*>>().withWebSocketEvent<WebSocket.Event.OnConnectionOpened<*>>(),
            any<Event.OnStateChange<*>>().withState<State.Connected>()
        )
    }

    @Test
    fun subscribe_givenStartEvent_andWebSocketFails_shouldOpenWebSocketClient_andRetry() {
        // Given
        lifecycleSubject.onNext(Lifecycle.State.Started)
        var subscriptionCount = 0
        val webSocket = mock<WebSocket>()
            .apply {
                given(open()).willReturn(
                    Flowable.empty<WebSocket.Event>()
                        .doOnSubscribe { subscriptionCount += 1 }
                        .toStream())
            }
        given(webSocketFactory.create()).willReturn(webSocket)
        val events = stateManager.observeEvent().toStream().test()

        // When
        stateManager.subscribe()
        scheduler.advanceTimeBy(RETRY_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)

        // Then
        assertThat(subscriptionCount).isEqualTo(2)
        events.awaitValues(
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Started>(),
            any<Event.OnStateChange<*>>().withState<State.Connecting>(),
            any<Event.OnWebSocket.Terminate>(),
            any<Event.OnStateChange<*>>().withState<State.WaitingToRetry>(),
            any<Event.OnRetry>(),
            any<Event.OnStateChange<*>>().withState<State.Connecting>(),
            any<Event.OnWebSocket.Terminate>(),
            any<Event.OnStateChange<*>>().withState<State.WaitingToRetry>()
        )
    }

    @Test
    fun subscribe_givenStartEvent_andStopWithReasonEvent_shouldCloseWebSocketClient() {
        // Given
        val reason = mock<ShutdownReason>()
        with(lifecycleSubject) {
            onNext(Lifecycle.State.Started)
            onNext(Lifecycle.State.Stopped.WithReason(reason))
        }
        val delayMillis = 100L
        val webSocket = mock<WebSocket>()
            .apply {
                given(open()).willReturn(
                    Flowable.empty<WebSocket.Event>()
                        .delay(delayMillis, TimeUnit.MILLISECONDS, scheduler)
                        .startWith(mock<WebSocket.Event.OnConnectionOpened<*>>()).toStream()
                )
            }
        given(webSocketFactory.create()).willReturn(webSocket)
        val events = stateManager.observeEvent().toStream().test()

        // When
        stateManager.subscribe()
        scheduler.advanceTimeBy(delayMillis, TimeUnit.MILLISECONDS)

        // Then
        then(webSocket).should().close(reason)
        events.awaitValues(
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Started>(),
            any<Event.OnStateChange<*>>().withState<State.Connecting>(),
            any<Event.OnWebSocket.Event<*>>().withWebSocketEvent<WebSocket.Event.OnConnectionOpened<*>>(),
            any<Event.OnStateChange<*>>().withState<State.Connected>(),
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Stopped.WithReason>(),
            any<Event.OnStateChange<*>>().withState<State.Disconnecting>(),
            any<Event.OnWebSocket.Terminate>(),
            any<Event.OnStateChange<*>>().withState<State.Disconnected>()
        )
    }

    @Test
    fun subscribe_givenStartEvent_andStartEvent_andStopWithReasonEvent_shouldCloseWebSocketClient() {
        // Given
        val reason = mock<ShutdownReason>()
        with(lifecycleSubject) {
            onNext(Lifecycle.State.Started)
            onNext(Lifecycle.State.Started)
            onNext(Lifecycle.State.Stopped.WithReason(reason))
        }
        val delayMillis = 100L
        val webSocket = mock<WebSocket>()
            .apply {
                given(open()).willReturn(
                    Flowable.empty<WebSocket.Event>()
                        .delay(delayMillis, TimeUnit.MILLISECONDS, scheduler)
                        .startWith(mock<WebSocket.Event.OnConnectionOpened<*>>()).toStream()
                )
            }
        given(webSocketFactory.create()).willReturn(webSocket)
        val events = stateManager.observeEvent().toStream().test()

        // When
        stateManager.subscribe()
        scheduler.advanceTimeBy(delayMillis, TimeUnit.MILLISECONDS)

        // Then
        then(webSocket).should().close(reason)
        events.awaitValues(
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Started>(),
            any<Event.OnStateChange<*>>().withState<State.Connecting>(),
            any<Event.OnWebSocket.Event<*>>().withWebSocketEvent<WebSocket.Event.OnConnectionOpened<*>>(),
            any<Event.OnStateChange<*>>().withState<State.Connected>(),
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Started>(),
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Stopped.WithReason>(),
            any<Event.OnStateChange<*>>().withState<State.Disconnecting>(),
            any<Event.OnWebSocket.Terminate>(),
            any<Event.OnStateChange<*>>().withState<State.Disconnected>()
        )
    }

    @Test
    fun subscribe_givenStartEvent_andStopAndAbortEvent_shouldCancelWebSocketClient() {
        // Given
        with(lifecycleSubject) {
            onNext(Lifecycle.State.Started)
            onNext(Lifecycle.State.Stopped.AndAborted)
        }
        val delayMillis = 100L
        val webSocket = mock<WebSocket>()
            .apply {
                given(open()).willReturn(
                    Flowable.empty<WebSocket.Event>()
                        .delay(delayMillis, TimeUnit.MILLISECONDS, scheduler)
                        .startWith(mock<WebSocket.Event.OnConnectionOpened<*>>()).toStream()
                )
            }
        given(webSocketFactory.create()).willReturn(webSocket)
        val events = stateManager.observeEvent().toStream().test()

        // When
        stateManager.subscribe()
        scheduler.advanceTimeBy(delayMillis, TimeUnit.MILLISECONDS)

        // Then
        then(webSocket).should().cancel()
        events.awaitValues(
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Started>(),
            any<Event.OnStateChange<*>>().withState<State.Connecting>(),
            any<Event.OnWebSocket.Event<*>>().withWebSocketEvent<WebSocket.Event.OnConnectionOpened<*>>(),
            any<Event.OnStateChange<*>>().withState<State.Connected>(),
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Stopped.AndAborted>(),
            any<Event.OnStateChange<*>>().withState<State.Disconnecting>(),
            any<Event.OnWebSocket.Terminate>(),
            any<Event.OnStateChange<*>>().withState<State.Disconnected>()
        )
    }

    @Test
    fun subscribe_givenStartEvent_andStopWithReasonEvent_andStartEvent_shouldOpenWebSocketClient() {
        // Given
        val reason = mock<ShutdownReason>()
        with(lifecycleSubject) {
            onNext(Lifecycle.State.Started)
            onNext(Lifecycle.State.Stopped.WithReason(reason))
            onNext(Lifecycle.State.Started)
        }
        var subscriptionCount = 0
        val delayMillis = 100L
        val webSocket = mock<WebSocket>()
            .apply {
                var processor: ReplayProcessor<WebSocket.Event>? = null
                given(open()).willAnswer {
                    processor = ReplayProcessor.create<WebSocket.Event>()
                    Flowable.just<WebSocket.Event>(mock<WebSocket.Event.OnConnectionOpened<*>>())
                        .delay(delayMillis, TimeUnit.MILLISECONDS, scheduler)
                        .subscribe(processor!!::onNext)
                    processor!!.doOnSubscribe { subscriptionCount += 1 }.toStream()
                }
                given(close(com.nhaarman.mockito_kotlin.any())).willAnswer {
                    Flowable.empty<WebSocket.Event>()
                        .delay(delayMillis, TimeUnit.MILLISECONDS, scheduler)
                        .subscribe(processor!!)
                    true
                }
            }
        given(webSocketFactory.create()).willReturn(webSocket)
        val events = stateManager.observeEvent().toStream().test()

        // When
        stateManager.subscribe()
        scheduler.advanceTimeBy(delayMillis + delayMillis + delayMillis + delayMillis, TimeUnit.MILLISECONDS)

        // Then
        assertThat(subscriptionCount).isEqualTo(2)
        events.awaitValues(
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Started>(),
            any<Event.OnStateChange<*>>().withState<State.Connecting>(),
            any<Event.OnWebSocket.Event<*>>().withWebSocketEvent<WebSocket.Event.OnConnectionOpened<*>>(),
            any<Event.OnStateChange<*>>().withState<State.Connected>(),
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Stopped.WithReason>(),
            any<Event.OnStateChange<*>>().withState<State.Disconnecting>(),
            any<Event.OnWebSocket.Terminate>(),
            any<Event.OnStateChange<*>>().withState<State.Disconnected>(),
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Started>(),
            any<Event.OnStateChange<*>>().withState<State.Connecting>(),
            any<Event.OnWebSocket.Event<*>>().withWebSocketEvent<WebSocket.Event.OnConnectionOpened<*>>(),
            any<Event.OnStateChange<*>>().withState<State.Connected>()
        )
    }

    @Test
    fun subscribe_givenLifecycleIsTerminated_shouldUnsubscribeToLifecycle() {
        // Given
        with(lifecycleSubject) {
            onNext(Lifecycle.State.Destroyed)
            onComplete()
        }

        val events = stateManager.observeEvent().toStream().test()

        // When
        stateManager.subscribe()
        scheduler.triggerActions()

        // Then
        events.awaitValues(
            any<Event.OnLifecycle.StateChange<*>>().withLifecycleState<Lifecycle.State.Destroyed>(),
            any<Event.OnLifecycle.Terminate>(),
            any<Event.OnStateChange<*>>().withState<State.Destroyed>()
        )
    }

    companion object {
        private const val RETRY_INTERVAL_MILLIS = 100L
    }
}
