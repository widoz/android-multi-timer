package dev.multitimer.app.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.multitimer.app.R
import dev.multitimer.app.alarm.TimerNotifier
import dev.multitimer.app.data.TimerRepository
import dev.multitimer.app.data.TimerState
import dev.multitimer.app.data.formatDuration
import dev.multitimer.app.ui.theme.TIMER_COLOR_COUNT
import kotlinx.coroutines.delay

private val QuickStarts = listOf(60L, 5 * 60L, 10 * 60L, 20 * 60L, 30 * 60L, 60 * 60L)

/** Sheet target: null = closed, NEW_TIMER = create, otherwise the id of the timer being edited. */
private const val NEW_TIMER = -1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val timers by TimerRepository.timers.collectAsStateWithLifecycle()
    var sheetTarget by rememberSaveable { mutableStateOf<Int?>(null) }
    var fullScreenId by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Clock for the UI only; it ticks while the screen is visible.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = System.currentTimeMillis()
                TimerRepository.finishDue(now)
                delay(200)
            }
        }
    }

    // Android 13+ notification permission
    var notificationsAllowed by remember { mutableStateOf(TimerNotifier.canPost(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = granted
        if (granted) TimerRepository.reconcile()
    }
    LaunchedEffect(Unit) {
        // The prompt exists on Android 13+; older versions have notifications on by default.
        if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LifecycleResumeEffect(Unit) {
        val allowed = TimerNotifier.canPost(context)
        if (allowed && !notificationsAllowed) TimerRepository.reconcile()
        notificationsAllowed = allowed
        onPauseOrDispose { }
    }

    val running = timers.count { it.state == TimerState.RUNNING }
    val ringing = timers.count { it.state == TimerState.FINISHED }
    val baseSummary = pluralStringResource(R.plurals.summary, running, running, timers.size)
    val summary = if (ringing > 0) pluralStringResource(R.plurals.summary_ringing, ringing, baseSummary, ringing) else baseSummary
    val nextColor = timers.size % TIMER_COLOR_COUNT

    fun deleteWithUndo(id: Int) {
        val deleted = TimerRepository.delete(id) ?: return
        val message = context.getString(R.string.deleted, deleted.timer.label)
        val undo = context.getString(R.string.undo)
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            val result = snackbarHost.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) TimerRepository.restore(deleted)
        }
    }

    val fullScreen = fullScreenId?.let { id -> timers.firstOrNull { it.id == id } }
    if (fullScreenId != null && fullScreen == null) {
        LaunchedEffect(fullScreenId) { fullScreenId = null } // deleted while full screen
    }
    if (fullScreen != null) {
        FullScreenTimer(
            timer = fullScreen,
            ended = timers
                .filter { it.state == TimerState.FINISHED && it.id != fullScreen.id }
                .sortedByDescending { it.endAt },
            now = now,
            onExit = { fullScreenId = null },
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.timers), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { TimerRepository.pauseAll() }, enabled = running > 0) {
                        Icon(Icons.Filled.Pause, contentDescription = stringResource(R.string.pause_all))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { sheetTarget = NEW_TIMER },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_timer)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!notificationsAllowed) {
                item(key = "permission") {
                    PermissionCard(onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    })
                }
            }
            item(key = "quick") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickStarts.forEach { secs ->
                        val text = formatDuration(secs * 1000)
                        AssistChip(
                            onClick = { TimerRepository.add(text, secs * 1000, nextColor) },
                            label = { Text(stringResource(R.string.quick_add, text)) },
                            // outline (not outlineVariant) keeps the chip edge at >= 3:1
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    }
                }
            }
            if (timers.isEmpty()) {
                item(key = "empty") { EmptyState() }
            }
            items(timers, key = { it.id }) { timer ->
                TimerCard(
                    timer = timer,
                    now = now,
                    onEdit = { sheetTarget = timer.id },
                    onDelete = { confirmDeleteId = timer.id },
                    onFullScreen = { fullScreenId = timer.id },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    val target = sheetTarget
    if (target != null) {
        val editing = timers.firstOrNull { it.id == target }
        if (target != NEW_TIMER && editing == null) {
            // The timer was deleted (e.g. from its notification) while its sheet was open.
            LaunchedEffect(target) { sheetTarget = null }
        } else {
            key(target) {
                TimerSheet(
                    editing = editing,
                    defaultColor = nextColor,
                    onDismiss = { sheetTarget = null },
                    onSave = { label, ms, color, sound ->
                        if (editing == null) TimerRepository.add(label, ms, color, sound)
                        else TimerRepository.edit(editing.id, label, ms, color, sound)
                    },
                )
            }
        }
    }

    val toDelete = timers.firstOrNull { it.id == confirmDeleteId }
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(stringResource(R.string.delete_title, toDelete.label)) },
            text = {
                Text(
                    stringResource(
                        when (toDelete.state) {
                            TimerState.RUNNING -> R.string.delete_running
                            TimerState.FINISHED -> R.string.delete_ringing
                            TimerState.PAUSED -> R.string.delete_idle
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteId = null
                        deleteWithUndo(toDelete.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    } else if (confirmDeleteId != null) {
        LaunchedEffect(confirmDeleteId) { confirmDeleteId = null }
    }
}

@Composable
private fun PermissionCard(onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.perm_body), style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = onOpenSettings) { Text(stringResource(R.string.perm_action)) }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Timer,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.empty),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
