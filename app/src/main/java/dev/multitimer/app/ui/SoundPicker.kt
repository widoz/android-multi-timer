package dev.multitimer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.multitimer.app.R
import dev.multitimer.app.alarm.SoundPreview
import dev.multitimer.app.alarm.Sounds
import dev.multitimer.app.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface PickerRow {
    data class Header(val text: String) : PickerRow
    /** [id] null = "App default". */
    data class Choice(val id: String?, val label: String) : PickerRow
}

/** Display name of a stored sound, looked up off the main thread. */
@Composable
fun rememberSoundTitle(uri: String): String {
    val context = LocalContext.current
    val title by produceState(initialValue = "", uri) {
        value = withContext(Dispatchers.IO) { Sounds.title(context, uri) }
    }
    return title
}

/**
 * Radio list of alarm sounds. Tapping one plays a preview; OK confirms.
 *
 * @param allowAppDefault adds an "App default (…)" option, returned as null (per-timer picker).
 */
@Composable
fun SoundPickerDialog(
    title: String,
    current: String?,
    allowAppDefault: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    val context = LocalContext.current
    val defaultSound by AppSettings.defaultSound.collectAsStateWithLifecycle()
    val phoneSounds by produceState<List<Sounds.Option>?>(initialValue = null) {
        val keep = setOfNotNull(current, defaultSound)
        value = withContext(Dispatchers.IO) { Sounds.alarmSounds(context, keep) }
    }
    val appDefaultLabel = stringResource(R.string.app_default, rememberSoundTitle(defaultSound))
    val phoneDefaultLabel = stringResource(R.string.sound_phone_default)
    val silentLabel = stringResource(R.string.sound_silent)
    var selected by rememberSaveable { mutableStateOf(current) }

    val preview = remember { SoundPreview(context) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    val appSoundTitles = Sounds.APP_SOUNDS.map { stringResource(it.title) }
    val appHeader = stringResource(R.string.sound_group_app)
    val phoneHeader = stringResource(R.string.sound_group_phone)
    val rows: List<PickerRow> = buildList {
        if (allowAppDefault) add(PickerRow.Choice(null, appDefaultLabel))
        add(PickerRow.Header(appHeader))
        Sounds.APP_SOUNDS.forEachIndexed { i, sound -> add(PickerRow.Choice(sound.id, appSoundTitles[i])) }
        add(PickerRow.Header(phoneHeader))
        add(PickerRow.Choice(Sounds.PHONE_DEFAULT, phoneDefaultLabel))
        phoneSounds?.forEach { add(PickerRow.Choice(it.uri, it.title)) }
        add(PickerRow.Choice(Sounds.SILENT, silentLabel))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(Modifier.heightIn(max = 360.dp).selectableGroup()) {
                    items(rows) { row ->
                        when (row) {
                            is PickerRow.Header -> Text(
                                row.text,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
                                    .semantics { heading() },
                            )
                            is PickerRow.Choice -> {
                                val isSelected = row.id == selected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .selectable(selected = isSelected, role = Role.RadioButton) {
                                            selected = row.id
                                            preview.play(row.id ?: defaultSound)
                                        }
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Text(row.label, modifier = Modifier.padding(start = 16.dp))
                                }
                            }
                        }
                    }
                    if (phoneSounds == null) {
                        item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
                    }
                }
                Text(
                    stringResource(R.string.sound_preview_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                preview.stop()
                onConfirm(selected)
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
