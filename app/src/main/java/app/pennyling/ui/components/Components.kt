package app.pennyling.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pennyling.domain.tr
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import kotlin.math.max

fun Modifier.card(r: Dp = 20.dp, color: Color? = null) = this.shadow(6.dp, RoundedCornerShape(r), ambientColor = C.shadow, spotColor = C.shadow)
    .clip(RoundedCornerShape(r)).background(color ?: C.card)

fun Modifier.tap(onClick: () -> Unit) = this.clickable(MutableInteractionSource(), null, onClick = onClick)

@Composable
fun Card(modifier: Modifier = Modifier, pad: Dp = 16.dp, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().card().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(pad), content = content)
}

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, color: Color = C.acc, ink: Color = C.accInk, enabled: Boolean = true, small: Boolean = false, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(if (small) 12.dp else 16.dp)).background(if (enabled) color else C.track)
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = if (small) 14.dp else 20.dp, vertical = if (small) 8.dp else 14.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.btn.copy(color = if (enabled) ink else C.mute, fontSize = if (small) T.smallB.fontSize else T.btn.fontSize), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Ghost(text: String, modifier: Modifier = Modifier, color: Color = C.ink, small: Boolean = false, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(if (small) 12.dp else 16.dp)).border(1.5.dp, C.line, RoundedCornerShape(if (small) 12.dp else 16.dp)).clickable(onClick = onClick)
        .padding(horizontal = if (small) 12.dp else 18.dp, vertical = if (small) 7.dp else 13.dp), contentAlignment = Alignment.Center) {
        Text(text, style = (if (small) T.smallB else T.btn).copy(color = color), maxLines = 1)
    }
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, color: Color = C.acc, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(50)).background(if (on) color else C.card2).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp)) {
        Text(text, style = T.smallB.copy(color = if (on) Color.White else C.ink), maxLines = 1)
    }
}

@Composable
fun Segments(items: List<String>, sel: Int, modifier: Modifier = Modifier, onSel: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.card2).padding(3.dp)) {
        items.forEachIndexed { i, s ->
            Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (i == sel) C.card else Color.Transparent).clickable { onSel(i) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(s, style = T.smallB.copy(color = if (i == sel) C.ink else C.mute), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ChipRow(items: List<String>, sel: Int, modifier: Modifier = Modifier, onSel: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, s -> Chip(s, i == sel) { onSel(i) } }
    }
}

@Composable
fun Section(text: String, modifier: Modifier = Modifier, action: String? = null, icon: Icons? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, C.acc, 16.dp); Spacer(Modifier.width(6.dp)) }
        Text(text.uppercase(), style = T.sec, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) Text(action, style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
    }
}

@Composable
fun Meter(v: Float, modifier: Modifier = Modifier, color: Color = C.acc, h: Dp = 8.dp, track: Color = C.track) {
    Box(modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(v.coerceIn(0f, 1f)).height(h).clip(RoundedCornerShape(50)).background(color))
    }
}

/** A number with a caption, for grids of facts. */
@Composable
fun Tile(value: String, label: String, modifier: Modifier = Modifier, delta: String? = null, good: Boolean = true, icon: Icons? = null, tint: Color = C.acc) {
    Column(modifier.card(16.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (icon != null) Box(Modifier.padding(bottom = 6.dp).size(30.dp).clip(CircleShape).background(tint.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(icon, tint, 17.dp) }
        Text(value, style = T.num, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = T.small, maxLines = 2)
        if (delta != null) Text(delta, style = T.tiny.copy(color = if (good) C.ok else C.bad))
    }
}

@Composable
fun TileGrid(items: List<Triple<String, String, String?>>, cols: Int = 2, icons: List<Pair<Icons, Color>> = emptyList()) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(cols).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { k, (v, l, d) -> val ic = icons.getOrNull(r * cols + k); Tile(v, l, Modifier.weight(1f), d, d?.startsWith("−") != true, ic?.first, ic?.second ?: C.acc) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A full-screen page over the tabs with a back arrow. */
@Composable
fun Page(title: String, onBack: () -> Unit, action: (@Composable RowScope.() -> Unit)? = null, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(C.bg).tap {}.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) { Icon(Icons.BACK, C.ink, 22.dp) }
            Text(title, style = T.h2, modifier = Modifier.weight(1f).padding(start = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            action?.invoke(this)
        }
        Column(Modifier.weight(1f).fillMaxWidth().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 18.dp).navigationBarsPadding().imePadding(),
            content = { content(); if (scroll) Spacer(Modifier.height(30.dp)) })
    }
}

/** A dialog-like card in the middle of a dim screen. */
@Composable
fun Popup(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    // a real dialog window: it lies over everything wherever it is called from
    androidx.compose.ui.window.Dialog(onDismiss, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().tap(onDismiss).imePadding(), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(22.dp).fillMaxWidth().card(24.dp).tap {}.verticalScroll(rememberScrollState()).padding(20.dp), content = content)
        }
    }
}

/** A sheet from the bottom. */
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Color(0x88000000)).tap(onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(C.bg).tap {}.navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(20.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(RoundedCornerShape(50)).background(C.line))
            Spacer(Modifier.height(12.dp)); content()
        }
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, number: Boolean = false, style: TextStyle = T.body, single: Boolean = true, center: Boolean = false) {
    var tv by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(value)) }
    if (tv.text != value) tv = tv.copy(text = value, selection = androidx.compose.ui.text.TextRange(value.length))
    var focused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    // a number is usually replaced, not edited: tapping it selects all of it
    androidx.compose.runtime.LaunchedEffect(focused) { if (focused && number) { kotlinx.coroutines.delay(40); tv = tv.copy(selection = androidx.compose.ui.text.TextRange(0, tv.text.length)) } }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(C.card2).padding(horizontal = 12.dp, vertical = 11.dp)) {
        if (value.isEmpty()) Text(hint, style = style.copy(color = C.mute), modifier = if (center) Modifier.fillMaxWidth() else Modifier, textAlign = if (center) TextAlign.Center else null)
        BasicTextField(tv, { tv = it; if (it.text != value) onChange(it.text) }, modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }, textStyle = style.copy(color = C.ink, textAlign = if (center) TextAlign.Center else TextAlign.Start), singleLine = single,
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default, cursorBrush = SolidColor(C.acc))
    }
}

// ---------------- charts ----------------

/** Bars with labels under them; [hi] marks one bar. */
@Composable
fun Bars(values: List<Pair<String, Double>>, modifier: Modifier = Modifier, color: Color = C.acc, hi: Int = -1, h: Dp = 120.dp, fmtv: ((Double) -> String)? = null) {
    val max = max(values.maxOfOrNull { it.second } ?: 0.0, 1e-9)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(h), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            values.forEachIndexed { i, (_, v) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (fmtv != null && v > 0 && values.size <= 12) Text(fmtv(v), style = T.tiny, maxLines = 1)
                    Box(Modifier.fillMaxWidth().height(max(2.0, (h.value - 16) * v / max).dp).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(if (v <= 0) C.track else if (hi < 0 || i == hi) color else color.copy(alpha = .45f)))
                }
            }
        }
        val step = if (values.size > 16) values.size / 6 + 1 else if (values.size > 12) 2 else 1
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            values.forEachIndexed { i, (l, _) -> Text(if (i % step == 0) l else "", style = T.tiny.copy(fontSize = T.tiny.fontSize * .9f), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1) }
        }
    }
}

/** A line through points (x is any increasing number); [trend] draws a second, smooth line. */
@Composable
fun Line(points: List<Pair<Double, Double>>, modifier: Modifier = Modifier, color: Color = C.acc, trend: List<Pair<Double, Double>>? = null, h: Dp = 140.dp, dots: Boolean = true) {
    if (points.isEmpty()) return
    Canvas(modifier.fillMaxWidth().height(h)) {
        val all = points + trend.orEmpty()
        val x0 = all.minOf { it.first }; val x1 = max(all.maxOf { it.first }, x0 + 1e-9)
        var y0 = all.minOf { it.second }; var y1 = all.maxOf { it.second }
        val pad = max((y1 - y0) * .12, 1e-6); y0 -= pad; y1 += pad
        fun o(p: Pair<Double, Double>) = Offset(((p.first - x0) / (x1 - x0) * (size.width - 12) + 6).toFloat(), (size.height - (p.second - y0) / (y1 - y0) * size.height).toFloat())
        for (i in 0..3) { val y = size.height * i / 3; drawLine(C.line, Offset(0f, y), Offset(size.width, y), 1f) }
        fun path(l: List<Pair<Double, Double>>) = Path().apply { l.forEachIndexed { i, p -> val q = o(p); if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) } }
        if (trend != null) drawPath(path(trend), color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path(points), if (trend != null) color.copy(alpha = .35f) else color, style = Stroke((if (trend != null) 1.5f else 3f) * density, cap = StrokeCap.Round))
        if (dots && points.size <= 60) points.forEach { drawCircle(if (trend != null) color.copy(alpha = .5f) else color, 3.dp.toPx(), o(it)) }
    }
}

/** A GitHub-like year of days, weeks as columns. */
@Composable
fun Heat(days: Map<Long, Int>, from: Long, to: Long, modifier: Modifier = Modifier) {
    val start = app.pennyling.domain.weekStart(from); val weeks = ((to - start) / 7 + 1).toInt()
    val max = max(1, days.filterKeys { it in from..to }.values.maxOrNull() ?: 1)
    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(if (weeks > 30) 92.dp else 130.dp)) {
            val cell = minOf(size.width / weeks, size.height / 7); val gap = cell * .18f
            for (w in 0 until weeks) for (d in 0 until 7) {
                val day = start + w * 7 + d; if (day < from || day > to) continue
                val v = days[day] ?: 0
                val col = if (v == 0) C.heat[0] else C.heat[(1 + (v * 3.99 / max).toInt()).coerceAtMost(4)]
                drawRoundRect(col, Offset(w * cell, d * cell), Size(cell - gap, cell - gap), CornerRadius(cell * .22f))
            }
        }
    }
}

/** Two-sided split bar: left share vs right share. */
@Composable
fun Split(a: Double, b: Double, la: String, lb: String, ca: Color = C.acc, cb: Color = C.ok) {
    val t = a + b; val f = if (t <= 0) .5f else (a / t).toFloat()
    Column {
        Row(Modifier.fillMaxWidth()) { Text("$la ${(f * 100).toInt()}%", style = T.smallB, modifier = Modifier.weight(1f)); Text("${100 - (f * 100).toInt()}% $lb", style = T.smallB) }
        Row(Modifier.fillMaxWidth().padding(top = 5.dp).height(10.dp).clip(RoundedCornerShape(50))) {
            if (f > 0) Box(Modifier.weight(f.coerceAtLeast(.001f)).fillMaxSize().background(ca))
            if (f < 1) Box(Modifier.weight((1 - f).coerceAtLeast(.001f)).fillMaxSize().background(cb))
        }
    }
}

fun lerp(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t)

/** A list row: title, subtitle, a value at the end. */
@Composable
fun Row2(title: String, sub: String? = null, value: String? = null, modifier: Modifier = Modifier, lead: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        if (lead != null) { lead(); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = T.small, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) Text(value, style = T.numS, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun Dot(color: Color, size: Dp = 10.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

@Composable
fun Empty(text: String, modifier: Modifier = Modifier) = Text(text, style = T.small, modifier = modifier.fillMaxWidth().padding(vertical = 18.dp), textAlign = TextAlign.Center)

@Composable
fun Centered(content: @Composable BoxScope.() -> Unit) = Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center, content = content)


/** A folded section: title and a one-line preview; the content opens on tap. Keeps busy screens calm. */
@Composable
fun Fold(title: String, preview: String? = null, modifier: Modifier = Modifier, open0: Boolean = false, icon: Icons? = null, tint: Color = C.acc, content: @Composable ColumnScope.() -> Unit) {
    var open by androidx.compose.runtime.saveable.rememberSaveable(title) { androidx.compose.runtime.mutableStateOf(open0) }
    // opening scrolls the page so what opened is in sight, instead of below the fold
    val seen = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    var touched by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(open) { if (open && touched) { kotlinx.coroutines.delay(320); seen.bringIntoView() } }
    Column(modifier.padding(top = 10.dp).fillMaxWidth().bringIntoViewRequester(seen).card(18.dp)) {
        Row(Modifier.fillMaxWidth().clickable { touched = true; open = !open }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Box(Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(icon, tint, 19.dp) }; Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = T.h3)
                if (preview != null && !open) Text(preview, style = T.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(if (open) Icons.UP else Icons.DOWN, C.mute, 20.dp)
        }
        androidx.compose.animation.AnimatedVisibility(open) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), content = content)
        }
    }
}

/** An icon in a soft round badge, to lead a row. */
@Composable
fun Badge(icon: Icons, tint: Color = C.acc, size: androidx.compose.ui.unit.Dp = 36.dp) =
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(icon, tint, size * .52f) }

/** [Fold] with its icon first, so calls read: icon, tint, title, preview. */
@Composable
fun Fold(icon: Icons, tint: Color, title: String, preview: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Fold(title, preview, modifier, false, icon, tint, content)

/** What a workout or a plan is made of: one rounded stripe of coloured parts by exercise family. */
@Composable
fun Stripe(parts: List<Pair<Color, Int>>, modifier: Modifier = Modifier, h: Dp = 6.dp) {
    if (parts.isEmpty()) return
    Row(modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(50))) {
        parts.forEach { (c, n) -> Box(Modifier.weight(n.coerceAtLeast(1).toFloat()).fillMaxSize().background(c)) }
    }
}

/**
 * A first-visit hint for a screen: what it is and how to use it. Shown until «Got it»;
 * the id lands in Settings.seen. Settings can bring them all back.
 */
@Composable
fun Hint(id: String, seen: Set<String>, icon: Icons, title: String, text: String, modifier: Modifier = Modifier) {
    if (id in seen) return
    val c = androidx.compose.ui.platform.LocalContext.current.let { (it.applicationContext as app.pennyling.PennylingApp).container }
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.accSoft).border(1.dp, C.acc.copy(alpha = .35f), RoundedCornerShape(20.dp)).padding(14.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(C.acc), contentAlignment = Alignment.Center) { Icon(icon, C.accInk, 20.dp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = T.bodyB)
            Text(text, style = T.small.copy(color = C.ink), modifier = Modifier.padding(top = 2.dp))
            Text(tr("Понятно", "Got it"), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(50)).clickable {
                c.write { settings.update { st -> st.copy(seen = st.seen + id) } } }.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}
