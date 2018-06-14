/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.statemachine

import com.tinder.statemachine.StateMachine.Matcher.Companion.any
import com.tinder.statemachine.StateMachine.Matcher.Companion.eq
import java.util.concurrent.atomic.AtomicReference

class StateMachine<STATE : Any, EVENT : Any> private constructor() {

    private val stateRef = AtomicReference<STATE>()
    private val stateDefinitions = linkedMapOf<Matcher<STATE, STATE>, StateDefinition<STATE, EVENT>>()
    private val onStateChangeListeners = mutableListOf<(STATE) -> Unit>()

    val state: STATE
        get() = stateRef.get()

    fun transition(event: EVENT): Boolean {
        val (existingState, newState) = synchronized(stateRef) {
            val existingState = stateRef.get()
            val newState = existingState.getNext(event) ?: return false
            stateRef.set(newState)
            existingState to newState
        }
        with(existingState) {
            notifyOnExit(event)
        }
        with(newState) {
            notifyOnChange()
            notifyOnEnter(event)
        }
        return true
    }

    private fun STATE.getDefinition() = stateDefinitions
        .filter { it.key.matches(this) }
        .map { it.value }
        .firstOrNull()
        .let { checkNotNull(it) }

    private fun STATE.getNext(event: EVENT): STATE? {
        for ((eventMatcher, action) in getDefinition().actions) {
            if (eventMatcher.matches(event)) {
                action(this, event)
            }
        }
        for ((eventMatcher, createNewState) in getDefinition().transitions) {
            if (eventMatcher.matches(event)) {
                return createNewState(this, event)
            }
        }
        return null
    }

    private fun STATE.notifyOnChange() = onStateChangeListeners.forEach { it(this) }

    private fun STATE.notifyOnEnter(cause: EVENT) = getDefinition().onEnterListeners.forEach { it(this, cause) }

    private fun STATE.notifyOnExit(cause: EVENT) = getDefinition().onExitListeners.forEach { it(this, cause) }

    class StateDefinition<STATE : Any, EVENT : Any> {
        val onEnterListeners = mutableListOf<(STATE, EVENT) -> Unit>()
        val onExitListeners = mutableListOf<(STATE, EVENT) -> Unit>()
        val actions = linkedMapOf<Matcher<EVENT, EVENT>, (STATE, EVENT) -> Unit>()
        val transitions = linkedMapOf<Matcher<EVENT, EVENT>, (STATE, EVENT) -> STATE>()
    }

    class Matcher<T : Any, out R : T>(private val clazz: Class<R>) {
        private val predicates = mutableListOf<(T) -> Boolean>({ clazz.isInstance(it) })

        fun where(predicate: R.() -> Boolean): Matcher<T, R> = apply {
            predicates.add {
                @Suppress("UNCHECKED_CAST")
                (it as R).predicate()
            }
        }

        fun matches(value: T) = predicates.all { it(value) }

        companion object {
            inline fun <T : Any, reified R : T> any(): Matcher<T, R> =
                Matcher(R::class.java)

            inline fun <T : Any, reified R : T> eq(value: R): Matcher<T, R> =
                Matcher<T, R>(R::class.java).where { this == value }
        }
    }

    class Builder<STATE : Any, EVENT : Any> {
        private val stateMachine = StateMachine<STATE, EVENT>()

        fun <S : STATE> state(
            stateMatcher: Matcher<STATE, S>,
            init: StateDefinitionBuilder<S>.() -> Unit
        ) = with(stateMachine) {
            stateDefinitions[stateMatcher] = StateDefinitionBuilder<S>().apply(init).build()
        }

        inline fun <reified S : STATE> state(noinline init: StateDefinitionBuilder<S>.() -> Unit) = state(any(), init)

        inline fun <reified S : STATE> state(state: S, noinline init: StateDefinitionBuilder<S>.() -> Unit): Unit =
            state(eq<STATE, S>(state), init)

        fun defaultState(defaultState: STATE) = with(stateMachine) {
            stateRef.set(defaultState)
        }

        fun onStateChange(listener: (STATE) -> Unit) = with(stateMachine) {
            onStateChangeListeners.add(listener)
        }

        fun build() = stateMachine.apply { requireNotNull(stateRef.get()) }

        inner class StateDefinitionBuilder<out S : STATE> {
            private val stateDefinition = StateDefinition<STATE, EVENT>()

            inline fun <reified E : EVENT> on(eventMatcher: Matcher<EVENT, E> = any()) = OnEvent(eventMatcher)

            inline fun <reified E : EVENT> on(event: E) = OnEvent<E>(eq(event))

            fun onEnter(listener: S.(EVENT) -> Unit) = with(stateDefinition) {
                onEnterListeners.add { state, cause ->
                    @Suppress("UNCHECKED_CAST")
                    listener(state as S, cause)
                }
            }

            fun onExit(listener: S.(EVENT) -> Unit) = with(stateDefinition) {
                onExitListeners.add { state, cause ->
                    @Suppress("UNCHECKED_CAST")
                    listener(state as S, cause)
                }
            }

            inner class OnEvent<out E : EVENT>(private val matcher: Matcher<EVENT, E>) {
                infix fun <T : STATE> transitionTo(createNewState: S.(E) -> T) = with(stateDefinition) {
                    transitions[matcher] = { state, event ->
                        @Suppress("UNCHECKED_CAST")
                        createNewState(state as S, event as E)
                    }
                }

                infix fun run(action: S.(E) -> Unit) = with(stateDefinition) {
                    actions[matcher] = { state, event ->
                        @Suppress("UNCHECKED_CAST")
                        action(state as S, event as E)
                    }
                }
            }

            fun build() = stateDefinition
        }
    }

    companion object {
        fun <STATE : Any, EVENT : Any> create(init: Builder<STATE, EVENT>.() -> Unit) =
            Builder<STATE, EVENT>().apply(init).build()
    }

}

