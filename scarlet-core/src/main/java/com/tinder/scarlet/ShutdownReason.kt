/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

/**
 * Used to initiate a shutdown of a WebSocket.
 *
 * @property code Status code as defined by [Section 7.4 of RFC 6455](http://tools.ietf.org/html/rfc6455#section-7.4)
 * or `0`.
 * @property reason Reason for shutting down.
 */
data class ShutdownReason(val code: Int, val reason: String) {
    companion object {
        private val NORMAL_CLOSURE_STATUS_CODE = 1000
        private val NORMAL_CLOSURE_REASON = "Normal closure"

        @JvmField
        val GRACEFUL = ShutdownReason(NORMAL_CLOSURE_STATUS_CODE, NORMAL_CLOSURE_REASON)
    }
}
