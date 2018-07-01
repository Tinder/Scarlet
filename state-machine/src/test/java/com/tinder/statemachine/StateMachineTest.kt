/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.statemachine

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
internal class StateMachineTest {

    class ObjectStateMachine {
        private val onStateChangeListener1 = mock<(State) -> Unit>()
        private val onStateChangeListener2 = mock<(State) -> Unit>()
        private val onStateAExitListener1 = mock<State.(Event) -> Unit>()
        private val onStateAExitListener2 = mock<State.(Event) -> Unit>()
        private val onStateCEnterListener1 = mock<State.(Event) -> Unit>()
        private val onStateCEnterListener2 = mock<State.(Event) -> Unit>()
        private val onStateAAction1OnEvent3 = mock<State.(Event) -> Unit>()
        private val onStateAAction2OnEvent3 = mock<State.(Event) -> Unit>()
        private val stateMachine = StateMachine.create<State, Event> {
            state<State.A> {
                onExit(onStateAExitListener1)
                onExit(onStateAExitListener2)
                on<Event.E1>() transitionTo {
                    State.B
                }
                on<Event.E2>() transitionTo {
                    State.C
                }
                on<Event.E3>() run(onStateAAction1OnEvent3)
                on<Event.E3>() run(onStateAAction2OnEvent3)
            }
            state<State.B> {
                on<Event.E3>() transitionTo {
                    State.C
                }
            }
            state<State.C> {
                onEnter(onStateCEnterListener1)
                onEnter(onStateCEnterListener2)
            }
            defaultState(State.A)
            onStateChange(onStateChangeListener1)
            onStateChange(onStateChangeListener2)
        }

        @Test
        fun state_shouldReturnDefaultState() {
            // When
            val state = stateMachine.state

            // Then
            assertThat(state).isEqualTo(State.A)
        }

        @Test
        fun transition_givenValidEvent_shouldReturnTrue() {
            // When
            val isSuccessfulFromStateAToStateB = stateMachine.transition(Event.E1)

            // Then
            assertThat(isSuccessfulFromStateAToStateB).isTrue()

            // When
            val isSuccessfulFromStateBToStateC = stateMachine.transition(Event.E3)

            // Then
            assertThat(isSuccessfulFromStateBToStateC).isTrue()
        }

        @Test
        fun transition_givenValidEvent_shouldCreateAndSetNewState() {
            // When
            stateMachine.transition(Event.E1)

            // Then
            assertThat(stateMachine.state).isEqualTo(State.B)

            // When
            stateMachine.transition(Event.E3)

            // Then
            assertThat(stateMachine.state).isEqualTo(State.C)
        }

        @Test
        fun transition_givenValidEvent_shouldTriggerOnStateChangeListener() {
            // When
            stateMachine.transition(Event.E1)

            // Then
            then(onStateChangeListener1).should().invoke(State.B)

            // When
            stateMachine.transition(Event.E3)

            // Then
            then(onStateChangeListener2).should().invoke(State.C)
        }

        @Test
        fun transition_givenValidEvent_shouldTriggerOnEnterListeners() {
            // When
            stateMachine.transition(Event.E2)

            // Then
            then(onStateCEnterListener1).should().invoke(State.C, Event.E2)
            then(onStateCEnterListener2).should().invoke(State.C, Event.E2)
        }

        @Test
        fun transition_givenValidEvent_shouldTriggerOnExitListeners() {
            // When
            stateMachine.transition(Event.E2)

            // Then
            then(onStateAExitListener1).should().invoke(State.A, Event.E2)
            then(onStateAExitListener2).should().invoke(State.A, Event.E2)
        }

        @Test
        fun transition_givenValidEventThatIsAction_shouldRunActions() {
            // When
            val hasTransitioned = stateMachine.transition(Event.E3)

            // Then
            assertThat(hasTransitioned).isFalse()
            then(onStateAAction1OnEvent3).should().invoke(State.A, Event.E3)
            then(onStateAAction2OnEvent3).should().invoke(State.A, Event.E3)
        }

        @Test
        fun transition_givenInvalidEvent_shouldReturnFalse() {
            // When
            val existingState = stateMachine.state
            val isSuccessful = stateMachine.transition(Event.E3)

            // Then
            assertThat(isSuccessful).isFalse()
            assertThat(stateMachine.state).isEqualTo(existingState)
        }
    }

    class ObjectStateMachineWithoutDefaultState {
        @Test
        fun create_givenNoDefaultState_shouldThrowIllegalArgumentException() {
            // Then
            assertThatIllegalArgumentException().isThrownBy {
                StateMachine.create<State, Event> {}
            }
        }
    }

    class ObjectStateMachineWithUndefinedState {
        private val stateMachine = StateMachine.create<State, Event> {
            defaultState(State.A)
        }

        @Test
        fun transition_givenUndefinedState_shouldThrowIllegalStateException() {
            // Then
            assertThatIllegalStateException()
                .isThrownBy {
                    stateMachine.transition(Event.E1)
                }
        }
    }

    class ConstantStateMachine {
        private val onStateChangeListener1 = mock<(String) -> Unit>()
        private val onStateChangeListener2 = mock<(String) -> Unit>()
        private val onStateCEnterListener1 = mock<String.(Int) -> Unit>()
        private val onStateCEnterListener2 = mock<String.(Int) -> Unit>()
        private val onStateAExitListener1 = mock<String.(Int) -> Unit>()
        private val onStateAExitListener2 = mock<String.(Int) -> Unit>()
        private val stateMachine = StateMachine.create<String, Int> {
            state(STATE_A) {
                onExit(onStateAExitListener1)
                onExit(onStateAExitListener2)
                on(EVENT_1) transitionTo {
                    STATE_B
                }
                on(EVENT_2) transitionTo {
                    STATE_C
                }
            }
            state(STATE_B) {
                on(EVENT_3) transitionTo {
                    STATE_C
                }
            }
            state(STATE_C) {
                onEnter(onStateCEnterListener1)
                onEnter(onStateCEnterListener2)
            }
            defaultState(STATE_A)
            onStateChange(onStateChangeListener1)
            onStateChange(onStateChangeListener2)
        }

        @Test
        fun state_shouldReturnDefaultState() {
            // When
            val state = stateMachine.state

            // Then
            assertThat(state).isEqualTo(STATE_A)
        }

        @Test
        fun transition_givenValidEvent_shouldReturnTrue() {
            // When
            val isSuccessfulFromStateAToStateB = stateMachine.transition(EVENT_1)

            // Then
            assertThat(isSuccessfulFromStateAToStateB).isTrue()

            // When
            val isSuccessfulFromStateBToStateC = stateMachine.transition(EVENT_3)

            // Then
            assertThat(isSuccessfulFromStateBToStateC).isTrue()
        }

        @Test
        fun transition_givenValidEvent_shouldCreateAndSetNewState() {
            // When
            stateMachine.transition(EVENT_1)

            // Then
            assertThat(stateMachine.state).isEqualTo(STATE_B)

            // When
            stateMachine.transition(EVENT_3)

            // Then
            assertThat(stateMachine.state).isEqualTo(STATE_C)
        }

        @Test
        fun transition_givenValidEvent_shouldTriggerOnStateChangeListener() {
            // When
            stateMachine.transition(EVENT_1)

            // Then
            then(onStateChangeListener1).should().invoke(STATE_B)

            // When
            stateMachine.transition(EVENT_3)

            // Then
            then(onStateChangeListener2).should().invoke(STATE_C)
        }

        @Test
        fun transition_givenValidEvent_shouldTriggerOnEnterListeners() {
            // When
            stateMachine.transition(EVENT_2)

            // Then
            then(onStateCEnterListener1).should().invoke(STATE_C, EVENT_2)
            then(onStateCEnterListener2).should().invoke(STATE_C, EVENT_2)
        }

        @Test
        fun transition_givenValidEvent_shouldTriggerOnExitListeners() {
            // When
            stateMachine.transition(EVENT_2)

            // Then
            then(onStateAExitListener1).should().invoke(STATE_A, EVENT_2)
            then(onStateAExitListener2).should().invoke(STATE_A, EVENT_2)
        }

        @Test
        fun transition_givenInvalidEvent_shouldReturnFalse() {
            // When
            val existingState = stateMachine.state
            val isSuccessful = stateMachine.transition(EVENT_3)

            // Then
            assertThat(isSuccessful).isFalse()
            assertThat(stateMachine.state).isEqualTo(existingState)
        }
    }

    class ConstantStateMachineWithoutDefaultState {
        @Test
        fun create_givenNoDefaultState_shouldThrowIllegalArgumentException() {
            // Then
            assertThatIllegalArgumentException().isThrownBy {
                StateMachine.create<String, Int> {}
            }
        }
    }

    class ConstantStateMachineWithUndefinedState {
        private val stateMachine = StateMachine.create<String, Int> {
            defaultState(STATE_A)
        }

        @Test
        fun transition_givenUndefinedState_shouldThrowIllegalStateException() {
            // Then
            assertThatIllegalStateException()
                .isThrownBy {
                    stateMachine.transition(EVENT_1)
                }
        }
    }

    private companion object {
        private sealed class State {
            object A : State()
            object B : State()
            object C : State()
        }

        private sealed class Event {
            object E1 : Event()
            object E2 : Event()
            object E3 : Event()
        }

        private const val STATE_A = "a"
        private const val STATE_B = "b"
        private const val STATE_C = "c"

        private const val EVENT_1 = 1
        private const val EVENT_2 = 2
        private const val EVENT_3 = 3
    }
}
