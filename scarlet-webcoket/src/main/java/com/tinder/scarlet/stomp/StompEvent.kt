/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.stomp

import com.tinder.scarlet.Message
import com.tinder.scarlet.utils.getRawType
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.websocket.OkHttpWebSocket
import com.tinder.scarlet.websocket.ShutdownReason
import okhttp3.Response
import okhttp3.WebSocket
import java.lang.reflect.Type

sealed class StompEvent {

}
