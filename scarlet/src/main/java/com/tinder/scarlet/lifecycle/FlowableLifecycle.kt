/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.LifecycleState
import io.reactivex.Flowable
import org.reactivestreams.Publisher

internal class FlowableLifecycle(
    private val flowable: Flowable<LifecycleState>
) : Lifecycle, Publisher<LifecycleState> by flowable {

    override fun combineWith(others: List<Lifecycle>): Lifecycle {
        val lifecycles = others + this
        val flowable = Flowable.combineLatest(lifecycles) { it.map { it as LifecycleState }.combine() }
        return FlowableLifecycle(flowable)
    }
}
