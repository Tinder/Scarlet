/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.gson

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import okio.Buffer
import java.io.OutputStreamWriter
import java.io.StringReader
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets.UTF_8

/**
 * A [message adapter][MessageAdapter] that uses Gson.
 */
class GsonMessageAdapter<T> private constructor(
    private val gson: Gson,
    private val typeAdapter: TypeAdapter<T>
) : MessageAdapter<T> {

    override fun fromMessage(message: Message): T {
        val stringValue = when (message) {
            is Message.Text -> message.value
            is Message.Bytes -> String(message.value)
        }
        val jsonReader = gson.newJsonReader(StringReader(stringValue))
        return typeAdapter.read(jsonReader)!!
    }

    override fun toMessage(data: T): Message {
        val buffer = Buffer()
        val writer = OutputStreamWriter(buffer.outputStream(), UTF_8)
        val jsonWriter = gson.newJsonWriter(writer)
        typeAdapter.write(jsonWriter, data)
        jsonWriter.close()
        val stringValue = buffer.readByteString().utf8()
        return Message.Text(stringValue)
    }

    class Factory(
        private val gson: Gson = DEFAULT_GSON
    ) : MessageAdapter.Factory {

        override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
            val typeAdapter = gson.getAdapter(TypeToken.get(type))
            return GsonMessageAdapter(gson, typeAdapter)
        }

        companion object {
            private val DEFAULT_GSON = Gson()
        }
    }
}
