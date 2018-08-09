/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.root.inject

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class CommonModule {

    @Provides
    fun provideApplicationContext(application: Application): Context {
        return application
    }
}
