package com.nexrhythm.app

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal enum class RhythmConfidence {
    LOW,
    MEDIUM,
    HIGH
}

internal data class RhythmSegment(
    val startMs: Long,
    val endMs: Long,
    val bpm: Int,
    val timeSignature: TimeSignature,
    val tempoConfidence: RhythmConfidence,
    val meterConfidence: RhythmConfidence
)

internal data class RhythmAnalysisResult(
    val durationMs: Long,
    val segments: List<RhythmSegment>
)

internal class RhythmAnalyzer(
    private val context: Context
) {
    fun analyze(uri: Uri): RhythmAnalysisResult {
        val decoded = decodeToMono(uri)

        return RhythmAnalysisEngine.analyze(
            samples = decoded.samples,
            sampleRate = decoded.sampleRate,
            durationMs = decoded.durationMs
        )
    }

    private fun decodeToMono(uri: Uri): DecodedAudio {
        val extractor = MediaExtractor()

        extractor.setDataSource(
            context,
            uri,
            null
        )

        val trackIndex =
            (0 until extractor.trackCount)
                .firstOrNull { index ->
                    extractor
                        .getTrackFormat(index)
                        .getString(MediaFormat.KEY_MIME)
                        ?.startsWith("audio/") == true
                }
                ?: run {
                    extractor.release()
                    error("No audio track found.")
                }

        extractor.selectTrack(trackIndex)

        val inputFormat =
            extractor.getTrackFormat(trackIndex)

        val mime =
            inputFormat.getString(MediaFormat.KEY_MIME)
                ?: run {
                    extractor.release()
                    error("Audio MIME type is unavailable.")
                }

        val durationMs =
            if (
                inputFormat.containsKey(
                    MediaFormat.KEY_DURATION
                )
            ) {
                inputFormat.getLong(
                    MediaFormat.KEY_DURATION
                ) / 1_000L
            } else {
                0L
            }

        inputFormat.setInteger(
            MediaFormat.KEY_PCM_ENCODING,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val decoder =
            MediaCodec.createDecoderByType(mime)

        decoder.configure(
            inputFormat,
            null,
            null,
            0
        )

        decoder.start()

        val outputInfo =
            MediaCodec.BufferInfo()

        val samples =
            FloatSampleBuilder()

        var outputSampleRate =
            inputFormat.getInteger(
                MediaFormat.KEY_SAMPLE_RATE
            )

        var outputChannels =
            inputFormat.getInteger(
                MediaFormat.KEY_CHANNEL_COUNT
            )

        var sourceEnded = false
        var decoderEnded = false
        var resampleAccumulator = 0L

        try {
            while (!decoderEnded) {
                if (!sourceEnded) {
                    val inputIndex =
                        decoder.dequeueInputBuffer(
                            DEQUEUE_TIMEOUT_US
                        )

                    if (inputIndex >= 0) {
                        val inputBuffer =
                            decoder.getInputBuffer(
                                inputIndex
                            ) ?: error(
                                "Decoder input buffer unavailable."
                            )

                        val sampleSize =
                            extractor.readSampleData(
                                inputBuffer,
                                0
                            )

                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec
                                    .BUFFER_FLAG_END_OF_STREAM
                            )
                            sourceEnded = true
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0
                            )
                            extractor.advance()
                        }
                    }
                }

                when (
                    val outputIndex =
                        decoder.dequeueOutputBuffer(
                            outputInfo,
                            DEQUEUE_TIMEOUT_US
                        )
                ) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat =
                            decoder.outputFormat

                        if (
                            outputFormat.containsKey(
                                MediaFormat.KEY_SAMPLE_RATE
                            )
                        ) {
                            outputSampleRate =
                                outputFormat.getInteger(
                                    MediaFormat
                                        .KEY_SAMPLE_RATE
                                )
                        }

                        if (
                            outputFormat.containsKey(
                                MediaFormat.KEY_CHANNEL_COUNT
                            )
                        ) {
                            outputChannels =
                                outputFormat.getInteger(
                                    MediaFormat
                                        .KEY_CHANNEL_COUNT
                                )
                        }
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        Unit
                    }

                    else -> {
                        if (outputIndex >= 0) {
                            val outputBuffer =
                                decoder.getOutputBuffer(
                                    outputIndex
                                )

                            if (
                                outputBuffer != null &&
                                outputInfo.size > 0
                            ) {
                                outputBuffer.position(
                                    outputInfo.offset
                                )
                                outputBuffer.limit(
                                    outputInfo.offset +
                                            outputInfo.size
                                )
                                outputBuffer.order(
                                    ByteOrder.LITTLE_ENDIAN
                                )

                                val frameBytes =
                                    outputChannels *
                                            PCM_BYTES_PER_SAMPLE

                                while (
                                    outputBuffer.remaining() >=
                                    frameBytes
                                ) {
                                    var mixed = 0f

                                    repeat(outputChannels) {
                                        mixed +=
                                            outputBuffer
                                                .short /
                                                    32768f
                                    }

                                    mixed /= outputChannels

                                    resampleAccumulator +=
                                        TARGET_SAMPLE_RATE

                                    if (
                                        resampleAccumulator >=
                                        outputSampleRate
                                    ) {
                                        samples.add(mixed)

                                        resampleAccumulator -=
                                            outputSampleRate
                                    }
                                }
                            }

                            decoder.releaseOutputBuffer(
                                outputIndex,
                                false
                            )

                            if (
                                outputInfo.flags and
                                MediaCodec
                                    .BUFFER_FLAG_END_OF_STREAM !=
                                0
                            ) {
                                decoderEnded = true
                            }
                        }
                    }
                }
            }
        } finally {
            extractor.release()

            try {
                decoder.stop()
            } finally {
                decoder.release()
            }
        }

        val outputSamples =
            samples.toFloatArray()

        if (outputSamples.isEmpty()) {
            error("Decoded audio is empty.")
        }

        val inferredDurationMs =
            outputSamples.size.toLong() *
                    1_000L /
                    TARGET_SAMPLE_RATE

        return DecodedAudio(
            samples = outputSamples,
            sampleRate = TARGET_SAMPLE_RATE,
            durationMs =
                if (durationMs > 0L) {
                    durationMs
                } else {
                    inferredDurationMs
                }
        )
    }

    private data class DecodedAudio(
        val samples: FloatArray,
        val sampleRate: Int,
        val durationMs: Long
    )

    companion object {
        private const val TARGET_SAMPLE_RATE = 8_000
        private const val PCM_BYTES_PER_SAMPLE = 2
        private const val DEQUEUE_TIMEOUT_US = 10_000L
    }
}

internal object RhythmAnalysisEngine {
    private const val MIN_BPM = 40
    private const val MAX_BPM = 240

    private const val WINDOW_MS = 12_000L
    private const val WINDOW_HOP_MS = 6_000L
    private const val MIN_ANALYSIS_MS = 4_000L

    private const val ONSET_FRAME_MS = 20L
    private const val BPM_MERGE_TOLERANCE = 3

    fun analyze(
        samples: FloatArray,
        sampleRate: Int,
        durationMs: Long
    ): RhythmAnalysisResult {
        require(sampleRate > 0)

        if (samples.isEmpty()) {
            return RhythmAnalysisResult(
                durationMs = durationMs,
                segments = emptyList()
            )
        }

        val actualDurationMs =
            if (durationMs > 0L) {
                durationMs
            } else {
                samples.size.toLong() *
                        1_000L /
                        sampleRate
            }

        val estimates =
            mutableListOf<WindowEstimate>()

        var windowStartMs = 0L

        while (windowStartMs < actualDurationMs) {
            val windowEndMs =
                min(
                    actualDurationMs,
                    windowStartMs + WINDOW_MS
                )

            if (
                windowEndMs - windowStartMs <
                MIN_ANALYSIS_MS &&
                estimates.isNotEmpty()
            ) {
                break
            }

            val startSample =
                (
                        windowStartMs *
                                sampleRate /
                                1_000L
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        samples.size
                    )

            val endSample =
                (
                        windowEndMs *
                                sampleRate /
                                1_000L
                        )
                    .toInt()
                    .coerceIn(
                        startSample,
                        samples.size
                    )

            if (
                endSample - startSample >=
                sampleRate
            ) {
                estimateWindow(
                    samples = samples,
                    startSample = startSample,
                    endSample = endSample,
                    sampleRate = sampleRate
                )?.let { estimate ->
                    estimates +=
                        estimate.copy(
                            startMs = windowStartMs
                        )
                }
            }

            if (windowEndMs >= actualDurationMs) {
                break
            }

            windowStartMs += WINDOW_HOP_MS
        }

        val stabilizedEstimates =
            stabilizeEstimates(
                estimates
            )

        return RhythmAnalysisResult(
            durationMs = actualDurationMs,
            segments =
                mergeEstimates(
                    estimates =
                        stabilizedEstimates,
                    durationMs =
                        actualDurationMs
                )
        )
    }

    private fun estimateWindow(
        samples: FloatArray,
        startSample: Int,
        endSample: Int,
        sampleRate: Int
    ): WindowEstimate? {
        val onsetEnvelope =
            buildOnsetEnvelope(
                samples = samples,
                startSample = startSample,
                endSample = endSample,
                sampleRate = sampleRate
            )

        if (onsetEnvelope.size < 16) {
            return null
        }

        val tempo =
            estimateTempo(
                onsetEnvelope = onsetEnvelope
            ) ?: return null

        val beatStrengths =
            collectBeatStrengths(
                onsetEnvelope = onsetEnvelope,
                lag = tempo.lag
            )

        val meter =
            estimateMeter(
                beatStrengths = beatStrengths
            )

        return WindowEstimate(
            startMs = 0L,
            bpm = tempo.bpm,
            timeSignature =
                meter.timeSignature,
            tempoConfidence =
                confidenceFromGap(
                    tempo.scoreGap
                ),
            meterConfidence =
                confidenceFromGap(
                    meter.scoreGap
                )
        )
    }

    private fun buildOnsetEnvelope(
        samples: FloatArray,
        startSample: Int,
        endSample: Int,
        sampleRate: Int
    ): FloatArray {
        val frameSize =
            max(
                32,
                (
                        sampleRate *
                                ONSET_FRAME_MS /
                                1_000L
                        ).toInt()
            )

        val frameCount =
            max(
                0,
                (endSample - startSample) /
                        frameSize
            )

        if (frameCount <= 1) {
            return FloatArray(0)
        }

        val energy =
            FloatArray(frameCount)

        for (frame in 0 until frameCount) {
            val frameStart =
                startSample +
                        frame * frameSize

            val frameEnd =
                min(
                    endSample,
                    frameStart + frameSize
                )

            var sumSquares = 0.0

            for (
            sampleIndex in
            frameStart until frameEnd
            ) {
                val value =
                    samples[sampleIndex]

                sumSquares +=
                    value * value
            }

            val count =
                max(
                    1,
                    frameEnd - frameStart
                )

            energy[frame] =
                sqrt(
                    sumSquares / count
                ).toFloat()
        }

        val envelope =
            FloatArray(frameCount)

        var maxOnset = 0f

        for (index in 1 until frameCount) {
            val previous =
                energy[index - 1]

            val current =
                energy[index]

            val onset =
                max(
                    0f,
                    current -
                            previous * 0.92f
                )

            envelope[index] = onset
            maxOnset =
                max(
                    maxOnset,
                    onset
                )
        }

        if (maxOnset <= 0f) {
            return envelope
        }

        for (index in envelope.indices) {
            envelope[index] /=
                maxOnset
        }

        return envelope
    }

    private fun estimateTempo(
        onsetEnvelope: FloatArray
    ): TempoEstimate? {
        val frameSeconds =
            ONSET_FRAME_MS /
                    1_000.0

        val minLag =
            (
                    60.0 /
                            MAX_BPM /
                            frameSeconds
                    )
                .roundToInt()
                .coerceAtLeast(1)

        val maxLag =
            (
                    60.0 /
                            MIN_BPM /
                            frameSeconds
                    )
                .roundToInt()
                .coerceAtMost(
                    onsetEnvelope.size - 2
                )

        if (maxLag <= minLag) {
            return null
        }

        val rawScores =
            mutableMapOf<Int, Float>()

        for (lag in minLag..maxLag) {
            var correlation = 0.0
            var count = 0

            for (
            index in
            lag until onsetEnvelope.size
            ) {
                correlation +=
                    onsetEnvelope[index] *
                            onsetEnvelope[
                                index - lag
                            ]

                count++
            }

            rawScores[lag] =
                if (count > 0) {
                    (
                            correlation /
                                    count
                            ).toFloat()
                } else {
                    0f
                }
        }

        var bestLag = minLag
        var bestScore =
            Float.NEGATIVE_INFINITY

        val adjustedScores =
            mutableListOf<
                    Pair<Int, Float>
                    >()

        for (lag in minLag..maxLag) {
            var score =
                rawScores[lag]
                    ?: 0f

            val doubleLag =
                lag * 2

            if (doubleLag <= maxLag) {
                score +=
                    (
                            rawScores[
                                doubleLag
                            ] ?: 0f
                            ) * 0.50f
            }

            val tripleLag =
                lag * 3

            if (tripleLag <= maxLag) {
                score +=
                    (
                            rawScores[
                                tripleLag
                            ] ?: 0f
                            ) * 0.25f
            }

            val bpm =
                60.0 /
                        (
                                lag *
                                        frameSeconds
                                )

            score *=
                when {
                    bpm < 60.0 ||
                            bpm > 200.0 -> {
                        0.88f
                    }

                    bpm < 80.0 ||
                            bpm > 180.0 -> {
                        0.96f
                    }

                    else -> {
                        1f
                    }
                }

            adjustedScores +=
                lag to score

            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }

        if (
            !bestScore.isFinite() ||
            bestScore <= 0f
        ) {
            return null
        }

        val secondScore =
            adjustedScores
                .asSequence()
                .filter { (lag, _) ->
                    abs(
                        lag - bestLag
                    ) >
                            max(
                                2,
                                bestLag / 12
                            )
                }
                .map {
                    it.second
                }
                .maxOrNull()
                ?: 0f

        val bpm =
            (
                    60.0 /
                            (
                                    bestLag *
                                            frameSeconds
                                    )
                    )
                .roundToInt()
                .coerceIn(
                    MIN_BPM,
                    MAX_BPM
                )

        val scoreGap =
            (
                    (
                            bestScore -
                                    secondScore
                            ) /
                            max(
                                bestScore,
                                0.0001f
                            )
                    )
                .coerceIn(
                    0f,
                    1f
                )

        return TempoEstimate(
            bpm = bpm,
            lag = bestLag,
            scoreGap = scoreGap
        )
    }

    private fun collectBeatStrengths(
        onsetEnvelope: FloatArray,
        lag: Int
    ): FloatArray {
        if (lag <= 0) {
            return FloatArray(0)
        }

        var bestPhase = 0
        var bestPhaseScore =
            Float.NEGATIVE_INFINITY

        for (phase in 0 until lag) {
            var score = 0f
            var index = phase

            while (
                index <
                onsetEnvelope.size
            ) {
                score +=
                    localPeak(
                        onsetEnvelope,
                        index,
                        2
                    )

                index += lag
            }

            if (score > bestPhaseScore) {
                bestPhaseScore = score
                bestPhase = phase
            }
        }

        val strengths =
            mutableListOf<Float>()

        var index = bestPhase

        while (index < onsetEnvelope.size) {
            strengths +=
                localPeak(
                    onsetEnvelope,
                    index,
                    2
                )

            index += lag
        }

        val rawStrengths =
            strengths.toFloatArray()

        if (rawStrengths.size < 3) {
            return rawStrengths
        }

        val maxStrength =
            rawStrengths.maxOrNull()
                ?: return rawStrengths

        if (maxStrength <= 0f) {
            return rawStrengths
        }

        var firstReliableIndex = 0

        while (
            firstReliableIndex <
            rawStrengths.lastIndex &&
            rawStrengths[firstReliableIndex] <
            maxStrength * 0.25f
        ) {
            firstReliableIndex++
        }

        return rawStrengths.copyOfRange(
            firstReliableIndex,
            rawStrengths.size
        )
    }

    private fun localPeak(
        values: FloatArray,
        center: Int,
        radius: Int
    ): Float {
        var peak = 0f

        val start =
            max(
                0,
                center - radius
            )

        val end =
            min(
                values.lastIndex,
                center + radius
            )

        for (index in start..end) {
            peak =
                max(
                    peak,
                    values[index]
                )
        }

        return peak
    }

    private fun estimateMeter(
        beatStrengths: FloatArray
    ): MeterEstimate {
        if (beatStrengths.size < 6) {
            return MeterEstimate(
                timeSignature =
                    TimeSignature.FOUR_FOUR,
                scoreGap = 0f
            )
        }

        val candidates =
            listOf(
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10,
                11,
                12
            )

        val results =
            mutableListOf<
                    Pair<Int, Float>
                    >()

        for (numerator in candidates) {
            if (
                beatStrengths.size <
                numerator * 2
            ) {
                continue
            }

            var bestScore =
                Float.NEGATIVE_INFINITY

            for (
            phase in
            0 until numerator
            ) {
                var accentSum = 0f
                var accentCount = 0

                var otherSum = 0f
                var otherCount = 0

                for (
                index in
                beatStrengths.indices
                ) {
                    if (
                        (
                                index - phase
                                )
                            .floorMod(
                                numerator
                            ) == 0
                    ) {
                        accentSum +=
                            beatStrengths[index]
                        accentCount++
                    } else {
                        otherSum +=
                            beatStrengths[index]
                        otherCount++
                    }
                }

                val accentMean =
                    accentSum /
                            max(
                                1,
                                accentCount
                            )

                val otherMean =
                    otherSum /
                            max(
                                1,
                                otherCount
                            )

                val complexityPenalty =
                    numerator * 0.004f

                val score =
                    accentMean -
                            otherMean * 0.35f -
                            complexityPenalty

                bestScore =
                    max(
                        bestScore,
                        score
                    )
            }

            results +=
                numerator to bestScore
        }

        if (results.isEmpty()) {
            return MeterEstimate(
                timeSignature =
                    TimeSignature.FOUR_FOUR,
                scoreGap = 0f
            )
        }

        val sorted =
            results.sortedByDescending {
                it.second
            }

        val best =
            sorted.first()

        val secondScore =
            sorted
                .drop(1)
                .firstOrNull()
                ?.second
                ?: best.second

        val denominator =
            when (best.first) {
                6,
                7,
                9,
                12 -> 8

                else -> 4
            }

        val scoreGap =
            if (best.second <= 0f) {
                0f
            } else {
                (
                        (
                                best.second -
                                        secondScore
                                ) /
                                max(
                                    best.second,
                                    0.0001f
                                )
                        )
                    .coerceIn(
                        0f,
                        1f
                    )
            }

        return MeterEstimate(
            timeSignature =
                TimeSignature(
                    numerator =
                        best.first,
                    denominator =
                        denominator
                ),
            scoreGap = scoreGap
        )
    }

    private fun stabilizeEstimates(
        estimates: List<WindowEstimate>
    ): List<WindowEstimate> {
        if (estimates.isEmpty()) {
            return emptyList()
        }

        val stabilized =
            mutableListOf<WindowEstimate>()

        var stableBpm =
            estimates.first().bpm

        val initialMeterWindow =
            estimates
                .take(3)

        val initialReliableMeter =
            initialMeterWindow
                .firstOrNull {
                    it.meterConfidence !=
                            RhythmConfidence.LOW
                }
                ?.timeSignature

        val initialConsensusMeter =
            initialMeterWindow
                .groupingBy {
                    it.timeSignature
                }
                .eachCount()
                .maxByOrNull {
                    it.value
                }
                ?.key

        var stableTimeSignature =
            initialReliableMeter
                ?: initialConsensusMeter
                ?: estimates
                    .first()
                    .timeSignature

        var pendingTempoBpm: Int? =
            null

        var pendingTempoCount = 0

        var pendingTempoStartIndex = -1

        var pendingTempoTotal = 0

        var pendingMeter:
                TimeSignature? =
            null

        var pendingMeterCount = 0

        var pendingMeterStartIndex = -1

        for (estimate in estimates) {
            var outputBpm =
                estimate.bpm

            val closeToStableTempo =
                abs(
                    estimate.bpm -
                            stableBpm
                ) <=
                        BPM_MERGE_TOLERANCE

            if (
                estimate.tempoConfidence ==
                RhythmConfidence.LOW
            ) {
                outputBpm =
                    stableBpm

                pendingTempoBpm = null
                pendingTempoCount = 0
                pendingTempoStartIndex = -1
                pendingTempoTotal = 0
            } else if (closeToStableTempo) {
                stableBpm =
                    estimate.bpm

                outputBpm =
                    estimate.bpm

                pendingTempoBpm = null
                pendingTempoCount = 0
                pendingTempoStartIndex = -1
                pendingTempoTotal = 0
            } else {
                val pending =
                    pendingTempoBpm

                if (
                    pending != null &&
                    abs(
                        estimate.bpm -
                                pending
                    ) <=
                    BPM_MERGE_TOLERANCE
                ) {
                    pendingTempoCount++

                    pendingTempoTotal +=
                        estimate.bpm
                } else {
                    pendingTempoBpm =
                        estimate.bpm

                    pendingTempoCount = 1

                    pendingTempoTotal =
                        estimate.bpm

                    pendingTempoStartIndex =
                        stabilized.size
                }

                val requiredWindows =
                    if (
                        isOctaveEquivalent(
                            stableBpm,
                            pendingTempoBpm
                                ?: estimate.bpm
                        )
                    ) {
                        3
                    } else {
                        2
                    }

                if (
                    pendingTempoCount >=
                    requiredWindows
                ) {
                    val confirmedBpm =
                        (
                                pendingTempoTotal
                                    .toFloat() /
                                        pendingTempoCount
                                )
                            .roundToInt()
                            .coerceIn(
                                MIN_BPM,
                                MAX_BPM
                            )

                    for (
                    index in
                    pendingTempoStartIndex
                            until
                            stabilized.size
                    ) {
                        stabilized[index] =
                            stabilized[index]
                                .copy(
                                    bpm =
                                        confirmedBpm
                                )
                    }

                    stableBpm =
                        confirmedBpm

                    outputBpm =
                        confirmedBpm

                    pendingTempoBpm = null
                    pendingTempoCount = 0
                    pendingTempoStartIndex = -1
                    pendingTempoTotal = 0
                } else {
                    outputBpm =
                        stableBpm
                }
            }

            var outputTimeSignature =
                stableTimeSignature

            if (
                estimate.meterConfidence ==
                RhythmConfidence.LOW
            ) {
                pendingMeter = null
                pendingMeterCount = 0
                pendingMeterStartIndex = -1
            } else if (
                estimate.timeSignature ==
                stableTimeSignature
            ) {
                pendingMeter = null
                pendingMeterCount = 0
                pendingMeterStartIndex = -1
            } else {
                if (
                    pendingMeter ==
                    estimate.timeSignature
                ) {
                    pendingMeterCount++
                } else {
                    pendingMeter =
                        estimate.timeSignature

                    pendingMeterCount = 1

                    pendingMeterStartIndex =
                        stabilized.size
                }

                if (
                    pendingMeterCount >= 2
                ) {
                    val confirmedMeter =
                        pendingMeter
                            ?: stableTimeSignature

                    for (
                    index in
                    pendingMeterStartIndex
                            until
                            stabilized.size
                    ) {
                        stabilized[index] =
                            stabilized[index]
                                .copy(
                                    timeSignature =
                                        confirmedMeter
                                )
                    }

                    stableTimeSignature =
                        confirmedMeter

                    outputTimeSignature =
                        confirmedMeter

                    pendingMeter = null
                    pendingMeterCount = 0
                    pendingMeterStartIndex = -1
                }
            }

            stabilized +=
                estimate.copy(
                    bpm =
                        outputBpm,
                    timeSignature =
                        outputTimeSignature
                )
        }

        return stabilized
    }

    private fun isOctaveEquivalent(
        firstBpm: Int,
        secondBpm: Int
    ): Boolean {
        val lower =
            min(
                firstBpm,
                secondBpm
            )

        val higher =
            max(
                firstBpm,
                secondBpm
            )

        return abs(
            higher -
                    lower * 2
        ) <=
                BPM_MERGE_TOLERANCE * 2
    }

    private fun mergeEstimates(
        estimates: List<WindowEstimate>,
        durationMs: Long
    ): List<RhythmSegment> {
        if (estimates.isEmpty()) {
            return emptyList()
        }

        val result =
            mutableListOf<RhythmSegment>()

        var groupStartMs = 0L
        var bpmTotal =
            estimates.first().bpm

        var groupCount = 1

        var signature =
            estimates.first()
                .timeSignature

        var tempoConfidence =
            estimates.first()
                .tempoConfidence

        var meterConfidence =
            estimates.first()
                .meterConfidence

        var previousBpm =
            estimates.first().bpm

        for (
        index in
        1 until estimates.size
        ) {
            val estimate =
                estimates[index]

            val sameSignature =
                estimate.timeSignature ==
                        signature

            val closeTempo =
                abs(
                    estimate.bpm -
                            previousBpm
                ) <=
                        BPM_MERGE_TOLERANCE

            if (
                sameSignature &&
                closeTempo
            ) {
                bpmTotal +=
                    estimate.bpm

                groupCount++

                tempoConfidence =
                    minConfidence(
                        tempoConfidence,
                        estimate
                            .tempoConfidence
                    )

                meterConfidence =
                    minConfidence(
                        meterConfidence,
                        estimate
                            .meterConfidence
                    )

                previousBpm =
                    estimate.bpm

                continue
            }

            val boundaryMs =
                (
                        estimate.startMs +
                                WINDOW_MS / 2L
                        )
                    .coerceIn(
                        groupStartMs,
                        durationMs
                    )

            result +=
                RhythmSegment(
                    startMs =
                        groupStartMs,
                    endMs =
                        boundaryMs,
                    bpm =
                        (
                                bpmTotal.toFloat() /
                                        groupCount
                                )
                            .roundToInt()
                            .coerceIn(
                                MIN_BPM,
                                MAX_BPM
                            ),
                    timeSignature =
                        signature,
                    tempoConfidence =
                        tempoConfidence,
                    meterConfidence =
                        meterConfidence
                )

            groupStartMs =
                boundaryMs

            bpmTotal =
                estimate.bpm

            groupCount = 1

            signature =
                estimate.timeSignature

            tempoConfidence =
                estimate
                    .tempoConfidence

            meterConfidence =
                estimate
                    .meterConfidence

            previousBpm =
                estimate.bpm
        }

        result +=
            RhythmSegment(
                startMs =
                    groupStartMs,
                endMs =
                    durationMs,
                bpm =
                    (
                            bpmTotal.toFloat() /
                                    groupCount
                            )
                        .roundToInt()
                        .coerceIn(
                            MIN_BPM,
                            MAX_BPM
                        ),
                timeSignature =
                    signature,
                tempoConfidence =
                    tempoConfidence,
                meterConfidence =
                    meterConfidence
            )

        return result
            .filter {
                it.endMs >
                        it.startMs
            }
    }

    private fun confidenceFromGap(
        gap: Float
    ): RhythmConfidence {
        return when {
            gap >= 0.28f -> {
                RhythmConfidence.HIGH
            }

            gap >= 0.12f -> {
                RhythmConfidence.MEDIUM
            }

            else -> {
                RhythmConfidence.LOW
            }
        }
    }

    private fun minConfidence(
        first: RhythmConfidence,
        second: RhythmConfidence
    ): RhythmConfidence {
        return if (
            first.ordinal <=
            second.ordinal
        ) {
            first
        } else {
            second
        }
    }

    private fun Int.floorMod(
        divisor: Int
    ): Int {
        val remainder =
            this % divisor

        return if (remainder < 0) {
            remainder + divisor
        } else {
            remainder
        }
    }

    private data class TempoEstimate(
        val bpm: Int,
        val lag: Int,
        val scoreGap: Float
    )

    private data class MeterEstimate(
        val timeSignature: TimeSignature,
        val scoreGap: Float
    )

    private data class WindowEstimate(
        val startMs: Long,
        val bpm: Int,
        val timeSignature: TimeSignature,
        val tempoConfidence: RhythmConfidence,
        val meterConfidence: RhythmConfidence
    )
}

private class FloatSampleBuilder(
    initialCapacity: Int = 65_536
) {
    private var values =
        FloatArray(initialCapacity)

    private var size = 0

    fun add(value: Float) {
        if (size == values.size) {
            values =
                values.copyOf(
                    values.size * 2
                )
        }

        values[size] = value
        size++
    }

    fun toFloatArray(): FloatArray {
        return values.copyOf(size)
    }
}