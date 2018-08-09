/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.root.inject

import android.app.Application
import com.tinder.app.echo.inject.EchoBotComponent
import com.tinder.app.gdax.inject.GdaxComponent
import com.tinder.app.root.ScarletDemoApplication
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [(CommonModule::class), (StethoModule::class)])
interface ApplicationComponent : GdaxComponent.Dependency, EchoBotComponent.Dependency {

    fun inject(application: ScarletDemoApplication)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }

    interface ComponentProvider {
        val applicationComponent: ApplicationComponent
    }
}
