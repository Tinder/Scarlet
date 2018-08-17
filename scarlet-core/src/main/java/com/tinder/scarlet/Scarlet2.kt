/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet


interface Scarlet2 {

    // event or state?
    sealed class ConnectionState {
        data class Opening internal constructor(
            val clientReason: ClientOpenReason
        ) : ConnectionState()

        data class Opened internal constructor(
            val clientReason: ClientOpenReason,
            val serverReason: ServerOpenReason,
            val topics: Set<Topic>
        ) : ConnectionState()

        data class Closing internal constructor(
            val clientReason: ClientCloseReason
        ) : ConnectionState()

        data class Closed internal constructor(
            val clientReason: ClientCloseReason,
            val serverReason: ServerCloseReason
        ) : ConnectionState()

        data class WaitingToRetry internal constructor(
            val retryCount: Int,
            val retryInMillis: Long
        ) : ConnectionState()

        object Destroyed : ConnectionState()
    }

    sealed class ClientEvent {

        data class OnLifecycleChanged(
            val lifecycleState: LifecycleState
        ) : ClientEvent()

        object OnTimerTick : ClientEvent()

        data class OnConnectionOpeningAcknowledged(
            val serverReason: ServerOpenReason
        ) : ClientEvent()

        data class OnConnectionClosingAcknowledged(
            val serverReason: ServerCloseReason
        ) : ClientEvent()

        data class OnConnectionFailed(
            val throwable: Throwable
        ) : ClientEvent()

        data class OnMessageReceived(val message: Message) : ClientEvent()

        data class OnMessageEnqueued(val message: Message) : ClientEvent()

        data class OnMessageSent(val message: Message) : ClientEvent()

        data class OnMessageDelivered(val message: Message) : ClientEvent()
    }

    sealed class SideEffect {
        object ScheduleTimerTick : SideEffect()


    }

    sealed class LifecycleState {
        object Started : LifecycleState()
        object Stopped : LifecycleState()
        object Destroyed : LifecycleState()
    }

    // data class MessageDeserialized<T>(val message: Deserialization<T>) : SideEffect()


    interface LifecycleFactory


    // user can receive Protocol.StateTransition => Message,
    // user can send Message through core


    // interpret the state
    interface StateTransitionAdapter {

    }

    interface WebSocketStateTransitionAdapter {

    }

    class WebSocketState {

    }



    interface Connection {

//        fun open(reason: ClientOpenReason, listener: Listener)
//        fun close(reason: ClientCloseReason)
//        fun send(topic: Topic, message: Message)
//        fun subscribe(topic: Topic)
//        fun unsubscribe(topic: Topic)

        interface Listener {
//            fun onConnectionOpened(reason: ServerOpenReason)
//            fun onConnectionClosed(reason: ServerCloseReason)
//            fun onConnectionFailed(reason: ServerCloseReason)

//            // Meta info
//            fun onMessageReceived(message: Message)
//            fun onMessageEnqueued(message: Message)
//            fun onMessageSent(message: Message)
//            fun onMessageDelivered(message: Message)
        }
    }


    // used to pass information around
    interface ClientOpenReason
    interface ClientCloseReason
    interface ServerOpenReason
    interface ServerCloseReason
    interface Topic {
        val id: String
    }

//    data class ConnectionStateTransition(
//        val from: ConnectionState,
//        val to: ConnectionState,
//        val event: Event,
//        val sideEffect: SideEffect?
//    )

    interface ReasonFactory {

    }


    // ConnectionStateTransition => WebSocketEvent

    // ConnectionStateTransitionInterpreter (look at EventMapper)

    // MessageConnectionStateTransitionInterpreter

    // MessageDeserializationConnectionStateTransitionInterpreter

    // LifecycleStateMessageConnectionStateTransitionInterpreter

    // StateConnectionStateTransitionInterpreter

    // WebSocketEventConnectionStateTransitionInterpreter

//    sealed class WebSocketEvent {
//        data class OnConnectionOpened(val webSocket: WebSocket) : WebSocketEvent()
//
//        data class OnMessageReceived(val message: Message) : WebSocketEvent()
//
//        data class OnConnectionClosing(val shutdownReason: ShutdownReason) : WebSocketEvent()
//
//        data class OnConnectionClosed(val shutdownReason: ShutdownReason) : WebSocketEvent()
//
//        data class OnConnectionFailed(val throwable: Throwable) : WebSocketEvent()
//    }

}
