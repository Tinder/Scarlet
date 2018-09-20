/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.internal.utils.RuntimePlatform
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class ProxyFactory(
    private val runtimePlatform: RuntimePlatform
) {

    fun <T> create(serviceInterface: Class<T>, listener: MethodInvocationListener): T {
        val proxy = Proxy.newProxyInstance(
            serviceInterface.classLoader,
            arrayOf(serviceInterface),
            createInvocationHandler(serviceInterface, listener)
        )
        return serviceInterface.cast(proxy)
    }

    private fun createInvocationHandler(
        serviceInterface: Class<*>,
        listener: MethodInvocationListener
    ): InvocationHandler {
        return InvocationHandler { proxy, method, nullableArgs ->
            val args = nullableArgs ?: arrayOf()
            if (runtimePlatform.isDefaultMethod(method)) {
                runtimePlatform.invokeDefaultMethod(method, serviceInterface, proxy, args)
            } else {
                listener.onMethodInvoked(method, args)
            }
        }
    }
}

interface MethodInvocationListener {
    fun onMethodInvoked(method: Method, args: Array<Any>)
}
