/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.v3.utils

interface RequestFactory<REQUEST : Any> {

    fun createRequest(): REQUEST
}


class StaticRequestFactory<REQUEST : Any>(
    private val request: REQUEST
) : RequestFactory<REQUEST> {
    override fun createRequest() = request

}
