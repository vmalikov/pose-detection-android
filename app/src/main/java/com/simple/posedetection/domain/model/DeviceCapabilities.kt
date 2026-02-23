package com.simple.posedetection.domain.model

data class DeviceCapabilities(
    val hasGpu: Boolean,
    val optimalThreadCount: Int,
    val performanceTier: PerformanceTier
)

enum class PerformanceTier { HIGH, MID, LOW }
