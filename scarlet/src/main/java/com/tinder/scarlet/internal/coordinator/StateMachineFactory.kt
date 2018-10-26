/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.coordinator

import com.tinder.StateMachine
import com.tinder.StateMachine.Matcher.Companion.any
import com.tinder.scarlet.Event
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.SideEffect
import com.tinder.scarlet.State

internal class StateMachineFactory {

    fun create(): StateMachine<State, Event, SideEffect> {
        return StateMachine.create {
            initialState(State.Disconnected)
            state<State.Disconnected> {
                on(lifecycleStarted) {
                    transitionTo(
                        State.WillConnect(retryCount = 0),
                        SideEffect.ScheduleRetry(0)
                    )
                }
                on(lifecycleStopped) {
                    dontTransition()
                }
                on(lifecycleDestroyed) {
                    transitionTo(State.Destroyed)
                }
            }
            state<State.WillConnect> {
                on<Event.OnShouldConnect> {
                    transitionTo(
                        State.Connecting(retryCount = retryCount + 1),
                        SideEffect.OpenProtocol
                    )
                }
                on(lifecycleStarted) {
                    dontTransition()
                }
                on(lifecycleStopped) {
                    transitionTo(
                        State.Disconnected,
                        SideEffect.CancelRetry
                    )
                }
                on(lifecycleDestroyed) {
                    transitionTo(
                        State.Destroyed,
                        SideEffect.CancelRetry
                    )
                }
            }
            state<State.Connecting> {
                on(protocolOpened) {
                    transitionTo(State.Connected)
                }
                on(protocolFailed) {
                    transitionTo(
                        State.WillConnect(retryCount = retryCount + 1),
                        SideEffect.ScheduleRetry(retryCount)
                    )
                }
            }
            state<State.Connected> {
                on(lifecycleStarted) {
                    dontTransition()
                }
                on(lifecycleStopped) {
                    transitionTo(
                        State.Disconnecting(forceClosed = false),
                        SideEffect.CloseProtocol
                    )
                }
                on(lifecycleDestroyed) {
                    transitionTo(
                        State.Disconnecting(forceClosed = true),
                        SideEffect.ForceCloseProtocol
                    )
                }
                on(protocolMessageReceived) {
                    dontTransition()
                }
                on(protocolClosing) {
                    transitionTo(
                        State.Disconnecting(forceClosed = false),
                        SideEffect.CloseProtocol
                    )
                }
                on(protocolFailed) {
                    transitionTo(
                        State.WillConnect(retryCount = 0),
                        SideEffect.ScheduleRetry(0)
                    )
                }
            }
            state<State.Disconnecting> {
                on(protocolClosing) {
                    dontTransition()
                }
                on(protocolClosed) {
                    if (forceClosed) {
                        transitionTo(State.Disconnected)
                    } else {
                        transitionTo(State.Destroyed)
                    }
                }
                on(protocolFailed) {
                    if (forceClosed) {
                        transitionTo(State.Disconnected)
                    } else {
                        transitionTo(State.Destroyed)
                    }
                }
            }
            state<State.Destroyed> {
                // Terminal state
            }
        }
    }

    private companion object {
        private val lifecycleStarted =
            any<Event, Event.OnLifecycleStateChange>().where { lifecycleState == LifecycleState.Started }

        private val lifecycleStopped =
            any<Event, Event.OnLifecycleStateChange>().where { lifecycleState == LifecycleState.Stopped }

        private val lifecycleDestroyed =
            any<Event, Event.OnLifecycleStateChange>().where { lifecycleState == LifecycleState.Completed }

        private val protocolOpened =
            any<Event, Event.OnProtocolEvent>().where { protocolEvent is ProtocolEvent.OnOpened }

        private val protocolMessageReceived =
            any<Event, Event.OnProtocolEvent>().where { protocolEvent is ProtocolEvent.OnMessageReceived }

        private val protocolClosing =
            any<Event, Event.OnProtocolEvent>().where { protocolEvent is ProtocolEvent.OnClosing }

        private val protocolClosed =
            any<Event, Event.OnProtocolEvent>().where { protocolEvent is ProtocolEvent.OnClosed }

        private val protocolFailed =
            any<Event, Event.OnProtocolEvent>().where { protocolEvent is ProtocolEvent.OnFailed }
    }
}
