/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.StateMachine
import com.tinder.scarlet.ConfigFactory
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Message
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.Topic

internal typealias ClientStateMachine = StateMachine<Client.State, Client.Event, Client.SideEffect>
internal typealias ConnectionStateMachine = StateMachine<Connection.State, Connection.Event, Connection.SideEffect>
internal typealias MessengerStateMachine = StateMachine<Messenger.State, Messenger.Event, Messenger.SideEffect>

class Coordinator(
    val configFactory: ConfigFactory,
    val lifecycle: Lifecycle,
    val protocolFactory: Protocol.Factory
) {

    private lateinit var clientStateMachine: ClientStateMachine
    private lateinit var connectionStateMachine: ConnectionStateMachine
    private val topicStateMachines: MutableMap<Topic, ConnectionStateMachine> =
        emptyMap<Topic, ConnectionStateMachine>().toMutableMap()
    private val messageStateMachines: MutableMap<Pair<Topic, Message>, MessengerStateMachine> =
        emptyMap<Pair<Topic, Message>, MessengerStateMachine>().toMutableMap()

    private lateinit var protocol: Protocol

    fun start() {
        connectionStateMachine = Connection.create(configFactory) { transition ->
            val sideEffect = transition.sideEffect!!
            when (sideEffect) {
                is Connection.SideEffect.ScheduleRetry -> {
                    // TODO timer
                }
                is Connection.SideEffect.OpenConnection -> {
                    protocol.open(object : Protocol.Listener {
                        override fun onProtocolOpened(clientOption: Any?, serverOption: Any?) {
                            connectionStateMachine.transition(
                                Connection.Event.OnConnectionOpened(
                                    clientOption,
                                    serverOption
                                )
                            )
                        }

                        override fun onProtocolClosed(clientOption: Any?, serverOption: Any?) {
                            connectionStateMachine.transition(
                                Connection.Event.OnConnectionClosed(
                                    clientOption,
                                    serverOption
                                )
                            )
                        }

                        override fun onProtocolFailed(error: Throwable) {
                            connectionStateMachine.transition(
                                Connection.Event.OnConnectionFailed(
                                    error
                                )
                            )
                        }

                        override fun onMessageReceived(
                            topic: Topic,
                            message: Message,
                            serverMessageInfo: Any?
                        ) {
                            clientStateMachine.transition(
                                Client.Event.OnShouldReceiveMessage(
                                    topic,
                                    message
                                )
                            )
                        }

                        override fun onMessageSent(
                            topic: Topic,
                            message: Message,
                            clientMessageInfo: Any?
                        ) {
                            messageStateMachines[(topic to message)]!!.transition(Messenger.Event.OnMessageSent)
                        }

                        override fun onMessageFailedToSend(
                            topic: Topic,
                            message: Message,
                            clientMessageInfo: Any?
                        ) {
                            messageStateMachines[(topic to message)]!!.transition(Messenger.Event.OnMessageFailed)
                        }

                        override fun onTopicSubscribed(topic: Topic) {
                            topicStateMachines[topic]!!.transition(
                                Connection.Event.OnConnectionOpened(
                                    null,
                                    null
                                )
                            )
                        }

                        override fun onTopicUnsubscribed(topic: Topic) {
                            topicStateMachines[topic]!!.transition(
                                Connection.Event.OnConnectionClosed(
                                    null,
                                    null
                                )
                            )
                        }
                    })
                }
                is Connection.SideEffect.CloseConnection -> {
                    protocol.close()
                }
                is Connection.SideEffect.ForceCloseConnection -> {
                    protocol.close()
                }
            }
        }

        // Topic connections
        connectionStateMachine = Connection.create(configFactory) { transition ->
            val sideEffect = transition.sideEffect!!
            when (sideEffect) {
                is Connection.SideEffect.ScheduleRetry -> {
                    // TODO timer
                }
                is Connection.SideEffect.OpenConnection -> {
                    protocol.subscribe(sideEffect.option as Topic, null)
                }
                is Connection.SideEffect.CloseConnection -> {
                    protocol.unsubscribe(sideEffect.option as Topic, null)
                }
                is Connection.SideEffect.ForceCloseConnection -> {
                    protocol.unsubscribe(sideEffect.option as Topic, null)
                }
            }
        }

        clientStateMachine = Client.create { transition ->
            val sideEffect = transition.sideEffect!!
            when (sideEffect) {
                is Client.SideEffect.SendMessage -> {
                    messageStateMachines[(sideEffect.topic to sideEffect.message)] =
                            Messenger.create(sideEffect.topic, sideEffect.message) {
                                val sideEffect2 = it.sideEffect!!
                                when (sideEffect2) {
                                    is Messenger.SideEffect.ScheduleRetry -> {
                                        // TODO timer
                                    }
                                    is Messenger.SideEffect.SendMessage -> {
                                        protocol.send(sideEffect.topic, sideEffect.message, null)
                                    }
                                    is Messenger.SideEffect.MarkAsSent -> {
                                        clientStateMachine.transition(
                                            Client.Event.OnShouldFinishSendingMessage(
                                                sideEffect.topic,
                                                sideEffect.message
                                            )
                                        )
                                    }
                                }
                            }
                }
                is Client.SideEffect.FinishSendingMessage -> {
                    messageStateMachines.remove(sideEffect.topic to sideEffect.message)

                }

                is Client.SideEffect.ReceiveMessage -> {
                    // TODO write to subject?
                }
                is Client.SideEffect.OpenAndSubscribe -> {
                    connectionStateMachine.transition(Connection.Event.OnLifecycleStarted)
                    // TODO topicStateMachine.transition(Connection.Event.OnLifecycleStarted)
                }
                is Client.SideEffect.CloseAndUnsubscribe -> {
                    connectionStateMachine.transition(Connection.Event.OnLifecycleStopped)
                    // TODO topicStateMachine.transition(Connection.Event.OnLifecycleStopped)
                }
                is Client.SideEffect.Subscribe -> {
//                    topicStateMachines[sideEffect.topic] = Connection.create()
                    topicStateMachines[sideEffect.topic]!!.transition(Connection.Event.OnLifecycleStarted)
                }
                is Client.SideEffect.Unsubscribe -> {
                    topicStateMachines[sideEffect.topic]!!.transition(Connection.Event.OnLifecycleStopped)
                }
            }
        }
    }

    fun observeEvent() {

    }

    fun publish(topic: Topic, message: Message) {
        clientStateMachine.transition(Client.Event.OnShouldSendMessage(topic, message))
    }

    fun connect() {
        clientStateMachine.transition(Client.Event.OnShouldOpen)
    }

    fun disconnect() {
        clientStateMachine.transition(Client.Event.OnShouldClose)
    }

    fun subscribe(topic: Topic) {
        clientStateMachine.transition(Client.Event.OnShouldSubscribe(topic))
    }

    fun unsubscribe(topic: Topic) {
        clientStateMachine.transition(Client.Event.OnShouldUnsubscribe(topic))
    }

}
