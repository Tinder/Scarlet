/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.lifecycle

import com.tinder.scarlet.v2.Lifecycle
import com.tinder.scarlet.v2.LifecycleState

internal class DefaultLifecycle(
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()
) : Lifecycle by lifecycleRegistry {

    init {
        lifecycleRegistry.onNext(LifecycleState.Started)
    }
}
