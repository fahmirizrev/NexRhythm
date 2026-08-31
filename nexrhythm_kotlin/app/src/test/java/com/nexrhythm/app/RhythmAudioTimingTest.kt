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
}