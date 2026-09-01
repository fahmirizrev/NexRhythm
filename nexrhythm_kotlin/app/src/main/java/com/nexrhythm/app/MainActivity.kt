package com.nexrhythm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        installSplashScreen()
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

private enum class TrainerMode(
    val label: String
) {
    BASIC("Basic"),
    EXERCISE("Exercise")
}

@Composable
private fun TrainerScreen(

    modifier: Modifier = Modifier
) {
    var trainerMode by remember {
        mutableStateOf(TrainerMode.BASIC)
    }

    var bpm by remember { mutableIntStateOf(60) }
    var subdivision by remember { mutableIntStateOf(2) }
    var timeSignature by remember { mutableStateOf(TimeSignature.FOUR_FOUR) }

    var exerciseMeasuresPerSubdivision by remember {
        mutableIntStateOf(2)
    }
    var exerciseSubdivision by remember {
        mutableIntStateOf(1)
    }
    var exerciseMeasure by remember {
        mutableIntStateOf(1)
    }
    var exerciseDirection by remember {
        mutableStateOf(ExerciseDirection.ASCENDING)
    }

    var metronomeEnabled by remember { mutableStateOf(true) }

    var metronomeSound by remember { mutableStateOf(MetronomeSoundMode.CLICK) }

    var guideEnabled by remember { mutableStateOf(true) }
    var guideSound by remember { mutableStateOf(GuideSoundMode.WOOD) }

    var isRunning by remember { mutableStateOf(false) }
    var activeStep by remember { mutableIntStateOf(-1) }
    var currentBeatIndex by remember { mutableIntStateOf(-1) }


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

    val displayedSubdivision =
        if (trainerMode == TrainerMode.EXERCISE) {
            exerciseSubdivision
        } else {
            subdivision
        }

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
        trainerMode,
        subdivision,
        timeSignature,
        exerciseMeasuresPerSubdivision
    ) {
        if (isRunning) {
            audioEngine.start(
                bpm = bpm,
                subdivision =
                    if (trainerMode == TrainerMode.EXERCISE) {
                        1
                    } else {
                        subdivision
                    },
                timeSignature = timeSignature,
                initialOptions = audioOptions,
                exerciseMeasuresPerSubdivision =
                    if (trainerMode == TrainerMode.EXERCISE) {
                        exerciseMeasuresPerSubdivision
                    } else {
                        null
                    }
            )
        } else {
            audioEngine.stop()
        }
    }


    LaunchedEffect(
        isRunning,
        trainerMode,
        subdivision,
        timeSignature
    ) {
        if (!isRunning) {
            activeStep = -1
            currentBeatIndex = -1
            return@LaunchedEffect
        }

        activeStep = 0
        currentBeatIndex = 0


        var visualSubdivision =
            if (trainerMode == TrainerMode.EXERCISE) {
                1
            } else {
                subdivision
            }

        if (trainerMode == TrainerMode.EXERCISE) {
            exerciseSubdivision = 1
            exerciseMeasure = 1
            exerciseDirection =
                ExerciseDirection.ASCENDING
        }

        var beatDurationNanos =
            60_000_000_000L / currentBpm

        var beatStartTimeNanos =
            withFrameNanos { it }

        var exerciseCompleted = false

        while (!exerciseCompleted) {
            withFrameNanos { frameTimeNanos ->
                val playbackState =
                    audioEngine.playbackState()

                currentBeatIndex =
                    playbackState.beatIndex

                if (trainerMode == TrainerMode.EXERCISE) {
                    if (playbackState.exerciseMeasure != null) {

                        exerciseSubdivision =
                            playbackState.subdivision

                        exerciseMeasure =
                            playbackState.exerciseMeasure

                        exerciseDirection =
                            playbackState.exerciseDirection
                                ?: ExerciseDirection.ASCENDING

                        if (playbackState.exerciseComplete) {

                            exerciseCompleted = true
                            return@withFrameNanos
                        }

                        if (
                            playbackState.subdivision !=
                            visualSubdivision
                        ) {
                            visualSubdivision =
                                playbackState.subdivision

                            beatStartTimeNanos =
                                frameTimeNanos

                            beatDurationNanos =
                                60_000_000_000L / currentBpm

                            activeStep = 0

                            return@withFrameNanos
                        }
                    }
                }

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
                        (
                                elapsedInBeat *
                                        visualSubdivision
                                ) / beatDurationNanos
                        ).toInt().coerceIn(
                        0,
                        visualSubdivision - 1
                    )
            }
        }

        if (
            trainerMode == TrainerMode.EXERCISE &&
            isRunning
        ) {
            isRunning = false
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        TrainerHeader()

        Spacer(modifier = Modifier.height(10.dp))

        TrainerModeSelector(
            selectedMode = trainerMode,
            onModeSelected = { selectedMode ->
                if (selectedMode != trainerMode) {
                    isRunning = false
                    activeStep = -1
                    trainerMode = selectedMode

                    if (selectedMode == TrainerMode.EXERCISE) {
                        exerciseSubdivision = 1
                        exerciseMeasure = 1
                        exerciseDirection =
                            ExerciseDirection.ASCENDING
                    }

                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TempoSection(

            bpm = bpm,
            timeSignature = timeSignature,
            onBpmChange = { bpm = it.coerceIn(40, 240) },
            onTimeSignatureChange = { selectedTimeSignature ->
                timeSignature = selectedTimeSignature

                if (
                    trainerMode == TrainerMode.EXERCISE &&
                    isRunning
                ) {
                    exerciseSubdivision = 1
                    exerciseMeasure = 1
                }
            }
        )


        Spacer(modifier = Modifier.height(10.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (trainerMode == TrainerMode.BASIC) {
            SubdivisionSection(
                selectedSubdivision = subdivision,
                onSubdivisionSelected = { subdivision = it }
            )
        } else {
            ExerciseSection(
                measuresPerSubdivision =
                    exerciseMeasuresPerSubdivision,
                currentSubdivision =
                    exerciseSubdivision,
                currentMeasure =
                    exerciseMeasure,
                currentDirection =
                    exerciseDirection,
                isRunning = isRunning,
                onMeasuresChange = { measures ->
                    exerciseMeasuresPerSubdivision =
                        measures.coerceIn(1, 8)
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        PracticeSection(
            subdivision = displayedSubdivision,
            activeStep = activeStep,
            timeSignature = timeSignature,
            currentBeatIndex = currentBeatIndex,
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
            onClick = {
                if (isRunning) {
                    isRunning = false
                } else {
                    if (trainerMode == TrainerMode.EXERCISE) {
                        exerciseSubdivision = 1
                        exerciseMeasure = 1
                        exerciseDirection =
                            ExerciseDirection.ASCENDING
                    }

                    isRunning = true

                }
            },
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
                text = when {
                    isRunning -> {
                        "■  STOP"
                    }

                    trainerMode == TrainerMode.EXERCISE -> {
                        "▶  START EXERCISE"
                    }

                    else -> {
                        "▶  START PRACTICE"
                    }
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
private fun TrainerModeSelector(
    selectedMode: TrainerMode,
    onModeSelected: (TrainerMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(9.dp)
            )
    ) {
        TrainerMode.entries.forEach { mode ->
            val selected = mode == selectedMode

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable {
                        onModeSelected(mode)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 12.sp,
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
private fun ExerciseSection(
    measuresPerSubdivision: Int,
    currentSubdivision: Int,
    currentMeasure: Int,
    currentDirection: ExerciseDirection,
    isRunning: Boolean,
    onMeasuresChange: (Int) -> Unit
) {
    val nextSubdivision =
        when (currentDirection) {
            ExerciseDirection.ASCENDING -> {
                if (currentSubdivision < 8) {
                    currentSubdivision + 1
                } else {
                    7
                }
            }

            ExerciseDirection.DESCENDING -> {
                if (currentSubdivision > 1) {
                    currentSubdivision - 1
                } else {
                    null
                }
            }
        }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionLabel("EXERCISE")

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Subdivision Ladder",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (subdivision in 1..8) {
                val selected =
                    subdivision == currentSubdivision

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
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
                        fontSize = 12.sp,
                        fontWeight = if (selected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Medium
                        }
                    )
                }

                if (subdivision < 8) {
                    Text(
                        text = "→",
                        modifier = Modifier.padding(
                            horizontal = 1.dp
                        ),
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    onMeasuresChange(
                        measuresPerSubdivision - 1
                    )
                },
                enabled =
                    !isRunning &&
                            measuresPerSubdivision > 1,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "−",
                    fontSize = 20.sp
                )
            }

            Text(
                text = buildString {
                    append(measuresPerSubdivision)
                    append(
                        if (measuresPerSubdivision == 1) {
                            " Measure"
                        } else {
                            " Measures"
                        }
                    )
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            TextButton(
                onClick = {
                    onMeasuresChange(
                        measuresPerSubdivision + 1
                    )
                },
                enabled =
                    !isRunning &&
                            measuresPerSubdivision < 8,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text =
                    if (
                        currentDirection ==
                        ExerciseDirection.ASCENDING
                    ) {
                        "Up"
                    } else {
                        "Down"
                    },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (nextSubdivision != null) {
                    "Measure $currentMeasure/" +
                            "$measuresPerSubdivision" +
                            "  ·  Next $nextSubdivision"
                } else {
                    "Measure $currentMeasure/" +
                            "$measuresPerSubdivision" +
                            "  ·  Final"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun PracticeSection(
    subdivision: Int,
    activeStep: Int,
    timeSignature: TimeSignature,
    currentBeatIndex: Int,
    modifier: Modifier = Modifier
) {
    val syllables = syllablesFor(subdivision)
    val beatCount = timeSignature.numerator

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
                .padding(bottom = 4.dp),
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
                        text =
                            if (beatCount == 1) {
                                "1 BEAT"
                            } else {
                                "$beatCount BEATS"
                            },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                ) {
                    val timelineStart = 18.dp
                    val timelineDuration =
                        maxWidth - timelineStart
                    val beatMarkerSize = 15.dp

                    BeatWaveform(
                        beatCount = beatCount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = timelineStart)
                    )

                    for (beatIndex in 0 until beatCount) {
                        val progress =
                            beatIndex.toFloat() /
                                    beatCount.toFloat()

                        val beatPosition =
                            timelineStart +
                                    (timelineDuration * progress)

                        val active =
                            currentBeatIndex == beatIndex

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = beatPosition -
                                            (beatMarkerSize / 2)
                                )
                                .align(Alignment.CenterStart)
                                .size(beatMarkerSize)
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
                                        MaterialTheme.colorScheme.primary
                                            .copy(alpha = 0.55f)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

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
    beatCount: Int,
    modifier: Modifier = Modifier
) {
    val waveformColor =
        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)

    Canvas(
        modifier = modifier.height(18.dp)
    ) {
        val safeBeatCount =
            beatCount.coerceAtLeast(1)

        val centerY = size.height / 2f
        val amplitude = size.height * 0.24f

        val beatWidth =
            size.width / safeBeatCount

        val markerRadius =
            7.5.dp.toPx()

        val markerGap =
            4.dp.toPx()

        val samplesPerBeat = 24

        for (beatIndex in 0 until safeBeatCount) {
            val beatStart =
                beatWidth * beatIndex

            val beatEnd =
                beatWidth * (beatIndex + 1)

            val segmentStart =
                beatStart +
                        markerRadius +
                        markerGap

            val segmentEnd =
                if (beatIndex < safeBeatCount - 1) {
                    beatEnd -
                            markerRadius -
                            markerGap
                } else {
                    beatEnd
                }

            if (segmentEnd <= segmentStart) {
                continue
            }

            val segmentWidth =
                segmentEnd - segmentStart

            val path = Path()

            for (sampleIndex in 0..samplesPerBeat) {
                val progress =
                    sampleIndex.toFloat() /
                            samplesPerBeat

                val x =
                    segmentStart +
                            (segmentWidth * progress)

                val phase =
                    progress *
                            2f *
                            PI.toFloat()

                val y =
                    centerY +
                            sin(phase) *
                            amplitude

                if (sampleIndex == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = waveformColor,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
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