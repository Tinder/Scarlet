/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Message
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.RequestFactory
import com.tinder.scarlet.Topic

class Coordinator(
    val lifecycle: Lifecycle,
    val protocolFactory: Protocol.Factory
) {

    private lateinit var clientStateCoordinator: ClientStateCoordinator
    private lateinit var protocolCoordinator: ProtocolCoordinator
    private lateinit var topicCoordinator: TopicCoordinator
    private lateinit var messageCoordinator: MessageCoordinator

    fun start() {
        protocolCoordinator.start()
    }

    fun observeEvent() {

    }

    fun publish(topic: Topic, message: Message) {
        clientStateCoordinator.send(topic, message)
    }

    fun connect() {
        clientStateCoordinator.open()
    }

    fun disconnect() {
        clientStateCoordinator.close()
    }

    fun subscribe(topic: Topic) {
        clientStateCoordinator.subscribe(topic)
    }

    fun unsubscribe(topic: Topic) {
        clientStateCoordinator.unsubscribe(topic)
    }

    private inner class ClientStateCoordinator {
        private val clientStateMachine = Client.create { transition ->
            val sideEffect = transition.sideEffect!!
            when (sideEffect) {
                is Client.SideEffect.SendMessage -> {
                    messageCoordinator.sendAndRetry(sideEffect.topic, sideEffect.message)
                }
                is Client.SideEffect.FinishSendingMessage -> {
                    messageCoordinator.clear(sideEffect.topic, sideEffect.message)
                }

                is Client.SideEffect.ReceiveMessage -> {
                    // TODO write to subject?
                }
                is Client.SideEffect.Open -> {
                    protocolCoordinator.openAndRetry()
                    topicCoordinator.subscribeAndRetry(sideEffect.topics)
                    messageCoordinator.sendAndRetry(sideEffect.messages)

                }
                is Client.SideEffect.Close -> {
                    protocolCoordinator.close()
                    topicCoordinator.unsubscribe(sideEffect.topics)
                    messageCoordinator.clear(sideEffect.messages)
                }
                is Client.SideEffect.Subscribe -> {
                    topicCoordinator.subscribeAndRetry(sideEffect.topic)
                }
                is Client.SideEffect.Unsubscribe -> {
                    topicCoordinator.unsubscribe(sideEffect.topic)
                }
            }
        }

        fun send(topic: Topic, message: Message) {
            clientStateMachine.transition(Client.Event.OnShouldSendMessage(topic, message))
        }

        fun finishSending(topic: Topic, message: Message) {
            clientStateMachine.transition(Client.Event.OnShouldFinishSendingMessage(topic, message))
        }


        fun receive(topic: Topic, message: Message) {
            clientStateMachine.transition(Client.Event.OnShouldReceiveMessage(topic, message))
        }

        fun open() {
            clientStateMachine.transition(Client.Event.OnShouldOpen)
        }

        fun close() {
            clientStateMachine.transition(Client.Event.OnShouldClose)
        }

        fun subscribe(topic: Topic) {
            clientStateMachine.transition(Client.Event.OnShouldSubscribe(topic))
        }

        fun unsubscribe(topic: Topic) {
            clientStateMachine.transition(Client.Event.OnShouldUnsubscribe(topic))
        }

    }

    private inner class ProtocolCoordinator {
        private val protocolGroupWorker = GroupWorker<Unit, Context, Unit, Context, Unit>()

        fun start() {
            protocolGroupWorker.add(
                Unit,
                object : RequestFactory<Context> {
                    override fun createRequest(): Context {
                        return Context(protocolFactory.create())
                    }
                },
                object : RequestFactory<Context> {
                    override fun createRequest(): Context {
                        return Context(protocolFactory.create())
                    }
                }
            ) { transition ->
                val sideEffect = transition.sideEffect!!
                when (sideEffect) {
                    is Worker.SideEffect.ScheduleRetry -> {
                        // TODO timer
                    }
                    is Worker.SideEffect.UnscheduleRetry -> {
                        // TODO timer
                    }
                    is Worker.SideEffect.StartWork -> {
                        sideEffect.request?.apply {
                            protocol.open(ProtocolListener())
                        }
                    }
                    is Worker.SideEffect.StopWork -> {
                        sideEffect.request?.apply {
                            protocol.close()
                        }
                    }
                    is Worker.SideEffect.ForceStopWork -> {
                        sideEffect.request?.apply {
                            protocol.close()
                        }
                    }
                }
            }
        }

        fun openAndRetry() {
            protocolGroupWorker.onLifecycleStarted()
        }

        fun close() {
            protocolGroupWorker.onLifecycleStopped()
        }

        private inner class ProtocolListener : Protocol.Listener {
            override fun onProtocolOpened(
                request: Any?,
                response: Any?
            ) {
                protocolGroupWorker.onWorkStarted(Unit)
                topicCoordinator.start()
                messageCoordinator.start()
            }

            override fun onProtocolClosed(
                request: Any?,
                response: Any?
            ) {
                protocolGroupWorker.onWorkStopped(Unit)
                topicCoordinator.stop()
                messageCoordinator.stop()
            }

            override fun onProtocolFailed(error: Throwable) {
                protocolGroupWorker.onWorkFailed(Unit, error)
                topicCoordinator.stop()
                messageCoordinator.stop()
            }

            override fun onMessageReceived(
                topic: Topic,
                message: Message,
                serverMessageInfo: Any?
            ) {
                clientStateCoordinator.receive(topic, message)
            }

            override fun onMessageSent(
                topic: Topic,
                message: Message,
                clientMessageInfo: Any?
            ) {
                messageCoordinator.onMessageSent(topic, message)
            }

            override fun onMessageFailedToSend(
                topic: Topic,
                message: Message,
                clientMessageInfo: Any?
            ) {
                messageCoordinator.onMessageFailedToSend(topic, message)

            }

            override fun onTopicSubscribed(topic: Topic) {
                topicCoordinator.onTopicSubscribed(topic)
            }

            override fun onTopicUnsubscribed(topic: Topic) {
                topicCoordinator.onTopicUnsubscribed(topic)
            }
        }
    }

    private inner class TopicCoordinator {
        private val topicGroupWorker = GroupWorker<Topic, Context, Unit, Context, Unit>()

        fun start(context: Context) {
            topicGroupWorker.onLifecycleStarted()
        }

        fun stop() {
            topicGroupWorker.onLifecycleStopped()
        }

        fun subscribeAndRetry(topic: Topic) {
            topicGroupWorker.add(
                topic,
                object : RequestFactory<Context> {
                    override fun createRequest(): Context {
                        return Context(protocolFactory.create())
                    }
                },
                object : RequestFactory<Context> {
                    override fun createRequest(): Context {
                        return Context(protocolFactory.create())
                    }
                }
            ) { transition ->
                val sideEffect = transition.sideEffect!!
                when (sideEffect) {
                    is Worker.SideEffect.ScheduleRetry -> {
                        // TODO timer
                    }
                    is Worker.SideEffect.UnscheduleRetry -> {
                        // TODO timer
                    }
                    is Worker.SideEffect.StartWork -> {
                        // TODO make clientOption the protocol?

                        protocol.subscribe(sideEffect.request as Topic, null)
                    }
                    is Worker.SideEffect.StopWork -> {
                        protocol.unsubscribe(sideEffect.option as Topic, null)
                    }
                    is Worker.SideEffect.ForceStopWork -> {
                        protocol.unsubscribe(sideEffect.option as Topic, null)
                    }
                }
            }
        }

        fun subscribeAndRetry(topics: Set<Topic>) {
            topics.forEach { topic ->
                subscribeAndRetry(topic)
            }
        }

        fun unsubscribe(topic: Topic) {
            topicGroupWorker.remove(topic)
        }

        fun unsubscribe(topics: Set<Topic>) {
            topics.forEach { topic -> unsubscribe(topic) }
        }

        fun onTopicSubscribed(topic: Topic) {
            topicGroupWorker.onWorkStarted(topic)
        }

        fun onTopicUnsubscribed(topic: Topic) {
            topicGroupWorker.onWorkStopped(topic)
        }
    }

    private inner class MessageCoordinator {
        private val messageGroupWorker = GroupWorker<Pair<Topic, Message>, Unit, Unit, Unit, Unit>()

        fun start(context: Context) {
            messageGroupWorker.onLifecycleStarted()
        }

        fun stop() {
            messageGroupWorker.onLifecycleStopped()
        }

        fun sendAndRetry(topic: Topic, message: Message) {
            messageGroupWorker.add(topic to message, Unit, Unit) {
                val sideEffect = it.sideEffect!!
                when (sideEffect) {
                    is Worker.SideEffect.ScheduleRetry -> {
                        // TODO timer
                    }
                    is Worker.SideEffect.UnscheduleRetry -> {
                        // TODO timer
                    }
                    is Worker.SideEffect.StartWork -> {
                        protocol.send(topic, message, null)
                    }
                    is Worker.SideEffect.StopWork -> {
                        clientStateCoordinator.finishSending(topic, message)
                    }
                }
            }
        }

        fun sendAndRetry(messages: Map<Topic, List<Message>>) {
            messages.forEach { (topic, messagesInTopic) ->
                messagesInTopic.forEach { message ->
                    sendAndRetry(topic, message)
                }
            }
        }

        fun clear(topic: Topic, message: Message) {
            messageGroupWorker.remove(topic to message)
        }

        fun clear(messages: Map<Topic, List<Message>>) {
            messages.forEach { (topic, messagesInTopic) ->
                messagesInTopic.forEach {
                    clear(topic, it)
                }
            }
        }

        fun onMessageSent(topic: Topic, message: Message) {
            messageGroupWorker.onWorkStopped(topic to message)
        }

        fun onMessageFailedToSend(topic: Topic, message: Message) {
            messageGroupWorker.onWorkFailed(topic to message, RuntimeException())
        }
    }

    data class Context(
        val protocol: Protocol,
        val topic: Topic? = null,
        val message: Message? = null
    )
}
