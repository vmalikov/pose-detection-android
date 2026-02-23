package com.simple.posedetection.di

import android.content.Context
import com.simple.posedetection.data.detector.PoseDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun providePoseDetector(@ApplicationContext context: Context): PoseDetector =
        PoseDetector(context)
}
