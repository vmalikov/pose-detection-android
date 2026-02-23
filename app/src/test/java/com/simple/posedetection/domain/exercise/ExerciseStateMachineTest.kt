package com.simple.posedetection.domain.exercise

import com.simple.posedetection.domain.model.PoseFeatures
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExerciseStateMachineTest {

    private fun features(kneeAngleLeft: Float, kneeAngleRight: Float) = PoseFeatures(
        kneeAngleLeft = kneeAngleLeft,
        kneeAngleRight = kneeAngleRight,
        hipAngleLeft = null,
        hipAngleRight = null,
        ankleAngleLeft = null,
        ankleAngleRight = null,
        torsoAngle = null,
        hipVerticalDisplacement = null,
        kneeHorizontalDisplacementLeft = null,
        kneeHorizontalDisplacementRight = null,
    )

    private val standingThreshold = 140f
    private val depthThreshold = 100f

    private fun config(debounceFrames: Int = 1, minDepthAtBottom: Boolean = true) = ExerciseConfig(
        depthKneeAngleDeg = depthThreshold,
        torsoToleranceDeg = 25f,
        kneeOverToeTolerance = 0.05f,
        debounceFrames = debounceFrames,
        minDepthAtBottom = minDepthAtBottom,
        standingKneeAngleDeg = standingThreshold
    )

    private lateinit var machine: ExerciseStateMachine

    @Before
    fun setUp() {
        machine = ExerciseStateMachine(config())
    }

    @Test
    fun update_nullFeatures_returnsCurrentStateUnchanged() {
        val s0 = machine.currentState()
        machine.update(null)
        val s1 = machine.currentState()
        assertEquals(s0.currentPhase, s1.currentPhase)
        assertEquals(s0.repCount, s1.repCount)
    }

    @Test
    fun update_standingStaysInStart() {
        val f = features(170f, 165f)
        machine.update(f)
        assertEquals(Phase.START, machine.currentState().currentPhase)
        assertEquals(0, machine.currentState().repCount)
    }

    @Test
    fun update_descendingTransitionsToDescendingWithDebounce1() {
        val fStand = features(170f, 165f)
        val fDesc = features(120f, 110f)
        machine.update(fStand)
        assertEquals(Phase.START, machine.currentState().currentPhase)
        machine.update(fDesc)
        machine.update(fDesc)
        assertEquals(Phase.DESCENDING, machine.currentState().currentPhase)
    }

    @Test
    fun update_fullCycleIncrementsRepCount() {
        val fStand = features(170f, 165f)
        val fDesc = features(120f, 110f)
        val fBottom = features(85f, 90f)
        val fAsc = features(120f, 115f)

        machine.update(fStand)
        assertEquals(Phase.START, machine.currentState().currentPhase)
        machine.update(fDesc)
        machine.update(fDesc)
        assertEquals(Phase.DESCENDING, machine.currentState().currentPhase)
        machine.update(fBottom)
        machine.update(fBottom)
        assertEquals(Phase.BOTTOM, machine.currentState().currentPhase)
        machine.update(fAsc)
        machine.update(fAsc)
        assertEquals(Phase.ASCENDING, machine.currentState().currentPhase)
        machine.update(fStand)
        machine.update(fStand)
        assertEquals(Phase.START, machine.currentState().currentPhase)
        assertEquals(1, machine.currentState().repCount)
    }

    @Test
    fun update_fullCycleWithoutReachingBottom_repNotCounted() {
        machine = ExerciseStateMachine(config(debounceFrames = 1, minDepthAtBottom = true))
        val fStand = features(170f, 165f)
        val fDesc = features(120f, 110f)
        repeat(2) { machine.update(fStand) }
        repeat(3) { machine.update(fDesc) }
        repeat(3) { machine.update(fStand) }
        assertEquals(0, machine.currentState().repCount)
    }

    @Test
    fun update_debounce2RequiresTwoConsecutiveFrames() {
        machine = ExerciseStateMachine(config(debounceFrames = 2))
        val fStand = features(170f, 165f)
        val fDesc = features(120f, 110f)

        machine.update(fStand)
        machine.update(fDesc)
        assertEquals(Phase.START, machine.currentState().currentPhase)
        machine.update(fDesc)
        assertEquals(Phase.START, machine.currentState().currentPhase)
        machine.update(fDesc)
        assertEquals(Phase.DESCENDING, machine.currentState().currentPhase)
    }

    @Test
    fun update_usesKneeMaxForStanding() {
        val fOneLegExtended = features(170f, 100f)
        machine.update(fOneLegExtended)
        assertEquals(Phase.START, machine.currentState().currentPhase)
    }

    @Test
    fun update_usesKneeAvgForDepth() {
        machine.update(features(170f, 165f))
        machine.update(features(120f, 110f))
        machine.update(features(120f, 110f))
        val fAvgAtDepth = features(95f, 105f)
        machine.update(fAvgAtDepth)
        machine.update(fAvgAtDepth)
        assertEquals(Phase.BOTTOM, machine.currentState().currentPhase)
    }
}
