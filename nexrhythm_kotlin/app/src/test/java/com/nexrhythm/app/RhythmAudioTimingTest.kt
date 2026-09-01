package com.nexrhythm.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmAudioTimingTest {

    @Test
    fun beatFramesAt120BpmAre24000() {
        assertEquals(
            24_000,
            RhythmAudioTiming.beatFrames(120)
        )
    }

    @Test
    fun denominatorChangesBeatDuration() {
        assertEquals(
            48_000,
            RhythmAudioTiming.beatFrames(
                bpm = 120,
                denominator = 2
            )
        )

        assertEquals(
            24_000,
            RhythmAudioTiming.beatFrames(
                bpm = 120,
                denominator = 4
            )
        )

        assertEquals(
            12_000,
            RhythmAudioTiming.beatFrames(
                bpm = 120,
                denominator = 8
            )
        )

        assertEquals(
            6_000,
            RhythmAudioTiming.beatFrames(
                bpm = 120,
                denominator = 16
            )
        )
    }

    @Test
    fun beatDurationNanosMatchesDenominator() {
        assertEquals(
            1_000_000_000L,
            RhythmAudioTiming.beatDurationNanos(
                bpm = 60,
                denominator = 4
            )
        )

        assertEquals(
            500_000_000L,
            RhythmAudioTiming.beatDurationNanos(
                bpm = 60,
                denominator = 8
            )
        )

        assertEquals(
            250_000_000L,
            RhythmAudioTiming.beatDurationNanos(
                bpm = 60,
                denominator = 16
            )
        )
    }

    @Test
    fun subdivisionFourUsesQuarterBeatOffsets() {
        val beatFrames =
            RhythmAudioTiming.beatFrames(120)

        val offsets =
            RhythmAudioTiming.subdivisionOffsets(
                beatFrames = beatFrames,
                subdivision = 4
            )

        assertArrayEquals(
            intArrayOf(
                0,
                6_000,
                12_000,
                18_000
            ),
            offsets
        )
    }

    @Test
    fun polyrhythmThreeTwoUsesSharedBeatCycle() {
        val beatFrames = 24_000

        assertArrayEquals(
            intArrayOf(
                0,
                8_000,
                16_000
            ),
            RhythmAudioTiming.polyrhythmOffsets(
                beatFrames = beatFrames,
                pulseCount = 3
            )
        )

        assertArrayEquals(
            intArrayOf(
                0,
                12_000
            ),
            RhythmAudioTiming.polyrhythmOffsets(
                beatFrames = beatFrames,
                pulseCount = 2
            )
        )
    }

    @Test
    fun polyrhythmFourThreeUsesSharedBeatCycle() {
        val beatFrames = 24_000

        assertArrayEquals(
            intArrayOf(
                0,
                6_000,
                12_000,
                18_000
            ),
            RhythmAudioTiming.polyrhythmOffsets(
                beatFrames = beatFrames,
                pulseCount = 4
            )
        )

        assertArrayEquals(
            intArrayOf(
                0,
                8_000,
                16_000
            ),
            RhythmAudioTiming.polyrhythmOffsets(
                beatFrames = beatFrames,
                pulseCount = 3
            )
        )
    }

    @Test
    fun polyrhythmBoundaryRatiosStayInsideBeat() {
        val beatFrames = 24_000

        val two =
            RhythmAudioTiming.polyrhythmOffsets(
                beatFrames = beatFrames,
                pulseCount = 2
            )

        val eight =
            RhythmAudioTiming.polyrhythmOffsets(
                beatFrames = beatFrames,
                pulseCount = 8
            )

        assertArrayEquals(
            intArrayOf(
                0,
                12_000
            ),
            two
        )

        assertArrayEquals(
            intArrayOf(
                0,
                3_000,
                6_000,
                9_000,
                12_000,
                15_000,
                18_000,
                21_000
            ),
            eight
        )

        assertTrue(two.last() < beatFrames)
        assertTrue(eight.last() < beatFrames)
    }

    @Test
    fun lastSubdivisionEventOccursBeforeNextBeat() {
        val beatFrames =
            RhythmAudioTiming.beatFrames(120)

        val offsets =
            RhythmAudioTiming.subdivisionOffsets(
                beatFrames = beatFrames,
                subdivision = 7
            )

        assertTrue(
            offsets.last() < beatFrames
        )
    }

    @Test
    fun fourFourWrapsAfterFourthBeat() {
        var beatIndex = 0

        assertEquals(0, beatIndex)

        beatIndex =
            RhythmAudioTiming.nextBeatInMeasure(
                currentBeatIndex = beatIndex,
                timeSignature = TimeSignature.FOUR_FOUR
            )
        assertEquals(1, beatIndex)

        beatIndex =
            RhythmAudioTiming.nextBeatInMeasure(
                currentBeatIndex = beatIndex,
                timeSignature = TimeSignature.FOUR_FOUR
            )
        assertEquals(2, beatIndex)

        beatIndex =
            RhythmAudioTiming.nextBeatInMeasure(
                currentBeatIndex = beatIndex,
                timeSignature = TimeSignature.FOUR_FOUR
            )
        assertEquals(3, beatIndex)

        beatIndex =
            RhythmAudioTiming.nextBeatInMeasure(
                currentBeatIndex = beatIndex,
                timeSignature = TimeSignature.FOUR_FOUR
            )
        assertEquals(0, beatIndex)
    }

    @Test
    fun threeFourWrapsAfterThirdBeat() {
        assertEquals(
            0,
            RhythmAudioTiming.nextBeatInMeasure(
                currentBeatIndex = 2,
                timeSignature = TimeSignature.THREE_FOUR
            )
        )
    }

    @Test
    fun sevenFourWrapsAfterSeventhBeat() {
        assertEquals(
            0,
            RhythmAudioTiming.nextBeatInMeasure(
                currentBeatIndex = 6,
                timeSignature = TimeSignature.SEVEN_FOUR
            )
        )
    }

    @Test
    fun configurableTimeSignatureSupportsTargetRange() {
        val timeSignature =
            TimeSignature(
                numerator = 24,
                denominator = 16
            )

        assertEquals(
            24,
            timeSignature.numerator
        )
        assertEquals(
            16,
            timeSignature.denominator
        )
        assertEquals(
            "24/16",
            timeSignature.label
        )
    }

    @Test
    fun twentyThreeEightWrapsAfterTwentyThirdBeat() {
        assertEquals(
            0,
            RhythmAudioTiming.nextBeatInMeasure(
                currentBeatIndex = 22,
                timeSignature =
                    TimeSignature(
                        numerator = 23,
                        denominator = 8
                    )
            )
        )
    }

    @Test
    fun customExerciseStaysUntilConfiguredMeasuresComplete() {
        val progress =
            RhythmAudioTiming.nextCustomExerciseProgress(
                subdivisions =
                    listOf(1, 3, 4, 6),
                currentSubdivision = 3,
                currentMeasure = 1,
                measuresPerSubdivision = 2,
                currentDirection =
                    ExerciseDirection.ASCENDING
            )

        assertEquals(3, progress.subdivision)
        assertEquals(2, progress.measure)
        assertEquals(
            ExerciseDirection.ASCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
    }

    @Test
    fun customExerciseAdvancesThroughSelectedSubdivisions() {
        val progress =
            RhythmAudioTiming.nextCustomExerciseProgress(
                subdivisions =
                    listOf(1, 3, 4, 6),
                currentSubdivision = 1,
                currentMeasure = 2,
                measuresPerSubdivision = 2,
                currentDirection =
                    ExerciseDirection.ASCENDING
            )

        assertEquals(3, progress.subdivision)
        assertEquals(1, progress.measure)
        assertEquals(
            ExerciseDirection.ASCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
    }

    @Test
    fun customExerciseTurnsDescendingWithoutRepeatingPeak() {
        val progress =
            RhythmAudioTiming.nextCustomExerciseProgress(
                subdivisions =
                    listOf(1, 3, 4, 6),
                currentSubdivision = 6,
                currentMeasure = 2,
                measuresPerSubdivision = 2,
                currentDirection =
                    ExerciseDirection.ASCENDING
            )

        assertEquals(4, progress.subdivision)
        assertEquals(1, progress.measure)
        assertEquals(
            ExerciseDirection.DESCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
    }

    @Test
    fun customExerciseDescendingUsesPreviousSelectedSubdivision() {
        val progress =
            RhythmAudioTiming.nextCustomExerciseProgress(
                subdivisions =
                    listOf(1, 3, 4, 6),
                currentSubdivision = 4,
                currentMeasure = 2,
                measuresPerSubdivision = 2,
                currentDirection =
                    ExerciseDirection.DESCENDING
            )

        assertEquals(3, progress.subdivision)
        assertEquals(1, progress.measure)
        assertEquals(
            ExerciseDirection.DESCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
    }

    @Test
    fun customExerciseTurnsAscendingAfterLowestSubdivision() {
        val progress =
            RhythmAudioTiming.nextCustomExerciseProgress(
                subdivisions =
                    listOf(1, 3, 4, 6),
                currentSubdivision = 1,
                currentMeasure = 2,
                measuresPerSubdivision = 2,
                currentDirection =
                    ExerciseDirection.DESCENDING
            )

        assertEquals(3, progress.subdivision)
        assertEquals(1, progress.measure)
        assertEquals(
            ExerciseDirection.ASCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
    }

    @Test
    fun customExerciseWithOneSubdivisionRepeatsIndefinitely() {
        val progress =
            RhythmAudioTiming.nextCustomExerciseProgress(
                subdivisions = listOf(4),
                currentSubdivision = 4,
                currentMeasure = 2,
                measuresPerSubdivision = 2,
                currentDirection =
                    ExerciseDirection.ASCENDING
            )

        assertEquals(4, progress.subdivision)
        assertEquals(1, progress.measure)
        assertEquals(
            ExerciseDirection.ASCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
    }
}
