/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.sse

import com.tinder.scarlet.Message
import com.tinder.scarlet.utils.getRawType
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.ProtocolEvent
import okhttp3.Response
import okhttp3.sse.EventSource
import java.lang.reflect.Type

sealed class EventSourceEvent {
    data class OnConnectionOpened(val eventSource: EventSource, val okHttpResponse: Response) : EventSourceEvent()

    data class OnMessageReceived(val message: Message) : EventSourceEvent()

    object OnConnectionClosed : EventSourceEvent()

    data class OnConnectionFailed(val throwable: Throwable) : EventSourceEvent()

    class Adapter : ProtocolEvent.Adapter<EventSourceEvent> {
        override fun fromEvent(event: ProtocolEvent): EventSourceEvent {
            return when (event) {
                is ProtocolEvent.OnOpened -> {
                    val response = event.response as OkHttpEventSource.OpenResponse
                    EventSourceEvent.OnConnectionOpened(response.eventSource, response.okHttpResponse)
                }
                is ProtocolEvent.OnMessageReceived -> {
                    EventSourceEvent.OnMessageReceived(event.message)
                }
                is ProtocolEvent.OnClosed -> {
                    EventSourceEvent.OnConnectionClosed
                }
                is ProtocolEvent.OnFailed -> {
                    EventSourceEvent.OnConnectionFailed(event.throwable ?: Throwable())
                }
                else -> throw IllegalArgumentException()
            }
        }

        class Factory : ProtocolEvent.Adapter.Factory {
            override fun create(type: Type, annotations: Array<Annotation>): ProtocolEvent.Adapter<*> {
                val receivingClazz = type.getRawType()
                require(EventSourceEvent::class.java.isAssignableFrom(receivingClazz)) {
                    "Only subclasses of EventSourceEvent are supported"
                }
                return Adapter()
            }
        }
    }
}
