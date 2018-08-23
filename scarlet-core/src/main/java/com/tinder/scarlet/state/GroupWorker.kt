/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.scarlet.RequestFactory
import com.tinder.scarlet.StaticRequestFactory

internal class GroupWorker<WORKER_ID : Any, START_REQUEST : Any, START_RESPONSE : Any, STOP_REQUEST : Any, STOP_RESPONSE : Any> {
    private val workerFactory =
        Worker.Factory<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>()
    private val workers =
        emptyMap<WORKER_ID, Worker<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>>().toMutableMap()

    // TODO keep the latest lifecycle state
    fun add(
        workerId: WORKER_ID,
        startRequestFactory: RequestFactory<START_REQUEST>,
        stopRequestFactory: RequestFactory<STOP_REQUEST>,
        listener: (Worker.Transition<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>) -> Unit
    ) {
        workers[workerId] = workerFactory.create(
            startRequestFactory,
            stopRequestFactory,
            listener
        )
    }

    fun add(
        workerId: WORKER_ID,
        startRequest: START_REQUEST,
        stopRequest: STOP_REQUEST,
        listener: (Worker.Transition<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>) -> Unit
    ) {
        workers[workerId] = workerFactory.create(
            StaticRequestFactory(startRequest),
            StaticRequestFactory(stopRequest),
            listener
        )
    }


    fun remove(
        workerId: WORKER_ID
    ) {
        workers.remove(workerId)
    }

    fun onLifecycleStarted() {
        workers.forEach {
            it.value.onLifecycleStarted()
        }
    }

    fun onLifecycleStopped() {
        workers.forEach {
            it.value.onLifecycleStopped()
        }
    }

    fun onLifecycleDestroyed() {
        workers.forEach {
            it.value.onLifecycleDestroyed()
        }
    }

    fun onShouldStart(
        workerId: WORKER_ID, response: START_RESPONSE? = null
    ) {
        workers[workerId]?.onShouldStart(response)
    }

    fun onWorkStarted(
        workerId: WORKER_ID, response: STOP_RESPONSE? = null
    ) {
        workers[workerId]?.onWorkStarted(response)
    }

    fun onWorkStopped(
        workerId: WORKER_ID, response: STOP_RESPONSE? = null
    ) {
        workers[workerId]?.onWorkStopped(response)
    }

    fun onWorkFailed(
        workerId: WORKER_ID, throwable: Throwable
    ) {
        workers[workerId]?.onWorkFailed(throwable)
    }

}


