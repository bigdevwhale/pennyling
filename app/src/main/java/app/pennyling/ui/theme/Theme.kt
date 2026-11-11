package app.pennyling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.pennyling.R

/** Colour tokens: oat paper, honey for action, moss for money in and things done, acorn gold for crumbs. */
class Pal(
    val dark: Boolean, val bg: Color, val card: Color, val card2: Color, val ink: Color, val mute: Color, val line: Color, val track: Color,
    val acc: Color, val accInk: Color, val accSoft: Color, val ok: Color, val okSoft: Color, val gold: Color, val goldSoft: Color, val bad: Color,
    val heat: List<Color>, val nav: Color, val shadow: Color, val snow: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Light = Pal(false, c(0xF6EEDF), c(0xFFFBF3), c(0xF0E6D4), c(0x2E2218), c(0x8A7A68), c(0xE8DCC8), c(0xEDE2CF),
    c(0xC8742F), c(0xFFFFFF), c(0xF8E3CD), c(0x4F8A5B), c(0xDDEDDC), c(0xD49A2A), c(0xFAEDCF), c(0xC4533A),
    listOf(c(0xEDE2CF), c(0xF4D3A8), c(0xE9A867), c(0xD07B36), c(0xA65420)), c(0xFFF9EF), Color(0x22704A2A), c(0x7FA7C9))

val Dark = Pal(true, c(0x1B1613), c(0x262019), c(0x30291F), c(0xF2E8DA), c(0xA3937F), c(0x3A3226), c(0x342C22),
    c(0xE38D45), c(0x1E120A), c(0x4A2F18), c(0x76B583), c(0x22352A), c(0xEBB84E), c(0x3D311A), c(0xEE7558),
    listOf(c(0x342C22), c(0x5E3F22), c(0x8F5A2A), c(0xC97A35), c(0xF4A55A)), c(0x221C16), Color(0x55000000), c(0x9CC3E3))

/** The palette in use; switched by the theme setting before the tree is drawn. */
object C {
    @Volatile var p: Pal = Light
    val dark get() = p.dark
    val bg get() = p.bg; val card get() = p.card; val card2 get() = p.card2; val ink get() = p.ink; val mute get() = p.mute
    val line get() = p.line; val track get() = p.track; val acc get() = p.acc; val accInk get() = p.accInk; val accSoft get() = p.accSoft
    val ok get() = p.ok; val okSoft get() = p.okSoft; val gold get() = p.gold; val goldSoft get() = p.goldSoft; val bad get() = p.bad
    val heat get() = p.heat; val nav get() = p.nav; val shadow get() = p.shadow; val snow get() = p.snow
}

val Nunito = FontFamily(Font(R.font.nunito_regular, FontWeight.Normal), Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold), Font(R.font.nunito_bold, FontWeight.ExtraBold))
val Inter = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_semibold, FontWeight.Bold))

object T {
    val title get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp, color = C.ink)
    val h2 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, color = C.ink)
    val h3 get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val body get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val bodyB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 17.sp, color = C.mute)
    val smallB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp, color = C.ink)
    val tiny get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, color = C.mute)
    val sec get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp, color = C.mute)
    val num get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.ink)
    val numS get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.ink)
    val big get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, color = C.ink)
    val btn get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
}

@Composable
fun PennyTheme(dark: Boolean, content: @Composable () -> Unit) {
    C.p = if (dark) Dark else Light
    val p = C.p
    val scheme = if (dark) darkColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    else lightColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    MaterialTheme(colorScheme = scheme, content = content)
}
