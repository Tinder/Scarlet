/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("TestUtils")

package com.tinder.scarlet.testutils

import com.tinder.scarlet.Stream
import org.assertj.core.api.Assertions.assertThat

fun <T : Any> Stream<T>.test() = TestStreamObserver(this)

inline fun <reified T : Any> any(noinline assertion: T.() -> Unit = {}): ValueAssert<T> = ValueAssert<T>()
    .assert { assertThat(this).isInstanceOf(T::class.java) }
    .assert(assertion)
