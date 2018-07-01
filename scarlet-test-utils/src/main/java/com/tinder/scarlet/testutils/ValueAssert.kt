/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.testutils

class ValueAssert<out T : Any> {
    private val assertions = mutableListOf<(Any) -> Unit>()

    fun assert(assertion: T.() -> Unit): ValueAssert<T> = apply {
        assertions.add {
            @Suppress("UNCHECKED_CAST")
            (it as T).assertion()
        }
    }

    fun execute(value: Any) = assertions.forEach { it(value) }
}
