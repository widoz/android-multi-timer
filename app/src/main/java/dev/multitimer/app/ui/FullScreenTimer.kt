package dev.multitimer.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.multitimer.app.R
import dev.multitimer.app.alarm.FullScreenState
import dev.multitimer.app.data.AppSettings
import dev.multitimer.app.data.TimerItem
import dev.multitimer.app.data.TimerRepository
import dev.multitimer.app.data.TimerState
import dev.multitimer.app.data.formatClock
import dev.multitimer.app.ui.theme.timerColor
import kotlinx.coroutines.launch

/**
 * One timer, large. Other timers that finish meanwhile appear in a tray that slides up from
 * the bottom, one swipeable card each. Keeps the screen on (setting) and hides the system bars
 * while visible, and tells the alarm service not to pop up its notification over it.
 *
 * @param ended other finished timers, newest first.
 */
@Composable
fun FullScreenTimer(timer: TimerItem, ended: List<TimerItem>, now: Long, onExit: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val view = LocalView.current
    val keepScreenOn by AppSettings.keepScreenOn.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onExit)

    // The screen stays on only while this view is shown and the setting is on.
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    // Hide the status and navigation bars; a swipe from an edge shows them for a moment.
    DisposableEffect(Unit) {
        val controller = context.findActivity()?.window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    // While visible, ringing notifications do not pop up: the tray already shows them.
    LifecycleResumeEffect(Unit) {
        FullScreenState.visible = true
        onPauseOrDispose { FullScreenState.visible = false }
    }

    val finished = timer.state == TimerState.FINISHED
    val remaining = timer.remainingAt(now)
    val fraction = if (timer.totalMs > 0) (remaining.toFloat() / timer.totalMs).coerceIn(0f, 1f) else 0f
    val accent = if (finished) scheme.error else timerColor(timer.colorIndex)
    val stayOnMessage = stringResource(R.string.screen_stays_on)
    val canTurnOffMessage = stringResource(R.string.screen_can_turn_off)

    // Keep showing the last cards while the tray animates closed.
    val lastEnded = remember { mutableListHolder<TimerItem>() }
    if (ended.isNotEmpty()) lastEnded.value = ended

    Surface(color = scheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .displayCutoutPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Filled.CloseFullscreen, contentDescription = stringResource(R.string.exit_full_screen))
                    }
                    Box(Modifier.size(10.dp).background(timerColor(timer.colorIndex), CircleShape))
                    Text(
                        timer.label,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    IconToggleButton(
                        checked = keepScreenOn,
                        onCheckedChange = { on ->
                            AppSettings.setKeepScreenOn(on)
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(if (on) stayOnMessage else canTurnOffMessage, duration = SnackbarDuration.Short)
                            }
                        },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            contentColor = scheme.onSurfaceVariant,
                            checkedContainerColor = scheme.primaryContainer,
                            checkedContentColor = scheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(
                            if (keepScreenOn) Icons.Filled.LightMode else Icons.Outlined.LightMode,
                            contentDescription = stringResource(R.string.keep_screen_on),
                        )
                    }
                }

                // The dial takes the space left; it moves up and shrinks when the tray opens.
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val dial = minOf(maxWidth, maxHeight, 300.dp)
                    val clockSize = with(LocalDensity.current) { (dial * 0.19f).toSp() }
                    Box(Modifier.size(dial), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxSize(),
                            color = accent,
                            trackColor = scheme.outlineVariant,
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round,
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = dial * 0.14f),
                        ) {
                            ShrinkToFitText(
                                text = formatClock(remaining),
                                style = ClockStyle.copy(fontSize = clockSize),
                                color = if (finished) scheme.error else scheme.onSurface,
                            )
                            Text(
                                timerStatus(timer, now),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val minusLabel = stringResource(R.string.cd_subtract_minute)
                    val plusLabel = stringResource(R.string.cd_add_minute)
                    OutlinedButton(
                        onClick = { TimerRepository.subtractMinute(timer.id) },
                        enabled = !finished && remaining > TimerRepository.MINUTE,
                        modifier = Modifier.semantics { contentDescription = minusLabel },
                    ) { Text(stringResource(R.string.subtract_minute)) }
                    val (icon, desc) = when (timer.state) {
                        TimerState.FINISHED -> Icons.Filled.Stop to R.string.stop
                        TimerState.RUNNING -> Icons.Filled.Pause to R.string.pause
                        TimerState.PAUSED -> Icons.Filled.PlayArrow to R.string.start
                    }
                    FilledIconButton(
                        onClick = { TimerRepository.toggle(timer.id) },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = when {
                            finished -> IconButtonDefaults.filledIconButtonColors(
                                containerColor = scheme.error, contentColor = scheme.onError,
                            )
                            timer.state == TimerState.PAUSED -> IconButtonDefaults.filledIconButtonColors(
                                containerColor = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer,
                            )
                            else -> IconButtonDefaults.filledIconButtonColors()
                        },
                    ) {
                        Icon(icon, contentDescription = stringResource(desc), modifier = Modifier.size(34.dp))
                    }
                    OutlinedButton(
                        onClick = { TimerRepository.addMinute(timer.id) },
                        modifier = Modifier.semantics { contentDescription = plusLabel },
                    ) { Text(stringResource(R.string.add_minute)) }
                }

                AnimatedVisibility(
                    visible = ended.isNotEmpty(),
                    enter = expandVertically(expandFrom = Alignment.Top),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    FinishedTray(lastEnded.value, now)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).padding(top = 72.dp))
        }
    }
}

/** Swipeable cards of finished timers, each with its own Stop and +1 min. */
@Composable
private fun FinishedTray(ended: List<TimerItem>, now: Long) {
    val scheme = MaterialTheme.colorScheme
    val current by rememberUpdatedState(ended)
    val pager = rememberPagerState(pageCount = { current.size })
    val scope = rememberCoroutineScope()
    val count = ended.size

    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = scheme.outline)
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (count == 1) stringResource(R.string.tray_one, ended.first().label)
                else pluralStringResource(R.plurals.tray_many, count, count),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (count > 1) {
                IconButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    enabled = pager.currentPage > 0,
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.tray_previous)) }
                IconButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    enabled = pager.currentPage < count - 1,
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.tray_next)) }
            }
        }
        HorizontalPager(
            state = pager,
            // The next card peeks in from the side, hinting that the cards can be swiped.
            contentPadding = PaddingValues(start = 20.dp, end = if (count > 1) 46.dp else 20.dp),
            pageSpacing = 10.dp,
            key = { current.getOrNull(it)?.id ?: it },
            modifier = Modifier.padding(vertical = 4.dp),
        ) { page ->
            current.getOrNull(page)?.let { FinishedCard(it, now) }
        }
        if (count > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(count) { i ->
                    val selected = i == pager.currentPage
                    val label = stringResource(R.string.tray_page, i + 1, count)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Tab) { scope.launch { pager.animateScrollToPage(i) } }
                            .semantics { contentDescription = label; this.selected = selected },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .width(if (selected) 18.dp else 8.dp)
                                .height(8.dp)
                                .background(if (selected) scheme.onSurface else scheme.outline, CircleShape)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FinishedCard(timer: TimerItem, now: Long) {
    val scheme = MaterialTheme.colorScheme
    val defaultSound by AppSettings.defaultSound.collectAsStateWithLifecycle()
    val sound = rememberSoundTitle(timer.soundUri ?: defaultSound)
    val stopLabel = stringResource(R.string.tray_stop_cd, timer.label)
    val plusLabel = stringResource(R.string.tray_add_minute_cd, timer.label)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = scheme.errorContainer,
        contentColor = scheme.onErrorContainer,
        border = BorderStroke(1.dp, scheme.error),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(timerColor(timer.colorIndex), CircleShape))
                        Text(
                            timer.label,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.tray_ended, formatAgo(now - timer.endAt), sound),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("00:00", style = ClockStyle.copy(fontSize = 24.sp), color = scheme.error)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { TimerRepository.reset(timer.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError),
                    modifier = Modifier.semantics { contentDescription = stopLabel },
                ) { Text(stringResource(R.string.stop)) }
                OutlinedButton(
                    onClick = { TimerRepository.addMinute(timer.id) },
                    border = BorderStroke(1.dp, scheme.error),
                    modifier = Modifier.semantics { contentDescription = plusLabel },
                ) { Text(stringResource(R.string.add_minute), color = scheme.onSurface) }
            }
        }
    }
}

/** 12 s, or 1 min 05 s. */
private fun formatAgo(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s < 60) "$s s" else "${s / 60} min ${(s % 60).toString().padStart(2, '0')} s"
}

private class Holder<T>(var value: List<T> = emptyList())
private fun <T> mutableListHolder() = Holder<T>()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
