package com.nexrhythm.app

import android.content.Context
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

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

internal enum class TimeSignature(
    val numerator: Int,
    val denominator: Int
) {
    TWO_FOUR(2, 4),
    THREE_FOUR(3, 4),
    FOUR_FOUR(4, 4),
    FIVE_FOUR(5, 4),
    SEVEN_FOUR(7, 4);

    val label: String
        get() = "$numerator/$denominator"
}

internal data class RhythmAudioOptions(
    val metronomeEnabled: Boolean,
    val metronomeSound: MetronomeSoundMode,
    val guideEnabled: Boolean,
    val guideSound: GuideSoundMode
)

internal object RhythmAudioTiming {
    const val SAMPLE_RATE = 48_000

    fun beatFrames(bpm: Int): Int {
        val safeBpm = bpm.coerceAtLeast(1)

        return (
                SAMPLE_RATE * 60.0 / safeBpm
                ).roundToInt().coerceAtLeast(1)
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

    fun nextBeatInMeasure(
        currentBeatIndex: Int,
        timeSignature: TimeSignature
    ): Int {
        return (
                currentBeatIndex + 1
                ) % timeSignature.numerator
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
            guideSound = GuideSoundMode.SNARE
        )
    )

    private val generation = AtomicInteger(0)

    @Volatile
    private var worker: Thread? = null

    @Volatile
    private var currentTrack: AudioTrack? = null

    fun updateOptions(
        newOptions: RhythmAudioOptions
    ) {
        options.set(newOptions)
    }

    @Synchronized
    fun start(
        bpm: Int,
        subdivision: Int,
        timeSignature: TimeSignature,
        initialOptions: RhythmAudioOptions
    ) {
        stopLocked()

        options.set(initialOptions)

        val runId = generation.incrementAndGet()

        worker = Thread(
            {
                renderLoop(
                    runId = runId,
                    bpm = bpm,
                    subdivision = subdivision,
                    timeSignature = timeSignature
                )
            },
            "NexRhythmAudio"
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
        bpm: Int,
        subdivision: Int,
        timeSignature: TimeSignature
    ) {
        val beatFrames = RhythmAudioTiming.beatFrames(bpm)

        val subdivisionOffsets =
            RhythmAudioTiming.subdivisionOffsets(
                beatFrames = beatFrames,
                subdivision = subdivision
            )

        val guideSyllables = syllablesFor(subdivision).map { syllable ->
            sampleBank.syllables.getValue(syllable)
        }

        val maxSampleFrames = sampleBank.maxSampleFrames

        val carry = IntArray(maxSampleFrames)
        val writeBuffer = ShortArray(WRITE_CHUNK_FRAMES)

        var beatIndex = 0

        val track = createAudioTrack()

        currentTrack = track

        try {
            if (!isCurrent(runId)) {
                return
            }

            track.play()

            while (isCurrent(runId)) {
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
                                sampleBank.beatClick

                            MetronomeSoundMode.VOICE_COUNT ->
                                sampleBank.counts[beatIndex]
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

                beatIndex =
                    RhythmAudioTiming.nextBeatInMeasure(
                        currentBeatIndex = beatIndex,
                        timeSignature = timeSignature
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
                add(guideWood)
                add(guideSnare)
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