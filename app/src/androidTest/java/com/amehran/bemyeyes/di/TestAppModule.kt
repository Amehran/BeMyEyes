package com.amehran.bemyeyes.di

import com.amehran.bemyeyes.domain.repository.ObjectDetector
import com.amehran.bemyeyes.domain.repository.TextToSpeechManager
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DetectorModule::class, ManagerModule::class] // Replace the real modules
)
object TestAppModule {

    @Provides
    @Singleton
    fun provideObjectDetector(): ObjectDetector {
        // Provide a mock for the tests
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideTextToSpeechManager(): TextToSpeechManager {
        // Provide a mock for the tests
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideVibrationManager(): com.amehran.bemyeyes.domain.repository.VibrationManager {
        return mockk(relaxed = true)
    }
}
