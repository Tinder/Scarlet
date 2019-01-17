/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection

import com.tinder.StateMachine
import com.tinder.scarlet.Event
import com.tinder.scarlet.Event.OnLifecycle
import com.tinder.scarlet.Event.OnRetry
import com.tinder.scarlet.Event.OnWebSocket
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Message
import com.tinder.scarlet.Session
import com.tinder.scarlet.SideEffect
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
import com.tinder.StateMachine.Matcher.Companion.any
import com.tinder.StateMachine.Transition.Valid
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import java.util.concurrent.TimeUnit

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
        private val stateMachine = StateMachine.create<State, Event, SideEffect> {
            state<Disconnected> {
                onEnter {
                    requestNextLifecycleState()
                }
                on(lifecycleStart()) {
                    val webSocketSession = open()
                    transitionTo(Connecting(session = webSocketSession, retryCount = 0))
                }
                on(lifecycleStop()) {
                    // No-op
                    requestNextLifecycleState()
                    dontTransition()
                }
                on<OnLifecycle.Terminate>() {
                    transitionTo(Destroyed)
                }
            }
            state<WaitingToRetry> {
                onEnter {
                    requestNextLifecycleState()
                }
                on<OnRetry> {
                    val webSocketSession = open()
                    transitionTo(Connecting(session = webSocketSession, retryCount = retryCount + 1))
                }
                on(lifecycleStart()) {
                    // No-op
                    requestNextLifecycleState()
                    dontTransition()
                }
                on(lifecycleStop()) {
                    cancelRetry()
                    transitionTo(Disconnected)
                }
                on<OnLifecycle.Terminate>() {
                    cancelRetry()
                    transitionTo(Destroyed)
                }
            }
            state<Connecting> {
                on(webSocketOpen()) {
                    transitionTo(Connected(session = session))
                }
                on<OnWebSocket.Terminate>() {
                    val backoffDuration = backoffStrategy.backoffDurationMillisAt(retryCount)
                    val timerDisposable = scheduleRetry(backoffDuration)
                    transitionTo(
                        WaitingToRetry(
                            timerDisposable = timerDisposable,
                            retryCount = retryCount,
                            retryInMillis = backoffDuration
                        )
                    )
                }
            }
            state<Connected> {
                onEnter {
                    requestNextLifecycleState()
                }
                on(lifecycleStart()) {
                    // No-op
                    requestNextLifecycleState()
                    dontTransition()
                }
                on(lifecycleStop()) {
                    initiateShutdown(it.state)
                    transitionTo(Disconnecting)
                }
                on<OnLifecycle.Terminate> {
                    session.webSocket.cancel()
                    transitionTo(Destroyed)
                }
                on<OnWebSocket.Terminate>() {
                    val backoffDuration = backoffStrategy.backoffDurationMillisAt(0)
                    val timerDisposable = scheduleRetry(backoffDuration)
                    transitionTo(
                        WaitingToRetry(
                            timerDisposable = timerDisposable,
                            retryCount = 0,
                            retryInMillis = backoffDuration
                        )
                    )
                }
            }
            state<Disconnecting> {
                on<OnWebSocket.Terminate> {
                    transitionTo(Disconnected)
                }
            }
            state<Destroyed> {
                onEnter {
                    lifecycleStateSubscriber.dispose()
                }
            }
            initialState(Disconnected)
            onTransition { transition ->
                transition.let {
                    if (it is Valid && it.fromState != it.toState) {
                        eventProcessor.onNext(Event.OnStateChange(state))
                    }
                }
            }
        }

        fun observeEvent(): Flowable<Event> = eventProcessor.onBackpressureBuffer()

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
                .onBackpressureBuffer()
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

        private fun webSocketOpen() = any<Event, Event.OnWebSocket.Event<*>>()
            .where { event is WebSocket.Event.OnConnectionOpened<*> }
    }

    class Factory(
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
