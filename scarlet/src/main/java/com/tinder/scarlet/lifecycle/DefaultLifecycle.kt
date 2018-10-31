/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.LifecycleState
import io.reactivex.Flowable

internal class DefaultLifecycle() : Lifecycle by FlowableLifecycle(
    Flowable.just(LifecycleState.Started)
)
