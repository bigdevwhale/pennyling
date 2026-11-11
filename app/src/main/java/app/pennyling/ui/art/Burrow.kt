package app.pennyling.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.pennyling.BuildConfig
import app.pennyling.data.Pet
import app.pennyling.domain.Shop
import kotlin.math.roundToInt

/**
 * The burrow: the generated room, the window onto the season of the pay period, the furniture bought,
 * the pantry sacks ([stock] 0..1 of the budget still there) and the team on the rug.
 */
@Composable
fun BurrowScene(team: List<Pet>, items: Set<String>, pose: Pose, modifier: Modifier = Modifier, preview: String? = null, sky: Sky? = null,
                winter: Float = 0f, stock: Float = 1f, snowDay: Boolean = false, onPet: ((Pet) -> Unit)? = null) {
    val ctx = LocalContext.current
    val tt by rememberInfiniteTransition(label = "sky").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    val t = if (BuildConfig.SHOTS) .3f else tt
    val bg = Art.get(ctx, "burrow_bg")
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.5f)) {
        val wDp = maxWidth; val hDp = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            val r = android.graphics.RectF(0f, 0f, size.width, size.height)
            // the sky goes first, under the picture: the window glass is cut out of it
            drawIntoCanvas { drawSky(it.nativeCanvas, r, sky ?: Sky(1f, 0f, app.pennyling.data.Weather.CLEAR), t, winter) }
            if (bg != null) drawImage(bg, IntOffset.Zero, IntSize(bg.width, bg.height), IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Medium)
            else {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFC98A4A), Color(0xFFA86A34))), size = size.copy(height = size.height * .68f))
                drawRect(Color(0xFF7A4E2A), Offset(0f, size.height * .68f), size.copy(height = size.height * .32f))
            }
            val shown = Shop.all.filter { it.id in items || it.id == preview }.sortedBy { if (it.wall) 0 else 1 }
            for (it in shown) {
                val img = Art.item(ctx, it.id) ?: continue
                val w = size.width * it.w; val h = w * img.height / img.width
                val x = size.width * it.x - w / 2; val y = if (it.wall) size.height * it.y - h / 2 else size.height * it.y - h
                drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(x.roundToInt(), y.roundToInt()), IntSize(w.roundToInt(), h.roundToInt()),
                    alpha = if (it.id == preview && it.id !in items) .85f else 1f, filterQuality = FilterQuality.Medium)
            }
            // the pantry: a pile of up to five sacks in the front corner, smaller as the period's money goes
            val sack = Art.prop(ctx, "sack")
            if (sack != null) {
                val n = (stock.coerceIn(0f, 1f) * 5 + .99f).toInt().coerceIn(0, 5)
                val spots = listOf(.035f to 0f, .085f to 0f, .135f to 0f, .06f to 1f, .11f to 1f)
                for (i in 0 until n) {
                    val w = size.width * .056f; val h = w * sack.height / sack.width
                    val (cx, row) = spots[i]
                    val x = size.width * cx - w / 2; val y = size.height * .985f - h * (1 + row * .62f)
                    drawImage(sack, IntOffset.Zero, IntSize(sack.width, sack.height), IntOffset(x.roundToInt(), y.roundToInt()), IntSize(w.roundToInt(), h.roundToInt()), filterQuality = FilterQuality.Medium)
                }
            }
            if (sky != null) drawIntoCanvas { drawNight(it.nativeCanvas, r, sky) }
            if (snowDay) { // a snow day: frost in the room's corners
                drawCircle(Brush.radialGradient(listOf(Color(0x66DDEEFF), Color.Transparent), Offset(0f, 0f), size.width * .3f), size.width * .3f, Offset(0f, 0f))
                drawCircle(Brush.radialGradient(listOf(Color(0x66DDEEFF), Color.Transparent), Offset(size.width, 0f), size.width * .3f), size.width * .3f, Offset(size.width, 0f))
            }
        }
        val tap = remember { MutableInteractionSource() }
        team.filter { it.slot in 1..2 }.forEach { p ->
            key(p.id) {
                // companions stand at the rug's edges, clear of the leader in the middle
                val s = hDp * .3f
                Box(Modifier.offset(x = wDp * (if (p.slot == 1) .14f else .66f), y = hDp * .95f - s)
                    .then(if (onPet != null) Modifier.clickable(tap, null) { onPet(p) } else Modifier)) { PetView(p, s, if (pose == Pose.WORK) Pose.CHEER else pose) }
            }
        }
        team.firstOrNull { it.slot == 0 }?.let { lead -> key(lead.id) { val s = hDp * .44f
            Box(Modifier.offset(x = (wDp - s) / 2, y = hDp * .99f - s).then(if (onPet != null) Modifier.clickable(tap, null) { onPet(lead) } else Modifier)) { PetView(lead, s, pose) } } }
    }
}
