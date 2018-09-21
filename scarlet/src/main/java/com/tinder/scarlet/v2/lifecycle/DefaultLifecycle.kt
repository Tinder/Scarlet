/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.lifecycle

import com.tinder.scarlet.v2.Lifecycle
import com.tinder.scarlet.v2.LifecycleState
import io.reactivex.Flowable

internal class DefaultLifecycle(
) : Lifecycle by FlowableLifecycle(Flowable.just(LifecycleState.Started))
