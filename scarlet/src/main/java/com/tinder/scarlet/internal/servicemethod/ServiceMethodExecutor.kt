/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import java.lang.reflect.Method

internal class ServiceMethodExecutor(
    internal val serviceMethods: Map<Method, ServiceMethod>
) {

    fun execute(method: Method, args: Array<Any>): Any {
        val serviceMethod = checkNotNull(serviceMethods[method]) { "Service method not found" }
        return when (serviceMethod) {
            is ServiceMethod.Send -> serviceMethod.execute(args[0])
            is ServiceMethod.Receive -> serviceMethod.execute()
        }
    }

    class Factory(
        private val runtimePlatform: RuntimePlatform,
        private val sendServiceMethodFactory: ServiceMethod.Send.Factory,
        private val receiveServiceMethodFactory: ServiceMethod.Receive.Factory
    ) {

        fun create(serviceInterface: Class<*>, connection: Connection): ServiceMethodExecutor {
            val serviceMethods = serviceInterface.findServiceMethods(connection)
            return ServiceMethodExecutor(serviceMethods)
        }

        private fun Class<*>.findServiceMethods(connection: Connection): Map<Method, ServiceMethod> {
            val methods = declaredMethods.filterNot { runtimePlatform.isDefaultMethod(it) }
            val serviceMethods = methods.map { it.toServiceMethod(connection) }
            return methods.zip(serviceMethods).toMap()
        }

        private fun Method.toServiceMethod(connection: Connection): ServiceMethod {
            val serviceMethodFactories = annotations.mapNotNull { it.findServiceMethodFactory() }
            require(serviceMethodFactories.size == 1) {
                "A method must have one and only one service method annotation: $this"
            }
            return serviceMethodFactories.first().create(connection, this)
        }

        private fun Annotation.findServiceMethodFactory(): ServiceMethod.Factory? =
            when (this) {
                is Send -> sendServiceMethodFactory
                is Receive -> receiveServiceMethodFactory
                else -> null
            }
    }
}
