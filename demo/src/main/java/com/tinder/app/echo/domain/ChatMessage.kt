/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.echo.domain

import android.graphics.Bitmap
import org.joda.time.DateTime

sealed class ChatMessage {
    abstract val id: Int
    abstract val source: Source
    abstract val timestamp: DateTime

    data class Text(
        override val id: Int,
        val value: String,
        override val source: Source,
        override val timestamp: DateTime = DateTime.now()
    ) : ChatMessage()

    data class Image(
        override val id: Int,
        val bitmap: Bitmap,
        override val source: Source,
        override val timestamp: DateTime = DateTime.now()
    ) : ChatMessage()

    enum class Source {
        SENT, RECEIVED
    }
}
