/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.echo.api

import android.graphics.Bitmap
import com.tinder.scarlet.Event
import com.tinder.scarlet.State
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow

interface EchoService {
    @Receive
    fun observeState(): Flow<State>

    @Receive
    fun observeEvent(): Flow<Event>

    @Receive
    fun observeText(): Flow<String>

    @Receive
    fun observeBitmap(): Flow<Bitmap>

    @Send
    fun sendText(message: String): Boolean

    @Send
    fun sendBitmap(message: Bitmap): Boolean
}
