/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.echo.presenter

import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import com.tinder.app.echo.domain.AuthStatus
import com.tinder.app.echo.domain.AuthStatusRepository
import com.tinder.app.echo.domain.ChatMessageRepository
import com.tinder.app.echo.target.EchoBotTarget
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class EchoBotPresenter @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
    private val authStatusRepository: AuthStatusRepository
) {

    private var targetState: TargetState = TargetState.MightBeBehind(0)
    private lateinit var target: EchoBotTarget

    private val compositeDisposable = CompositeDisposable()

    fun takeTarget(target: EchoBotTarget, displayingMessageCount: Int) {
        this.target = target
        this.targetState = TargetState.MightBeBehind(displayingMessageCount)
        setup()
    }

    fun dropTarget() {
        compositeDisposable.clear()
    }

    private fun setup() {
        val chatMessageSubscription = chatMessageRepository.observeChatMessage()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ chatMessages ->
                val targetState = targetState
                when (targetState) {
                    is TargetState.MightBeBehind -> {
                        if (targetState.messageCount == 0) {
                            target.setMessages(chatMessages)
                        } else {
                            for (i in targetState.messageCount until chatMessages.size) {
                                target.addMessage(chatMessages[i])
                            }
                        }
                        this.targetState = TargetState.UpToDate
                    }
                    TargetState.UpToDate -> {
                        if (chatMessages.isEmpty()) {
                            target.clearAllMessages()
                        } else {
                            target.addMessage(chatMessages.last())
                        }
                    }
                }
            }, { e ->
                Timber.e(e)
            })

        val authStatusSubscription = authStatusRepository.observeAuthStatus()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                when (it) {
                    AuthStatus.LOGGED_IN -> target.showLoggedIn()
                    AuthStatus.LOGGED_OUT -> target.showLoggedOut()
                }
            }, { e ->
                Timber.e(e)
            })

        compositeDisposable.addAll(chatMessageSubscription, authStatusSubscription)
    }

    fun sendText(text: String) {
        Completable.fromAction { chatMessageRepository.addTextMessage(text) }
            .subscribeOn(Schedulers.computation())
            .subscribe()
    }

    fun sendImage(imagePath: String) {
        Completable.fromAction {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val ratio = IMAGE_MAX_WIDTH.toFloat() / Math.max(bitmap.width, bitmap.height)
            val (targetWidth, targetHeight) = (bitmap.width * ratio).toInt() to (bitmap.height * ratio).toInt()
            val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, targetWidth, targetHeight)
            chatMessageRepository.addImageMessage(thumbnail)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe()
    }

    fun clearAllMessages() {
        chatMessageRepository.clearAllMessages()
    }

    fun toggleAuthStatus() {
        val newAuthStatus = when (authStatusRepository.getAuthStatus()) {
            AuthStatus.LOGGED_IN -> AuthStatus.LOGGED_OUT
            AuthStatus.LOGGED_OUT -> AuthStatus.LOGGED_IN
        }
        authStatusRepository.updateAuthStatus(newAuthStatus)
    }

    sealed class TargetState {
        data class MightBeBehind(val messageCount: Int) : TargetState()
        object UpToDate : TargetState()
    }

    private companion object {
        private const val IMAGE_MAX_WIDTH = 200
    }
}
