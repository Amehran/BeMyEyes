package com.amehran.bemyeyes.di

import android.content.Context
import com.amehran.bemyeyes.data.repository.TfLiteObjectDetector
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetectorModule {

    @Provides
    @Singleton
    fun provideObjectDetector(
        @ApplicationContext context: Context
    ): ObjectDetector {
        return TfLiteObjectDetector(context)
    }
}
