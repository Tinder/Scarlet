package com.tinder.app.socketio.chatroom.koin

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
import com.tinder.scarlet.socketio.SocketIoClient
import com.tinder.scarlet.socketio.SocketIoTopic
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val chatRoomModule = module {

    // TODO sub module private

    factory("default") {
        Scarlet.Configuration(
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory()),
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
        Scarlet(
            SocketIoTopic("add user"),
            get("default"),
            get(CHAT_ROOM_SCARLET)
        )
            .create<AddUserTopic>()
    }

    single {
        Scarlet(
            SocketIoTopic("new message"),
            get("default"),
            get(CHAT_ROOM_SCARLET)
        )
            .create<NewMessageTopic>()
    }

    single {
        Scarlet(
            SocketIoTopic("typing"),
            get("default"),
            get(CHAT_ROOM_SCARLET)
        )
            .create<TypingStartedTopic>()
    }

    single {
        Scarlet(
            SocketIoTopic("stop typing"),
            get("default"),
            get(CHAT_ROOM_SCARLET)
        )
            .create<TypingStoppedTopic>()
    }

    single {
        Scarlet(
            SocketIoTopic("user joined"),
            get("default"),
            get(CHAT_ROOM_SCARLET)
        )
            .create<UserJoinedTopic>()
    }

    single {
        Scarlet(
            SocketIoTopic("user left"),
            get("default"),
            get(CHAT_ROOM_SCARLET)
        )
            .create<UserLeftTopic>()
    }

    single { ChatMessageRepository(get(), get(), get(), get(), get(), get(), get()) }

    viewModel { ChatRoomViewModel(get()) }

}

private const val CHAT_SERVER_URL = "https://socket-io-chat.now.sh/"

private const val CHAT_ROOM_SCARLET = "chatroom scarlet"