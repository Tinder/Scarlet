Scarlet
===
[![CircleCI](https://circleci.com/gh/Tinder/Scarlet.svg?style=svg)](https://circleci.com/gh/Tinder/Scarlet)
[![Release](https://jitpack.io/v/tinder/scarlet.svg)](https://jitpack.io/#tinder/scarlet)

A Retrofit inspired WebSocket client for Kotlin, Java, and Android.

Tutorial
---
- [Taming WebSocket with Scarlet][tutorial]
- [A talk][slides] at [Conference for Kotliners][kotliners]

Usage
---
In this example, we read the realtime Bitcoin price from [Gdax WebSocket Feed][gdax-websocket-feed].
For more information, please check out the [demo app][demo-app].

Declare a WebSocket client using an interface:

~~~ kotlin
interface GdaxService {
	@Receive
	fun observeOnConnectionOpenedEvent(): Flowable<WebSocket.Event.OnConnectionOpen<*>>
	@Send
	fun sendSubscribe(subscribe: Subscribe)
	@Receive
 	fun observeTicker(): Flowable<Ticker>
}
~~~

Use Scarlet to create an implementation:

~~~ kotlin
val scarletInstance = Scarlet.Builder()
    .webSocketFactory(okHttpClient.newWebSocketFactory("wss://ws-feed.gdax.com"))
    .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
    .addStreamAdapterFactory(RxJava2StreamAdapter.Factory())
    .build()

val gdaxService = scarletInstance.create<GdaxService>()
~~~

Send a `Subscribe` message upon connection open and the server will start streaming tickers which contain the latest price.


~~~ kotlin
val BITCOIN_TICKER_SUBSCRIBE_MESSAGE = Subscribe(
    productIds = listOf("BTC-USD"),
    channels = listOf("ticker")
)

gdaxService.observeOnConnectionOpenedEvent()
    .subscribe({
        gdaxService.sendSubscribe(BITCOIN_TICKER_SUBSCRIBE_MESSAGE)
    })

gdaxService.observeTicker()
    .subscribe({ ticker ->
        Log.d("Bitcoin price is ${ticker.price} at ${ticker.time}")
    })
~~~

###  Android
Scarlet is driven by a [StateMachine](https://github.com/Tinder/StateMachine).

<img width="600 px" src="/example/scarlet-state-machine.png"/>

TODO

Download
--------
While we are working on Bintray support, Scarlet is available via [JitPack](jitpack).

##### Maven:
```xml
<repository>
	<id>jitpack.io</id>
	<url>https://jitpack.io</url>
</repository>
<dependency>
	<groupId>com.github.tinder.scarlet</groupId>
	<artifactId>scarlet</artifactId>
	<version>0.1.3</version>
</dependency>
```

##### Gradle:
```groovy
repositories {
	// ...
	maven { url "https://jitpack.io" }
}

implementation 'com.github.tinder.scarlet:scarlet:0.1.2'
```

### Plug-in Roadmap
`WebSocket.Factory`
- [x] `OkHttpClient`
- [x] `MockHttpServer`

`MessageAdapter.Factory`
- [x] `moshi`
- [x] `gson`
- [x] `protobuf`
- [ ] `jackson`
- [ ] `simple-xml`

`StreamAdapter.Factory`
- [x] `RxJava2`
- [x] `RxJava1`
- [x] `Kotlin Coroutine`

`Lifecycle`
- [x] `AndroidLifecycle`

`BackoffStrategy`
- [x] `Linear`
- [x] `Exponential`
- [x] `ExponentialWithJitter`

Copyright
---
~~~
Copyright (c) 2018, Match Group, LLC
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Match Group, LLC nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL MATCH GROUP, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
~~~

 [gdax-websocket-feed]: https://docs.gdax.com/#websocket-feed
 [latest-jar]: https://tinder.jfrog.io/tinder/webapp/#/artifacts/browse/tree/General/libs-release-local/com/tinder/scarlet/scarlet
 [demo-app]: /demo/src/main/java/com/tinder/app
 [tutorial]: https://tech.gotinder.com/taming-websocket-with-scarlet/
 [slides]: https://speakerdeck.com/zhxnlai/taming-websocket-with-scarlet
 [kotliners]:https://www.conferenceforkotliners.com/
 [jitpack]: https://jitpack.io/
