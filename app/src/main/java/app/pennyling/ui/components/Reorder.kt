package app.pennyling.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Things put in a new order by a long press and a drag, in a list or a grid alike: the held one follows the finger
 * and takes the place of whatever it is dragged over. Show [items] (not the source list) while it lives.
 */
class Reorder<T>(private val key: (T) -> Any) {
    internal var source: List<T> = emptyList()
    /** A new order and the source it was made from: it shows until the source changes (the database sends it back). */
    private var moved by mutableStateOf<Pair<List<T>, List<T>>?>(null)
    val items: List<T> get() = moved?.takeIf { it.first == source }?.second ?: source
    var dragging by mutableStateOf<Any?>(null); private set
    private var finger by mutableStateOf(Offset.Zero)
    private var before = emptyList<Any>()
    internal val slots = mutableStateMapOf<Any, Rect>()
    internal var done: (List<T>) -> Unit = {}

    internal fun start(k: Any) {
        val at = slots[k] ?: return
        before = items.map(key); dragging = k; finger = at.center
    }

    internal fun drag(by: Offset) {
        val k = dragging ?: return
        finger += by
        // the finger has to reach the middle half of a neighbour (top to bottom), so places don't flicker at the edges
        val over = items.firstOrNull { key(it) != k && slots[key(it)]?.let { b -> finger.x in b.left..b.right && finger.y in (b.top + b.height / 4)..(b.bottom - b.height / 4) } == true } ?: return
        val from = items.indexOfFirst { key(it) == k }; val to = items.indexOf(over)
        if (from < 0 || to < 0) return
        moved = source to items.toMutableList().apply { add(to, removeAt(from)) }
    }

    internal fun end() {
        if (dragging == null) return
        dragging = null
        if (items.map(key) != before) done(items)
    }

    /** Where the held one is drawn: under the finger, wherever its slot is now. */
    internal fun shift(k: Any): Offset = if (dragging == k) slots[k]?.let { finger - it.center } ?: Offset.Zero else Offset.Zero

    fun holds(k: Any) = dragging == k
}

/** A [Reorder] over [source]; [onDone] gets the new order once a drag changes it. */
@Composable
fun <T> rememberReorder(source: List<T>, key: (T) -> Any, onDone: (List<T>) -> Unit): Reorder<T> {
    val r = remember { Reorder(key) }
    val cb by rememberUpdatedState(onDone)
    r.done = { cb(it) }
    r.source = source
    return r
}

/**
 * Makes this the item [k] of [r]: a long press lifts it, a drag moves it; the others glide to their new places.
 * Wrap each item in `key(k) { }` so it stays the same through a move (or the drag would drop at the first swap).
 */
fun Modifier.reorderable(r: Reorder<*>, k: Any): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val glide = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    this
        .onGloballyPositioned { lc ->
            val now = lc.boundsInRoot(); val was = r.slots[k]
            r.slots[k] = now
            // a neighbour pushed aside by the held one slides over instead of jumping
            if (was != null && r.dragging != null && !r.holds(k) && was.topLeft != now.topLeft)
                scope.launch { glide.snapTo(glide.value + was.topLeft - now.topLeft); glide.animateTo(Offset.Zero, spring(stiffness = Spring.StiffnessMediumLow)) }
        }
        .zIndex(if (r.holds(k)) 1f else 0f)
        .graphicsLayer {
            val s = r.shift(k) + glide.value; translationX = s.x; translationY = s.y
            if (r.holds(k)) { scaleX = 1.05f; scaleY = 1.05f; shadowElevation = 8.dp.toPx() }
        }
        .pointerInput(r, k) {
            detectDragGesturesAfterLongPress(
                onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); r.start(k) },
                onDrag = { ch, by -> ch.consume(); r.drag(by) },
                onDragEnd = { r.end() },
                onDragCancel = { r.end() },
            )
        }
}

/** A grid of equal columns in one parent, so an item keeps itself (and its drag) when it moves to another row. */
@Composable
fun Tiles(columns: Int, modifier: Modifier = Modifier, gap: Dp = 6.dp, rowGap: Dp = 4.dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { ms, cs ->
        val g = gap.roundToPx(); val rg = rowGap.roundToPx()
        val w = ((cs.maxWidth - g * (columns - 1)) / columns).coerceAtLeast(0)
        val ps = ms.map { it.measure(Constraints(minWidth = w, maxWidth = w)) }
        val rows = ps.chunked(columns)
        val hs = rows.map { row -> row.maxOf { it.height } }
        val height = hs.sum() + rg * (rows.size - 1).coerceAtLeast(0)
        layout(cs.maxWidth, height) {
            var y = 0
            rows.forEachIndexed { j, row -> row.forEachIndexed { i, p -> p.place(i * (w + g), y) }; y += hs[j] + rg }
        }
    }
}
