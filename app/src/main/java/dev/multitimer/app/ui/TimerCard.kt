package dev.multitimer.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExposurePlus1
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.multitimer.app.R
import dev.multitimer.app.data.TimerItem
import dev.multitimer.app.data.TimerRepository
import dev.multitimer.app.data.TimerState
import dev.multitimer.app.data.formatClock
import dev.multitimer.app.data.formatDuration
import dev.multitimer.app.ui.theme.timerColor
import java.util.Date
import kotlin.math.ceil

private val ClockStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 26.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
)

@Composable
fun TimerCard(timer: TimerItem, now: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val finished = timer.state == TimerState.FINISHED
    val remaining = timer.remainingAt(now)
    val fraction = if (timer.totalMs > 0) (remaining.toFloat() / timer.totalMs).coerceIn(0f, 1f) else 0f
    val accent = if (finished) scheme.error else timerColor(timer.colorIndex)

    val status = when {
        finished -> stringResource(R.string.status_done)
        timer.state == TimerState.RUNNING ->
            stringResource(R.string.status_ends, DateFormat.getTimeFormat(context).format(Date(timer.endAt)))
        timer.isReady -> stringResource(R.string.status_ready, formatDuration(timer.totalMs))
        else -> stringResource(R.string.status_paused, formatDuration(timer.totalMs))
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (finished) scheme.errorContainer else scheme.surfaceContainerLow,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, if (finished) scheme.error else scheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxSize(),
                    color = accent,
                    trackColor = scheme.outlineVariant,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    "${ceil(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(timerColor(timer.colorIndex), CircleShape))
                    Text(
                        timer.label,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    formatClock(remaining),
                    style = ClockStyle,
                    color = if (finished) scheme.error else scheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val (icon, desc) = when (timer.state) {
                    TimerState.FINISHED -> Icons.Filled.Stop to R.string.stop
                    TimerState.RUNNING -> Icons.Filled.Pause to R.string.pause
                    TimerState.PAUSED -> Icons.Filled.PlayArrow to R.string.start
                }
                FilledIconButton(
                    onClick = { TimerRepository.toggle(timer.id) },
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
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
                    Icon(icon, contentDescription = stringResource(desc))
                }
                Row {
                    SmallAction(Icons.Filled.ExposurePlus1, R.string.add_minute) { TimerRepository.addMinute(timer.id) }
                    SmallAction(Icons.Filled.Refresh, R.string.reset) { TimerRepository.reset(timer.id) }
                    SmallAction(Icons.Filled.Delete, R.string.delete) { TimerRepository.delete(timer.id) }
                }
            }
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, description: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            icon,
            contentDescription = stringResource(description),
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
