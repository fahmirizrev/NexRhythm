package com.nexrhythm.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class RhythmAnalysisEngineTest {

    @Test
    fun detectsApproximate120BpmFromSyntheticPulseTrain() {
        val samples =
            syntheticPulseTrain(
                bpm = 120,
                beatsPerAccent = 4,
                durationSeconds = 30
            )

        val result =
            RhythmAnalysisEngine.analyze(
                samples = samples,
                sampleRate = SAMPLE_RATE,
                durationMs = 30_000L
            )

        assertTrue(
            result.segments.isNotEmpty()
        )

        val bpm =
            result.segments.first().bpm

        assertTrue(
            bpm in 116..124
        )

        assertEquals(
            1,
            result.segments.size
        )
    }

    @Test
    fun repeatedFourBeatAccentPrefersFourBeatMeter() {
        val samples =
            syntheticPulseTrain(
                bpm = 120,
                beatsPerAccent = 4,
                durationSeconds = 36
            )

        val result =
            RhythmAnalysisEngine.analyze(
                samples = samples,
                sampleRate = SAMPLE_RATE,
                durationMs = 36_000L
            )

        assertTrue(
            result.segments.isNotEmpty()
        )

        assertEquals(
            4,
            result
                .segments
                .first()
                .timeSignature
                .numerator
        )

        assertEquals(
            1,
            result.segments.size
        )
    }

    @Test
    fun repeatedSevenBeatAccentCanProduceOddMeterEstimate() {
        val samples =
            syntheticPulseTrain(
                bpm = 90,
                beatsPerAccent = 7,
                durationSeconds = 56
            )

        val result =
            RhythmAnalysisEngine.analyze(
                samples = samples,
                sampleRate = SAMPLE_RATE,
                durationMs = 56_000L
            )

        assertTrue(
            result.segments.isNotEmpty()
        )

        assertEquals(
            7,
            result
                .segments
                .first()
                .timeSignature
                .numerator
        )

        assertEquals(
            1,
            result.segments.size
        )
    }

    @Test
    fun prefers150PulseOver75HalfTimeAccent() {
        val samples =
            syntheticPulseTrain(
                bpm = 150,
                beatsPerAccent = 2,
                durationSeconds = 36
            )

        val result =
            RhythmAnalysisEngine.analyze(
                samples = samples,
                sampleRate = SAMPLE_RATE,
                durationMs = 36_000L
            )

        assertTrue(
            result.segments.isNotEmpty()
        )

        val bpm =
            result
                .segments
                .first()
                .bpm

        assertTrue(
            bpm in 146..154
        )
    }

    private fun syntheticPulseTrain(
        bpm: Int,
        beatsPerAccent: Int,
        durationSeconds: Int
    ): FloatArray {
        val samples =
            FloatArray(
                SAMPLE_RATE *
                        durationSeconds
            )

        val beatSamples =
            (
                    SAMPLE_RATE *
                            60.0 /
                            bpm
                    )
                .roundToInt()

        var beat = 0
        var position = 0

        while (
            position <
            samples.size
        ) {
            val amplitude =
                if (
                    beat %
                    beatsPerAccent ==
                    0
                ) {
                    1f
                } else {
                    0.48f
                }

            val pulseLength =
                minOf(
                    SAMPLE_RATE / 80,
                    samples.size -
                            position
                )

            for (
            offset in
            0 until pulseLength
            ) {
                samples[
                    position + offset
                ] =
                    amplitude *
                            (
                                    1f -
                                            offset
                                                .toFloat() /
                                            pulseLength
                                    )
            }

            beat++
            position += beatSamples
        }

        return samples
    }

    companion object {
        private const val SAMPLE_RATE =
            8_000
    }
}
