/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.socketio.chatroom.domain

import com.tinder.app.socketio.chatroom.api.ChatRoomService
import com.tinder.app.socketio.chatroom.api.NewMessageTopic
import com.tinder.app.socketio.chatroom.api.TypingStartedTopic
import com.tinder.app.socketio.chatroom.api.TypingStoppedTopic
import com.tinder.app.socketio.chatroom.api.UserJoinedTopic
import com.tinder.app.socketio.chatroom.api.UserLeftTopic
import com.tinder.app.socketio.chatroom.api.model.NewMessageUpdate
import com.tinder.app.socketio.chatroom.domain.model.ChatMessage
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
    private val chatRoomService: ChatRoomService,
    private val newMessageTopic: NewMessageTopic,
    private val typingStartedTopic: TypingStartedTopic,
    private val typingStoppedTopic: TypingStoppedTopic,
    private val userJoinedTopic: UserJoinedTopic,
    private val userLeftTopic: UserLeftTopic
) {
    private val messageCount = AtomicInteger()
    private val messagesRef = AtomicReference<List<ChatMessage>>()
    private val messagesProcessor = BehaviorProcessor.create<List<ChatMessage>>()

    init {
//        chatRoomService.observeStateTransition()
//            .observeOn(Schedulers.io())
//            .subscribe({ stateTransition ->
//                val event = stateTransition.event
//                val description = when (event) {
//                    is Event.OnLifecycleStateChange -> when (event.lifecycleState) {
//                        LifecycleState.Started -> "\uD83C\uDF1D On Lifecycle Start"
//                        LifecycleState.Stopped -> "\uD83C\uDF1A On Lifecycle Stop"
//                        LifecycleState.Completed -> "\uD83D\uDCA5 On Lifecycle Terminate"
//                    }
//                    is Event.OnProtocolEvent -> {
//                        when (stateTransition.toState) {
//                            is State.WillConnect -> "\uD83D\uDCA4 WaitingToRetry"
//                            is State.Connecting -> "⏳ Connecting"
//                            is State.Connected -> "\uD83D\uDEEB Connected"
//                            is State.Disconnecting -> "⏳ Disconnecting"
//                            State.Disconnected -> "\uD83D\uDEEC Disconnected"
//                            State.Destroyed -> "\uD83D\uDCA5 Destroyed"
//                        }
//                    }
//                    Event.OnShouldConnect -> "⏰ Should Connect"
//                }
//                val chatMessage =
//                    ChatMessage.Text(generateMessageId(), description, ChatMessage.Source.RECEIVED)
//                addChatMessage(chatMessage)
//            }, { e ->
//                Timber.e(e)
//            })
//
//        echoService.observeWebSocketEvent()
//            .observeOn(Schedulers.io())
//            .subscribe({ event ->
//                val description = when (event) {
//                    is WebSocketEvent.OnConnectionOpened -> "\uD83D\uDEF0️ On WebSocket Connection Opened"
//                    is WebSocketEvent.OnMessageReceived -> "\uD83D\uDEF0️ On WebSocket NewMessageUpdate Received"
//                    is WebSocketEvent.OnConnectionClosing -> "\uD83D\uDEF0️ On WebSocket Connection Closing"
//                    is WebSocketEvent.OnConnectionClosed -> "\uD83D\uDEF0️ On WebSocket Connection Closed"
//                    is WebSocketEvent.OnConnectionFailed -> "\uD83D\uDEF0️ On WebSocket Connection Failed"
//                }
//                val chatMessage =
//                    ChatMessage.Text(generateMessageId(), description, ChatMessage.Source.RECEIVED)
//                addChatMessage(chatMessage)
//            }, { e ->
//                Timber.e(e)
//            })

        newMessageTopic.observeNewMessage()
            .observeOn(Schedulers.io())
            .subscribe({ newMessage ->
                val chatMessage = ChatMessage(
                    generateMessageId(),
                    newMessage.message,
                    ChatMessage.Source.Received(newMessage.username),
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        typingStartedTopic.observeTypingStarted()
            .observeOn(Schedulers.io())
            .subscribe({ newMessage ->
                val chatMessage = ChatMessage(
                    generateMessageId(),
                    "Started typing",
                    ChatMessage.Source.Received(newMessage.username),
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        typingStoppedTopic.observeTypingStopped()
            .observeOn(Schedulers.io())
            .subscribe({ newMessage ->
                val chatMessage = ChatMessage(
                    generateMessageId(),
                    "Stopped typing",
                    ChatMessage.Source.Received(newMessage.username),
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

    }

    fun observeChatMessage(): Flowable<List<ChatMessage>> = messagesProcessor

    fun addNewMessage(text: String) {
        Completable.fromAction {
            val chatMessage = ChatMessage(generateMessageId(), text, ChatMessage.Source.Sent)
            addChatMessage(chatMessage)

            newMessageTopic.sendNewMessage(NewMessageUpdate(USER_NAME, text))
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

private const val USER_NAME = "scarlet"