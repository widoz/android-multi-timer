package dev.multitimer.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.multitimer.app.R
import dev.multitimer.app.data.AppSettings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.multitimer.app.data.TimerItem
import dev.multitimer.app.data.TimerState
import dev.multitimer.app.data.formatClock
import dev.multitimer.app.ui.theme.TIMER_COLOR_COUNT
import dev.multitimer.app.ui.theme.timerColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Bottom sheet for creating a timer, or editing [editing] when it is not null.
 * Changing the duration of a timer that already counted some time asks for confirmation.
 */
fun TimerSheet(
    editing: TimerItem?,
    defaultColor: Int,
    onDismiss: () -> Unit,
    onSave: (label: String, durationMs: Long, colorIndex: Int, soundUri: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val initialSeconds = editing?.let { it.totalMs / 1000 } ?: (5 * 60L)
    var label by rememberSaveable { mutableStateOf(editing?.label ?: "") }
    var hours by rememberSaveable { mutableStateOf(two(initialSeconds / 3600)) }
    var minutes by rememberSaveable { mutableStateOf(two(initialSeconds % 3600 / 60)) }
    var seconds by rememberSaveable { mutableStateOf(two(initialSeconds % 60)) }
    var color by rememberSaveable { mutableIntStateOf(editing?.colorIndex ?: defaultColor) }
    var sound by rememberSaveable { mutableStateOf(editing?.soundUri) } // null = app default
    var pickingSound by rememberSaveable { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var confirmRestartMs by rememberSaveable { mutableStateOf<Long?>(null) }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(if (editing == null) R.string.new_timer else R.string.edit_timer),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text(stringResource(R.string.label)) },
                placeholder = { Text(stringResource(R.string.label_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.duration), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DurationField(hours, R.string.hours, Modifier.weight(1f)) { hours = it; showError = false }
                DurationField(minutes, R.string.minutes, Modifier.weight(1f)) { minutes = it; showError = false }
                DurationField(seconds, R.string.seconds, Modifier.weight(1f)) { seconds = it; showError = false }
            }
            if (showError) {
                Text(
                    stringResource(R.string.duration_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text(stringResource(R.string.sound), style = MaterialTheme.typography.labelLarge)
            SoundRow(sound = sound, onClick = { pickingSound = true })

            Text(stringResource(R.string.color), style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.selectableGroup(),
            ) {
                repeat(TIMER_COLOR_COUNT) { i ->
                    val selected = i == color
                    val name = stringResource(ColorNames[i])
                    // 48dp touch target around a 36dp swatch; announced as "Teal, selected" etc.
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .selectable(selected = selected, role = Role.RadioButton) { color = i }
                            .semantics { contentDescription = name },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(timerColor(i), CircleShape)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else timerColor(i),
                                    shape = CircleShape,
                                )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
            ) {
                TextButton(onClick = { close() }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = {
                    val h = hours.toLongOrNull() ?: 0
                    val m = (minutes.toLongOrNull() ?: 0).coerceAtMost(59)
                    val s = (seconds.toLongOrNull() ?: 0).coerceAtMost(59)
                    val totalMs = (h * 3600 + m * 60 + s) * 1000
                    when {
                        totalMs <= 0 -> showError = true
                        editing != null && totalMs != editing.totalMs && editing.hasProgress ->
                            confirmRestartMs = totalMs
                        else -> {
                            onSave(label, totalMs, color, sound)
                            close()
                        }
                    }
                }) {
                    Text(stringResource(if (editing == null) R.string.start else R.string.save))
                }
            }
        }
    }

    if (pickingSound) {
        SoundPickerDialog(
            title = stringResource(R.string.timer_sound),
            current = sound,
            allowAppDefault = true,
            onDismiss = { pickingSound = false },
            onConfirm = { sound = it },
        )
    }

    val pendingMs = confirmRestartMs
    if (editing != null && pendingMs != null) {
        AlertDialog(
            onDismissRequest = { confirmRestartMs = null },
            title = { Text(stringResource(R.string.restart_title)) },
            text = {
                Text(
                    if (editing.state == TimerState.RUNNING) {
                        stringResource(R.string.restart_running, editing.label, formatClock(pendingMs))
                    } else {
                        stringResource(
                            R.string.restart_paused,
                            editing.label,
                            formatClock(editing.remainingMs),
                            formatClock(pendingMs),
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestartMs = null
                    onSave(label, pendingMs, color, sound)
                    close()
                }) { Text(stringResource(R.string.restart)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestartMs = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private val ColorNames = listOf(
    R.string.color_teal,
    R.string.color_amber,
    R.string.color_violet,
    R.string.color_green,
    R.string.color_rose,
)

/** Outlined row showing the timer's sound; opens the picker. */
@Composable
private fun SoundRow(sound: String?, onClick: () -> Unit) {
    val defaultSound by AppSettings.defaultSound.collectAsStateWithLifecycle()
    val label = if (sound == null) {
        stringResource(R.string.app_default, rememberSoundTitle(defaultSound))
    } else {
        rememberSoundTitle(sound)
    }
    val description = stringResource(R.string.sound_row_cd, label)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun two(n: Long) = n.toString().padStart(2, '0')

/** Running, or paused partway through: changing the duration would lose that progress. */
private val TimerItem.hasProgress: Boolean
    get() = state == TimerState.RUNNING || (state == TimerState.PAUSED && remainingMs < totalMs)

@Composable
private fun DurationField(value: String, unit: Int, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter(Char::isDigit).take(2)) },
        label = { Text(stringResource(unit)) },
        singleLine = true,
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}
