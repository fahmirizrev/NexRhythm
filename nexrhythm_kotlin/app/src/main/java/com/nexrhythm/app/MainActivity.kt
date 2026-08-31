package com.nexrhythm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexrhythm.app.ui.theme.NexRhythmTheme
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NexRhythmTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    TrainerScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainerScreen(
    modifier: Modifier = Modifier
) {
    var bpm by remember { mutableIntStateOf(60) }
    var subdivision by remember { mutableIntStateOf(2) }
    var timeSignature by remember { mutableStateOf(TimeSignature.FOUR_FOUR) }
    var metronomeEnabled by remember { mutableStateOf(true) }
    var metronomeSound by remember { mutableStateOf(MetronomeSoundMode.CLICK) }

    var guideEnabled by remember { mutableStateOf(true) }
    var guideSound by remember { mutableStateOf(GuideSoundMode.WOOD) }

    var isRunning by remember { mutableStateOf(false) }
    var activeStep by remember { mutableIntStateOf(-1) }

    val context = LocalContext.current

    val audioEngine = remember(context) {
        RhythmAudioEngine(
            context.applicationContext
        )
    }

    val audioOptions = RhythmAudioOptions(
        metronomeEnabled = metronomeEnabled,
        metronomeSound = metronomeSound,
        guideEnabled = guideEnabled,
        guideSound = guideSound
    )

    val currentBpm by rememberUpdatedState(bpm)

    DisposableEffect(audioEngine) {
        onDispose {
            audioEngine.release()
        }
    }

    LaunchedEffect(
        audioEngine,
        audioOptions
    ) {
        audioEngine.updateOptions(
            audioOptions
        )
    }

    LaunchedEffect(
        audioEngine,
        bpm
    ) {
        audioEngine.updateBpm(bpm)
    }

    LaunchedEffect(
        audioEngine,
        isRunning,
        subdivision,
        timeSignature
    ) {
        if (isRunning) {
            audioEngine.start(
                bpm = bpm,
                subdivision = subdivision,
                timeSignature = timeSignature,
                initialOptions = audioOptions
            )
        } else {
            audioEngine.stop()
        }
    }

    LaunchedEffect(
        isRunning,
        subdivision,
        timeSignature
    ) {
        if (!isRunning) {
            activeStep = -1
            return@LaunchedEffect
        }

        activeStep = 0

        var beatDurationNanos =
            60_000_000_000L / currentBpm

        var beatStartTimeNanos =
            withFrameNanos { it }

        while (true) {
            withFrameNanos { frameTimeNanos ->
                var elapsedInBeat =
                    frameTimeNanos - beatStartTimeNanos

                while (elapsedInBeat >= beatDurationNanos) {
                    beatStartTimeNanos += beatDurationNanos

                    beatDurationNanos =
                        60_000_000_000L / currentBpm

                    elapsedInBeat =
                        frameTimeNanos - beatStartTimeNanos
                }

                activeStep = (
                        (elapsedInBeat * subdivision) / beatDurationNanos
                        ).toInt().coerceIn(0, subdivision - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        TrainerHeader()

        Spacer(modifier = Modifier.height(14.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        TempoSection(
            bpm = bpm,
            timeSignature = timeSignature,
            onBpmChange = { bpm = it.coerceIn(40, 240) },
            onTimeSignatureChange = { timeSignature = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        SubdivisionSection(
            selectedSubdivision = subdivision,
            onSubdivisionSelected = { subdivision = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        PracticeSection(
            subdivision = subdivision,
            activeStep = activeStep,
            modifier = Modifier.weight(1f)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        AudioControls(
            metronomeEnabled = metronomeEnabled,
            metronomeSound = metronomeSound,
            onMetronomeChanged = { metronomeEnabled = it },
            onMetronomeSoundChanged = { metronomeSound = it },
            guideEnabled = guideEnabled,
            guideSound = guideSound,
            onGuideChanged = { guideEnabled = it },
            onGuideSoundChanged = { guideSound = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { isRunning = !isRunning },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isRunning) {
                    "■  STOP"
                } else {
                    "▶  START PRACTICE"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun TrainerHeader() {
    Column {
        Text(
            text = "NexRhythm",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Rhythm Trainer",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun TempoSection(
    bpm: Int,
    timeSignature: TimeSignature,
    onBpmChange: (Int) -> Unit,
    onTimeSignatureChange: (TimeSignature) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionLabel("TEMPO")

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onBpmChange(bpm - 1) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Text(
                        text = "−",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bpm.toString(),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "BPM",
                        modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    )
                }

                TextButton(
                    onClick = { onBpmChange(bpm + 1) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Text(
                        text = "+",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            TimeSignatureDropdown(
                selectedTimeSignature = timeSignature,
                onTimeSignatureSelected = onTimeSignatureChange,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Slider(
            value = bpm.toFloat(),
            onValueChange = { rawValue ->
                val snappedBpm = (
                        (rawValue / 5f).roundToInt() * 5
                        ).coerceIn(40, 240)

                if (snappedBpm != bpm) {
                    hapticFeedback.performHapticFeedback(
                        HapticFeedbackType.SegmentFrequentTick
                    )
                    onBpmChange(snappedBpm)
                }
            },
            valueRange = 40f..240f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun TimeSignatureDropdown(
    selectedTimeSignature: TimeSignature,
    onTimeSignatureSelected: (TimeSignature) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
    ) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .width(68.dp)
                .height(44.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(10.dp)
                ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedTimeSignature.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val chevronColor =
                    MaterialTheme.colorScheme.onSurfaceVariant

                Canvas(
                    modifier = Modifier.size(14.dp)
                ) {
                    val path = Path().apply {
                        moveTo(
                            x = size.width * 0.25f,
                            y = size.height * 0.40f
                        )
                        lineTo(
                            x = size.width * 0.50f,
                            y = size.height * 0.65f
                        )
                        lineTo(
                            x = size.width * 0.75f,
                            y = size.height * 0.40f
                        )
                    }

                    drawPath(
                        path = path,
                        color = chevronColor,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TimeSignature.entries.forEach { timeSignature ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = timeSignature.label,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onTimeSignatureSelected(timeSignature)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SubdivisionSection(
    selectedSubdivision: Int,
    onSubdivisionSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionLabel("SUBDIVISION")

        Spacer(modifier = Modifier.height(10.dp))

        for (rowIndex in 0 until 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (columnIndex in 1..4) {
                    val subdivision = rowIndex * 4 + columnIndex
                    val selected = subdivision == selectedSubdivision

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable {
                                onSubdivisionSelected(subdivision)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = subdivision.toString(),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 14.sp,
                                fontWeight = if (selected) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Medium
                                }
                            )
                        }
                    }
                }
            }

            if (rowIndex == 0) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PracticeSection(
    subdivision: Int,
    activeStep: Int,
    modifier: Modifier = Modifier
) {
    val syllables = syllablesFor(subdivision)
    val beatActive = activeStep == 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        SectionLabel("PRACTICE")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BEAT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "1 BEAT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                ) {
                    val timelineStart = 18.dp
                    val beatMarkerSize = 15.dp

                    Box(
                        modifier = Modifier
                            .offset(
                                x = timelineStart - (beatMarkerSize / 2)
                            )
                            .align(Alignment.CenterStart)
                            .size(beatMarkerSize)
                            .clip(CircleShape)
                            .background(
                                if (beatActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (beatActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                },
                                shape = CircleShape
                            )
                    )

                    BeatWaveform(
                        active = beatActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = timelineStart)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOTES",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "$subdivision NOTES",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    val timelineStart = 18.dp
                    val timelineDuration = maxWidth - timelineStart
                    val stepWidth = 36.dp

                    syllables.forEachIndexed { index, syllable ->
                        val progress = index.toFloat() / subdivision.toFloat()
                        val notePosition =
                            timelineStart + (timelineDuration * progress)

                        PracticeStep(
                            syllable = syllable,
                            active = activeStep == index,
                            modifier = Modifier
                                .width(stepWidth)
                                .offset(
                                    x = notePosition - (stepWidth / 2)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BeatWaveform(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val waveformColor = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
    }

    Canvas(
        modifier = modifier.height(18.dp)
    ) {
        val centerY = size.height / 2f
        val amplitude = size.height * 0.24f
        val cycles = 5f
        val samples = 96

        // Diameter bulatan onset = 15.dp, jadi radius = 7.5.dp.
        // Waveform mulai dari sisi kanan bulatan, bukan dari tengahnya.
        val startInset = 7.5.dp.toPx()
        val usableWidth = (size.width - startInset).coerceAtLeast(0f)

        val path = Path()

        for (index in 0..samples) {
            val progress = index.toFloat() / samples
            val x = startInset + (usableWidth * progress)

            val phase = progress * cycles * 2f * PI.toFloat()
            val y = centerY + sin(phase) * amplitude

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = waveformColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun PracticeStep(
    syllable: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(15.dp)
                .clip(CircleShape)
                .background(
                    if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.height(9.dp))

        Text(
            text = syllable,
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 12.sp,
            fontWeight = if (active) {
                FontWeight.SemiBold
            } else {
                FontWeight.Medium
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AudioControls(
    metronomeEnabled: Boolean,
    metronomeSound: MetronomeSoundMode,
    onMetronomeChanged: (Boolean) -> Unit,
    onMetronomeSoundChanged: (MetronomeSoundMode) -> Unit,
    guideEnabled: Boolean,
    guideSound: GuideSoundMode,
    onGuideChanged: (Boolean) -> Unit,
    onGuideSoundChanged: (GuideSoundMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AudioControlGroup(
            label = "Metronome",
            selectedSound = metronomeSound.label,
            soundOptions = MetronomeSoundMode.entries.map { it.label },
            checked = metronomeEnabled,
            onSoundSelected = { selectedLabel ->
                MetronomeSoundMode.entries
                    .firstOrNull { it.label == selectedLabel }
                    ?.let(onMetronomeSoundChanged)
            },
            onCheckedChange = onMetronomeChanged,
            modifier = Modifier.weight(1f)
        )

        AudioControlGroup(
            label = "Guide",
            selectedSound = guideSound.label,
            soundOptions = GuideSoundMode.entries.map { it.label },
            checked = guideEnabled,
            onSoundSelected = { selectedLabel ->
                GuideSoundMode.entries
                    .firstOrNull { it.label == selectedLabel }
                    ?.let(onGuideSoundChanged)
            },
            onCheckedChange = onGuideChanged,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AudioControlGroup(
    label: String,
    selectedSound: String,
    soundOptions: List<String>,
    checked: Boolean,
    onSoundSelected: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SoundDropdown(
                selectedSound = selectedSound,
                options = soundOptions,
                onSoundSelected = onSoundSelected,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SoundDropdown(
    selectedSound: String,
    options: List<String>,
    onSoundSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
    ) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedSound,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                val chevronColor = MaterialTheme.colorScheme.onSurfaceVariant

                Canvas(
                    modifier = Modifier.size(16.dp)
                ) {
                    val path = Path().apply {
                        moveTo(
                            x = size.width * 0.25f,
                            y = size.height * 0.40f
                        )
                        lineTo(
                            x = size.width * 0.50f,
                            y = size.height * 0.65f
                        )
                        lineTo(
                            x = size.width * 0.75f,
                            y = size.height * 0.40f
                        )
                    }

                    drawPath(
                        path = path,
                        color = chevronColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onSoundSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        textAlign = TextAlign.Center
    )
}

internal fun syllablesFor(subdivision: Int): List<String> {
    return when (subdivision) {
        1 -> listOf("TA")
        2 -> listOf("TA", "KA")
        3 -> listOf("TA", "KI", "TA")
        4 -> listOf("TA", "KA", "DI", "MI")
        5 -> listOf("TA", "KA", "TA", "KI", "TA")
        6 -> listOf("TA", "KA", "DI", "MI", "TA", "KA")
        7 -> listOf("TA", "KA", "DI", "MI", "TA", "KI", "TA")
        8 -> listOf("TA", "KA", "DI", "MI", "TA", "KA", "JU", "NU")
        else -> listOf("TA")
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun TrainerScreenPreview() {
    NexRhythmTheme(
        darkTheme = false
    ) {
        TrainerScreen()
    }
}