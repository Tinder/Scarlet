/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.di

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.retry.BackoffStrategy
import dagger.BindsInstance
import dagger.Component
import io.reactivex.Scheduler
import javax.inject.Singleton

@Singleton
@Component(modules = [CommonModule::class])
internal interface ScarletComponent {

    fun scarlet(): Scarlet

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun webSocketFactory(factory: WebSocket.Factory): Builder

        @BindsInstance
        fun lifecycle(lifecycle: Lifecycle): Builder

        @BindsInstance
        fun schdeduler(scheduler: Scheduler): Builder

        @BindsInstance
        fun backoffStrategy(backoffStrategy: BackoffStrategy): Builder

        @BindsInstance
        fun messageAdapterFactories(messageAdapterFactories: List<MessageAdapter.Factory>): Builder

        @BindsInstance
        fun streamAdapterFactories(streamAdapterFactories: List<StreamAdapter.Factory>): Builder

        fun build(): ScarletComponent
    }
}
