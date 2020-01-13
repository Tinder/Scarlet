package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.stomp.core.StompMessage

interface Connection {

    /**
     * Send the given message.
     * @param message the message
     * @return true if the message was enqueued.
     */
    fun send(message: StompMessage): Boolean

    /**
     * Register a task to invoke after a period of read inactivity.
     * @param runnable the task to invoke
     * @param duration the amount of inactive time in milliseconds
     */
    fun onReadInactivity(duration: Long, runnable: () -> Unit)

    /**
     * Register a task to invoke after a period of write inactivity.
     * @param runnable the task to invoke
     * @param duration the amount of inactive time in milliseconds
     */
    fun onWriteInactivity(duration: Long, runnable: () -> Unit)

    /**
     * Force close the connection.
     */
    fun forceClose()

    /**
     * Close the connection.
     */
    fun close()

}