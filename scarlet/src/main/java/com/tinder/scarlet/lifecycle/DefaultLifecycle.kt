/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
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
