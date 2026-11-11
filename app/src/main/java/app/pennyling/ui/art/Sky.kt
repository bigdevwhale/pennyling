package app.pennyling.ui.art

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import app.pennyling.data.Outside
import app.pennyling.data.Settings
import app.pennyling.data.Sun
import app.pennyling.data.Weather
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * What the burrow window shows: [day] 0 night .. 1 full day, [glow] 0..1 how much dawn or dusk colours it,
 * and the weather (CLEAR when unknown).
 */
data class Sky(val day: Float, val glow: Float, val weather: Weather) {
    val night get() = day < .25f

    companion object {
        /** The sky now in the user's town: the real weather and the sun by its rise and set there. */
        fun of(s: Settings, now: Long = System.currentTimeMillis()): Sky {
            val o = Outside.of(s.outside)?.takeIf { now - it.at < 6 * 3600_000L }
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val (rise, set) = when {
                o != null && o.sunrise in today..(today + 86400_000) -> o.sunrise to o.sunset
                !s.lat.isNaN() -> Sun.riseSet(s.lat, s.lon, now)
                else -> (today + 7 * 3600_000L) to (today + 19 * 3600_000L)
            }
            val h = 3600_000f
            // light rises over the hour around sunrise and falls over the hour around sunset
            val day = min(((now - rise) / h + .5f).coerceIn(0f, 1f), ((set - now) / h + .5f).coerceIn(0f, 1f))
            val glow = max(1f - abs(now - rise) / (1.2f * h), 1f - abs(now - set) / (1.2f * h)).coerceIn(0f, 1f)
            return Sky(day, glow, o?.weather ?: Weather.CLEAR)
        }
    }
}

/** The round window of the burrow picture, as parts of its width and height. */
private const val WX0 = .0638f; private const val WX1 = .1862f; private const val WY0 = .2012f; private const val WY1 = .4082f

private fun mix(a: Int, b: Int, t: Float): Int {
    fun ch(c: Int, s: Int) = (c shr s) and 0xFF
    fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t.coerceIn(0f, 1f)).toInt()
    return (0xFF shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
}

/**
 * Paints the view out of the round window of a burrow picture drawn in [r]: the real sky, and a hill whose
 * season follows the pay period – [winter] 0 is harvest gold right after payday, 1 deep snow before the next one.
 * [t] 0..1 loops for rain, snow, leaves and twinkling.
 */
fun drawSky(c: Canvas, r: RectF, sky: Sky, t: Float, winter: Float = 0f) {
    val w = r.width(); val h = r.height()
    val pane = RectF(r.left + w * WX0, r.top + h * WY0, r.left + w * WX1, r.top + h * WY1)
    val x0 = pane.left; val y0 = pane.top; val pw = pane.width(); val ph = pane.height()
    val grey = sky.weather == Weather.OVERCAST || sky.weather == Weather.RAIN || sky.weather == Weather.STORM || sky.weather == Weather.FOG || sky.weather == Weather.SNOW
    var top = mix(0xFF0B1430.toInt(), 0xFF4F9BE0.toInt(), sky.day); var bottom = mix(0xFF1D2B57.toInt(), 0xFFA9D8F5.toInt(), sky.day)
    if (sky.glow > 0) { top = mix(top, 0xFF5A4C9C.toInt(), sky.glow * .6f); bottom = mix(bottom, 0xFFF59A62.toInt(), sky.glow * .8f) }
    if (grey) { top = mix(top, mix(0xFF262C38.toInt(), 0xFF8C96A2.toInt(), sky.day), .75f); bottom = mix(bottom, mix(0xFF2E3442.toInt(), 0xFFB3BBC4.toInt(), sky.day), .75f) }
    val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    c.save()
    c.clipPath(android.graphics.Path().apply { addOval(pane, android.graphics.Path.Direction.CW) })
    c.drawRect(pane, Paint().apply { shader = LinearGradient(0f, y0, 0f, y0 + ph, top, bottom, Shader.TileMode.CLAMP) })
    if (sky.night && !grey) {
        dot.color = 0xFFFFF6D8.toInt()
        for (k in 0 until 9) { val tw = .5f + .5f * sin((t * 2 * PI + k * 1.7).toFloat()); dot.alpha = (120 + 120 * tw).toInt()
            c.drawCircle(x0 + pw * ((k * 37) % 100) / 100f, y0 + ph * ((k * 53) % 55) / 100f + ph * .06f, w * .0018f, dot) }
        dot.color = 0xFFFFF1C2.toInt(); dot.alpha = 255; c.drawCircle(x0 + pw * .68f, y0 + ph * .3f, ph * .1f, dot)
        dot.color = top; c.drawCircle(x0 + pw * .68f + ph * .04f, y0 + ph * .27f, ph * .085f, dot)
    }
    if (!sky.night && sky.weather == Weather.CLEAR) { dot.color = 0xFFFFE89A.toInt(); dot.alpha = 255; c.drawCircle(x0 + pw * .32f, y0 + ph * .3f, ph * .1f, dot) }
    if (sky.weather == Weather.CLOUDY || sky.weather == Weather.CLEAR || grey && sky.weather != Weather.FOG) {
        dot.color = if (grey) mix(0xFF39404E.toInt(), 0xFFD5DAE0.toInt(), sky.day) else if (sky.night) 0xFF4A5576.toInt() else 0xFFFFFFFF.toInt(); dot.alpha = 225
        for (k in 0 until (if (grey) 3 else 1)) { val cx = x0 + pw * ((.1f + t * .5f + k * .4f) % 1.3f - .15f); val cy = y0 + ph * (.22f + k * .14f)
            c.drawCircle(cx, cy, ph * .08f, dot); c.drawCircle(cx + ph * .1f, cy - ph * .04f, ph * .1f, dot); c.drawCircle(cx + ph * .2f, cy, ph * .07f, dot) }
    }
    // the hill: autumn gold, browning, then snow piling up as the period runs out
    val gold = 0xFFD9932F.toInt(); val brown = 0xFF8A6A3E.toInt(); val snow = 0xFFF2F6FA.toInt()
    val land = if (winter < .45f) mix(gold, brown, winter / .45f) else mix(brown, snow, ((winter - .45f) / .3f).coerceIn(0f, 1f))
    val dim = 1f - (1f - sky.day) * .55f
    val shade = mix(0xFF101830.toInt(), land, dim)
    val hill = android.graphics.Path().apply {
        moveTo(x0, y0 + ph * .72f); cubicTo(x0 + pw * .3f, y0 + ph * .6f, x0 + pw * .6f, y0 + ph * .66f, x0 + pw, y0 + ph * .62f)
        lineTo(x0 + pw, y0 + ph); lineTo(x0, y0 + ph); close() }
    c.drawPath(hill, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = shade })
    // a little fir on the hill, white-capped in winter
    val fx = x0 + pw * .7f; val fy = y0 + ph * .66f
    val fir = android.graphics.Path().apply { moveTo(fx, fy - ph * .2f); lineTo(fx + pw * .07f, fy); lineTo(fx - pw * .07f, fy); close() }
    c.drawPath(fir, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mix(0xFF0E1A14.toInt(), 0xFF2F5E3A.toInt(), dim) })
    if (winter > .6f) c.drawPath(android.graphics.Path().apply { moveTo(fx, fy - ph * .2f); lineTo(fx + pw * .035f, fy - ph * .1f); lineTo(fx - pw * .035f, fy - ph * .1f); close() },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mix(0xFF8090A0.toInt(), snow, dim); alpha = ((winter - .6f) / .4f * 255).toInt().coerceIn(0, 255) })
    // leaves early in the period, snowflakes late (or real snow)
    if (winter < .4f && sky.weather != Weather.SNOW) {
        dot.color = 0xFFE07A2E.toInt(); dot.alpha = 230
        for (k in 0 until 4) { val fx2 = ((k * .27f + t * .2f) % 1f) + .05f * sin((t * 6 + k).toFloat()); val fy2 = ((t * .8f + k * .25f) % 1f)
            c.drawCircle(x0 + pw * fx2, y0 + ph * .9f * fy2, w * .0026f, dot) }
    }
    if (winter > .55f || sky.weather == Weather.SNOW) {
        dot.color = 0xFFFFFFFF.toInt(); dot.alpha = 230
        val n = if (sky.weather == Weather.SNOW) 14 else (4 + winter * 8).toInt()
        for (k in 0 until n) { val fx2 = ((k * 0.173f) % 1f) + .03f * sin((t * 6 + k).toFloat()); val fy2 = ((t + k * .1f) % 1f)
            c.drawCircle(x0 + pw * fx2, y0 - ph * .05f + ph * 1.1f * fy2, w * .002f, dot) }
    }
    if (sky.weather == Weather.RAIN || sky.weather == Weather.STORM) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99C8D8EE.toInt(); strokeWidth = w * .0012f }
        for (k in 0 until 10) { val fx2 = ((k * 0.137f) % 1f); val fy2 = ((t * 3 + k * .23f) % 1f)
            val x = x0 + pw * fx2; val y = y0 - ph * .1f + ph * 1.2f * fy2; c.drawLine(x, y, x - ph * .04f, y + ph * .1f, line) }
        if (sky.weather == Weather.STORM && t % .5f < .025f) { dot.color = 0xFFFFFFFF.toInt(); dot.alpha = 160; c.drawRect(pane, dot) }
    }
    if (sky.weather == Weather.FOG) { dot.color = 0xFFE6E9EE.toInt(); dot.alpha = 150; c.drawRect(pane, dot) }
    c.restore()
}

/** The burrow darkens at night (over the room and its things, under the pets); the lantern keeps a warm pool. */
fun drawNight(c: Canvas, r: RectF, sky: Sky) {
    val w = r.width(); val h = r.height()
    if (sky.day < .9f) {
        val dark = ((1 - sky.day) * .3f * 255).toInt()
        c.drawRect(r, Paint().apply { color = (dark shl 24) or 0x101830 })
        c.drawCircle(r.left + w * .24f, r.top + h * .24f, w * .4f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(r.left + w * .24f, r.top + h * .24f, w * .4f, (((1 - sky.day) * .25f * 255).toInt() shl 24) or 0xFFC977, 0x00FFC977, Shader.TileMode.CLAMP) })
    }
}
