/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Timed
import org.reactivestreams.Publisher

internal class FlowableLifecycle(
    private val flowable: Flowable<Lifecycle.State>,
    private val scheduler: Scheduler
) : Lifecycle, Publisher<Lifecycle.State> by flowable {

    override fun combineWith(vararg others: Lifecycle): Lifecycle {
        val lifecycles = listOf<Lifecycle>(this) + others
        val timedLifecycleStateFlowables = lifecycles.map {
            Flowable.fromPublisher<Lifecycle.State>(it)
                .timestamp(scheduler)
        }
        @Suppress("UNCHECKED_CAST")
        val flowable = Flowable.combineLatest(timedLifecycleStateFlowables, { it.map { it as Timed<Lifecycle.State> } })
            .map(List<Timed<Lifecycle.State>>::combine)
        return FlowableLifecycle(flowable, scheduler)
    }
}
