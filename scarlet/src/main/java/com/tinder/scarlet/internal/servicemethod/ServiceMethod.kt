/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.utils.hasUnresolvableType
import com.tinder.scarlet.utils.toStream
import io.reactivex.Flowable
import io.reactivex.Scheduler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal sealed class ServiceMethod {

    interface Factory {
        fun create(connection: Connection, method: Method): ServiceMethod
    }

    class Send(
        private val connection: Connection,
        private val messageAdapter: MessageAdapter<Any>
    ) : ServiceMethod() {

        fun execute(data: Any): Any {
            val message = messageAdapter.toMessage(data)
            return connection.send(message)
        }

        class Factory(private val messageAdapterResolver: MessageAdapterResolver) : ServiceMethod.Factory {
            override fun create(connection: Connection, method: Method): Send {
                method.requireParameterTypes(Any::class.java) {
                    "Send method must have one and only one parameter: $method"
                }
                method.requireReturnTypeIsOneOf(Boolean::class.java, Void.TYPE) {
                    "Send method must return Boolean or Void: $method"
                }

                val messageType = method.getFirstParameterType()
                val annotations = method.getFirstParameterAnnotations()
                val adapter = messageAdapterResolver.resolve(messageType, annotations)
                return Send(connection, adapter)
            }
        }
    }

    class Receive(
        internal val eventMapper: EventMapper<*>,
        private val connection: Connection,
        private val scheduler: Scheduler,
        private val streamAdapter: StreamAdapter<Any, Any>
    ) : ServiceMethod() {

        fun execute(): Any {
            val stream = Flowable.defer { connection.observeEvent() }
                .observeOn(scheduler)
                .flatMapMaybe(eventMapper::mapToData)
                .toStream()
            return streamAdapter.adapt(stream)
        }

        class Factory(
            private val scheduler: Scheduler,
            private val eventMapperFactory: EventMapper.Factory,
            private val streamAdapterResolver: StreamAdapterResolver
        ) : ServiceMethod.Factory {
            override fun create(connection: Connection, method: Method): Receive {
                method.requireParameterTypes { "Receive method must have zero parameter: $method" }
                method.requireReturnTypeIsOneOf(ParameterizedType::class.java) {
                    "Receive method must return ParameterizedType: $method"
                }
                method.requireReturnTypeIsResolvable {
                    "Method return type must not include a type variable or wildcard: ${method.genericReturnType}"
                }

                val eventMapper = createEventMapper(method)
                val streamAdapter = createStreamAdapter(method)
                return Receive(eventMapper, connection, scheduler, streamAdapter)
            }

            private fun createEventMapper(method: Method): EventMapper<*> =
                eventMapperFactory.create(method.genericReturnType as ParameterizedType, method.annotations)

            private fun createStreamAdapter(method: Method): StreamAdapter<Any, Any> =
                streamAdapterResolver.resolve(method.genericReturnType)
        }
    }

    companion object {
        private inline fun Method.requireParameterTypes(vararg types: Class<*>, lazyMessage: () -> Any) {
            require(genericParameterTypes.size == types.size, lazyMessage)
            require(genericParameterTypes.zip(types).all { (t1, t2) -> t2 === t1 || t2.isInstance(t1) }, lazyMessage)
        }

        private inline fun Method.requireReturnTypeIsOneOf(vararg types: Class<*>, lazyMessage: () -> Any) =
            require(types.any { it === genericReturnType || it.isInstance(genericReturnType) }, lazyMessage)

        private inline fun Method.requireReturnTypeIsResolvable(lazyMessage: () -> Any) =
            require(!genericReturnType.hasUnresolvableType(), lazyMessage)

        private fun Method.getFirstParameterType(): Type = genericParameterTypes.first()

        private fun Method.getFirstParameterAnnotations(): Array<Annotation> = parameterAnnotations.first()
    }
}
