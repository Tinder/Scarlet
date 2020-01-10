package com.tinder.scarlet.stomp.okhttp

interface Connection {

    /**
     * Send the given message.
     * @param message the message
     * @return a
     * message was successfully sent
     */
    fun send(message: String): Boolean

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