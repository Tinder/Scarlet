/*
 * © 2019 Match Group, LLC.
 */

package com.tinder.scarlet.internal.common

import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.Event
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.State
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.Stream
import com.tinder.scarlet.utils.getParameterUpperBound
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

internal annotation class MyAnnotation

@Suppress("UNUSED")
internal interface Types {
    fun streamOfStateTransition(): Stream<StateTransition>

    fun streamOfState(): Stream<State>

    fun streamOfStateSubclassDisconnecting(): Stream<State.Disconnecting>

    fun streamOfStateSubclassDisconnected(): Stream<State.Disconnected>

    @MyAnnotation
    fun streamOfEvent(): Stream<Event>

    fun streamOfEventSubclassOnProtocolEvent(): Stream<Event.OnProtocolEvent>

    fun streamOfEventSubclassOnRetry(): Stream<Event.OnShouldConnect>

    fun streamOfEventSubclassOnLifecycle(): Stream<Event.OnLifecycleStateChange>

    fun streamOfLifecycleState(): Stream<LifecycleState>

    fun streamOfLifecycleStateSubclassStarted(): Stream<LifecycleState.Started>

    fun streamOfLifecycleStateSubclassStopped(): Stream<LifecycleState.Stopped>

    fun streamOfProtocolEvent(): Stream<ProtocolEvent>

    fun streamOfProtocolEventSubclassOnConnectionOpened(): Stream<ProtocolEvent.OnOpened>

    fun streamOfProtocolEventSubclassOnMessageReceived(): Stream<ProtocolEvent.OnMessageReceived>

    fun streamOfDeserializationOfString(): Stream<Deserialization<String>>

    fun streamOfString(): Stream<String>

    fun streamOfByteArray(): Stream<ByteArray>
}

internal fun KFunction<*>.toMessageTypeAndAnnotations(): Pair<Type, Array<Annotation>> {
    return (javaMethod!!.genericReturnType as ParameterizedType).getParameterUpperBound(0) to javaMethod!!.annotations
}
