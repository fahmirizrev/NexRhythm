package com.nexrhythm.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexrhythm.app.ui.theme.NexRhythmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RhythmAnalyzerActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            NexRhythmTheme {
                Scaffold(
                    modifier =
                        Modifier.fillMaxSize(),
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .background
                ) { innerPadding ->
                    RhythmAnalyzerScreen(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    innerPadding
                                ),
                        onBack = {
                            finish()
                        },
                        onUseSegment = { segment ->
                            setResult(
                                Activity.RESULT_OK,
                                Intent()
                                    .putExtra(
                                        EXTRA_BPM,
                                        segment.bpm
                                    )
                                    .putExtra(
                                        EXTRA_NUMERATOR,
                                        segment
                                            .timeSignature
                                            .numerator
                                    )
                                    .putExtra(
                                        EXTRA_DENOMINATOR,
                                        segment
                                            .timeSignature
                                            .denominator
                                    )
                            )

                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_BPM =
            "rhythm_analyzer_bpm"

        const val EXTRA_NUMERATOR =
            "rhythm_analyzer_numerator"

        const val EXTRA_DENOMINATOR =
            "rhythm_analyzer_denominator"
    }
}

@Composable
private fun RhythmAnalyzerScreen(
    onBack: () -> Unit,
    onUseSegment: (
        RhythmSegment
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context =
        androidx.compose.ui.platform
            .LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()

    val analyzer =
        remember(context) {
            RhythmAnalyzer(
                context.applicationContext
            )
        }

    var selectedUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var selectedName by remember {
        mutableStateOf<String?>(null)
    }

    var result by remember {
        mutableStateOf<
                RhythmAnalysisResult?
                >(null)
    }

    var selectedSegmentIndex by remember {
        mutableIntStateOf(0)
    }

    var isAnalyzing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val audioPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .OpenDocument()
        ) { uri ->
            if (uri != null) {
                selectedUri = uri

                selectedName =
                    queryDisplayName(
                        context = context,
                        uri = uri
                    )

                result = null
                selectedSegmentIndex = 0
                errorMessage = null
            }
        }

    Column(
        modifier =
            modifier.padding(
                horizontal = 20.dp,
                vertical = 14.dp
            )
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                contentPadding =
                    PaddingValues(0.dp)
            ) {
                Text(
                    text = "←",
                    fontSize = 22.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {
                Text(
                    text = "NexRhythm",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onBackground,
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        "Rhythm Analyzer",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text = "AUDIO",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight =
                FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                selectedName
                    ?: "No audio selected",
            modifier =
                Modifier.fillMaxWidth(),
            color =
                if (
                    selectedName == null
                ) {
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSurface
                },
            fontSize = 14.sp,
            fontWeight =
                FontWeight.Medium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick = {
                audioPicker.launch(
                    arrayOf("audio/*")
                )
            },
            enabled = !isAnalyzing,
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(10.dp),
            colors =
                ButtonDefaults
                    .buttonColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant,
                        contentColor =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )
        ) {
            Text(
                text =
                    if (
                        selectedUri == null
                    ) {
                        "IMPORT AUDIO"
                    } else {
                        "CHANGE AUDIO"
                    },
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        HorizontalDivider(
            color =
                MaterialTheme
                    .colorScheme
                    .outlineVariant
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                "RHYTHM ANALYSIS",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight =
                FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        when {
            isAnalyzing -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        Text(
                            text =
                                "Analyzing the full track…",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text =
                            errorMessage
                                ?: "Analysis failed.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        fontSize = 12.sp,
                        textAlign =
                            TextAlign.Center
                    )
                }
            }

            result != null -> {
                val segments =
                    result
                        ?.segments
                        .orEmpty()

                if (segments.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                "No stable rhythm was detected.",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {
                        itemsIndexed(
                            segments
                        ) { index,
                            segment ->
                            RhythmSegmentRow(
                                segment =
                                    segment,
                                selected =
                                    index ==
                                            selectedSegmentIndex,
                                onClick = {
                                    selectedSegmentIndex =
                                        index
                                }
                            )
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text =
                            "Import a song, then analyze its tempo and time signature across the full duration.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign =
                            TextAlign.Center
                    )
                }
            }
        }

        if (
            !isAnalyzing &&
            result == null
        ) {
            Button(
                onClick = {
                    val uri =
                        selectedUri
                            ?: return@Button

                    errorMessage = null
                    isAnalyzing = true

                    coroutineScope.launch {
                        try {
                            result =
                                withContext(
                                    Dispatchers
                                        .Default
                                ) {
                                    analyzer
                                        .analyze(
                                            uri
                                        )
                                }

                            selectedSegmentIndex =
                                0
                        } catch (
                            throwable: Throwable
                        ) {
                            errorMessage =
                                throwable
                                    .message
                                    ?: "Analysis failed."
                        } finally {
                            isAnalyzing =
                                false
                        }
                    }
                },
                enabled =
                    selectedUri != null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                shape =
                    RoundedCornerShape(10.dp),
                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer,
                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
            ) {
                Text(
                    text =
                        "ANALYZE",
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }

        val selectedSegment =
            result
                ?.segments
                ?.getOrNull(
                    selectedSegmentIndex
                )

        if (
            !isAnalyzing &&
            selectedSegment != null
        ) {
            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Button(
                onClick = {
                    onUseSegment(
                        selectedSegment
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                shape =
                    RoundedCornerShape(10.dp),
                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer,
                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
            ) {
                Text(
                    text =
                        "USE ${selectedSegment.bpm} BPM · ${selectedSegment.timeSignature.label}",
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RhythmSegmentRow(
    segment: RhythmSegment,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        if (selected) {
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                        } else {
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                                .copy(
                                    alpha =
                                        0.42f
                                )
                        },
                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                )
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            Text(
                text =
                    "${formatTime(segment.startMs)} – ${formatTime(segment.endMs)}",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight =
                    FontWeight.Medium
            )

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(
                text =
                    "${segment.bpm} BPM",
                color =
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurface
                    },
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "Tempo ${segment.tempoConfidence.name.lowercase()}",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 9.sp
            )
        }

        Column(
            horizontalAlignment =
                Alignment.End
        ) {
            Text(
                text =
                    segment
                        .timeSignature
                        .label,
                color =
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurface
                    },
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "Meter ${segment.meterConfidence.name.lowercase()}",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

private fun queryDisplayName(
    context: android.content.Context,
    uri: Uri
): String {
    val projection =
        arrayOf(
            android.provider
                .OpenableColumns
                .DISPLAY_NAME
        )

    context.contentResolver
        .query(
            uri,
            projection,
            null,
            null,
            null
        )
        ?.use { cursor ->
            val index =
                cursor.getColumnIndex(
                    android.provider
                        .OpenableColumns
                        .DISPLAY_NAME
                )

            if (
                index >= 0 &&
                cursor.moveToFirst()
            ) {
                return cursor
                    .getString(index)
            }
        }

    return uri
        .lastPathSegment
        ?: "Selected audio"
}

private fun formatTime(
    milliseconds: Long
): String {
    val totalSeconds =
        milliseconds /
                1_000L

    val minutes =
        totalSeconds /
                60L

    val seconds =
        totalSeconds %
                60L

    return "%02d:%02d"
        .format(
            minutes,
            seconds
        )
}