package com.tinder.scarlet.messageadapter.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

class JacksonMessageAdapter<T> private constructor(
    private val objectMapper: ObjectMapper,
    private val klass: Class<T>
) : MessageAdapter<T> {

    override fun fromMessage(message: Message): T {
        return when (message) {
            is Message.Text -> objectMapper.readValue(message.value, klass)
            is Message.Bytes -> objectMapper.readValue(message.value, klass)
        }
    }

    override fun toMessage(data: T): Message {
        val body = objectMapper.writeValueAsString(data)
        return Message.Text(body)
    }

    class Factory constructor(
        private val objectMapper: ObjectMapper = DEFAULT_OBJECT_MAPPER
    ) : MessageAdapter.Factory {
        override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> =
            JacksonMessageAdapter(objectMapper, type.getRawType())

        companion object {
            private val DEFAULT_OBJECT_MAPPER = ObjectMapper()
        }
    }
}
