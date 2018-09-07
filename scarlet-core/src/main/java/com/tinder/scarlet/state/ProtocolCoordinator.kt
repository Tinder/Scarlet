/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.scarlet.Message
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.RequestFactory
import com.tinder.scarlet.Topic
import com.tinder.scarlet.state.utils.GroupWorker
import com.tinder.scarlet.state.utils.Worker

class ProtocolCoordinator(
    private val serviceLocator: ServiceLocator
) {
    private val protocolGroupWorker =
        GroupWorker<Unit, Unit, Protocol, Unit, Protocol, Unit>()

    fun start() {
        protocolGroupWorker.add(
            Unit,
            object :
                RequestFactory<Protocol> {
                override fun createRequest(): Protocol {
                    return serviceLocator.protocolFactory.create()
                }
            },
            object :
                RequestFactory<Protocol> {
                override fun createRequest(): Protocol {
                    return serviceLocator.protocolFactory.create()
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
                        open(ProtocolListener(this))
                    }
                }
                is Worker.SideEffect.StopWork -> {
                    sideEffect.request?.apply {
                        close()
                    }
                }
                is Worker.SideEffect.ForceStopWork -> {
                    sideEffect.request?.apply {
                        close()
                    }
                }
            }
        }
    }

    fun openAndRetry() {
        protocolGroupWorker.onLifecycleStarted(Unit)
    }

    fun close() {
        protocolGroupWorker.onLifecycleStopped()
    }

    private inner class ProtocolListener(
        private val protocol: Protocol
    ) : Protocol.Listener {
        override fun onProtocolOpened(
            request: Any?,
            response: Any?
        ) {
            protocolGroupWorker.onWorkStarted(Unit)
            serviceLocator.topicCoordinator.start(protocol)
            serviceLocator.messageCoordinator.start(protocol)
        }

        override fun onProtocolClosed(
            request: Any?,
            response: Any?
        ) {
            protocolGroupWorker.onWorkStopped(Unit)
            serviceLocator.topicCoordinator.stop()
            serviceLocator.messageCoordinator.stop()
        }

        override fun onProtocolFailed(error: Throwable) {
            protocolGroupWorker.onWorkFailed(Unit, error)
            serviceLocator.topicCoordinator.stop()
            serviceLocator.messageCoordinator.stop()
        }

        override fun onMessageReceived(
            topic: Topic,
            message: Message,
            serverMessageInfo: Any?
        ) {
            serviceLocator.clientStateCoordinator.receive(topic, message)
        }

        override fun onMessageSent(
            topic: Topic,
            message: Message,
            clientMessageInfo: Any?
        ) {
            serviceLocator.messageCoordinator.onMessageSent(topic, message)
        }

        override fun onMessageFailedToSend(
            topic: Topic,
            message: Message,
            clientMessageInfo: Any?
        ) {
            serviceLocator.messageCoordinator.onMessageFailedToSend(topic, message)

        }

        override fun onTopicSubscribed(topic: Topic) {
            serviceLocator.topicCoordinator.onTopicSubscribed(topic)
        }

        override fun onTopicUnsubscribed(topic: Topic) {
            serviceLocator.topicCoordinator.onTopicUnsubscribed(topic)
        }
    }
}
