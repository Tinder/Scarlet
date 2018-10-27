/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.websocket.echo.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import com.tinder.app.websocket.echo.api.EchoService
import com.tinder.app.websocket.echo.view.EchoBotViewModel
import com.tinder.scarlet.Event
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.State
import com.tinder.scarlet.websocket.WebSocketEvent
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import org.joda.time.DateTime
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ChatMessageRepository(
    private val echoService: EchoService
) {
    private val messageCount = AtomicInteger()
    private val messagesRef = AtomicReference<List<ChatMessage>>()
    private val messagesProcessor = BehaviorProcessor.create<List<ChatMessage>>()

    init {
        echoService.observeStateTransition()
            .observeOn(Schedulers.io())
            .subscribe({ stateTransition ->
                val event = stateTransition.event
                val description = when (event) {
                    is Event.OnLifecycleStateChange -> when (event.lifecycleState) {
                        LifecycleState.Started -> "\uD83C\uDF1D On Lifecycle Start"
                        LifecycleState.Stopped -> "\uD83C\uDF1A On Lifecycle Stop"
                        LifecycleState.Completed -> "\uD83D\uDCA5 On Lifecycle Terminate"
                    }
                    is Event.OnProtocolEvent -> {
                        when (stateTransition.toState) {
                            is State.WillConnect -> "\uD83D\uDCA4 WaitingToRetry"
                            is State.Connecting -> "⏳ Connecting"
                            is State.Connected -> "\uD83D\uDEEB Connected"
                            is State.Disconnecting -> "⏳ Disconnecting"
                            State.Disconnected -> "\uD83D\uDEEC Disconnected"
                            State.Destroyed -> "\uD83D\uDCA5 Destroyed"
                        }
                    }
                    Event.OnShouldConnect -> "⏰ On Retry"
                }
                val chatMessage =
                    ChatMessage.Text(generateMessageId(), description, ChatMessage.Source.RECEIVED)
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        echoService.observeWebSocketEvent()
            .observeOn(Schedulers.io())
            .subscribe({ event ->
                val description = when (event) {
                    is WebSocketEvent.OnConnectionOpened -> "\uD83D\uDEF0️ On WebSocket Connection Opened"
                    is WebSocketEvent.OnMessageReceived -> "\uD83D\uDEF0️ On WebSocket Message Received"
                    is WebSocketEvent.OnConnectionClosing -> "\uD83D\uDEF0️ On WebSocket Connection Closing"
                    is WebSocketEvent.OnConnectionClosed -> "\uD83D\uDEF0️ On WebSocket Connection Closed"
                    is WebSocketEvent.OnConnectionFailed -> "\uD83D\uDEF0️ On WebSocket Connection Failed"
                }
                val chatMessage =
                    ChatMessage.Text(generateMessageId(), description, ChatMessage.Source.RECEIVED)
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        echoService.observeText()
            .observeOn(Schedulers.io())
            .subscribe({ text ->
                val chatMessage = ChatMessage.Text(
                    generateMessageId(),
                    text,
                    ChatMessage.Source.RECEIVED,
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        echoService.observeBitmap()
            .observeOn(Schedulers.io())
            .subscribe({ bitmap ->
                val chatMessage = ChatMessage.Image(
                    generateMessageId(),
                    bitmap,
                    ChatMessage.Source.RECEIVED,
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })
    }

    fun observeChatMessage(): Flowable<List<ChatMessage>> = messagesProcessor

    fun addTextMessage(text: String) {
        Completable.fromAction {
            val chatMessage = ChatMessage.Text(generateMessageId(), text, ChatMessage.Source.SENT)
            addChatMessage(chatMessage)

            echoService.sendText(text)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe()
    }

    fun addImageMessage(imagePath: String, imageMaxWidth: Int) {
        Completable.fromAction {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val ratio = imageMaxWidth.toFloat() / Math.max(bitmap.width, bitmap.height)
            val (targetWidth, targetHeight) = (bitmap.width * ratio).toInt() to (bitmap.height * ratio).toInt()
            val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, targetWidth, targetHeight)
            val chatMessage = ChatMessage.Image(generateMessageId(), thumbnail, ChatMessage.Source.SENT)
            addChatMessage(chatMessage)

            echoService.sendBitmap(bitmap)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe()
    }

    fun clearAllMessages() {
        messagesRef.set(listOf())
        messagesProcessor.onNext(listOf())
    }

    private fun addChatMessage(chatMessage: ChatMessage) {
        val existingMessages = messagesRef.get() ?: listOf()
        val messages = existingMessages + chatMessage
        messagesRef.set(messages)
        messagesProcessor.onNext(messages)
    }

    private fun generateMessageId(): Int = messageCount.getAndIncrement()
}
