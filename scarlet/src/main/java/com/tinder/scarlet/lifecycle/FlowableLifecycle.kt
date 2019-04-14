/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.LifecycleState
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import java.lang.reflect.Method

internal class FlowableLifecycle(
        private val flowable: Flowable<LifecycleState>
) : Lifecycle, Publisher<LifecycleState> by flowable {

    companion object {
        const val CONNECTIVITY_ON_LIFECYCLE_CLASS_NAME = "com.tinder.scarlet.lifecycle.android.ConnectivityOnLifecycle"
        const val UNREGISTER_CONNECTIVITY_BROADCAST_RECEIVER_METHOD_NAME = "unregisterReceiver"
    }

    override fun combineWith(others: List<Lifecycle>): Lifecycle {
        val lifecycles = others + this

        val flowable =
                Flowable.combineLatest(lifecycles) { lifecycle ->
                    val state = lifecycle.map { it as LifecycleState }.combine()
                    try {
                        lifecycles.unregisterConnectivityBroadcastReceiver(state)
                    } catch (e: Exception) {
                    }
                    state
                }
        return FlowableLifecycle(flowable)
    }

    private fun List<Lifecycle>.unregisterConnectivityBroadcastReceiver(state: LifecycleState) {
        if (state == LifecycleState.Completed) {
            val connectivityClazz: Class<*>? = Class.forName(CONNECTIVITY_ON_LIFECYCLE_CLASS_NAME)
            connectivityClazz?.let {
                val connectivityOnLifecycle: Lifecycle? = this.find { connectivityClazz.isInstance(it) }
                connectivityOnLifecycle?.let {
                    val obj: Any? = connectivityClazz.cast(connectivityOnLifecycle)
                    val method: Method? = connectivityClazz.getMethod(UNREGISTER_CONNECTIVITY_BROADCAST_RECEIVER_METHOD_NAME)
                    if (obj != null && method != null) {
                        method.invoke(obj)
                    }
                }
            }
        }
    }
}
