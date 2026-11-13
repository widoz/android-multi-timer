package dev.multitimer.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

/**
 * Drag-and-drop state of the timer list. While a timer is lifted, the list shows a local order
 * ([order]) in which the timer already sits at its landing slot, so LazyColumn animates the other
 * cards aside. The lifted card is drawn at its slot plus [translation], which keeps it under the
 * finger even while the list scrolls. The repository is updated once, on release.
 */
@Stable
class ReorderState(private val listState: LazyListState) {
    /** The lifted timer, or null. Stays set while the card settles into its slot after release. */
    var draggingId by mutableStateOf<Int?>(null)
        private set

    /** True from lift to release; false while the card settles. */
    var active by mutableStateOf(false)
        private set

    private var order by mutableStateOf<List<Int>?>(null)
    private var startTop = 0f
    private var dragDelta by mutableFloatStateOf(0f)
    private var settleOffset by mutableStateOf<Float?>(null)
    /** Items before the first timer (quick-start row, permission card). */
    private var headerCount = 0

    /** [items] in the local drag order while dragging, else unchanged. */
    fun <T> arrange(items: List<T>, id: (T) -> Int): List<T> {
        val o = order ?: return items
        val byId = items.associateBy(id)
        val extra = items.filter { id(it) !in o } // added meanwhile: new timers go on top
        return extra + o.mapNotNull { byId[it] }
    }

    /** Called when the repository's order changes: a committed drop has landed, so the local order goes. */
    fun syncWith(ids: List<Int>) {
        if (draggingId == null) order = null
        if (draggingId != null && draggingId !in ids) { // deleted while lifted
            draggingId = null; active = false; settleOffset = null; order = null
        }
    }

    /** Index of the lifted timer in the local order. */
    fun targetIndex(): Int = order?.indexOf(draggingId) ?: -1

    fun start(id: Int, ids: List<Int>): Boolean {
        val info = itemInfo(id) ?: return false
        headerCount = info.index - ids.indexOf(id)
        startOrder = ids // a cancelled drag puts the card back here
        order = ids
        startTop = info.offset.toFloat()
        dragDelta = 0f
        settleOffset = null
        draggingId = id
        active = true
        return true
    }

    /** Moves the lifted card by [dy]; returns true when it passed another card. */
    fun dragBy(dy: Float): Boolean {
        dragDelta += dy
        return swapIfNeeded()
    }

    /** Vertical offset to draw the lifted card at, relative to its slot. */
    fun translation(): Float {
        settleOffset?.let { return it }
        val id = draggingId ?: return 0f
        val info = itemInfo(id) ?: return 0f
        return startTop + dragDelta - info.offset
    }

    /** Animates the lifted card into its slot, then returns its final index (or null if cancelled). */
    suspend fun release(commit: Boolean): Int? {
        val id = draggingId ?: return null
        active = false
        if (!commit) order = order?.let { o -> o.filter { it != id }.toMutableList().also { it.add(startIndexOf(id), id) } }
        val anim = Animatable(translation())
        settleOffset = anim.value
        anim.animateTo(0f, tween(180)) { settleOffset = value }
        val to = targetIndex()
        val changed = commit && to != startIndexOf(id)
        // Nothing to commit: drop the local order now. Otherwise keep it until the repository
        // emits the new order (syncWith), so the list never flashes the old order for a frame.
        if (!changed) order = null
        draggingId = null
        settleOffset = null
        return to.takeIf { changed }
    }

    private var startOrder: List<Int> = emptyList()
    private fun startIndexOf(id: Int) = startOrder.indexOf(id).coerceAtLeast(0)

    /**
     * How far (px) the lifted card's centre is inside the [edge] zone at the top (negative) or
     * bottom (positive) of the visible list; 0 elsewhere. Only counts once the card has been
     * dragged toward that edge, so lifting a card that already sits near an edge (for example to
     * open the Move menu) never scrolls the list by itself.
     */
    fun edgeOverlap(edge: Float): Float {
        if (!active) return 0f
        val id = draggingId ?: return 0f
        val info = itemInfo(id) ?: return 0f
        val layout = listState.layoutInfo
        val center = startTop + dragDelta + info.size / 2f
        val visibleBottom = (layout.viewportEndOffset - layout.afterContentPadding).toFloat()
        return when {
            dragDelta < 0f && center < edge -> center - edge
            dragDelta > 0f && center > visibleBottom - edge -> center - (visibleBottom - edge)
            else -> 0f
        }
    }

    private fun itemInfo(id: Int): LazyListItemInfo? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }

    private fun swapIfNeeded(): Boolean {
        val id = draggingId ?: return false
        val o = order ?: return false
        val items = listState.layoutInfo.visibleItemsInfo
        val cur = items.firstOrNull { it.key == id } ?: return false
        val idx = o.indexOf(id)
        if (cur.index - headerCount != idx) return false // last swap not laid out yet
        val center = startTop + dragDelta + cur.size / 2f
        val below = o.getOrNull(idx + 1)?.let { n -> items.firstOrNull { it.key == n } }
        val above = o.getOrNull(idx - 1)?.let { n -> items.firstOrNull { it.key == n } }
        val to = when {
            below != null && center > below.offset + below.size / 2f -> idx + 1
            above != null && center < above.offset + above.size / 2f -> idx - 1
            else -> return false
        }
        // LazyColumn keeps the first visible item in place by key; keep it by index instead,
        // so moving the first visible card doesn't scroll the list.
        val first = listState.firstVisibleItemIndex
        if (first == idx + headerCount || first == to + headerCount) {
            listState.requestScrollToItem(first, listState.firstVisibleItemScrollOffset)
        }
        order = o.toMutableList().apply { add(to, removeAt(idx)) }
        return true
    }
}

/**
 * Long press (outside the card's buttons) lifts the card, then dragging moves it.
 * [onEnd] gets true when the finger moved before release, false for a press released in place.
 */
fun Modifier.longPressDrag(
    key: Any,
    enabled: Boolean,
    onStart: () -> Boolean,
    onDrag: (Float) -> Unit,
    onEnd: (moved: Boolean, completed: Boolean) -> Unit,
): Modifier = if (!enabled) this else pointerInput(key) {
    awaitEachGesture {
        // Buttons consume their own presses, so a long press on a button does nothing here.
        val down = awaitFirstDown(requireUnconsumed = true)
        val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        if (!onStart()) return@awaitEachGesture
        var total = Offset.Zero
        var moved = false
        var completed = false
        try {
            completed = drag(press.id) { change ->
                val d = change.positionChange()
                total += d
                if (!moved && total.getDistance() > viewConfiguration.touchSlop) moved = true
                if (moved) onDrag(d.y)
                change.consume()
            }
        } finally {
            // Also runs if the card leaves the composition mid-drag, so the list never stays lifted.
            onEnd(moved, completed)
        }
    }
}
