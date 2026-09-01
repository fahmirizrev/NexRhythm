package com.nexrhythm.app

import android.content.Context
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal enum class MetronomeSoundMode(
    val label: String
) {
    CLICK("Click"),
    VOICE_COUNT("Voice Count")
}

internal enum class GuideSoundMode(
    val label: String
) {
    WOOD("Wood"),
    SNARE("Snare"),
    SYLLABLES("Syllables")
}

internal data class TimeSignature(
    val numerator: Int,
    val denominator: Int
) {
    init {
        require(
            numerator in MIN_NUMERATOR..MAX_NUMERATOR
        )

        require(
            denominator in SUPPORTED_DENOMINATORS
        )
    }

    val label: String
        get() = "$numerator/$denominator"

    companion object {
        const val MIN_NUMERATOR = 2
        const val MAX_NUMERATOR = 24

        val SUPPORTED_DENOMINATORS =
            listOf(2, 4, 8, 16)

        val TWO_FOUR = TimeSignature(2, 4)
        val THREE_FOUR = TimeSignature(3, 4)
        val FOUR_FOUR = TimeSignature(4, 4)
        val FIVE_FOUR = TimeSignature(5, 4)
        val SEVEN_FOUR = TimeSignature(7, 4)
    }
}

internal data class RhythmAudioOptions(
    val metronomeEnabled: Boolean,
    val metronomeSound: MetronomeSoundMode,
    val guideEnabled: Boolean,
    val guideSound: GuideSoundMode
)

internal data class PolyrhythmAudioOptions(
    val metronomeEnabled: Boolean,
    val layerAEnabled: Boolean,
    val layerBEnabled: Boolean
)

internal enum class ExerciseDirection {
    ASCENDING,
    DESCENDING
}

internal data class RhythmExerciseProgress(
    val subdivision: Int,
    val measure: Int,
    val direction: ExerciseDirection,
    val completed: Boolean
)

internal data class RhythmPlaybackState(
    val subdivision: Int,
    val beatIndex: Int,
    val exerciseMeasure: Int?,
    val exerciseDirection: ExerciseDirection?,
    val exerciseComplete: Boolean
)


internal object RhythmAudioTiming {

    const val SAMPLE_RATE = 48_000

    fun beatFrames(
        bpm: Int,
        denominator: Int = 4
    ): Int {
        val safeBpm =
            bpm.coerceAtLeast(1)

        require(
            denominator in
                    TimeSignature.SUPPORTED_DENOMINATORS
        )

        return (
                SAMPLE_RATE *
                        60.0 /
                        safeBpm *
                        4.0 /
                        denominator
                ).roundToInt()
            .coerceAtLeast(1)
    }

    fun beatDurationNanos(
        bpm: Int,
        denominator: Int = 4
    ): Long {
        val safeBpm =
            bpm.coerceAtLeast(1)

        require(
            denominator in
                    TimeSignature.SUPPORTED_DENOMINATORS
        )

        return (
                60_000_000_000.0 /
                        safeBpm *
                        4.0 /
                        denominator
                ).roundToLong()
            .coerceAtLeast(1L)
    }

    fun subdivisionOffsets(
        beatFrames: Int,
        subdivision: Int
    ): IntArray {
        val safeSubdivision = subdivision.coerceAtLeast(1)

        return IntArray(safeSubdivision) { index ->
            (
                    beatFrames.toLong() * index / safeSubdivision
                    ).toInt()
        }
    }

    fun polyrhythmOffsets(
        beatFrames: Int,
        pulseCount: Int
    ): IntArray {
        require(pulseCount in 2..8)

        return IntArray(pulseCount) { index ->
            (
                    beatFrames.toLong() *
                            index /
                            pulseCount
                    ).toInt()
        }
    }

    fun nextBeatInMeasure(
        currentBeatIndex: Int,
        timeSignature: TimeSignature
    ): Int {
        return (
                currentBeatIndex + 1
                ) % timeSignature.numerator
    }

    fun nextCustomExerciseProgress(
        subdivisions: List<Int>,
        currentSubdivision: Int,
        currentMeasure: Int,
        measuresPerSubdivision: Int,
        currentDirection: ExerciseDirection
    ): RhythmExerciseProgress {
        val safeSubdivisions =
            subdivisions
                .map {
                    it.coerceIn(1, 8)
                }
                .distinct()
                .sorted()

        if (safeSubdivisions.isEmpty()) {
            return RhythmExerciseProgress(
                subdivision = 1,
                measure = 1,
                direction = ExerciseDirection.ASCENDING,
                completed = true
            )
        }

        val currentIndex =
            safeSubdivisions
                .indexOf(currentSubdivision)
                .let { index ->
                    if (index >= 0) {
                        index
                    } else {
                        0
                    }
                }

        val safeMeasuresPerSubdivision =
            measuresPerSubdivision.coerceAtLeast(1)

        val safeMeasure =
            currentMeasure.coerceIn(
                1,
                safeMeasuresPerSubdivision
            )

        if (
            safeMeasure <
            safeMeasuresPerSubdivision
        ) {
            return RhythmExerciseProgress(
                subdivision =
                    safeSubdivisions[currentIndex],
                measure = safeMeasure + 1,
                direction = currentDirection,
                completed = false
            )
        }

        if (safeSubdivisions.size == 1) {
            return RhythmExerciseProgress(
                subdivision =
                    safeSubdivisions.first(),
                measure = 1,
                direction = currentDirection,
                completed = false
            )
        }

        return when (currentDirection) {
            ExerciseDirection.ASCENDING -> {
                if (
                    currentIndex <
                    safeSubdivisions.lastIndex
                ) {
                    RhythmExerciseProgress(
                        subdivision =
                            safeSubdivisions[
                                currentIndex + 1
                            ],
                        measure = 1,
                        direction =
                            ExerciseDirection.ASCENDING,
                        completed = false
                    )
                } else {
                    RhythmExerciseProgress(
                        subdivision =
                            safeSubdivisions[
                                currentIndex - 1
                            ],
                        measure = 1,
                        direction =
                            ExerciseDirection.DESCENDING,
                        completed = false
                    )
                }
            }

            ExerciseDirection.DESCENDING -> {
                if (currentIndex > 0) {
                    RhythmExerciseProgress(
                        subdivision =
                            safeSubdivisions[
                                currentIndex - 1
                            ],
                        measure = 1,
                        direction =
                            ExerciseDirection.DESCENDING,
                        completed = false
                    )
                } else {
                    RhythmExerciseProgress(
                        subdivision =
                            safeSubdivisions[1],
                        measure = 1,
                        direction =
                            ExerciseDirection.ASCENDING,
                        completed = false
                    )
                }
            }
        }
    }

}


internal class RhythmAudioEngine(
    context: Context
) {
    private val sampleBank = SampleBank(context.resources)

    private val options = AtomicReference(
        RhythmAudioOptions(
            metronomeEnabled = true,
            metronomeSound = MetronomeSoundMode.CLICK,
            guideEnabled = true,
            guideSound = GuideSoundMode.WOOD
        )
    )

    private val polyrhythmOptions = AtomicReference(
        PolyrhythmAudioOptions(
            metronomeEnabled = true,
            layerAEnabled = true,
            layerBEnabled = true
        )
    )

    private val generation = AtomicInteger(0)
    private val currentBpm = AtomicInteger(60)

    private val playbackState = AtomicReference(
        RhythmPlaybackState(
            subdivision = 2,
            beatIndex = 0,
            exerciseMeasure = null,
            exerciseDirection = null,
            exerciseComplete = false
        )
    )


    @Volatile
    private var worker: Thread? = null


    @Volatile
    private var currentTrack: AudioTrack? = null

    fun updateOptions(
        newOptions: RhythmAudioOptions
    ) {
        options.set(newOptions)
    }

    fun updatePolyrhythmOptions(
        newOptions: PolyrhythmAudioOptions
    ) {
        polyrhythmOptions.set(newOptions)
    }

    fun updateBpm(
        bpm: Int
    ) {
        currentBpm.set(
            bpm.coerceAtLeast(1)
        )
    }

    fun playbackState(): RhythmPlaybackState {
        return playbackState.get()
    }

    @Synchronized
    fun start(
        bpm: Int,
        subdivision: Int,
        timeSignature: TimeSignature,
        initialOptions: RhythmAudioOptions,
        exerciseMeasuresPerSubdivision: Int? = null,
        exerciseSequence: List<Int>? = null
    ) {
        stopLocked()

        currentBpm.set(
            bpm.coerceAtLeast(1)
        )

        options.set(initialOptions)

        val customExerciseSequence =
            exerciseSequence
                ?.map {
                    it.coerceIn(1, 8)
                }
                ?.distinct()
                ?.sorted()

        if (
            exerciseSequence != null &&
            customExerciseSequence.isNullOrEmpty()
        ) {
            return
        }

        val initialSubdivision =
            customExerciseSequence
                ?.first()
                ?: subdivision.coerceIn(1, 8)

        playbackState.set(
            RhythmPlaybackState(
                subdivision = initialSubdivision,
                beatIndex = 0,
                exerciseMeasure =
                    if (
                        exerciseMeasuresPerSubdivision !=
                        null
                    ) {
                        1
                    } else {
                        null
                    },
                exerciseDirection =
                    if (
                        exerciseMeasuresPerSubdivision !=
                        null &&
                        customExerciseSequence == null
                    ) {
                        ExerciseDirection.ASCENDING
                    } else {
                        null
                    },
                exerciseComplete = false
            )
        )


        val runId = generation.incrementAndGet()

        worker = Thread(
            {
                renderLoop(
                    runId = runId,
                    subdivision = initialSubdivision,
                    timeSignature = timeSignature,
                    exerciseMeasuresPerSubdivision =
                        exerciseMeasuresPerSubdivision,
                    exerciseSequence =
                        customExerciseSequence
                )
            },
            "NexRhythmAudio"
        ).apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }


    @Synchronized
    fun startPolyrhythm(
        bpm: Int,
        layerA: Int,
        layerB: Int,
        timeSignature: TimeSignature,
        initialOptions: PolyrhythmAudioOptions
    ) {
        stopLocked()

        currentBpm.set(
            bpm.coerceAtLeast(1)
        )

        val safeLayerA =
            layerA.coerceIn(2, 8)

        val safeLayerB =
            layerB.coerceIn(2, 8)

        polyrhythmOptions.set(
            initialOptions
        )

        playbackState.set(
            RhythmPlaybackState(
                subdivision = 1,
                beatIndex = 0,
                exerciseMeasure = null,
                exerciseDirection = null,
                exerciseComplete = false
            )
        )

        val runId =
            generation.incrementAndGet()

        worker = Thread(
            {
                renderPolyrhythmLoop(
                    runId = runId,
                    layerA = safeLayerA,
                    layerB = safeLayerB,
                    timeSignature = timeSignature
                )
            },
            "NexRhythmPolyrhythmAudio"
        ).apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    @Synchronized
    fun stop() {
        stopLocked()
    }

    fun release() {
        stop()
    }

    private fun stopLocked() {
        generation.incrementAndGet()

        worker?.interrupt()

        currentTrack?.let { track ->
            runCatching {
                track.pause()
            }

            runCatching {
                track.flush()
            }

            runCatching {
                track.stop()
            }
        }

        worker = null
    }

    private fun renderLoop(
        runId: Int,
        subdivision: Int,
        timeSignature: TimeSignature,
        exerciseMeasuresPerSubdivision: Int?,
        exerciseSequence: List<Int>?
    ) {
        val exerciseMeasures =
            exerciseMeasuresPerSubdivision
                ?.coerceAtLeast(1)

        val customExerciseSequence =
            exerciseSequence

        var currentSubdivision =
            subdivision.coerceIn(1, 8)

        var exerciseMeasure = 1

        var exerciseDirection =
            ExerciseDirection.ASCENDING

        var guideSyllables =

            syllablesFor(currentSubdivision).map { syllable ->
                sampleBank.syllables.getValue(syllable)
            }

        val maxSampleFrames = sampleBank.maxSampleFrames

        val carry = IntArray(maxSampleFrames)
        val writeBuffer = ShortArray(WRITE_CHUNK_FRAMES)

        var beatIndex = 0

        playbackState.set(
            RhythmPlaybackState(
                subdivision = currentSubdivision,
                beatIndex = beatIndex,
                exerciseMeasure =
                    if (exerciseMeasures != null) {
                        exerciseMeasure
                    } else {
                        null
                    },
                exerciseDirection =
                    if (
                        exerciseMeasures != null &&
                        customExerciseSequence == null
                    ) {
                        exerciseDirection
                    } else {
                        null
                    },
                exerciseComplete = false
            )
        )

        val track = createAudioTrack()

        currentTrack = track

        try {
            if (!isCurrent(runId)) {
                return
            }

            track.play()

            while (isCurrent(runId)) {
                val beatFrames =
                    RhythmAudioTiming.beatFrames(
                        bpm = currentBpm.get(),
                        denominator =
                            timeSignature.denominator
                    )

                val subdivisionOffsets =
                    RhythmAudioTiming.subdivisionOffsets(
                        beatFrames = beatFrames,
                        subdivision = currentSubdivision
                    )

                val mixBuffer = IntArray(
                    beatFrames + maxSampleFrames
                )

                carry.copyInto(
                    destination = mixBuffer
                )

                val currentOptions = options.get()

                if (currentOptions.metronomeEnabled) {
                    val beatSample =
                        when (currentOptions.metronomeSound) {
                            MetronomeSoundMode.CLICK ->
                                if (beatIndex == 0) {
                                    sampleBank.beatAccent
                                } else {
                                    sampleBank.beatClick
                                }

                            MetronomeSoundMode.VOICE_COUNT ->
                                sampleBank.counts
                                    .getOrNull(beatIndex)
                                    ?: sampleBank.beatClick
                        }

                    mixSample(
                        target = mixBuffer,
                        startFrame = 0,
                        sample = beatSample,
                        gain = BEAT_GAIN
                    )
                }

                if (currentOptions.guideEnabled) {
                    subdivisionOffsets.forEachIndexed { step, startFrame ->
                        val guideSample =
                            when (currentOptions.guideSound) {
                                GuideSoundMode.WOOD ->
                                    sampleBank.guideWood

                                GuideSoundMode.SNARE ->
                                    sampleBank.guideSnare

                                GuideSoundMode.SYLLABLES ->
                                    guideSyllables[step]
                            }

                        mixSample(
                            target = mixBuffer,
                            startFrame = startFrame,
                            sample = guideSample,
                            gain = GUIDE_GAIN
                        )
                    }
                }

                val completed = writeBeat(
                    track = track,
                    runId = runId,
                    mixBuffer = mixBuffer,
                    beatFrames = beatFrames,
                    writeBuffer = writeBuffer
                )

                if (!completed) {
                    break
                }

                for (index in carry.indices) {
                    carry[index] =
                        mixBuffer[beatFrames + index]
                }

                val nextBeatIndex =
                    RhythmAudioTiming.nextBeatInMeasure(
                        currentBeatIndex = beatIndex,
                        timeSignature = timeSignature
                    )

                if (
                    nextBeatIndex == 0 &&
                    exerciseMeasures != null &&
                    customExerciseSequence != null
                ) {
                    val nextProgress =
                        RhythmAudioTiming
                            .nextCustomExerciseProgress(
                                subdivisions =
                                    customExerciseSequence,
                                currentSubdivision =
                                    currentSubdivision,
                                currentMeasure =
                                    exerciseMeasure,
                                measuresPerSubdivision =
                                    exerciseMeasures,
                                currentDirection =
                                    exerciseDirection
                            )

                    currentSubdivision =
                        nextProgress.subdivision

                    exerciseMeasure =
                        nextProgress.measure

                    exerciseDirection =
                        nextProgress.direction

                    if (nextProgress.completed) {
                        playbackState.set(
                            RhythmPlaybackState(
                                subdivision =
                                    currentSubdivision,
                                beatIndex = 0,
                                exerciseMeasure =
                                    exerciseMeasure,
                                exerciseDirection = null,
                                exerciseComplete = true
                            )
                        )

                        break
                    }

                    guideSyllables =
                        syllablesFor(
                            currentSubdivision
                        ).map { syllable ->
                            sampleBank.syllables
                                .getValue(syllable)
                        }
                }

                beatIndex = nextBeatIndex

                playbackState.set(
                    RhythmPlaybackState(
                        subdivision = currentSubdivision,
                        beatIndex = beatIndex,
                        exerciseMeasure =
                            if (exerciseMeasures != null) {
                                exerciseMeasure
                            } else {
                                null
                            },
                        exerciseDirection =
                            if (
                                exerciseMeasures != null &&
                                customExerciseSequence == null
                            ) {
                                exerciseDirection
                            } else {
                                null
                            },
                        exerciseComplete = false
                    )
                )
            }
        } finally {
            runCatching {
                track.pause()
            }

            runCatching {
                track.flush()
            }

            runCatching {
                track.stop()
            }

            runCatching {
                track.release()
            }

            if (currentTrack === track) {
                currentTrack = null
            }
        }
    }


    private fun renderPolyrhythmLoop(
        runId: Int,
        layerA: Int,
        layerB: Int,
        timeSignature: TimeSignature
    ) {
        val maxSampleFrames =
            sampleBank.maxSampleFrames

        val carry =
            IntArray(maxSampleFrames)

        val writeBuffer =
            ShortArray(WRITE_CHUNK_FRAMES)

        var beatIndex = 0

        val track = createAudioTrack()

        currentTrack = track

        try {
            if (!isCurrent(runId)) {
                return
            }

            track.play()

            while (isCurrent(runId)) {
                val beatFrames =
                    RhythmAudioTiming.beatFrames(
                        bpm = currentBpm.get(),
                        denominator =
                            timeSignature.denominator
                    )

                val layerAOffsets =
                    RhythmAudioTiming
                        .polyrhythmOffsets(
                            beatFrames = beatFrames,
                            pulseCount = layerA
                        )

                val layerBOffsets =
                    RhythmAudioTiming
                        .polyrhythmOffsets(
                            beatFrames = beatFrames,
                            pulseCount = layerB
                        )

                val mixBuffer =
                    IntArray(
                        beatFrames +
                                maxSampleFrames
                    )

                carry.copyInto(
                    destination = mixBuffer
                )

                val currentOptions =
                    polyrhythmOptions.get()

                if (
                    currentOptions
                        .metronomeEnabled
                ) {
                    val metronomeSample =
                        if (beatIndex == 0) {
                            sampleBank
                                .polyMetronomeAccent
                        } else {
                            sampleBank
                                .polyMetronomeClick
                        }

                    mixSample(
                        target = mixBuffer,
                        startFrame = 0,
                        sample = metronomeSample,
                        gain =
                            POLYRHYTHM_METRONOME_GAIN
                    )
                }

                if (
                    currentOptions.layerAEnabled
                ) {
                    layerAOffsets.forEach { startFrame ->
                        mixSample(
                            target = mixBuffer,
                            startFrame = startFrame,
                            sample =
                                sampleBank
                                    .polyLayerAWood,
                            gain =
                                POLYRHYTHM_LAYER_GAIN
                        )
                    }
                }

                if (
                    currentOptions.layerBEnabled
                ) {
                    layerBOffsets.forEach { startFrame ->
                        mixSample(
                            target = mixBuffer,
                            startFrame = startFrame,
                            sample =
                                sampleBank
                                    .polyLayerBBlock,
                            gain =
                                POLYRHYTHM_LAYER_GAIN
                        )
                    }
                }

                val completed =
                    writeBeat(
                        track = track,
                        runId = runId,
                        mixBuffer = mixBuffer,
                        beatFrames = beatFrames,
                        writeBuffer = writeBuffer
                    )

                if (!completed) {
                    break
                }

                for (index in carry.indices) {
                    carry[index] =
                        mixBuffer[
                            beatFrames + index
                        ]
                }

                beatIndex =
                    RhythmAudioTiming
                        .nextBeatInMeasure(
                            currentBeatIndex =
                                beatIndex,
                            timeSignature =
                                timeSignature
                        )

                playbackState.set(
                    RhythmPlaybackState(
                        subdivision = 1,
                        beatIndex = beatIndex,
                        exerciseMeasure = null,
                        exerciseDirection = null,
                        exerciseComplete = false
                    )
                )
            }
        } finally {
            runCatching {
                track.pause()
            }

            runCatching {
                track.flush()
            }

            runCatching {
                track.stop()
            }

            runCatching {
                track.release()
            }

            if (currentTrack === track) {
                currentTrack = null
            }
        }
    }

    private fun writeBeat(
        track: AudioTrack,
        runId: Int,
        mixBuffer: IntArray,
        beatFrames: Int,
        writeBuffer: ShortArray
    ): Boolean {
        var frameOffset = 0

        while (
            frameOffset < beatFrames &&
            isCurrent(runId)
        ) {
            val frameCount = minOf(
                writeBuffer.size,
                beatFrames - frameOffset
            )

            for (index in 0 until frameCount) {
                writeBuffer[index] =
                    mixBuffer[frameOffset + index]
                        .coerceIn(
                            Short.MIN_VALUE.toInt(),
                            Short.MAX_VALUE.toInt()
                        )
                        .toShort()
            }

            val written = track.write(
                writeBuffer,
                0,
                frameCount,
                AudioTrack.WRITE_BLOCKING
            )

            if (written <= 0) {
                return false
            }

            frameOffset += written
        }

        return frameOffset == beatFrames
    }

    private fun mixSample(
        target: IntArray,
        startFrame: Int,
        sample: ShortArray,
        gain: Float
    ) {
        if (startFrame >= target.size) {
            return
        }

        val availableFrames = minOf(
            sample.size,
            target.size - startFrame
        )

        for (index in 0 until availableFrames) {
            target[startFrame + index] +=
                (sample[index] * gain).roundToInt()
        }
    }

    private fun createAudioTrack(): AudioTrack {
        val minBufferBytes = AudioTrack.getMinBufferSize(
            RhythmAudioTiming.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferBytes = maxOf(
            if (minBufferBytes > 0) {
                minBufferBytes
            } else {
                0
            },
            WRITE_CHUNK_FRAMES * BYTES_PER_FRAME * 2
        )

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_MUSIC
                    )
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    .setSampleRate(
                        RhythmAudioTiming.SAMPLE_RATE
                    )
                    .setChannelMask(
                        AudioFormat.CHANNEL_OUT_MONO
                    )
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(
                AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
            )
            .setBufferSizeInBytes(bufferBytes)
            .build()
    }

    private fun isCurrent(
        runId: Int
    ): Boolean {
        return generation.get() == runId &&
                !Thread.currentThread().isInterrupted
    }

    private class SampleBank(
        resources: Resources
    ) {
        val beatClick =
            loadPcm16MonoWav(
                resources,
                R.raw.beat_click
            )

        val beatAccent =
            loadPcm16MonoWav(
                resources,
                R.raw.beat_accent
            )

        val guideWood =
            loadPcm16MonoWav(
                resources,
                R.raw.guide_wood
            )

        val guideSnare =
            loadPcm16MonoWav(
                resources,
                R.raw.guide_snare
            )

        val polyMetronomeClick =
            loadPcm16MonoWav(
                resources,
                R.raw.poly_metronome_click
            )

        val polyMetronomeAccent =
            loadPcm16MonoWav(
                resources,
                R.raw.poly_metronome_accent
            )

        val polyLayerAWood =
            loadPcm16MonoWav(
                resources,
                R.raw.poly_layer_a_wood
            )

        val polyLayerBBlock =
            loadPcm16MonoWav(
                resources,
                R.raw.poly_layer_b_block
            )

        val counts = listOf(
            R.raw.count_01,
            R.raw.count_02,
            R.raw.count_03,
            R.raw.count_04,
            R.raw.count_05,
            R.raw.count_06,
            R.raw.count_07,
            R.raw.count_08,
            R.raw.count_09,
            R.raw.count_10,
            R.raw.count_11,
            R.raw.count_12,
            R.raw.count_13,
            R.raw.count_14,
            R.raw.count_15,
            R.raw.count_16
        ).map { resourceId ->
            loadPcm16MonoWav(
                resources,
                resourceId
            )
        }

        val syllables = mapOf(
            "TA" to loadPcm16MonoWav(
                resources,
                R.raw.syllable_ta
            ),
            "KA" to loadPcm16MonoWav(
                resources,
                R.raw.syllable_ka
            ),
            "KI" to loadPcm16MonoWav(
                resources,
                R.raw.syllable_ki
            ),
            "DI" to loadPcm16MonoWav(
                resources,
                R.raw.syllable_di
            ),
            "MI" to loadPcm16MonoWav(
                resources,
                R.raw.syllable_mi
            ),
            "JU" to loadPcm16MonoWav(
                resources,
                R.raw.syllable_ju
            ),
            "NU" to loadPcm16MonoWav(
                resources,
                R.raw.syllable_nu
            )
        )

        val maxSampleFrames: Int =
            buildList {
                add(beatClick)
                add(beatAccent)
                add(guideWood)
                add(guideSnare)
                add(polyMetronomeClick)
                add(polyMetronomeAccent)
                add(polyLayerAWood)
                add(polyLayerBBlock)
                addAll(counts)
                addAll(syllables.values)
            }.maxOf { sample ->
                sample.size
            }
    }

    companion object {
        private const val WRITE_CHUNK_FRAMES = 1024
        private const val BYTES_PER_FRAME = 2

        private const val BEAT_GAIN = 0.68f
        private const val GUIDE_GAIN = 0.62f

        private const val POLYRHYTHM_METRONOME_GAIN =
            0.42f

        private const val POLYRHYTHM_LAYER_GAIN =
            0.40f

        private fun loadPcm16MonoWav(
            resources: Resources,
            resourceId: Int
        ): ShortArray {
            val bytes = resources
                .openRawResource(resourceId)
                .use { input ->
                    input.readBytes()
                }

            require(
                bytes.size >= 12 &&
                        ascii(bytes, 0) == "RIFF" &&
                        ascii(bytes, 8) == "WAVE"
            ) {
                "Unsupported WAV resource: $resourceId"
            }

            var offset = 12

            var audioFormat = -1
            var channels = -1
            var sampleRate = -1
            var bitsPerSample = -1

            var dataOffset = -1
            var dataSize = -1

            while (offset + 8 <= bytes.size) {
                val chunkId = ascii(
                    bytes,
                    offset
                )

                val chunkSize = readIntLe(
                    bytes,
                    offset + 4
                )

                val chunkDataOffset = offset + 8

                require(
                    chunkSize >= 0 &&
                            chunkDataOffset + chunkSize <= bytes.size
                ) {
                    "Invalid WAV chunk in resource: $resourceId"
                }

                when (chunkId) {
                    "fmt " -> {
                        require(chunkSize >= 16)

                        audioFormat = readShortLe(
                            bytes,
                            chunkDataOffset
                        )

                        channels = readShortLe(
                            bytes,
                            chunkDataOffset + 2
                        )

                        sampleRate = readIntLe(
                            bytes,
                            chunkDataOffset + 4
                        )

                        bitsPerSample = readShortLe(
                            bytes,
                            chunkDataOffset + 14
                        )
                    }

                    "data" -> {
                        dataOffset = chunkDataOffset
                        dataSize = chunkSize
                    }
                }

                offset =
                    chunkDataOffset +
                            chunkSize +
                            (chunkSize and 1)
            }

            require(audioFormat == 1) {
                "WAV must use PCM encoding"
            }

            require(channels == 1) {
                "WAV must be mono"
            }

            require(
                sampleRate == RhythmAudioTiming.SAMPLE_RATE
            ) {
                "WAV must be 48000 Hz"
            }

            require(bitsPerSample == 16) {
                "WAV must be PCM 16-bit"
            }

            require(
                dataOffset >= 0 &&
                        dataSize > 0 &&
                        dataSize % 2 == 0
            ) {
                "WAV data chunk is missing or invalid"
            }

            val samples = ShortArray(
                dataSize / 2
            )

            var byteOffset = dataOffset

            for (index in samples.indices) {
                val low =
                    bytes[byteOffset].toInt() and 0xFF

                val high =
                    bytes[byteOffset + 1].toInt()

                samples[index] =
                    ((high shl 8) or low).toShort()

                byteOffset += 2
            }

            return samples
        }

        private fun ascii(
            bytes: ByteArray,
            offset: Int
        ): String {
            return String(
                bytes,
                offset,
                4,
                Charsets.US_ASCII
            )
        }

        private fun readShortLe(
            bytes: ByteArray,
            offset: Int
        ): Int {
            return (
                    (bytes[offset].toInt() and 0xFF) or
                            (
                                    (bytes[offset + 1].toInt() and 0xFF)
                                            shl 8
                                    )
                    )
        }

        private fun readIntLe(
            bytes: ByteArray,
            offset: Int
        ): Int {
            return (
                    (bytes[offset].toInt() and 0xFF) or
                            (
                                    (bytes[offset + 1].toInt() and 0xFF)
                                            shl 8
                                    ) or
                            (
                                    (bytes[offset + 2].toInt() and 0xFF)
                                            shl 16
                                    ) or
                            (
                                    (bytes[offset + 3].toInt() and 0xFF)
                                            shl 24
                                    )
                    )
        }
    }
}