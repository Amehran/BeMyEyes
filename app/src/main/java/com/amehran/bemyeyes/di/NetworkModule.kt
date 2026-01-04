package com.amehran.bemyeyes.di

import com.amehran.bemyeyes.BuildConfig
import com.amehran.bemyeyes.data.remote.api.BeMyEyesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // val BASE_URL = "https://bemyeyes-backend-xz4vizivoq-uc.a.run.app/" // Cloud Run URL
        // val BASE_URL = "http://10.0.2.2:8000/" // Local Emulator URL
        val BASE_URL = "http://127.0.0.1:8000/" // ADB Reverse Tunnel
        // For safety we can trim and add it, or just trust the config.
        val baseUrl = if (BASE_URL.endsWith("/")) BASE_URL else "${BASE_URL}/"
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideBeMyEyesApi(retrofit: Retrofit): BeMyEyesApi {
        return retrofit.create(BeMyEyesApi::class.java)
    }
}
