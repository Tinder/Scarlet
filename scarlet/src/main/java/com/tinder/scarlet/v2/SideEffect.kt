/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

sealed class SideEffect {
    data class ScheduleRetry(val retryCount: Int) : SideEffect()
    object CancelRetry : SideEffect()
    object OpenProtocol : SideEffect()
    object CloseProtocol : SideEffect()
    object ForceCloseProtocol : SideEffect()
}
