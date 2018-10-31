/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.websocket.echo.domain

import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor

class AuthStatusRepository {

    private val authStatusProcessor = BehaviorProcessor.createDefault<AuthStatus>(AuthStatus.LOGGED_IN)

    fun observeAuthStatus(): Flowable<AuthStatus> = authStatusProcessor

    fun toggleAuthStatus() {
        val newAuthStatus = when (getAuthStatus()) {
            AuthStatus.LOGGED_IN -> AuthStatus.LOGGED_OUT
            AuthStatus.LOGGED_OUT -> AuthStatus.LOGGED_IN
        }
        updateAuthStatus(newAuthStatus)
    }

    private fun getAuthStatus(): AuthStatus = authStatusProcessor.value!!

    private fun updateAuthStatus(authStatus: AuthStatus) = authStatusProcessor.onNext(authStatus)
}
