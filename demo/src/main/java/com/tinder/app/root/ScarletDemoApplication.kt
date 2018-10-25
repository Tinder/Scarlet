/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.root

import android.app.Application
import androidx.multidex.MultiDex
import com.facebook.stetho.Stetho
import com.tinder.app.echo.inject.DaggerEchoBotComponent
import com.tinder.app.echo.inject.EchoBotComponent
import com.tinder.app.gdax.inject.DaggerGdaxComponent
import com.tinder.app.gdax.inject.GdaxComponent
import com.tinder.app.root.inject.ApplicationComponent
import com.tinder.app.root.inject.DaggerApplicationComponent
import timber.log.Timber
import javax.inject.Inject

class ScarletDemoApplication : Application(),
    ApplicationComponent.ComponentProvider,
    GdaxComponent.ComponentProvider,
    EchoBotComponent.ComponentProvider {
    override lateinit var applicationComponent: ApplicationComponent
    override lateinit var echoBotComponent: EchoBotComponent
    override lateinit var gdaxComponent: GdaxComponent

    @Inject
    lateinit var stethoInitializer: Stetho.Initializer

    override fun onCreate() {
        super.onCreate()
        applicationComponent = DaggerApplicationComponent.builder()
            .application(this)
            .build()
        applicationComponent.inject(this)

        echoBotComponent = DaggerEchoBotComponent.builder()
            .dependency(applicationComponent)
            .build()

        gdaxComponent = DaggerGdaxComponent.builder()
            .dependency(applicationComponent)
            .build()

        MultiDex.install(this)
        Timber.plant(Timber.DebugTree())
        Stetho.initialize(stethoInitializer)
    }
}
