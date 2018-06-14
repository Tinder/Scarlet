/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

@file:JvmName("FlowableUtils")

package com.tinder.scarlet.utils

import io.reactivex.Flowable

fun <T> Flowable<T>.toStream() = FlowableStream(this)
