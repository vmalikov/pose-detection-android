package com.simple.posedetection.domain.exercise

import com.simple.posedetection.domain.model.PoseFeatures

interface Validator {
    fun validate(features: PoseFeatures, phase: Phase): ValidationResult
}
