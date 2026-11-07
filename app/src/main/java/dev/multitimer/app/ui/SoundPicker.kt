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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.multitimer.app.R
import dev.multitimer.app.alarm.SoundPreview
import dev.multitimer.app.alarm.Sounds
import dev.multitimer.app.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    val options: List<Pair<String?, String>> = buildList {
        if (allowAppDefault) add(null to appDefaultLabel)
        add(Sounds.PHONE_DEFAULT to phoneDefaultLabel)
        phoneSounds?.forEach { add(it.uri to it.title) }
        add(Sounds.SILENT to silentLabel)
    }.distinctBy { it.first } // never show the same entry twice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (phoneSounds == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 360.dp).selectableGroup()) {
                    items(options) { (uri, label) ->
                        val isSelected = uri == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(selected = isSelected, role = Role.RadioButton) {
                                    selected = uri
                                    preview.play(uri ?: defaultSound)
                                }
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Text(label, modifier = Modifier.padding(start = 16.dp))
                        }
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
