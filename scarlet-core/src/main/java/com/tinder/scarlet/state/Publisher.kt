/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.scarlet.Message
import com.tinder.scarlet.Topic
import com.tinder.StateMachine
import com.tinder.StateMachine.Companion.create
import com.tinder.scarlet.ConfigFactory
import com.tinder.scarlet.state.Publisher.State.Closed
import com.tinder.scarlet.state.Publisher.State.WillOpen
import com.tinder.scarlet.state.Publisher.State.Opening
import com.tinder.scarlet.state.Publisher.State.Opened
import com.tinder.scarlet.state.Publisher.State.Closing
import com.tinder.scarlet.state.Publisher.State.Destroyed
import com.tinder.scarlet.state.Publisher.Event.OnMessageDelivered
import com.tinder.scarlet.state.Publisher.Event.OnMessageEnqueued
import com.tinder.scarlet.state.Publisher.Event.OnMessageSent
import com.tinder.scarlet.state.Publisher.SideEffect.SendMessage

internal class Publisher {

    fun send() {

    }

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

    sealed class State {
        data class Opening internal constructor(
            val retryCount: Int,
            val clientOption: Any? = null
        ) : State()

        data class Opened internal constructor(
            val clientOption: Any? = null,
            val serverOption: Any? = null,
            val topics: Set<Topic>  = emptySet()
        ) : State()

        data class Closing internal constructor(
            val clientOption: Any? = null
        ) : State()

        data class Closed internal constructor(
            val clientOption: Any? = null,
            val serverOption: Any? = null
        ) : State()

        data class WillOpen internal constructor(
            val retryCount: Int
        ) : State()

        object Destroyed : State()
    }

    sealed class Event {

        // resume

        data class OnMessageEnqueued(val message: Message, val option: Any?) : Event()

        data class OnMessageSent(val message: Message, val option: Any?) : Event()

        data class OnMessageDelivered(val message: Message, val option: Any?) : Event()
    }

    sealed class SideEffect {
        data class SendMessage(
            val topic: Topic,
            val message: Message,
            val option: Any? = null
        ) : SideEffect()


        // maybe side effect for the user?

    }

}
