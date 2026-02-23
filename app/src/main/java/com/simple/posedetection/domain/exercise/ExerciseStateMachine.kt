package com.simple.posedetection.domain.exercise

import com.simple.posedetection.domain.model.PoseFeatures

/**
 * Deterministic state machine for squat (side view).
 * Transitions: START -> DESCENDING -> BOTTOM -> ASCENDING -> START.
 * Rep counted only on full cycle. Debounces to avoid oscillation.
 */
class ExerciseStateMachine(
    private val config: ExerciseConfig
) {
    private var state = ExerciseState(currentPhase = Phase.START, repCount = 0)
    private var reachedBottomThisCycle = false

    fun update(features: PoseFeatures?): ExerciseState {
        if (features == null) return state
        val kneeAvg = features.kneeAngleAvg() ?: return state
        val kneeMax = features.kneeAngleMax() ?: return state
        val standingThreshold = config.standingKneeAngleDeg
        val depthThreshold = config.depthKneeAngleDeg
        val n = config.debounceFrames.coerceAtLeast(1)

        // Use kneeMax for standing (START/ASCENDING): at least one leg extended = standing.
        // Use kneeAvg for depth (DESCENDING/BOTTOM): average depth for phase transitions.
        val candidatePhase = when (state.currentPhase) {
            Phase.START ->
                if (kneeMax < standingThreshold) Phase.DESCENDING else Phase.START
            Phase.DESCENDING ->
                if (kneeAvg <= depthThreshold) Phase.BOTTOM else Phase.DESCENDING
            Phase.BOTTOM ->
                if (kneeAvg > depthThreshold) Phase.ASCENDING else Phase.BOTTOM
            Phase.ASCENDING ->
                if (kneeMax >= standingThreshold) Phase.START else Phase.ASCENDING
            Phase.END ->
                Phase.START
        }

        val sameCandidate = candidatePhase == state.lastCandidatePhase
        val nextCounter = if (sameCandidate) state.debounceCounter + 1 else 0
        val nextCandidate = candidatePhase

        if (sameCandidate && nextCounter >= n) {
            val newPhase = candidatePhase
            val newRepCount = if (state.currentPhase == Phase.ASCENDING && newPhase == Phase.START) {
                if (config.minDepthAtBottom && reachedBottomThisCycle) state.repCount + 1 else state.repCount
            } else state.repCount
            if (newPhase == Phase.BOTTOM) reachedBottomThisCycle = true
            if (newPhase == Phase.START) reachedBottomThisCycle = false
            state = ExerciseState(
                currentPhase = newPhase,
                repCount = newRepCount,
                debounceCounter = 0,
                lastCandidatePhase = null
            )
        } else {
            state = state.copy(
                debounceCounter = nextCounter,
                lastCandidatePhase = nextCandidate
            )
        }
        return state
    }

    fun currentState(): ExerciseState = state
}
