/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.socketio.chatroom.koin

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tinder.app.socketio.chatroom.api.AddUserTopic
import com.tinder.app.socketio.chatroom.api.ChatRoomService
import com.tinder.app.socketio.chatroom.api.NewMessageTopic
import com.tinder.app.socketio.chatroom.api.TypingStartedTopic
import com.tinder.app.socketio.chatroom.api.TypingStoppedTopic
import com.tinder.app.socketio.chatroom.api.UserJoinedTopic
import com.tinder.app.socketio.chatroom.api.UserLeftTopic
import com.tinder.app.socketio.chatroom.domain.ChatMessageRepository
import com.tinder.app.socketio.chatroom.view.ChatRoomViewModel
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.socketio.client.SocketIoClient
import com.tinder.scarlet.socketio.client.SocketIoEventName
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val chatRoomModule = module {

    // TODO sub module private

    factory("default") {
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        Scarlet.Configuration(
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory(moshi)),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )
    }

    single(CHAT_ROOM_SCARLET) {
        Scarlet(
            SocketIoClient({ CHAT_SERVER_URL }),
            get("default")
        )
    }

    single {
        get<Scarlet>(CHAT_ROOM_SCARLET)
            .create<ChatRoomService>()
    }

    single {
        get<Scarlet>(CHAT_ROOM_SCARLET)
            .child(
                SocketIoEventName("add user"),
                get("default")
            )
            .create<AddUserTopic>()
    }

    single {
        get<Scarlet>(CHAT_ROOM_SCARLET)
            .child(
                SocketIoEventName("new message"),
                get("default")
            )
            .create<NewMessageTopic>()
    }

    single {
        get<Scarlet>(CHAT_ROOM_SCARLET)
            .child(
                SocketIoEventName("typing"),
                get("default")
            )
            .create<TypingStartedTopic>()
    }

    single {
        get<Scarlet>(CHAT_ROOM_SCARLET)
            .child(
                SocketIoEventName("stop typing"),
                get("default")
            )
            .create<TypingStoppedTopic>()
    }

    single {
        get<Scarlet>(CHAT_ROOM_SCARLET)
            .child(
                SocketIoEventName("user joined"),
                get("default")
            )
            .create<UserJoinedTopic>()
    }

    single {
        get<Scarlet>(CHAT_ROOM_SCARLET)
            .child(
                SocketIoEventName("user left"),
                get("default")
            )
            .create<UserLeftTopic>()
    }

    single { ChatMessageRepository(get(), get(), get(), get(), get(), get(), get()) }

    viewModel { ChatRoomViewModel(get()) }
}

private const val CHAT_SERVER_URL = "https://socket-io-chat.now.sh/"

private const val CHAT_ROOM_SCARLET = "chatroom scarlet"