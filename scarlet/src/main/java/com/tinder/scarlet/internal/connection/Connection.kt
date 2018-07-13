/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection

import com.tinder.scarlet.Event
import com.tinder.scarlet.Event.OnLifecycle
import com.tinder.scarlet.Event.OnRetry
import com.tinder.scarlet.Event.OnWebSocket
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Message
import com.tinder.scarlet.Session
import com.tinder.scarlet.State
import com.tinder.scarlet.State.Connected
import com.tinder.scarlet.State.Connecting
import com.tinder.scarlet.State.Destroyed
import com.tinder.scarlet.State.Disconnected
import com.tinder.scarlet.State.Disconnecting
import com.tinder.scarlet.State.WaitingToRetry
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.internal.connection.subscriber.LifecycleStateSubscriber
import com.tinder.scarlet.internal.connection.subscriber.RetryTimerSubscriber
import com.tinder.scarlet.internal.connection.subscriber.WebSocketEventSubscriber
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.retry.BackoffStrategy
import com.tinder.statemachine.StateMachine
import com.tinder.statemachine.StateMachine.Matcher.Companion.any
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal class Connection(
    val stateManager: StateManager
) {

    fun startForever() = stateManager.subscribe()

    fun observeEvent(): Flowable<Event> = stateManager.observeEvent()

    fun send(message: Message): Boolean {
        val state = stateManager.state
        return when (state) {
            is Connected -> state.session.webSocket.send(message)
            else -> false
        }
    }

    internal class StateManager(
        val lifecycle: Lifecycle,
        private val webSocketFactory: WebSocket.Factory,
        private val backoffStrategy: BackoffStrategy,
        private val scheduler: Scheduler
    ) {
        val state: State
            get() = stateMachine.state

        private val lifecycleStateSubscriber = LifecycleStateSubscriber(this)
        private val eventProcessor = PublishProcessor.create<Event>()
        private val stateMachine = StateMachine.create<State, Event> {
            state<Disconnected> {
                onEnter {
                    requestNextLifecycleState()
                }
                on(lifecycleStart()) transitionTo {
                    val webSocketSession = open()
                    Connecting(session = webSocketSession, retryCount = 0)
                }
                on(lifecycleStop()) run {
                    // No-op
                    requestNextLifecycleState()
                }
                on<OnLifecycle.Terminate>() transitionTo {
                    Destroyed
                }
            }
            state<WaitingToRetry> {
                onEnter {
                    requestNextLifecycleState()
                }
                on<OnRetry>() transitionTo {
                    val webSocketSession = open()
                    Connecting(session = webSocketSession, retryCount = retryCount + 1)
                }
                on(lifecycleStart()) run {
                    // No-op
                    requestNextLifecycleState()
                }
                on(lifecycleStop()) transitionTo {
                    cancelRetry()
                    Disconnected
                }
                on<OnLifecycle.Terminate>() transitionTo {
                    cancelRetry()
                    Destroyed
                }
            }
            state<Connecting> {
                on(webSocketOpen()) transitionTo {
                    Connected(session = session)
                }
                on<OnWebSocket.Terminate>() transitionTo {
                    val backoffDuration = backoffStrategy.backoffDurationMillisAt(retryCount)
                    val timerDisposable = scheduleRetry(backoffDuration)
                    WaitingToRetry(
                        timerDisposable = timerDisposable,
                        retryCount = retryCount,
                        retryInMillis = backoffDuration
                    )
                }
            }
            state<Connected> {
                onEnter {
                    requestNextLifecycleState()
                }
                on(lifecycleStart()) run {
                    // No-op
                    requestNextLifecycleState()
                }
                on(lifecycleStop()) transitionTo {
                    initiateShutdown(it.state)
                    Disconnecting
                }
                on<OnLifecycle.Terminate>() transitionTo {
                    session.webSocket.cancel()
                    Destroyed
                }
                on<OnWebSocket.Terminate>() transitionTo {
                    val backoffDuration = backoffStrategy.backoffDurationMillisAt(0)
                    val timerDisposable = scheduleRetry(backoffDuration)
                    WaitingToRetry(
                        timerDisposable = timerDisposable,
                        retryCount = 0,
                        retryInMillis = backoffDuration
                    )
                }
            }
            state<Disconnecting> {
                on<OnWebSocket.Terminate>() transitionTo {
                    Disconnected
                }
            }
            state<Destroyed> {
                onEnter {
                    lifecycleStateSubscriber.dispose()
                }
            }
            defaultState(Disconnected)
            onStateChange { state ->
                eventProcessor.onNext(Event.OnStateChange(state))
            }
        }

        fun observeEvent(): Flowable<Event> = eventProcessor

        fun subscribe() {
            lifecycle.subscribe(lifecycleStateSubscriber)
        }

        fun handleEvent(event: Event) {
            eventProcessor.onNext(event)
            stateMachine.transition(event)
        }

        private fun open(): Session {
            val webSocket = webSocketFactory.create()
            val subscriber = WebSocketEventSubscriber(this)
            Flowable.fromPublisher(webSocket.open())
                .observeOn(scheduler)
                .cast(WebSocket.Event::class.java)
                .subscribe(subscriber)
            return Session(webSocket, subscriber)
        }

        private fun scheduleRetry(duration: Long): Disposable {
            val retryTimerScheduler = RetryTimerSubscriber(this)
            Flowable.timer(duration, TimeUnit.MILLISECONDS, scheduler)
                .subscribe(retryTimerScheduler)
            return retryTimerScheduler
        }

        private fun requestNextLifecycleState() = lifecycleStateSubscriber.requestNext()

        private fun Connected.initiateShutdown(state: Lifecycle.State) {
            when (state) {
                is Lifecycle.State.Stopped.WithReason -> session.webSocket.close(state.shutdownReason)
                Lifecycle.State.Stopped.AndAborted -> session.webSocket.cancel()
            }
        }

        private fun WaitingToRetry.cancelRetry() = timerDisposable.dispose()

        private fun lifecycleStart() =
            any<Event, Event.OnLifecycle.StateChange<*>>().where { state == Lifecycle.State.Started }

        private fun lifecycleStop() =
            any<Event, Event.OnLifecycle.StateChange<*>>().where { state is Lifecycle.State.Stopped }

        private fun webSocketOpen() = any<Event, Event.OnWebSocket.WebSocketEvent<*>>()
            .where { event is WebSocket.Event.OnConnectionOpened<*> }
    }

    @Singleton
    class Factory @Inject constructor(
        private val lifecycle: Lifecycle,
        private val webSocketFactory: WebSocket.Factory,
        private val backoffStrategy: BackoffStrategy,
        private val scheduler: Scheduler
    ) {
        private val sharedLifecycle: Lifecycle by lazy { createSharedLifecycle() }

        fun create(): Connection {
            val stateManager = StateManager(sharedLifecycle, webSocketFactory, backoffStrategy, scheduler)
            return Connection(stateManager)
        }

        private fun createSharedLifecycle() = LifecycleRegistry()
            .apply { lifecycle.subscribe(this) }
    }
}
