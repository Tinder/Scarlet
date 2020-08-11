package com.tinder.scarlet.stomp.okhttp.client

enum class WebSocketCode(val code: Int, val reason: String? = null) {

    CLOSE_NORMAL(1000, "Normal closure"),
    CLOSE_GOING_AWAY(1001, "Unexpected closure from the Server"),
    CLOSED_NO_STATUS(1005, "Expected close status, received none"), ;

    companion object {

        /**
         * @return true is this unexpected situation error code
         */
        fun isUnexpectedClose(code: Int): Boolean {
            return code == CLOSE_GOING_AWAY.code || code == CLOSED_NO_STATUS.code
        }
    }
}