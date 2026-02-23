package com.simple.posedetection.domain.util

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Computes the angle at point B formed by three points A-B-C (angle between BA and BC)
 * using: θ = arccos((BA·BC) / (|BA||BC|)).
 *
 * @return Angle in degrees in [0, 180], or null if vectors are degenerate (zero length).
 */
fun angleDegrees(
    a: Pair<Float, Float>,
    b: Pair<Float, Float>,
    c: Pair<Float, Float>
): Float? {
    val bax = a.first - b.first
    val bay = a.second - b.second
    val bcx = c.first - b.first
    val bcy = c.second - b.second

    val lenBa = sqrt(bax * bax + bay * bay)
    val lenBc = sqrt(bcx * bcx + bcy * bcy)
    if (lenBa < 1e-6f || lenBc < 1e-6f) return null

    var cosTheta = (bax * bcx + bay * bcy) / (lenBa * lenBc)
    cosTheta = cosTheta.coerceIn(-1f, 1f)
    val rad = acos(cosTheta)
    return (rad * 180f / PI.toFloat())
}
