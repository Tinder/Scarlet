/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.stub

import com.tinder.scarlet.internal.utils.RuntimePlatform
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class StubInterface(
    private val stubMethods: Map<Method, StubMethod>,
    private val callback: Callback
) {

    fun invoke(method: Method, args: Array<Any>): Any {
        val stubMethod = checkNotNull(stubMethods[method]) { "Stub method not found" }
        return when (stubMethod) {
            is StubMethod.Send -> callback.send(stubMethod, args[0])
            is StubMethod.Receive -> callback.receive(stubMethod)
        }
    }

    interface Callback {
        fun send(stubMethod: StubMethod.Send, data: Any): Any
        fun receive(stubMethod: StubMethod.Receive): Any
    }

    internal class Factory(
        private val runtimePlatform: RuntimePlatform,
        private val callback: Callback,
        private val stubMethodFactory: StubMethod.Factory
    ) {

        fun <T> create(anInterface: Class<T>): T {
            validateInterface(anInterface)
            val stubMethods = anInterface.findStubMethods()
            val stubInterface = StubInterface(stubMethods, callback)
            val proxy = Proxy.newProxyInstance(
                anInterface.classLoader,
                arrayOf(anInterface),
                createInvocationHandler(anInterface, stubInterface)
            )
            return anInterface.cast(proxy)
        }

        private fun Class<*>.findStubMethods(): Map<Method, StubMethod> {
            val methods = declaredMethods.filterNot { runtimePlatform.isDefaultMethod(it) }
            val stubMethods = methods.mapNotNull { stubMethodFactory.create(it) }
            return methods.zip(stubMethods).toMap()
        }

        private fun validateInterface(anInterface: Class<*>) {
            require(anInterface.isInterface) { "Service declarations must be interfaces." }

            // Prevent API interfaces from extending other interfaces. This not only avoids a bug in
            // Android (http://b.android.com/58753) but it forces composition of API declarations which is
            // the recommended pattern.
            require(anInterface.interfaces.isEmpty()) { "Service interfaces must not extend other interfaces." }
        }

        private fun createInvocationHandler(
            anInterface: Class<*>,
            stubInterface: StubInterface
        ): InvocationHandler {
            return InvocationHandler { proxy, method, nullableArgs ->
                val args = nullableArgs ?: arrayOf()
                if (runtimePlatform.isDefaultMethod(method)) {
                    runtimePlatform.invokeDefaultMethod(method, anInterface, proxy, args)
                } else {
                    stubInterface.invoke(method, args)
                }
            }
        }
    }
}
