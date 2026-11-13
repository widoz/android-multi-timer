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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
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
    val listState = rememberLazyListState()
    val reorder = remember(listState) { ReorderState(listState) }
    var moveMenuId by remember { mutableStateOf<Int?>(null) }
    var flash by remember { mutableStateOf<Pair<Int, Long>?>(null) } // card that just moved via the menu
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val density = LocalDensity.current

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
    val ids = timers.map { it.id }
    LaunchedEffect(ids) { reorder.syncWith(ids) }
    val shown = reorder.arrange(timers) { it.id }
    val canReorder = timers.size >= 2

    // Moves without dragging (Move menu, TalkBack): commit, keep the card visible, announce.
    fun moveTo(id: Int, to: Int) {
        val from = ids.indexOf(id)
        val target = to.coerceIn(0, ids.lastIndex)
        if (from < 0 || target == from) return
        TimerRepository.move(id, target)
        val label = timers[from].label
        view.announceForAccessibility(context.getString(R.string.moved_to, label, target + 1, ids.size))
        flash = id to System.nanoTime()
        scope.launch {
            val header = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key is Int }
                ?.let { it.index - shown.indexOfFirst { t -> t.id == it.key } } ?: 1
            val index = header + target
            if (listState.layoutInfo.visibleItemsInfo.none { it.index == index && it.offset >= 0 }) {
                listState.animateScrollToItem(index)
            }
        }
    }

    // Auto-scroll while a lifted card is held near the top or bottom edge.
    val edgePx = with(density) { 56.dp.toPx() }
    LaunchedEffect(reorder.active) {
        while (reorder.active) {
            withFrameNanos { }
            val over = reorder.edgeOverlap(edgePx)
            if (over != 0f) {
                val step = (over / 4f).coerceIn(-edgePx / 2, edgePx / 2)
                listState.scrollBy(step)
                if (reorder.dragBy(0f)) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

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
            state = listState,
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
            items(shown, key = { it.id }) { timer ->
                val lifted = reorder.draggingId == timer.id
                val index = ids.indexOf(timer.id)
                val moves = moveOptions(index, ids.size)
                val actionLabels = moves.map { stringResource(it.label) }
                val flashAlpha = remember { Animatable(0f) }
                LaunchedEffect(flash) {
                    if (flash?.first == timer.id) {
                        flashAlpha.snapTo(1f)
                        flashAlpha.animateTo(0f, tween(600))
                    }
                }
                Box(
                    Modifier
                        .zIndex(if (lifted) 1f else 0f)
                        .animateItem(
                            // The lifted card follows the finger itself; the others glide aside.
                            placementSpec = if (lifted) null else spring(
                                stiffness = Spring.StiffnessMediumLow,
                                visibilityThreshold = IntOffset.VisibilityThreshold,
                            ),
                        ),
                ) {
                    if (lifted) {
                        // Shaded layer where the card will land
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                    RoundedCornerShape(20.dp),
                                )
                        )
                    }
                    TimerCard(
                        timer = timer,
                        now = now,
                        onEdit = { sheetTarget = timer.id },
                        onDelete = { confirmDeleteId = timer.id },
                        onFullScreen = { fullScreenId = timer.id },
                        lifted = lifted,
                        moveActions = moves.mapIndexed { i, m ->
                            CustomAccessibilityAction(actionLabels[i]) { moveTo(timer.id, m.target); true }
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                if (lifted) {
                                    translationY = reorder.translation()
                                    scaleX = 1.02f
                                    scaleY = 1.02f
                                    shadowElevation = 12.dp.toPx()
                                    shape = RoundedCornerShape(20.dp)
                                    clip = false
                                }
                            }
                            .longPressDrag(
                                key = timer.id,
                                enabled = canReorder,
                                onStart = {
                                    moveMenuId = null
                                    // pointerInput keeps this lambda from its first composition: read the list fresh
                                    reorder.start(timer.id, TimerRepository.timers.value.map { it.id }).also {
                                        if (it) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { dy ->
                                    if (reorder.dragBy(dy)) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onEnd = { moved, completed ->
                                    scope.launch {
                                        val to = reorder.release(commit = moved && completed)
                                        if (to != null) TimerRepository.move(timer.id, to)
                                        if (!moved && completed) moveMenuId = timer.id
                                    }
                                },
                            ),
                    )
                    if (flashAlpha.value > 0f) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .alpha(flashAlpha.value)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                    RoundedCornerShape(20.dp),
                                )
                        )
                    }
                    MoveMenu(
                        expanded = moveMenuId == timer.id,
                        label = timer.label,
                        moves = moves,
                        onMove = { target -> moveMenuId = null; moveTo(timer.id, target) },
                        onDismiss = { moveMenuId = null },
                    )
                }
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

/** A move the timer at [index] can make, as offered by the Move menu and TalkBack. */
private data class MoveOption(val label: Int, val icon: ImageVector, val target: Int)

private fun moveOptions(index: Int, count: Int): List<MoveOption> = buildList {
    if (index < 0 || count < 2) return@buildList
    if (index > 0) {
        add(MoveOption(R.string.move_to_top, Icons.Filled.VerticalAlignTop, 0))
        add(MoveOption(R.string.move_up, Icons.Filled.KeyboardArrowUp, index - 1))
    }
    if (index < count - 1) {
        add(MoveOption(R.string.move_down, Icons.Filled.KeyboardArrowDown, index + 1))
        add(MoveOption(R.string.move_to_bottom, Icons.Filled.VerticalAlignBottom, count - 1))
    }
}

/** Opened by a long press released in place: the way to reorder without dragging. */
@Composable
private fun MoveMenu(
    expanded: Boolean,
    label: String,
    moves: List<MoveOption>,
    onMove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(56.dp, (-48).dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            stringResource(R.string.move_title, label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        // All four are listed; the ones that don't apply are disabled, as in the mockup.
        listOf(
            R.string.move_to_top to Icons.Filled.VerticalAlignTop,
            R.string.move_up to Icons.Filled.KeyboardArrowUp,
            R.string.move_down to Icons.Filled.KeyboardArrowDown,
            R.string.move_to_bottom to Icons.Filled.VerticalAlignBottom,
        ).forEach { (text, icon) ->
            val option = moves.firstOrNull { it.label == text }
            DropdownMenuItem(
                text = { Text(stringResource(text)) },
                leadingIcon = { Icon(icon, contentDescription = null) },
                enabled = option != null,
                onClick = { option?.let { onMove(it.target) } },
            )
        }
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
