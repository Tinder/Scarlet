package com.tinder.scarlet.internal.stub

import com.tinder.scarlet.internal.utils.RuntimePlatform
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class ProxyFactory(
    private val runtimePlatform: RuntimePlatform
) {

    fun <T> create(anInterface: Class<T>, stubInterface: StubInterface): T {
        val proxy = Proxy.newProxyInstance(
            anInterface.classLoader,
            arrayOf(anInterface),
            createInvocationHandler(anInterface, stubInterface)
        )
        return anInterface.cast(proxy)
    }

    private fun createInvocationHandler(
        anInterface: Class<*>,
        stubInterface: StubInterface
    ): InvocationHandler {
        return InvocationHandler { proxy, method, nullableArgs ->
            val args = nullableArgs ?: arrayOf()
            when {
                runtimePlatform.isDefaultMethod(method) -> runtimePlatform.invokeDefaultMethod(
                    method,
                    anInterface,
                    proxy,
                    args
                )
                isJavaObjectMethod(method) -> handleJavaObjectMethod(
                    method,
                    stubInterface,
                    anInterface,
                    proxy,
                    args
                )
                else -> stubInterface.invoke(method, args)
            }
        }
    }

    private fun isJavaObjectMethod(method: Method) = method.declaringClass == Object::class.java

    private fun handleJavaObjectMethod(
        method: Method,
        serviceInstance: StubInterface,
        anInterface: Class<*>,
        proxy: Any,
        args: Array<out Any>
    ): Any {
        return when {
            isEquals(method) -> proxy === args[0]
            isToString(method) -> "Scarlet service implementation for ${anInterface.name}"
            isHashCode(method) -> serviceInstance.hashCode()
            else -> throw IllegalStateException("Cannot execute $method")
        }
    }

    private fun isHashCode(method: Method) =
        method.name == "hashCode" && method.parameterTypes.isEmpty()

    private fun isToString(method: Method) =
        method.name == "toString" && method.parameterTypes.isEmpty()

    private fun isEquals(method: Method) =
        method.name == "equals" && arrayOf(Object::class.java).contentEquals(method.parameterTypes)
}