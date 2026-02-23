package com.simple.posedetection.data.device

import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

private const val TAG = "DeviceCapability"

data class DeviceCapabilities(
    val hasGpu: Boolean,
    val optimalThreadCount: Int,
    val performanceTier: PerformanceTier
)

enum class PerformanceTier { HIGH, MID, LOW }

object DeviceCapabilityDetector {

    fun detect(): DeviceCapabilities {
        val gpuAvailable = probeGpuDelegate()
        val threadCount = optimalThreadCount()
        return DeviceCapabilities(
            hasGpu = gpuAvailable,
            optimalThreadCount = threadCount,
            performanceTier = classifyDevice(gpuAvailable, threadCount)
        )
    }

    // Two-level probe so we only report GPU=true when it genuinely works:
    //   1. CompatibilityList.isDelegateSupportedOnThisDevice — fast driver query that
    //      catches unsupported hardware without allocating anything heavy.
    //   2. Actually create + immediately close a GpuDelegate — flushes out drivers
    //      that report support but crash on real allocation (common on some Mali/Adreno
    //      combinations on older firmware).
    //
    // The entire block catches Throwable, not just Exception, because a missing
    // native library surfaces as NoClassDefFoundError or UnsatisfiedLinkError —
    // both are Errors, not Exceptions, and would otherwise be uncaught.
    private fun probeGpuDelegate(): Boolean {
        return try {
            CompatibilityList().use { compatList ->
                if (!compatList.isDelegateSupportedOnThisDevice) {
                    Log.d(TAG, "GPU delegate: not supported on this device")
                    return false
                }
                // Dry-run: allocate and immediately release to surface driver bugs
                GpuDelegate().close()
                Log.d(TAG, "GPU delegate: probe OK")
                true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "GPU delegate probe failed (${t.javaClass.simpleName}): ${t.message}")
            false
        }
    }

    private fun optimalThreadCount(): Int {
        // Use half of available cores, capped at 4.
        // Using all cores starves the UI thread; beyond 4 threads TFLite
        // CPU inference rarely improves and sync overhead dominates.
        return when (Runtime.getRuntime().availableProcessors()) {
            in 8..Int.MAX_VALUE -> 4
            in 4..7             -> 2
            else                      -> 1
        }
    }

    private fun classifyDevice(hasGpu: Boolean, threadCount: Int): PerformanceTier = when {
        hasGpu              -> PerformanceTier.HIGH
        threadCount >= 4    -> PerformanceTier.MID
        else                -> PerformanceTier.LOW
    }
}
