package app.pennyling.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.domain.tr
import app.pennyling.ui.Data
import app.pennyling.ui.components.Btn
import app.pennyling.ui.components.Chip
import app.pennyling.ui.components.Ghost
import app.pennyling.ui.components.Segments
import app.pennyling.ui.components.Sheet
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The card preview with its options; shares or saves only from here. Other cards are a tap away. */
@Composable
fun ShareSheet(first: ShareReq, d: Data, onClose: () -> Unit) {
    val ctx = LocalContext.current; val c = ctx.container
    var look by remember { mutableStateOf(Look(false, d.s.cardBg, d.s.hideSums)) }
    var req by remember { mutableStateOf(first) }
    val card by produceState<Pair<android.graphics.Bitmap, String>?>(null, look, req, d) {
        value = if (d.pets.isEmpty()) null else withContext(Dispatchers.Default) { runCatching { Painter(ctx, d, look).render(req) }.getOrNull() } }
    val others = listOf(first) + listOf(ShareReq(CardKind.PERIOD), ShareReq(CardKind.BURROW), ShareReq(CardKind.COLLECTION), ShareReq(CardKind.YEAR),
        ShareReq(CardKind.PET, d.leader?.id ?: 0)).filter { it.kind != first.kind }
    Sheet(onClose) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            others.forEach { r -> Chip(r.kind.label, req == r) { req = r } } }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(if (look.story) .7f else .94f).align(Alignment.CenterHorizontally).aspectRatio(if (look.story) 1080f / 1920 else 1080f / 1350)
            .clip(RoundedCornerShape(18.dp)).background(C.card2), contentAlignment = Alignment.Center) {
            card?.let { Image(it.first.asImageBitmap(), null, Modifier.fillMaxWidth()) } ?: Text(tr("Рисую…", "Drawing…"), style = T.small)
        }
        Spacer(Modifier.height(12.dp))
        Segments(listOf(tr("Лента 4:5", "Feed 4:5"), tr("Сторис 9:16", "Story 9:16")), if (look.story) 1 else 0) { look = look.copy(story = it == 1) }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BACKS.forEachIndexed { i, (a, b, _) ->
                Box(Modifier.size(34.dp).clip(CircleShape).background(if (a == -1) Brush.verticalGradient(listOf(Color(0xFFC98A4A), Color(0xFF3B2A22))) else Brush.verticalGradient(listOf(Color(a), Color(b))))
                    .border(if (look.bg == i) 3.dp else 1.dp, if (look.bg == i) C.ink else C.line, CircleShape).clickable { look = look.copy(bg = i) })
            }
            Spacer(Modifier.weight(1f))
            Chip(tr("Без сумм", "No sums"), look.hide) { look = look.copy(hide = !look.hide) }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ghost(tr("Сохранить", "Save"), Modifier.weight(1f)) { card?.let { if (Share.save(ctx, it.first)) c.say(tr("Сохранено в галерею", "Saved to the gallery")) } }
            Btn(tr("Поделиться", "Share"), Modifier.weight(1f), enabled = card != null) { card?.let { (b, t) -> c.write { settings.update { s -> s.copy(cardBg = look.bg, hideSums = look.hide) } }; Share.send(ctx, b, t) } }
        }
    }
}
