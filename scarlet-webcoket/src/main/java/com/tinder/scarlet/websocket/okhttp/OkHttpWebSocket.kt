/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Scarlet2
import com.tinder.scarlet.Stream
import org.reactivestreams.Subscriber

class OkHttpWebSocket : Scarlet2.Connection {
    override fun start(observer: Stream.Observer<Scarlet2.ConnectionStateTransition>): Stream.Disposable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subscribe(s: Subscriber<in Scarlet2.ConnectionStateTransition>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNext(data: Scarlet2.Event) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onError(throwable: Throwable) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onComplete() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

