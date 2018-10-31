/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.websocket.echo.domain

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.lifecycle.LifecycleRegistry

class LoggedInLifecycle private constructor(
    authStatusRepository: AuthStatusRepository,
    private val lifecycleRegistry: LifecycleRegistry
) : Lifecycle by lifecycleRegistry {

    constructor(authStatusRepository: AuthStatusRepository) : this(authStatusRepository,
        LifecycleRegistry()
    )

    init {
        authStatusRepository.observeAuthStatus()
            .map {
                when (it) {
                    AuthStatus.LOGGED_IN -> LifecycleState.Started
                    AuthStatus.LOGGED_OUT -> LifecycleState.Stopped
                }
            }
            .subscribe(lifecycleRegistry)
    }
}
