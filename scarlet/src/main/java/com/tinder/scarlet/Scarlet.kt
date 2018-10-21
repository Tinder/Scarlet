/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import com.tinder.scarlet.Scarlet.Builder
import com.tinder.scarlet.internal.Service
import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.internal.servicemethod.EventMapper
import com.tinder.scarlet.internal.servicemethod.MessageAdapterResolver
import com.tinder.scarlet.internal.servicemethod.ServiceMethod
import com.tinder.scarlet.internal.servicemethod.ServiceMethodExecutor
import com.tinder.scarlet.internal.servicemethod.StreamAdapterResolver
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.lifecycle.DefaultLifecycle
import com.tinder.scarlet.messageadapter.builtin.BuiltInMessageAdapterFactory
import com.tinder.scarlet.retry.BackoffStrategy
import com.tinder.scarlet.retry.ExponentialBackoffStrategy
import com.tinder.scarlet.streamadapter.builtin.BuiltInStreamAdapterFactory
import io.reactivex.schedulers.Schedulers
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

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
class Scarlet internal constructor(
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
            when {
                runtimePlatform.isDefaultMethod(method) -> runtimePlatform.invokeDefaultMethod(method, serviceInterface, proxy, args)
                isJavaObjectMethod(method) -> handleJavaObjectMethod(method, serviceInstance, serviceInterface, proxy, args)
                else -> serviceInstance.execute(method, args)
            }
        }

    private fun isJavaObjectMethod(method: Method) = method.declaringClass == Object::class.java

    private fun handleJavaObjectMethod(method: Method, serviceInstance: Service, serviceInterface: Class<*>, proxy: Any, args: Array<out Any>): Any {
        return when {
            isEquals(method) -> proxy === args[0]
            isToString(method) -> "Scarlet service implementation for ${serviceInterface.name}"
            isHashCode(method) -> serviceInstance.hashCode()
            else -> throw IllegalStateException("Cannot execute $method")
        }
    }

    private fun isHashCode(method: Method) = method.name == "hashCode" && method.parameterTypes.isEmpty()

    private fun isToString(method: Method) = method.name == "toString" && method.parameterTypes.isEmpty()

    private fun isEquals(method: Method) =
        method.name == "equals" && arrayOf(Object::class.java).contentEquals(method.parameterTypes)

    /**
     * Build a new [Scarlet] instance.
     *
     * [webSocketFactory] is required. All other methods are optional.
     */
    class Builder {
        private var webSocketFactory: WebSocket.Factory? = null
        private var lifecycle: Lifecycle = DEFAULT_LIFECYCLE
        private var backoffStrategy: BackoffStrategy = DEFAULT_RETRY_STRATEGY
        private val messageAdapterFactories = mutableListOf<MessageAdapter.Factory>()
        private val streamAdapterFactories = mutableListOf<StreamAdapter.Factory>()
        private val platform = RuntimePlatform.get()

        fun webSocketFactory(factory: WebSocket.Factory): Builder = apply { webSocketFactory = factory }

        /**
         * Set the [Lifecycle] that determines when to connect and disconnect.
         */
        fun lifecycle(lifecycle: Lifecycle): Builder = apply { this.lifecycle = lifecycle }

        fun backoffStrategy(backoffStrategy: BackoffStrategy): Builder =
            apply { this.backoffStrategy = backoffStrategy }

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
        fun build(): Scarlet = Scarlet(platform, createServiceFactory())

        private fun createServiceFactory(): Service.Factory = Service.Factory(
            createConnectionFactory(),
            createServiceMethodExecutorFactory()
        )

        private fun createConnectionFactory(): Connection.Factory =
            Connection.Factory(lifecycle, checkNotNull(webSocketFactory), backoffStrategy, DEFAULT_SCHEDULER)

        private fun createServiceMethodExecutorFactory(): ServiceMethodExecutor.Factory {
            val messageAdapterResolver = createMessageAdapterResolver()
            val streamAdapterResolver = createStreamAdapterResolver()
            val eventMapperFactory = EventMapper.Factory(messageAdapterResolver)
            val sendServiceMethodFactory = ServiceMethod.Send.Factory(messageAdapterResolver)
            val receiveServiceMethodFactory = ServiceMethod.Receive.Factory(
                DEFAULT_SCHEDULER, eventMapperFactory, streamAdapterResolver
            )
            return ServiceMethodExecutor.Factory(platform, sendServiceMethodFactory, receiveServiceMethodFactory)
        }

        private fun createMessageAdapterResolver(): MessageAdapterResolver =
            MessageAdapterResolver(messageAdapterFactories.apply { add(BuiltInMessageAdapterFactory()) }.toList())

        private fun createStreamAdapterResolver(): StreamAdapterResolver =
            StreamAdapterResolver(streamAdapterFactories.apply { add(BuiltInStreamAdapterFactory()) }.toList())

        private companion object {
            private val DEFAULT_LIFECYCLE = DefaultLifecycle()
            private val RETRY_BASE_DURATION = 1000L
            private val RETRY_MAX_DURATION = 10000L
            private val DEFAULT_RETRY_STRATEGY =
                ExponentialBackoffStrategy(RETRY_BASE_DURATION, RETRY_MAX_DURATION)
            private val DEFAULT_SCHEDULER = Schedulers.computation() // TODO same thread option for debugging
        }
    }
}
