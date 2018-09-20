/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.retry.BackoffStrategy
import java.lang.reflect.Method


class Scarlet internal constructor(
    private val proxyFactory: ProxyFactory
) : MethodInvocationListener {

    fun <T> create(service: Class<T>): T = proxyFactory.create(service, this)

    /**
     * Same as [create].
     */
    inline fun <reified T : Any> create(): T = create(T::class.java)

    override fun onMethodInvoked(method: Method, args: Array<Any>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    data class Configuration(
        val protocol: Protocol,
        val topic: Topic = Topic.Main,
        val lifecycle: Lifecycle,
        val backoffStrategy: BackoffStrategy,
        val streamAdapters: List<Any> = emptyList(),
        val messageAdapters: List<Any> = emptyList()
    )

    class Factory {
        fun create(configuration: Configuration): Scarlet {

        }
    }
}

