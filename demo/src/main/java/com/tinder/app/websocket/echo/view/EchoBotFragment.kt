/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.websocket.echo.view

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.github.bassaer.chatmessageview.model.ChatUser
import com.github.bassaer.chatmessageview.model.Message
import com.github.bassaer.chatmessageview.view.ChatView
import com.tinder.R
import com.tinder.app.websocket.echo.domain.AuthStatus
import com.tinder.app.websocket.echo.domain.ChatMessage
import droidninja.filepicker.FilePickerBuilder
import droidninja.filepicker.FilePickerConst
import org.koin.android.ext.android.inject
import java.util.Locale

class EchoBotFragment : Fragment() {

    private val viewModel: EchoBotViewModel by inject()
    private lateinit var chatView: ChatView
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_echo_bot, container, false) as View

        chatView = view.findViewById(R.id.chat_view)
        with(chatView) {
            setOnClickSendButtonListener(View.OnClickListener { sendMessage() })
            setOnClickOptionButtonListener(View.OnClickListener { showImagePicker() })
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

        viewModel.authStatus
            .observe(this, Observer<AuthStatus> {
                when (it) {
                    AuthStatus.LOGGED_IN -> showLoggedIn()
                    AuthStatus.LOGGED_OUT -> showLoggedOut()
                }
            })

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        with(menu) {
            val authMenuItem = add(Menu.NONE, AUTH_MENU_ITEM_ID, Menu.NONE, LOGOUT_MENU_ITEM_TITLE)
            authMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

            val clearAllMenuItem =
                add(Menu.NONE, CLEAR_ALL_MENU_ITEM_ID, Menu.NONE, CLEAR_ALL_MENU_ITEM_TITLE)
            clearAllMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        this.menu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            AUTH_MENU_ITEM_ID -> viewModel.toggleAuthStatus()
            CLEAR_ALL_MENU_ITEM_ID -> viewModel.clearAllMessages()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FilePickerConst.REQUEST_CODE_PHOTO -> {
                if (resultCode != Activity.RESULT_OK || data == null) {
                    return
                }
                val photoPaths = mutableListOf<String>()
                photoPaths.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA))
                if (photoPaths.isEmpty()) {
                    return
                }
                viewModel.sendImage(photoPaths[0])
            }
        }
    }

    private fun addMessage(chatMessage: ChatMessage) {
        val message = chatMessage.toMessage()
        with(chatView) {
            when (chatMessage.source) {
                ChatMessage.Source.SENT -> send(message)
                ChatMessage.Source.RECEIVED -> receive(message)
            }
        }
    }

    private fun setMessages(chatMessages: List<ChatMessage>) {
        chatView.getMessageView().init(chatMessages.map { it.toMessage() })
    }

    private fun showLoggedIn() {
        val menu = menu ?: return
        val menuItem = menu.findItem(AUTH_MENU_ITEM_ID)
        menuItem.title = LOGOUT_MENU_ITEM_TITLE
    }

    private fun showLoggedOut() {
        val menu = menu ?: return
        val menuItem = menu.findItem(AUTH_MENU_ITEM_ID)
        menuItem.title = LOGIN_MENU_ITEM_TITLE
    }

    private fun sendMessage() {
        viewModel.sendText(chatView.inputText)
        chatView.inputText = ""
    }

    private fun showImagePicker() {
        FilePickerBuilder.getInstance().setMaxCount(1)
            .setActivityTheme(R.style.AppTheme)
            .pickPhoto(this)
    }

    companion object {
        private const val AUTH_MENU_ITEM_ID = 200
        private const val LOGIN_MENU_ITEM_TITLE = "Log in"
        private const val LOGOUT_MENU_ITEM_TITLE = "Log out"
        private const val CLEAR_ALL_MENU_ITEM_ID = 201
        private const val CLEAR_ALL_MENU_ITEM_TITLE = "Clear All"

        private val AVATAR = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
            .apply {
                val canvas = Canvas(this)
                canvas.drawColor(Color.rgb(255, 36, 0))
            }

        private val DEMO_USER = ChatUser(0, "Me", AVATAR)
        private val SCARLET_USER = ChatUser(1, "Scarlet", AVATAR)

        private fun ChatMessage.toMessage(): Message = Message.Builder()
            .setCreatedAt(timestamp.toCalendar(Locale.getDefault()))
            .apply {
                when (this@toMessage) {
                    is ChatMessage.Text -> setMessageText(value)
                    is ChatMessage.Image -> setPicture(bitmap)
                        .setType(Message.Type.PICTURE)
                }
                when (source) {
                    ChatMessage.Source.SENT -> setUser(DEMO_USER)
                        .setRightMessage(true)
                        .hideIcon(true)
                    ChatMessage.Source.RECEIVED -> setUser(SCARLET_USER)
                        .setRightMessage(false)
                }
            }
            .build()
    }
}
