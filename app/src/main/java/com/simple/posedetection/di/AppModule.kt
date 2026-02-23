package com.simple.posedetection.di

import android.content.Context
import com.simple.posedetection.data.device.DeviceCapabilityDetector
import com.simple.posedetection.data.detector.PoseDetectorImpl
import com.simple.posedetection.domain.exercise.ExerciseDefinition
import com.simple.posedetection.domain.exercise.validator.BackAngleValidator
import com.simple.posedetection.domain.exercise.validator.DepthValidator
import com.simple.posedetection.domain.exercise.validator.KneeForwardValidator
import com.simple.posedetection.domain.exercise.squat.SquatFitnessConfig
import com.simple.posedetection.domain.exercise.squat.createSquatDefinition
import com.simple.posedetection.domain.port.PoseDetector
import com.simple.posedetection.domain.processor.PoseProcessor
import com.simple.posedetection.domain.processor.PoseProcessorConfig
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

    @Singleton
    @Provides
    fun providePoseProcessor(): PoseProcessor =
        PoseProcessor(PoseProcessorConfig())

    @Singleton
    @Provides
    fun provideExerciseDefinition(): ExerciseDefinition {
        val config = SquatFitnessConfig.config
        return createSquatDefinition(
            config = config,
            validators = listOf(
                DepthValidator(config),
                BackAngleValidator(config),
                KneeForwardValidator(config)
            )
        )
    }
}