/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.socketio

import com.tinder.scarlet.Message
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.ProtocolSpecificEvent
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

sealed class SocketIoEvent : ProtocolSpecificEvent {

    object OnConnectionOpened : SocketIoEvent()

    data class OnMessageReceived(
        val message: Message
    ) : SocketIoEvent()

    object OnConnectionClosed : SocketIoEvent()

    data class OnConnectionFailed(
        val throwable: Throwable
    ) : SocketIoEvent()

    class Adapter : ProtocolSpecificEventAdapter {
        override fun fromEvent(event: ProtocolEvent): ProtocolSpecificEvent {
            return when (event) {
                is ProtocolEvent.OnOpened -> {
                    SocketIoEvent.OnConnectionOpened
                }
                is ProtocolEvent.OnMessageReceived -> {
                    SocketIoEvent.OnMessageReceived(
                        event.message
                    )
                }
                is ProtocolEvent.OnClosed -> {
                    SocketIoEvent.OnConnectionClosed
                }
                is ProtocolEvent.OnFailed -> {
                    SocketIoEvent.OnConnectionFailed(event.throwable ?: Throwable())
                }
                else -> throw IllegalArgumentException()
            }
        }

        class Factory : ProtocolSpecificEventAdapter.Factory {
            override fun create(
                type: Type,
                annotations: Array<Annotation>
            ): ProtocolSpecificEventAdapter {
                val receivingClazz = type.getRawType()
                require(SocketIoEvent::class.java.isAssignableFrom(receivingClazz)) {
                    "Only subclasses of SocketIoEvent are supported"
                }
                return Adapter()
            }
        }
    }
}
