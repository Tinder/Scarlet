/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.utils

import com.tinder.scarlet.testutils.test
import io.reactivex.processors.ReplayProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class FlowableStreamTest {

    private val processor = ReplayProcessor.create<Any>()
    private val flowableStream = FlowableStream<Any>(processor)

    @Test
    fun start_shouldSubscribeToFlowable() {
        // When
        flowableStream.start(EmptyStreamObserver())

        // Then
        assertThat(processor.hasSubscribers()).isTrue()
    }

    @Test
    fun start_givenOnNextValues_shouldForwardValues() {
        // Given
        val values = arrayOf("string", true, 505, Unit, listOf(726))
        values.forEach { processor.onNext(it) }

        // When
        val testStreamObserver = flowableStream.test()

        // Then
        assertThat(testStreamObserver.values).containsExactly(*values)
    }

    @Test
    fun start_givenOnError_shouldUnsubscribeToFlowable() {
        // Given
        processor.onError(RuntimeException())

        // When
        flowableStream.start(EmptyStreamObserver())

        // Then
        assertThat(processor.hasSubscribers()).isFalse()
    }

    @Test
    fun start_givenOnComplete_shouldUnsubscribeToFlowable() {
        // Given
        processor.onComplete()

        // When
        flowableStream.start(EmptyStreamObserver())

        // Then
        assertThat(processor.hasSubscribers()).isFalse()
    }

    @Test
    fun dispose_shouldUnsubscribeToFlowable() {
        // Given
        val disposible = flowableStream.start(EmptyStreamObserver())

        // When
        disposible.dispose()

        // Then
        assertThat(processor.hasSubscribers()).isFalse()
    }
}
