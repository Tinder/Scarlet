/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

// internal const val TEXT = "hello"
// internal val MESSAGE = Message.Text(TEXT)
// internal val WEB_SOCKET_EVENT_ON_MESSAGE_RECEIVED = ProtocolEvent.OnMessageReceived(MESSAGE)
// internal val MALFORMED_MESSAGE = mock<Message>()
// internal val WEB_SOCKET_EVENT_ON_MALFORMED_MESSAGE_RECEIVED = ProtocolEvent.OnMessageReceived(MALFORMED_MESSAGE)
// internal val THROWABLE = IllegalArgumentException()
// internal val SUCCESSFUL_DESERIALIZATION = Deserialization.Success(TEXT)
// internal val FAILED_DESERIALIZATION = Deserialization.Error<String>(THROWABLE)
//
// internal val ON_LIFECYCLE_START = Event.OnLifecycleStateChange(LifecycleState.Started)
// internal val ON_LIFECYCLE_STOP =
//    Event.OnLifecycleStateChange(mock<LifecycleState.Stopped>())
// internal val ON_LIFECYCLE_TERMINATED = Event.OnLifecycleStateChange(mock<LifecycleState.Completed>())
//
// internal val ON_WEB_SOCKET_CONNECTION_OPENED =
//    Event.OnProtocolEvent(mock<ProtocolEvent.OnOpened>())
// internal val ON_WEB_SOCKET_MESSAGE_RECEIVED = Event.OnProtocolEvent(WEB_SOCKET_EVENT_ON_MESSAGE_RECEIVED)
// internal val ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED =
//    Event.OnProtocolEvent(WEB_SOCKET_EVENT_ON_MALFORMED_MESSAGE_RECEIVED)
// internal val ON_WEB_SOCKET_CONNECTION_CLOSING =
//    Event.OnProtocolEvent(mock<ProtocolEvent.OnClosing>())
// internal val ON_WEB_SOCKET_CONNECTION_CLOSED =
//    Event.OnProtocolEvent(mock<ProtocolEvent.OnClosed>())
// internal val ON_WEB_SOCKET_CONNECTION_FAILED =
//    Event.OnProtocolEvent(mock<ProtocolEvent.OnFailed>())
//
// internal val ON_RETRY = Event.OnShouldConnect

// internal val ON_STATE_CHANGE_TO_CONNECTING = Event.OnStateChange(mock<State.Connecting>())
// internal val ON_STATE_CHANGE_TO_CONNECTED = Event.OnStateChange(mock<State.Connected>())
// internal val ON_STATE_CHANGE_TO_WAITING_TO_RETRY = Event.OnStateChange(mock<State.WaitingToRetry>())
// internal val ON_STATE_CHANGE_TO_DISCONNECTING = Event.OnStateChange(mock<State.Disconnecting>())
// internal val ON_STATE_CHANGE_TO_DISCONNECTED = Event.OnStateChange(mock<State.Disconnected>())
// internal val ON_STATE_CHANGE_TO_DESTROYED = Event.OnStateChange(mock<State.Destroyed>())
//
// internal val SERVER_MESSAGES = listOf(
//    ON_LIFECYCLE_START,
//    ON_STATE_CHANGE_TO_CONNECTING,
//    ON_WEB_SOCKET_CONNECTION_OPENED,
//    ON_STATE_CHANGE_TO_CONNECTED,
//    ON_WEB_SOCKET_MESSAGE_RECEIVED,
//    ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED,
//    ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED,
//    ON_WEB_SOCKET_MESSAGE_RECEIVED,
//    ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED
// )
//
// internal val SERVER_CLOSURE = listOf(
//    ON_LIFECYCLE_START,
//    ON_STATE_CHANGE_TO_CONNECTING,
//    ON_WEB_SOCKET_CONNECTION_OPENED,
//    ON_STATE_CHANGE_TO_CONNECTED,
//    ON_WEB_SOCKET_MESSAGE_RECEIVED,
//    ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED,
//    ON_WEB_SOCKET_CONNECTION_CLOSING,
//    ON_WEB_SOCKET_CONNECTION_CLOSED,
//    ON_STATE_CHANGE_TO_WAITING_TO_RETRY,
//    ON_RETRY,
//    ON_STATE_CHANGE_TO_CONNECTING,
//    ON_STATE_CHANGE_TO_WAITING_TO_RETRY
// )
//
// internal val CLIENT_CLOSURE = listOf(
//    ON_LIFECYCLE_START,
//    ON_STATE_CHANGE_TO_CONNECTING,
//    ON_WEB_SOCKET_CONNECTION_OPENED,
//    ON_STATE_CHANGE_TO_CONNECTED,
//    ON_LIFECYCLE_STOP,
//    ON_STATE_CHANGE_TO_DISCONNECTING,
//    ON_WEB_SOCKET_CONNECTION_CLOSING,
//    ON_WEB_SOCKET_CONNECTION_CLOSED,
//    ON_STATE_CHANGE_TO_DISCONNECTED,
//    ON_LIFECYCLE_TERMINATED,
//    ON_STATE_CHANGE_TO_DESTROYED
// )
//
// internal val CLIENT_ABORT = listOf(
//    ON_LIFECYCLE_START,
//    ON_STATE_CHANGE_TO_CONNECTING,
//    ON_WEB_SOCKET_CONNECTION_OPENED,
//    ON_STATE_CHANGE_TO_CONNECTED,
//    ON_LIFECYCLE_TERMINATED,
//    ON_STATE_CHANGE_TO_DISCONNECTING,
//    ON_WEB_SOCKET_CONNECTION_FAILED,
//    ON_STATE_CHANGE_TO_DISCONNECTED,
//    ON_LIFECYCLE_TERMINATED,
//    ON_STATE_CHANGE_TO_DESTROYED
// )