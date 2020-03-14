/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.testutils

import com.tinder.scarlet.Stream
import io.reactivex.processors.PublishProcessor
import org.assertj.core.api.Assertions.assertThat

class TestStreamObserver<out T : Any>(stream: Stream<T>) {
    private val publishProcessor = PublishProcessor.create<T>()
    private val testSubscriber = publishProcessor.test()

    init {
        stream.subscribe(publishProcessor)
    }

    val values: List<T>
        get() = testSubscriber.values()

    val errors: List<Throwable>
        get() = testSubscriber.errors()

    val completions: Long
        get() = testSubscriber.completions()

    fun awaitCountAndCheck(exactly: Int) {
        testSubscriber.assertNoErrors()
        testSubscriber.awaitCount(exactly)
        assertThat(values).describedAs("values: $values").hasSize(exactly)
    }

    fun awaitCountAtLeast(atLeast: Int) {
        testSubscriber.assertNoErrors()
        testSubscriber.awaitCount(atLeast)
        assertThat(values).isNotEmpty
    }

    fun awaitValues(vararg valueAsserts: ValueAssert<Any>) {
        awaitCountAndCheck(valueAsserts.size)
        testSubscriber.assertNoErrors()
        valueAsserts.zip(values).forEachIndexed { index, (valueAssert, value) ->
            try {
                valueAssert.execute(value)
            } catch (cause: AssertionError) {
                throw AssertionError("assertion at index $index failed", cause)
            }
        }
    }

    fun awaitValuesIncluding(vararg valueAsserts: ValueAssert<Any>) {
        testSubscriber.assertNoErrors()
        valueAsserts.zip(values).forEachIndexed { index, (valueAssert, value) ->
            try {
                valueAssert.execute(value)
            } catch (cause: AssertionError) {
                throw AssertionError("assertion at index $index failed", cause)
            }
        }
    }
}
