package com.simple.posedetection.domain.exercise.squat

import com.simple.posedetection.domain.exercise.ExerciseConfig
import com.simple.posedetection.domain.exercise.ExerciseDefinition
import com.simple.posedetection.domain.exercise.Phase
import com.simple.posedetection.domain.exercise.Validator

object SquatFitnessConfig {
    /** Loose: depth <= 115° (catch brief bottom), torso < 40°, standing when max knee >= 138°, 1-frame debounce. */
    val config: ExerciseConfig = ExerciseConfig(
        depthKneeAngleDeg = 115f,
        torsoToleranceDeg = 40f,
        kneeOverToeTolerance = 0.05f,
        debounceFrames = 1,
        minDepthAtBottom = true,
        standingKneeAngleDeg = 138f
    )
}

object SquatRehabConfig {
    /** Strict: knee < 90°, torso < 15°, knee-over-toe rule; standing >= 138°, debounce 3. */
    val config: ExerciseConfig = ExerciseConfig(
        depthKneeAngleDeg = 90f,
        torsoToleranceDeg = 15f,
        kneeOverToeTolerance = 0.02f,
        debounceFrames = 3,
        minDepthAtBottom = true,
        standingKneeAngleDeg = 138f
    )
}

/**
 * Squat exercise definition. Validators are supplied when building the definition
 * (e.g. in DI or when creating the pipeline) so they can use the same config.
 */
fun createSquatDefinition(
    config: ExerciseConfig,
    validators: List<Validator>
): ExerciseDefinition = ExerciseDefinition(
    name = "Squat",
    phases = listOf(Phase.START, Phase.DESCENDING, Phase.BOTTOM, Phase.ASCENDING, Phase.END),
    validators = validators,
    config = config
)
