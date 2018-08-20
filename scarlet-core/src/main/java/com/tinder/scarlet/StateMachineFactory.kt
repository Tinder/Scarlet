/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import com.tinder.StateMachine
import com.tinder.StateMachine.Companion.create
import com.tinder.scarlet.Event.OnConnectionClosed
import com.tinder.scarlet.Event.OnConnectionFailed
import com.tinder.scarlet.Event.OnConnectionOpened
import com.tinder.scarlet.Event.OnLifecycleDestroyed
import com.tinder.scarlet.Event.OnLifecycleStarted
import com.tinder.scarlet.Event.OnLifecycleStopped
import com.tinder.scarlet.Event.OnMessageDelivered
import com.tinder.scarlet.Event.OnMessageEnqueued
import com.tinder.scarlet.Event.OnMessageReceived
import com.tinder.scarlet.Event.OnMessageSent
import com.tinder.scarlet.Event.OnShouldSendMessage
import com.tinder.scarlet.Event.OnShouldOpenConnection
import com.tinder.scarlet.Event.OnShouldSubscribe
import com.tinder.scarlet.Event.OnShouldUnsubscribe
import com.tinder.scarlet.SideEffect.CloseConnection
import com.tinder.scarlet.SideEffect.ForceCloseConnection
import com.tinder.scarlet.SideEffect.OpenConnection
import com.tinder.scarlet.SideEffect.ScheduleConnection
import com.tinder.scarlet.SideEffect.SendMessage
import com.tinder.scarlet.SideEffect.Subscribe
import com.tinder.scarlet.SideEffect.Unsubscribe
import com.tinder.scarlet.State.Closed
import com.tinder.scarlet.State.Closing
import com.tinder.scarlet.State.Destroyed
import com.tinder.scarlet.State.Opened
import com.tinder.scarlet.State.Opening
import com.tinder.scarlet.State.WillOpen

class StateMachineFactory(
    private val configFactory: ConfigFactory
) {

    fun create(): StateMachine<State, Event, SideEffect> {
        return create {
            initialState(Closed())
            state<Closed> {
                on<OnLifecycleStarted> {
                    transitionTo(
                        WillOpen(retryCount = 0),
                        ScheduleConnection(0)
                    )
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed)
                }
            }
            state<WillOpen> {
                on<OnShouldOpenConnection> {
                    val clientOption = configFactory.createClientOpenOption()
                    transitionTo(Opening(retryCount, clientOption), OpenConnection(clientOption))
                }
                on<OnLifecycleStopped> {
                    transitionTo(Closed(), CloseConnection())
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed, ForceCloseConnection())
                }
            }
            state<Opening> {
                on<OnConnectionOpened> {
                    transitionTo(Opened())
                }
                on<OnConnectionFailed> {
                    transitionTo(
                        WillOpen(retryCount + 1),
                        ScheduleConnection(retryCount)
                    )
                }
            }
            state<Opened> {
                on<OnShouldSubscribe> {
                    dontTransition(Subscribe(it.topic))
                }
                on<OnShouldUnsubscribe> {
                    dontTransition(Unsubscribe(it.topic))
                }
                on<OnShouldSendMessage> {
                    val messageOption = configFactory.createSendMessageOption()
                    dontTransition(SendMessage(it.topic, it.message, messageOption))
                }
                on<OnLifecycleStopped> {
                    val clientOption = configFactory.createClientCloseOption()
                    transitionTo(Closing(clientOption), CloseConnection(clientOption))
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed, ForceCloseConnection())
                }
                on<OnMessageReceived> {
                    dontTransition()
                }
                on<OnMessageEnqueued> {
                    dontTransition()
                }
                on<OnMessageSent> {
                    dontTransition()
                }
                on<OnMessageDelivered> {
                    dontTransition()
                }
                on<OnConnectionFailed> {
                    transitionTo(
                        WillOpen(retryCount = 0),
                        ScheduleConnection(0)
                    )
                }
            }
            state<Closing> {
                on<OnConnectionClosed> {
                    transitionTo(Closed(clientOption, it.serverOption))
                }
            }
            state<Destroyed> {
            }
        }
    }
}
