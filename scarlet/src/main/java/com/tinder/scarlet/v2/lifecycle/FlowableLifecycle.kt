/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.lifecycle

import com.tinder.scarlet.v2.Lifecycle
import com.tinder.scarlet.v2.LifecycleState
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Timed
import org.reactivestreams.Publisher

internal class FlowableLifecycle(
    private val flowable: Flowable<LifecycleState>,
    private val scheduler: Scheduler
) : Lifecycle, Publisher<LifecycleState> by flowable {

    override fun combineWith(vararg others: Lifecycle): Lifecycle {
        val lifecycles = listOf<Lifecycle>(this) + others
        val timedLifecycleStateFlowables = lifecycles.map {
            Flowable.fromPublisher<LifecycleState>(it)
                .timestamp(scheduler)
        }
        @Suppress("UNCHECKED_CAST")
        val flowable = Flowable.combineLatest(timedLifecycleStateFlowables, { it.map { it as Timed<LifecycleState> } })
            .map(List<Timed<LifecycleState>>::combine)
        return FlowableLifecycle(flowable, scheduler)
    }
}
