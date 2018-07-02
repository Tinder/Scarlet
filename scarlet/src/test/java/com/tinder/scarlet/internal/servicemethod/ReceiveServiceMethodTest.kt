/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.only
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Event
import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.streamadapter.rxjava2.FlowableStreamAdapter
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.processors.ReplayProcessor
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ReceiveServiceMethodTest {

    private val webSocketClient = mock<Connection>()
    private val scheduler = TestScheduler()
    private val eventMapper = mock<EventMapper<Any>>()
    private val serviceMethod = ServiceMethod.Receive(
        eventMapper, webSocketClient, scheduler, FlowableStreamAdapter()
    )

    @Test
    fun execute_shouldBeLazy() {
        // When
        serviceMethod.execute()
        scheduler.triggerActions()

        // Then
        then(webSocketClient).should(never()).observeEvent()
    }

    @Test
    fun execute_shouldOnlyObserveAndTransformWebSocketClientEvents() {
        // Given
        val event1 = mock<Event>()
        val event2 = mock<Event>()
        val replayProcessor = ReplayProcessor.create<Event>()
            .apply { onNext(event1) }
            .apply { onNext(event2) }
        given(webSocketClient.observeEvent()).willReturn(replayProcessor)
        given(eventMapper.mapToData(any())).willReturn(Maybe.empty())

        // When
        val flowable = serviceMethod.execute() as Flowable<*>
        val testSubscriber = flowable.test()
        scheduler.triggerActions()

        // Then
        then(webSocketClient).should(only()).observeEvent()
        then(eventMapper).should().mapToData(event1)
        then(eventMapper).should().mapToData(event2)
        testSubscriber.assertNotTerminated()
    }
}
