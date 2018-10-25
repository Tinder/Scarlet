package com.tinder.scarlet.testutils.v2

import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.v2.lifecycle.LifecycleRegistry
import com.tinder.scarlet.websocket.WebSocketEvent
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class OkHttpWebSocketConnection(

) : TestRule {

    val mockWebServer = MockWebServer()

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain.outerRule(mockWebServer)
            .around(Resource(mockWebServer))
            .apply(base, description)
    }

    class Resource(
        private val mockWebServer: MockWebServer
    ) : ExternalResource() {
        private val serverUrlString by lazy { mockWebServer.url("/").toString() }
        private val serverLifecycleRegistry = LifecycleRegistry()
        private lateinit var serverWebSocketEventObserver: TestStreamObserver<WebSocketEvent>

        private val clientLifecycleRegistry = LifecycleRegistry()
        private lateinit var clientWebSocketEventObserver: TestStreamObserver<WebSocketEvent>


        override fun before() {
            mockWebServer
        }

        override fun after() {
        }


    }
}