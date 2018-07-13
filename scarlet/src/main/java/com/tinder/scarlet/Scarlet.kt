/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import com.tinder.scarlet.Scarlet.Builder
import com.tinder.scarlet.internal.Service
import com.tinder.scarlet.internal.di.DaggerScarletComponent
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.lifecycle.DefaultLifecycle
import com.tinder.scarlet.messageadapter.builtin.BuiltInMessageAdapterFactory
import com.tinder.scarlet.retry.BackoffStrategy
import com.tinder.scarlet.retry.ExponentialBackoffStrategy
import com.tinder.scarlet.streamadapter.builtin.BuiltInStreamAdapterFactory
import io.reactivex.schedulers.Schedulers
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import javax.inject.Inject

/**
 * Scarlet is a [Retrofit](http://square.github.io/retrofit/)-inspired [WebSocket](https://tools.ietf.org/html/rfc6455)
 * client for Kotlin and Java. Based on annotations, it turns an interface into a WebSocket API that creates and manages
 * a WebSocket connection. You may create Scarlet instances using [the builder][Builder] and generate an implementation
 * of your interface using [create].
 *
 * For example,
 *
 * ~~~kotlin
 * val scarlet = new Scarlet.Builder()
 *     .lifecycle(AndroidLifecycle())
 *     .backoffStrategy(ExponentialBackoffStrategy())
 *     .addMessageAdapterFactory(ProtobufMessageAdapter.Factory())
 *     .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
 *     .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
 *     .build()
 *
 * val service = scarlet.create<MyService>("wss://example.com")
 * ~~~
 */
class Scarlet @Inject internal constructor(
    private val runtimePlatform: RuntimePlatform,
    private val serviceFactory: Service.Factory
) {

    /**
     * Creates an implementation of the WebSocket API defined by the [service] interface.
     *
     * You may use the following annotations to define a WebSocket API:
     *
     * ### [@Receive][com.tinder.scarlet.ws.Receive]
     * Observe [incoming messages][Message] or [WebSocket events][WebSocket.Event]. This method takes zero parameters and returns
     * an infinite [Stream]. The generic parameter of the stream can be [WebSocket.Event], [String], [ByteArray], and [Message].
     * Custom types will be converted by [MessageAdapter.Factory]. 
     * Custom stream types will be converted by [StreamAdapter.Factory]. 
     *
     * For example:
     * ~~~kotlin
     * interface MyService {
     *   @Receive
     *   fun observeText(): Stream<String>
     *
     *   @Receive
     *   fun observeBytes(): Stream<ByteArray>
     *
     *   @Receive
     *   fun observeMessages(): Stream<Message>
     *
     *   @Receive
     *   fun observeEvents(): Stream<Event>
     * }
     * ~~~
     *
     * ### [@Send][com.tinder.scarlet.ws.Send]
     * Attempts to enqueue an outgoing message. This method takes one parameter and immediately returns a `Boolean` or
     * `Void`. By default, [String], [ByteArray], and [Message] may be used. Custom types will be converted by
     * [MessageAdapter.Factory]. 
     *
     * This method returns true if the message was enqueued. Messages that would overflow the outgoing message buffer
     * will be rejected and trigger a graceful shutdown of this web socket. This method returns false in that case, and
     * in any other case where this web socket is closing, closed, or canceled.
     *
     * For example:
     * ~~~kotlin
     * interface MyService {
     *   @Send
     *   fun sendText(text: String): Boolean
     * }
     * interface MyService2 {
     *   @Send
     *   fun sendText(text: String)
     * }
     * ~~~
     */
    fun <T> create(service: Class<T>): T = implementService(service)

    /**
     * Same as [create].
     */
    inline fun <reified T : Any> create(): T = create(T::class.java)

    private fun <T> implementService(serviceInterface: Class<T>): T {
        val serviceInstance = serviceFactory.create(serviceInterface)
        serviceInstance.startForever()
        val proxy = Proxy.newProxyInstance(
            serviceInterface.classLoader,
            arrayOf(serviceInterface),
            createInvocationHandler(serviceInterface, serviceInstance)
        )
        return serviceInterface.cast(proxy)
    }

    private fun createInvocationHandler(serviceInterface: Class<*>, serviceInstance: Service): InvocationHandler =
        InvocationHandler { proxy, method, nullableArgs ->
            val args = nullableArgs ?: arrayOf()
            if (runtimePlatform.isDefaultMethod(method)) {
                runtimePlatform.invokeDefaultMethod(method, serviceInterface, proxy, args)
            } else {
                serviceInstance.execute(method, args)
            }
        }

    /**
     * Build a new [Scarlet] instance.
     *
     * [webSocketFactory] is required. All other methods are optional.
     */
    class Builder {
        private val messageAdapterFactories = mutableListOf<MessageAdapter.Factory>()
        private val streamAdapterFactories = mutableListOf<StreamAdapter.Factory>()
        private val componentBuilder = DaggerScarletComponent.builder()
            .lifecycle(DEFAULT_LIFECYCLE)
            .backoffStrategy(DEFAULT_RETRY_STRATEGY)

        fun webSocketFactory(factory: WebSocket.Factory): Builder = apply { componentBuilder.webSocketFactory(factory) }

        /**
         * Set the [Lifecycle] that determines when to connect and disconnect.
         */
        fun lifecycle(lifecycle: Lifecycle): Builder = apply { componentBuilder.lifecycle(lifecycle) }

        fun backoffStrategy(backoffStrategy: BackoffStrategy): Builder =
            apply { componentBuilder.backoffStrategy(backoffStrategy) }

        /**
         * Add a [MessageAdapter.Factory] for supporting service method return types other than [String], [ByteArray],
         * and [Message].
         */
        fun addMessageAdapterFactory(factory: MessageAdapter.Factory): Builder =
            apply { messageAdapterFactories.add(factory) }

        /**
         * Add a [StreamAdapter.Factory] for supporting service method return types other than [Stream].
         */
        fun addStreamAdapterFactory(factory: StreamAdapter.Factory): Builder =
            apply { streamAdapterFactories.add(factory) }

        /**
         * Create a [Scarlet] instance using the configured values.
         */
        fun build(): Scarlet {
            return componentBuilder
                .schdeduler(DEFAULT_SCHEDULER)
                .messageAdapterFactories(messageAdapterFactories + BuiltInMessageAdapterFactory())
                .streamAdapterFactories(streamAdapterFactories + BuiltInStreamAdapterFactory())
                .build()
                .scarlet()
        }

        private companion object {
            private val DEFAULT_LIFECYCLE = DefaultLifecycle()
            private const val RETRY_BASE_DURATION = 1000L
            private const val RETRY_MAX_DURATION = 10000L
            private val DEFAULT_RETRY_STRATEGY = ExponentialBackoffStrategy(RETRY_BASE_DURATION, RETRY_MAX_DURATION)
            private val DEFAULT_SCHEDULER = Schedulers.computation()
        }
    }
}
