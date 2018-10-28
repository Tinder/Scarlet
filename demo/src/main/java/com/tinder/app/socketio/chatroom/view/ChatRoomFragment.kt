package com.tinder.app.socketio.chatroom.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.Observer
import com.github.bassaer.chatmessageview.model.ChatUser
import com.github.bassaer.chatmessageview.model.Message
import com.github.bassaer.chatmessageview.view.ChatView
import com.tinder.R
import com.tinder.app.socketio.chatroom.domain.model.ChatMessage
import io.reactivex.Flowable
import org.koin.android.ext.android.inject
import java.util.Locale
import java.util.concurrent.TimeUnit

class ChatRoomFragment : Fragment() {

    private val viewModel: ChatRoomViewModel by inject()
    private lateinit var chatView: ChatView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatroom, container, false) as View

        chatView = view.findViewById(R.id.chat_view)
        with(chatView) {
            setOnClickSendButtonListener(android.view.View.OnClickListener { sendMessage() })
            setOnClickOptionButtonListener(android.view.View.OnClickListener { })
            setOnIconClickListener(object : Message.OnIconClickListener {
                override fun onIconClick(message: Message) {
                }
            })
            setOnIconLongClickListener(object : Message.OnIconLongClickListener {
                override fun onIconLongClick(message: Message) {
                }
            })
            setOnBubbleClickListener(object : Message.OnBubbleClickListener {
                override fun onClick(message: Message) {
                }
            })
            setOnBubbleLongClickListener(object : Message.OnBubbleLongClickListener {
                override fun onLongClick(message: Message) {
                }
            })
            addInputChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.handleTyping()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

            })
        }

        viewModel.chatMessages
            .observe(this, Observer<List<ChatMessage>> { chatMessages ->
                val numberOfMessages = chatView.getMessageView().messageList.size
                if (numberOfMessages == 0) {
                    setMessages(chatMessages)
                } else {
                    for (i in numberOfMessages until chatMessages.size) {
                        addMessage(chatMessages[i])
                    }
                }
            })

        LiveDataReactiveStreams.fromPublisher(Flowable.fromPublisher(
            LiveDataReactiveStreams.toPublisher(
                this,
                viewModel.typingSignal
            )
        )
            .filter { it }
            .debounce(TYPING_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
            .observe(this, Observer<Boolean> { isTyping ->
                viewModel.handleTypingStopped()
            })

        return view
    }

    private fun setMessages(chatMessages: List<ChatMessage>) {
        chatView.getMessageView().init(chatMessages.map { it.toMessage() })
    }

    private fun addMessage(chatMessage: ChatMessage) {
        val message = chatMessage.toMessage()
        with(chatView) {
            when (chatMessage.source) {
                ChatMessage.Source.Sent -> send(message)
                is ChatMessage.Source.Received -> receive(message)
            }
        }
    }

    private fun sendMessage() {
        viewModel.sendText(chatView.inputText)
        chatView.inputText = ""
    }

    companion object {
        private const val TYPING_TIMEOUT_MILLIS = 600L

        private val AVATAR = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
            .apply {
                val canvas = Canvas(this)
                canvas.drawColor(Color.rgb(255, 36, 0))
            }

        private val DEMO_USER = ChatUser(0, "Me", AVATAR)

        private fun ChatMessage.toMessage(): Message = Message.Builder()
            .setCreatedAt(timestamp.toCalendar(Locale.getDefault()))
            .apply {
                setMessageText(value)
                when (source) {
                    ChatMessage.Source.Sent -> setUser(DEMO_USER)
                        .setRightMessage(true)
                        .hideIcon(true)
                    is ChatMessage.Source.Received -> {
                        val user = ChatUser(source.username.hashCode(), source.username, AVATAR)
                        setUser(user)
                            .setRightMessage(false)
                    }
                }
            }
            .build()
    }
}