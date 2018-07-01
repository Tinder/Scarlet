/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("FlowableUtils")

package com.tinder.scarlet.utils

import io.reactivex.Flowable

fun <T> Flowable<T>.toStream() = FlowableStream(this)
