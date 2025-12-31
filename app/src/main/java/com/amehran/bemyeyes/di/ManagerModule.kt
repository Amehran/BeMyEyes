package com.amehran.bemyeyes.di

import android.content.Context
import com.amehran.bemyeyes.data.repository.AndroidTextToSpeechManager
import com.amehran.bemyeyes.domain.repository.TextToSpeechManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ManagerModule {

    @Provides
    @Singleton
    fun provideTextToSpeechManager(
        @ApplicationContext context: Context
    ): TextToSpeechManager {
        return AndroidTextToSpeechManager(context)
    }

    @Provides
    @Singleton
    fun provideVibrationManager(
        @ApplicationContext context: Context
    ): com.amehran.bemyeyes.domain.repository.VibrationManager {
        return com.amehran.bemyeyes.data.repository.SystemVibrationManager(context)
    }

    @Provides
    @Singleton
    fun provideSpeechManager(
        @ApplicationContext context: Context
    ): com.amehran.bemyeyes.domain.repository.SpeechManager {
        return com.amehran.bemyeyes.data.repository.AndroidSpeechManager(context)
    }
}
