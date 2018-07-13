/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.di

import com.tinder.scarlet.internal.utils.RuntimePlatform
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal class CommonModule {

    @Provides
    @Singleton
    fun provideRuntimePlatform(): RuntimePlatform {
        return RuntimePlatform.get()
    }
}
