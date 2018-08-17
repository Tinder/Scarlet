/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet


interface Scarlet2 {

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
