/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal

import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.internal.servicemethod.ServiceMethodExecutor
import java.lang.reflect.Method

internal class Service(
    private val connection: Connection,
    private val serviceMethodExecutor: ServiceMethodExecutor
) {

    fun startForever() = connection.startForever()

    fun execute(method: Method, args: Array<Any>) = serviceMethodExecutor.execute(method, args)

    class Factory(
        private val connectionFactory: Connection.Factory,
        private val serviceMethodExecutorFactory: ServiceMethodExecutor.Factory
    ) {

        fun create(serviceInterface: Class<*>): Service {
            validateService(serviceInterface)
            val connection = connectionFactory.create()
            val serviceMethodExecutor = serviceMethodExecutorFactory.create(serviceInterface, connection)
            return Service(connection, serviceMethodExecutor)
        }

        private fun validateService(service: Class<*>) {
            require(service.isInterface) { "Service declarations must be interfaces." }

            // Prevent API interfaces from extending other interfaces. This not only avoids a bug in
            // Android (http://b.android.com/58753) but it forces composition of API declarations which is
            // the recommended pattern.
            require(service.interfaces.isEmpty()) { "Service interfaces must not extend other interfaces." }
        }
    }
}
