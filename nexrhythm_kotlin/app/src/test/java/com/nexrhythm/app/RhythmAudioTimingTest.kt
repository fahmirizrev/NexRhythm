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
    fun mvpTimeSignaturesUseQuarterNoteDenominator() {
        TimeSignature.entries.forEach { timeSignature ->
            assertEquals(
                4,
                timeSignature.denominator
            )
        }
    }

    @Test
    fun exerciseStaysOnSubdivisionUntilConfiguredMeasuresComplete() {
        val progress =
            RhythmAudioTiming.nextExerciseProgress(
                currentSubdivision = 3,
                currentMeasure = 1,
                measuresPerSubdivision = 2,
                currentDirection = ExerciseDirection.ASCENDING
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
    fun ascendingExerciseAdvancesSubdivisionNormally() {
        val progress =
            RhythmAudioTiming.nextExerciseProgress(
                currentSubdivision = 3,
                currentMeasure = 2,
                measuresPerSubdivision = 2,
                currentDirection = ExerciseDirection.ASCENDING
            )

        assertEquals(4, progress.subdivision)
        assertEquals(1, progress.measure)
        assertEquals(
            ExerciseDirection.ASCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
    }

    @Test
    fun exerciseTurnsDescendingAfterSubdivisionEight() {
        val progress =
            RhythmAudioTiming.nextExerciseProgress(
                currentSubdivision = 8,
                currentMeasure = 2,
                measuresPerSubdivision = 2,
                currentDirection = ExerciseDirection.ASCENDING
            )

        assertEquals(7, progress.subdivision)
        assertEquals(1, progress.measure)
        assertEquals(
            ExerciseDirection.DESCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
    }

    @Test
    fun descendingExerciseMovesToLowerSubdivision() {
        val progress =
            RhythmAudioTiming.nextExerciseProgress(
                currentSubdivision = 7,
                currentMeasure = 2,
                measuresPerSubdivision = 2,
                currentDirection = ExerciseDirection.DESCENDING
            )

        assertEquals(6, progress.subdivision)
        assertEquals(1, progress.measure)
        assertEquals(
            ExerciseDirection.DESCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
    }

    @Test
    fun exerciseTurnsAscendingAfterDescendingSubdivisionOne() {
        val progress =
            RhythmAudioTiming.nextExerciseProgress(
                currentSubdivision = 1,
                currentMeasure = 2,
                measuresPerSubdivision = 2,
                currentDirection = ExerciseDirection.DESCENDING
            )

        assertEquals(2, progress.subdivision)
        assertEquals(1, progress.measure)
        assertEquals(
            ExerciseDirection.ASCENDING,
            progress.direction
        )
        assertEquals(false, progress.completed)
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
