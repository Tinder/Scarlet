/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.root.inject

import android.app.Application
import com.facebook.stetho.DumperPluginsProvider
import com.facebook.stetho.InspectorModulesProvider
import com.facebook.stetho.Stetho
import dagger.Module
import dagger.Provides

@Module
class StethoModule {

    @Provides
    internal fun provideStethoInitializer(
        initializerBuilder: Stetho.InitializerBuilder,
        inspectorModulesProvider: InspectorModulesProvider,
        dumperPluginsProvider: DumperPluginsProvider
    ): Stetho.Initializer {
        return initializerBuilder
            .enableDumpapp(dumperPluginsProvider)
            .enableWebKitInspector(inspectorModulesProvider)
            .build()
    }

    @Provides
    internal fun provideStethoInitializerBuilder(application: Application): Stetho.InitializerBuilder {
        return Stetho.newInitializerBuilder(application)
    }

    @Provides
    internal fun provideStethoDumperPluginsProvider(
        application: Application
    ): DumperPluginsProvider {
        val plugins = Stetho.DefaultDumperPluginsBuilder(application).finish()
        return DumperPluginsProvider { plugins }
    }

    @Provides
    internal fun provideInspectorModulesProvider(application: Application): InspectorModulesProvider {
        return Stetho.defaultInspectorModulesProvider(application)
    }
}
