/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.echo.domain

import com.tinder.app.echo.inject.EchoBotScope
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import javax.inject.Inject

@EchoBotScope
class AuthStatusRepository @Inject constructor() {

    private val authStatusProcessor = BehaviorProcessor.createDefault<AuthStatus>(AuthStatus.LOGGED_IN)

    fun getAuthStatus(): AuthStatus = authStatusProcessor.value!!

    fun observeAuthStatus(): Flowable<AuthStatus> = authStatusProcessor

    fun updateAuthStatus(authStatus: AuthStatus) = authStatusProcessor.onNext(authStatus)
}
