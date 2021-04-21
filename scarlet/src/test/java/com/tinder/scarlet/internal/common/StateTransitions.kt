/*
 * © 2019 Match Group, LLC.
 */

package com.tinder.scarlet.internal.common

import org.mockito.kotlin.mock
import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.Event
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.Message
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.State
import com.tinder.scarlet.StateTransition

internal const val TEXT = "hello"
internal val MESSAGE = Message.Text(TEXT)
internal val PROTOCOL_EVENT_ON_MESSAGE_RECEIVED = ProtocolEvent.OnMessageReceived(MESSAGE, mock())
internal val MALFORMED_MESSAGE = mock<Message>()
internal val PROTOCOL_EVENT_ON_MALFORMED_MESSAGE_RECEIVED =
    ProtocolEvent.OnMessageReceived(MALFORMED_MESSAGE, mock())
internal val THROWABLE = IllegalArgumentException()
internal val SUCCESSFUL_DESERIALIZATION = Deserialization.Success(
    TEXT,
    MESSAGE
)
internal val FAILED_DESERIALIZATION = Deserialization.Error<String>(
    THROWABLE,
    MALFORMED_MESSAGE
)

internal val ON_LIFECYCLE_START = Event.OnLifecycleStateChange(LifecycleState.Started)
internal val ON_LIFECYCLE_STOP =
    Event.OnLifecycleStateChange(mock<LifecycleState.Stopped>())
internal val ON_LIFECYCLE_TERMINATED =
    Event.OnLifecycleStateChange(mock<LifecycleState.Completed>())

internal val ON_PROTOCOL_CONNECTION_OPENED =
    Event.OnProtocolEvent(mock<ProtocolEvent.OnOpened>())
internal val ON_PROTOCOL_MESSAGE_RECEIVED =
    Event.OnProtocolEvent(PROTOCOL_EVENT_ON_MESSAGE_RECEIVED)
internal val ON_PROTOCOL_MESSAGE_RECEIVED_MALFORMED =
    Event.OnProtocolEvent(PROTOCOL_EVENT_ON_MALFORMED_MESSAGE_RECEIVED)
internal val ON_PROTOCOL_CONNECTION_CLOSING =
    Event.OnProtocolEvent(mock<ProtocolEvent.OnClosing>())
internal val ON_PROTOCOL_CONNECTION_CLOSED =
    Event.OnProtocolEvent(mock<ProtocolEvent.OnClosed>())
internal val ON_PROTOCOL_CONNECTION_FAILED =
    Event.OnProtocolEvent(mock<ProtocolEvent.OnFailed>())

internal val ON_SHOULD_CONNECT = Event.OnShouldConnect

internal val CONNECTING_STATE = mock<State.Connecting>()
internal val CONNECTED_STATE = mock<State.Connected>()
internal val WILL_CONNECT_STATE = mock<State.WillConnect>()
internal val DISCONNECTING_STATE = mock<State.Disconnecting>()
internal val DISCONNECTED_STATE = mock<State.Disconnected>()
internal val DESTROYED_STATE = mock<State.Destroyed>()

internal val SERVER_MESSAGES = listOf(
    StateTransition(
        fromState = DISCONNECTED_STATE,
        event = ON_LIFECYCLE_START,
        toState = WILL_CONNECT_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = WILL_CONNECT_STATE,
        event = ON_SHOULD_CONNECT,
        toState = CONNECTING_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTING_STATE,
        event = ON_PROTOCOL_CONNECTION_OPENED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_PROTOCOL_MESSAGE_RECEIVED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_PROTOCOL_MESSAGE_RECEIVED_MALFORMED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_PROTOCOL_MESSAGE_RECEIVED_MALFORMED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_PROTOCOL_MESSAGE_RECEIVED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_PROTOCOL_MESSAGE_RECEIVED_MALFORMED,
        toState = CONNECTED_STATE,
        sideEffect = null
    )
)

internal val SERVER_CLOSURE = listOf(
    StateTransition(
        fromState = DISCONNECTED_STATE,
        event = ON_LIFECYCLE_START,
        toState = WILL_CONNECT_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = WILL_CONNECT_STATE,
        event = ON_SHOULD_CONNECT,
        toState = CONNECTING_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTING_STATE,
        event = ON_PROTOCOL_CONNECTION_OPENED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_PROTOCOL_MESSAGE_RECEIVED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_PROTOCOL_MESSAGE_RECEIVED_MALFORMED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_PROTOCOL_CONNECTION_CLOSING,
        toState = DISCONNECTING_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = DISCONNECTING_STATE,
        event = ON_PROTOCOL_CONNECTION_CLOSED,
        toState = WILL_CONNECT_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = WILL_CONNECT_STATE,
        event = ON_SHOULD_CONNECT,
        toState = CONNECTING_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTING_STATE,
        event = ON_PROTOCOL_CONNECTION_CLOSED,
        toState = WILL_CONNECT_STATE,
        sideEffect = null
    )
)

internal val CLIENT_CLOSURE = listOf(
    StateTransition(
        fromState = DISCONNECTED_STATE,
        event = ON_LIFECYCLE_START,
        toState = WILL_CONNECT_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = WILL_CONNECT_STATE,
        event = ON_SHOULD_CONNECT,
        toState = CONNECTING_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTING_STATE,
        event = ON_PROTOCOL_CONNECTION_OPENED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_LIFECYCLE_STOP,
        toState = DISCONNECTING_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = DISCONNECTING_STATE,
        event = ON_PROTOCOL_CONNECTION_CLOSED,
        toState = DISCONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = DISCONNECTED_STATE,
        event = ON_LIFECYCLE_TERMINATED,
        toState = DESTROYED_STATE,
        sideEffect = null
    )
)

internal val CLIENT_ABORT = listOf(
    StateTransition(
        fromState = DISCONNECTED_STATE,
        event = ON_LIFECYCLE_START,
        toState = WILL_CONNECT_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = WILL_CONNECT_STATE,
        event = ON_SHOULD_CONNECT,
        toState = CONNECTING_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTING_STATE,
        event = ON_PROTOCOL_CONNECTION_OPENED,
        toState = CONNECTED_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = CONNECTED_STATE,
        event = ON_LIFECYCLE_TERMINATED,
        toState = DISCONNECTING_STATE,
        sideEffect = null
    ),
    StateTransition(
        fromState = DISCONNECTING_STATE,
        event = ON_PROTOCOL_CONNECTION_FAILED,
        toState = DESTROYED_STATE,
        sideEffect = null
    )
)