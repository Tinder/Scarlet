/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.stub

import com.tinder.scarlet.internal.utils.RuntimePlatform
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class StubInterface(
    val stubMethods: Map<Method, StubMethod>,
    private val callback: Callback
) {

    fun invoke(method: Method, args: Array<Any>): Any {
        val stubMethod = checkNotNull(stubMethods[method]) { "Stub method not found" }
        return when (stubMethod) {
            is StubMethod.Send -> {
                require(args.size == 1) { "Send method only take one argument" }
                callback.send(stubMethod, args[0])
            }
            is StubMethod.Receive -> {
                require(args.isEmpty()) { "Receive method only take zero argument" }
                callback.receive(stubMethod)
            }
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

        fun <T> create(anInterface: Class<T>): StubInterface {
            validateInterface(anInterface)
            val stubMethods = anInterface.findStubMethods()
            return StubInterface(stubMethods, callback)
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
    }
}
