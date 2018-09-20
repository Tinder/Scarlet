/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.stub

import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.internal.servicemethod.MessageAdapterResolver
import com.tinder.scarlet.internal.servicemethod.StreamAdapterResolver
import com.tinder.scarlet.utils.getParameterUpperBound
import com.tinder.scarlet.utils.hasUnresolvableType
import com.tinder.scarlet.v2.StateTransition
import com.tinder.scarlet.v2.transitionadapter.StateTransitionAdapterResolver
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal sealed class StubMethod {

    class Send(
        val messageAdapter: MessageAdapter<Any>
    ) : StubMethod()

    class Receive(
        val stateTransitionAdatper: StateTransition.Adapter<Any>,
        val streamAdapter: StreamAdapter<Any, Any>
    ) : StubMethod()

    class Factory(
        private val streamAdapterResolver: StreamAdapterResolver,
        private val messageAdapterResolver: MessageAdapterResolver,
        private val stateTransitionAdapterResolver: StateTransitionAdapterResolver
    ) {

        fun create(method: Method): StubMethod? {
            val annotations = method.annotations.filter { isStubMethodAnnotation(it) }
            require(annotations.size == 1) {
                "A method must have one and only one service method annotation: $this"
            }
            return when (annotations.first()) {
                is com.tinder.scarlet.ws.Receive -> createReceive(method)
                is com.tinder.scarlet.ws.Send -> createSend(method)
                else -> null
            }
        }

        private fun isStubMethodAnnotation(annotation: Annotation): Boolean {
            return when (annotation) {
                is com.tinder.scarlet.ws.Receive,
                is com.tinder.scarlet.ws.Send -> true
                else -> false
            }
        }

        private fun createSend(method: Method): Send {
            method.requireParameterTypes(Any::class.java) {
                "Send method must have one and only one parameter: $method"
            }
            method.requireReturnTypeIsOneOf(Boolean::class.java, Void.TYPE) {
                "Send method must return Boolean or Void: $method"
            }

            val messageType = method.getFirstParameterType()
            val annotations = method.getFirstParameterAnnotations()
            val adapter = messageAdapterResolver.resolve(messageType, annotations)
            return Send(adapter)
        }

        private fun createReceive(method: Method): Receive {
            method.requireParameterTypes { "Receive method must have zero parameter: $method" }
            method.requireReturnTypeIsOneOf(ParameterizedType::class.java) {
                "Receive method must return ParameterizedType: $method"
            }
            method.requireReturnTypeIsResolvable {
                "Method return type must not include a type variable or wildcard: ${method.genericReturnType}"
            }

            val streamType = method.genericReturnType as ParameterizedType
            val messageType = streamType.getFirstTypeArgument()
            val annotations = method.annotations

            val stateTransitionAdatper = stateTransitionAdapterResolver.resolve(messageType, annotations)
            val streamAdapter = streamAdapterResolver.resolve(streamType)
            return Receive(
                stateTransitionAdatper,
                streamAdapter
            )
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

        private fun ParameterizedType.getFirstTypeArgument(): Type = getParameterUpperBound(0)
    }
}
