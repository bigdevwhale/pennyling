package app.pennyling.ui.art

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.pennyling.BuildConfig
import app.pennyling.data.Pet
import app.pennyling.domain.Pets
import app.pennyling.domain.Stage
import app.pennyling.domain.stage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Pictures from res/drawable-nodpi by name, decoded once. */
object Art {
    private val cache = HashMap<String, ImageBitmap?>()
    @SuppressLint("DiscouragedApi")
    @Synchronized fun get(ctx: Context, name: String): ImageBitmap? = cache.getOrPut(name) {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) null else BitmapFactory.decodeResource(ctx.resources, id, BitmapFactory.Options().apply { inScaled = false })?.asImageBitmap()
    }
    fun pet(ctx: Context, species: String, frame: Int) = get(ctx, "pet_${species}_$frame")
    fun egg(ctx: Context, species: String) = get(ctx, "egg_$species")
    fun item(ctx: Context, id: String) = get(ctx, "item_$id")
    fun prop(ctx: Context, id: String) = get(ctx, "prop_$id")
}

/** What a pet is doing. */
enum class Pose { IDLE, WORK, CHEER, SLEEP }

/** The frame of the sheet for a stage and pose: 0 baby, 1 teen, 2 adult, 3 move, 4 cheer, 5 asleep. */
fun frameOf(stage: Stage, pose: Pose): Int = when {
    pose == Pose.SLEEP && stage >= Stage.ADULT -> 5
    pose == Pose.WORK && stage >= Stage.ADULT -> 3
    pose == Pose.CHEER && stage >= Stage.ADULT -> 4
    stage == Stage.BABY -> 0
    stage == Stage.TEEN -> 1
    else -> 2
}

private val still = BuildConfig.SHOTS

/** Hue turned for the rare shiny pets. */
private fun shinyFilter(): ColorFilter {
    val a = PI.toFloat() * .55f; val c = cos(a); val s = sin(a)
    val m = floatArrayOf(
        .213f + c * .787f - s * .213f, .715f - c * .715f - s * .715f, .072f - c * .072f + s * .928f, 0f, 0f,
        .213f - c * .213f + s * .143f, .715f + c * .285f + s * .140f, .072f - c * .072f - s * .283f, 0f, 0f,
        .213f - c * .213f - s * .787f, .715f - c * .715f + s * .715f, .072f + c * .928f + s * .072f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f)
    return ColorFilter.colorMatrix(ColorMatrix(m))
}

/** A pet drawn in a square of [size]. It breathes when idle, does its thing when working, hops when cheering, dozes asleep. */
@Composable
fun PetView(pet: Pet, size: Dp, pose: Pose = Pose.IDLE, modifier: Modifier = Modifier) {
    val st = pet.stage()
    if (st == Stage.EGG) { EggView(pet.species, size, pet.growth.toFloat() / Pets.HATCH, modifier); return }
    Creature(pet.species, st, pose, size, 70f, pet.shiny, modifier, phase = (pet.id % 7) / 7f)
}

@Composable
fun Creature(species: String, st: Stage, pose: Pose, size: Dp, shape: Float = 70f, shiny: Boolean = false, modifier: Modifier = Modifier, phase: Float = 0f) {
    val ctx = LocalContext.current
    val img = Art.pet(ctx, species, frameOf(st, pose))
    val t by rememberInfiniteTransition(label = "pet").animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val time = if (still) .25f else (t + phase) % 1f
    val round = ((60f - shape) / 60f).coerceIn(0f, 1f) * .14f   // up to 14% wider and shorter
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        // shadow
        drawOval(Color(0x33000000), Offset(w * .22f, h * .9f), Size(w * .56f, h * .07f))
        if (st == Stage.MASTER) drawCircle(Brush.radialGradient(listOf(Color(0x66FFD36B), Color.Transparent), Offset(w / 2, h * .55f), w * .55f), w * .55f, Offset(w / 2, h * .55f))
        val wave = sin(time * 2 * PI).toFloat()
        val (sx, sy, dy) = when (pose) {
            Pose.IDLE -> Triple(1f + wave * .012f, 1f - wave * .016f, 0f)
            Pose.SLEEP -> Triple(1f + wave * .015f, 1f + wave * .02f, 0f)
            Pose.WORK -> { val w2 = sin(time * 2 * PI).toFloat(); Triple(1f - w2 * .008f, 1f + w2 * .014f, -w2 * h * .006f) }
            Pose.CHEER -> { val hop = sin(time * 2 * PI).toFloat(); Triple(1f, 1f, -hop * hop * h * .07f) }
        }
        if (img == null) { fallback(species, st, w, h, dy); return@Canvas }
        val scale = when (st) { Stage.BABY -> .62f; Stage.TEEN -> .8f; else -> .96f }
        val box = minOf(w / img.width, h * .9f / img.height) * scale
        // a fixed picture size, moved and stretched by the canvas transform: smooth, no pixel snapping between frames
        val bw = (img.width * box).roundToInt(); val bh = (img.height * box).roundToInt()
        val foot = Offset(w / 2, h * .93f)
        withTransform({ translate(0f, dy); scale(sx + round, sy - round * .6f, foot) }) {
            drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - bw) / 2).roundToInt(), (foot.y - bh).roundToInt()),
                IntSize(bw, bh), filterQuality = FilterQuality.Medium, colorFilter = if (shiny) shinyFilter() else null)
        }
        if (pose == Pose.SLEEP && !still) {
            val z = (time * 3) % 1f
            drawCircle(Color(0x88FFFFFF), w * .02f * (1 + z), Offset(w * (.72f + z * .08f), h * (.35f - z * .15f)))
        }
    }
}

private fun DrawScope.fallback(species: String, st: Stage, w: Float, h: Float, dy: Float) {
    val col = Color(Pets.of(species).color)
    val r = when (st) { Stage.BABY -> .22f; Stage.TEEN -> .28f; else -> .34f } * w
    drawOval(col, Offset(w / 2 - r, h * .9f - r * 1.7f + dy), Size(r * 2, r * 1.7f))
    drawCircle(Color.White, r * .16f, Offset(w / 2 + r * .25f, h * .9f - r * 1.15f + dy)); drawCircle(Color(0xFF2A2420), r * .09f, Offset(w / 2 + r * .28f, h * .9f - r * 1.13f + dy))
}

/** An egg that wobbles more the closer it is to hatching ([ready] 0..1). */
@Composable
fun EggView(species: String, size: Dp, ready: Float, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val img = Art.egg(ctx, species)
    val t by rememberInfiniteTransition(label = "egg").animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "t")
    val time = if (still) .3f else t
    Box(modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width; val h = this.size.height
            drawOval(Color(0x33000000), Offset(w * .3f, h * .88f), Size(w * .4f, h * .06f))
            val wob = (if (time < .3f) sin(time / .3f * 4 * PI).toFloat() else 0f) * (4f + 10f * ready.coerceIn(0f, 1f))
            withTransform({ rotate(wob, Offset(w / 2, h * .9f)) }) {
                if (img != null) {
                    val box = minOf(w * .6f / img.width, h * .78f / img.height); val dw = img.width * box; val dh = img.height * box
                    drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - dw) / 2).roundToInt(), (h * .91f - dh).roundToInt()), IntSize(dw.roundToInt(), dh.roundToInt()), filterQuality = FilterQuality.Medium)
                } else {
                    val col = Color(Pets.of(species).color)
                    drawOval(Brush.verticalGradient(listOf(col.copy(alpha = .75f), col)), Offset(w * .28f, h * .2f), Size(w * .44f, h * .7f))
                    drawOval(Color(0x55FFFFFF), Offset(w * .36f, h * .3f), Size(w * .1f, h * .16f))
                }
            }
        }
    }
}

/** A small still picture of a species (its grown-up frame), or its egg when [egg]. For category marks and lists. */
@Composable
fun Mini(species: String, size: Dp, modifier: Modifier = Modifier, egg: Boolean = false, dim: Boolean = false) {
    val ctx = LocalContext.current
    val img = if (egg) Art.egg(ctx, species) else Art.pet(ctx, species, 2)
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        if (img == null) { drawCircle(Color(Pets.of(species).color).copy(alpha = if (dim) .3f else 1f), w * .36f, Offset(w / 2, h / 2)); return@Canvas }
        val box = minOf(w / img.width, h / img.height) * .92f
        val dw = img.width * box; val dh = img.height * box
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - dw) / 2).roundToInt(), ((h - dh) / 2).roundToInt()), IntSize(dw.roundToInt(), dh.roundToInt()),
            filterQuality = FilterQuality.Medium, colorFilter = if (dim) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null, alpha = if (dim) .35f else 1f)
    }
}

/** A prop from the props sheet (acorn, jar, boulder…), fitted into [size]. */
@Composable
fun Prop(id: String, size: Dp, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val ctx = LocalContext.current
    val img = Art.prop(ctx, id)
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        if (img == null) { drawCircle(Color(0xFFB07A3A).copy(alpha = alpha), w * .35f, Offset(w / 2, h / 2)); return@Canvas }
        val box = minOf(w / img.width, h / img.height)
        val dw = img.width * box; val dh = img.height * box
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(((w - dw) / 2).roundToInt(), ((h - dh) / 2).roundToInt()), IntSize(dw.roundToInt(), dh.roundToInt()), alpha = alpha, filterQuality = FilterQuality.Medium)
    }
}
