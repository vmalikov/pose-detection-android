package com.simple.posedetection.di

import android.content.Context
import com.simple.posedetection.data.device.DeviceCapabilityDetector
import com.simple.posedetection.data.detector.PoseDetectorImpl
import com.simple.posedetection.domain.port.PoseDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providePoseDetector(@ApplicationContext context: Context): PoseDetector =
        PoseDetectorImpl(context, DeviceCapabilityDetector.detect())
}