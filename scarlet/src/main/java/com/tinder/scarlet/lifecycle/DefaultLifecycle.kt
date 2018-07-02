/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle

internal class DefaultLifecycle(
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()
) : Lifecycle by lifecycleRegistry {

    init {
        lifecycleRegistry.onNext(Lifecycle.State.Started)
    }
}
