/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import io.reactivex.Flowable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness

//@RunWith(Parameterized::class)
//class NoOpStateTransitionAdapterTest(
//    private val events: List<Event>
//) {
//    @Suppress("UNUSED")
//    @get:Rule
//    val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)
//
//    private val eventMapper = EventMapper.NoOp
//
//    @Test
//    fun mapToData_shouldReturnEvents() {
//        // When
//        val results = Flowable.fromIterable(events)
//            .flatMapMaybe(eventMapper::mapToData)
//            .blockingIterable()
//            .toList()
//
//        // Then
//        assertThat(results).isEqualTo(events)
//    }
//
//    companion object {
//        @Parameterized.Parameters
//        @JvmStatic
//        fun data() = listOf(
//            param(events = SERVER_MESSAGES),
//            param(events = SERVER_CLOSURE),
//            param(events = CLIENT_CLOSURE),
//            param(events = CLIENT_ABORT)
//        )
//
//        private fun param(events: List<Event>) = arrayOf(events)
//    }
//}
