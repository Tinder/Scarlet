/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import com.tinder.StateMachine
import com.tinder.StateMachine.Companion.create
import com.tinder.scarlet.State.Closed
import com.tinder.scarlet.State.Closing
import com.tinder.scarlet.State.Opening
import com.tinder.scarlet.State.Opened
import com.tinder.scarlet.State.WaitingToRetry
import com.tinder.scarlet.State.Destroyed
import com.tinder.scarlet.Event.OnSendMethodCalled
import com.tinder.scarlet.Event.OnReceiveMethodCalled
import com.tinder.scarlet.Event.OnTopicSubscriptionStarted
import com.tinder.scarlet.Event.OnTopicSubscriptionStopped
import com.tinder.scarlet.Event.OnLifecycleStarted
import com.tinder.scarlet.Event.OnLifecycleStopped
import com.tinder.scarlet.Event.OnLifecycleDestroyed
import com.tinder.scarlet.Event.OnTimerTick
import com.tinder.scarlet.Event.OnConnectionOpeningAcknowledged
import com.tinder.scarlet.Event.OnConnectionClosingAcknowledged
import com.tinder.scarlet.Event.OnConnectionFailed
import com.tinder.scarlet.Event.OnMessageReceived
import com.tinder.scarlet.Event.OnMessageEnqueued
import com.tinder.scarlet.Event.OnMessageSent
import com.tinder.scarlet.Event.OnMessageDelivered
import com.tinder.scarlet.SideEffect.ScheduleTimer
import com.tinder.scarlet.SideEffect.OpenConnection
import com.tinder.scarlet.SideEffect.CloseConnection
import com.tinder.scarlet.SideEffect.SendMessage
import com.tinder.scarlet.SideEffect.Subscribe
import com.tinder.scarlet.SideEffect.ForceCloseConnection
import com.tinder.scarlet.SideEffect.Unsubscribe

class StateMachineFactory {

    fun create(): StateMachine<State, Event, SideEffect> {
        return create {
            initialState(Closed())
            state<Closed> {
                on<OnLifecycleStarted> {
                    transitionTo(Opening(retryCount = 0), OpenConnection())
                }
                on<OnLifecycleStopped> {
                    dontTransition()
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed)
                }
            }
            state<WaitingToRetry> {
                on<OnTimerTick> {
                    transitionTo(Opening(retryCount), OpenConnection())
                }
                on<OnLifecycleStopped> {
                    transitionTo(Closed(), CloseConnection())
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed, ForceCloseConnection())
                }
            }
            state<Opening> {
                on<OnConnectionOpeningAcknowledged> {
                    transitionTo(Opened())
                }
                on<OnConnectionFailed> {
                    transitionTo(
                        WaitingToRetry(retryCount + 1),
                        ScheduleTimer(retryCount)
                    )
                }
            }
            state<Opened> {
                on<OnTopicSubscriptionStarted> {
                    dontTransition(Subscribe(it.topic))
                }
                on<OnTopicSubscriptionStopped> {
                    dontTransition(Unsubscribe(it.topic))
                }
                on<OnSendMethodCalled> {
                    dontTransition() // SendMessage()
                }
                on<OnReceiveMethodCalled> {
                    dontTransition() // ???
                }

                on<OnLifecycleStopped> {
                    transitionTo(Closing(), CloseConnection())
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
                        WaitingToRetry(retryCount = 0),
                        ScheduleTimer(0)
                    )
                }
            }
            state<Closing> {
                on<OnConnectionClosingAcknowledged> {
                    transitionTo(Closed())
                }
            }
            state<Destroyed> {
            }
        }
    }
}
