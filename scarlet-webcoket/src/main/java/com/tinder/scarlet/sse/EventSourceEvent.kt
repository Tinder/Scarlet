/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.sse

import com.tinder.scarlet.Message
import com.tinder.scarlet.utils.getRawType
import com.tinder.scarlet.v2.Protocol
import okhttp3.Response
import okhttp3.sse.EventSource
import java.lang.reflect.Type

sealed class EventSourceEvent {
    data class OnConnectionOpened(val eventSource: EventSource, val okHttpResponse: Response) : EventSourceEvent()

    data class OnMessageReceived(val message: Message) : EventSourceEvent()

    object OnConnectionClosed : EventSourceEvent()

    data class OnConnectionFailed(val throwable: Throwable) : EventSourceEvent()

    class Adapter : Protocol.EventAdapter<EventSourceEvent> {
        override fun fromEvent(event: Protocol.Event): EventSourceEvent {
            return when (event) {
                is Protocol.Event.OnOpened -> {
                    val response = event.response as OkHttpEventSource.OpenResponse
                    EventSourceEvent.OnConnectionOpened(response.eventSource, response.okHttpResponse)
                }
                is Protocol.Event.OnMessageReceived -> {
                    EventSourceEvent.OnMessageReceived(event.message)
                }
                is Protocol.Event.OnClosed -> {
                    EventSourceEvent.OnConnectionClosed
                }
                is Protocol.Event.OnFailed -> {
                    EventSourceEvent.OnConnectionFailed(event.throwable ?: Throwable())
                }
                else -> throw IllegalArgumentException()
            }
        }

        class Factory : Protocol.EventAdapter.Factory {
            override fun create(type: Type, annotations: Array<Annotation>): Protocol.EventAdapter<*> {
                val receivingClazz = type.getRawType()
                require(EventSourceEvent::class.java.isAssignableFrom(receivingClazz)) {
                    "Only subclasses of EventSourceEvent are supported"
                }
                return Adapter()
            }
        }
    }
}
