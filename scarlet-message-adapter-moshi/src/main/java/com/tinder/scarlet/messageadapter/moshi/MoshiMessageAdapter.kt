/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.Moshi
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import okio.ByteString
import java.lang.reflect.Type

/**
 * A [message adapter][MessageAdapter] that uses Moshi.
 */
class MoshiMessageAdapter<T> private constructor(
    private val jsonAdapter: JsonAdapter<T>
) : MessageAdapter<T> {

    override fun fromMessage(message: Message): T {
        val stringValue = when (message) {
            is Message.Text -> message.value
            is Message.Bytes -> {
                val byteString = ByteString.of(message.value, 0, message.value.size)
                // Moshi has no document-level API so the responsibility of BOM skipping falls to whatever is delegating
                // to it. Since it's a UTF-8-only library as well we only honor the UTF-8 BOM.
                if (byteString.startsWith(UTF8_BOM)) {
                    byteString.substring(UTF8_BOM.size()).utf8()
                } else {
                    byteString.utf8()
                }
            }
        }
        return jsonAdapter.fromJson(stringValue)!!
    }

    override fun toMessage(data: T): Message {
        val stringValue = jsonAdapter.toJson(data)
        return Message.Text(stringValue)
    }

    class Factory constructor(
        private val moshi: Moshi = DEFAULT_MOSHI,
        private val config: Config = Config()
    ) : MessageAdapter.Factory {

        override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
            val jsonAnnotations = filterJsonAnnotations(annotations)
            var adapter = moshi.adapter<Any>(type, jsonAnnotations)

            with(config) {
                if (lenient) {
                    adapter = adapter.lenient()
                }
                if (serializeNull) {
                    adapter = adapter.serializeNulls()
                }
                if (failOnUnknown) {
                    adapter = adapter.failOnUnknown()
                }
            }

            return MoshiMessageAdapter(adapter)
        }

        private fun filterJsonAnnotations(annotations: Array<Annotation>): Set<Annotation> {
            return annotations
                .filter { it.annotationClass.java.isAnnotationPresent(JsonQualifier::class.java) }
                .toSet()
        }

        /**
         * Used to configure `moshi` adapters.
         *
         * @param lenient lenient when reading and writing.
         * @param serializeNull include null values into the serialized JSON.
         * @param failOnUnknown use [JsonAdapter.failOnUnknown] adapters.
         */
        data class Config(
            val lenient: Boolean = false,
            val serializeNull: Boolean = false,
            val failOnUnknown: Boolean = false
        )
    }

    private companion object {
        private val DEFAULT_MOSHI = Moshi.Builder().build()
        private val UTF8_BOM = ByteString.decodeHex("EFBBBF")
    }
}
