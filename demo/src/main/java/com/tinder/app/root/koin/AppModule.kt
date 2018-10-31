/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.root.koin

import com.facebook.stetho.DumperPluginsProvider
import com.facebook.stetho.Stetho
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module.module

val appModule = module {

    factory {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BASIC)
            )
            .build()
    }

    single("foreground") { AndroidLifecycle.ofApplicationForeground(get()) }

    single {
        Stetho.newInitializerBuilder(get())
            .enableDumpapp(get())
            .enableWebKitInspector(get())
            .build()
    }

    factory {
        val plugins = Stetho.DefaultDumperPluginsBuilder(get()).finish()
        DumperPluginsProvider { plugins }
    }

    factory {
        Stetho.defaultInspectorModulesProvider(get())
    }
}