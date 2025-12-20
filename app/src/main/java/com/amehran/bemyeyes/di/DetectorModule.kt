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
        return com.amehran.bemyeyes.data.repository.MediaPipeObjectDetector(
            context = context,
            modelPath = "efficientdet-lite0.tflite", // Use Lite0 for better speed/smoothing balance
            confidenceThreshold = 0.5f // MediaPipe is robust, 0.5 is safe
        )
    }
}
