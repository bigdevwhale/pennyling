package app.pennyling.ui.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.pennyling.R
import app.pennyling.data.Pet
import app.pennyling.domain.Pets
import app.pennyling.domain.Period
import app.pennyling.domain.Shop
import app.pennyling.domain.Span
import app.pennyling.domain.Stage
import app.pennyling.domain.Stats
import app.pennyling.domain.fmtDayShort
import app.pennyling.domain.moneyR
import app.pennyling.domain.stage
import app.pennyling.domain.title
import app.pennyling.domain.tr
import app.pennyling.ui.Data
import app.pennyling.ui.art.Art
import app.pennyling.ui.art.Pose
import app.pennyling.ui.art.frameOf
import java.io.File
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

enum class CardKind(val ru: String, val en: String) {
    PERIOD("Месяц", "Month"), PET("Питомец", "Pet"), GOAL("Банка", "Jar"), YEAR("Год", "Year"), BURROW("Норка", "Burrow"), COLLECTION("Коллекция", "Collection");
    val label get() = tr(ru, en)
}

/** What to draw: a card kind and what it is about (a period's start day, a pet, a jar, a year). */
data class ShareReq(val kind: CardKind, val id: Long = 0)

/** How a card looks: the format, the background, whether sums are hidden (shares stay). */
data class Look(val story: Boolean = false, val bg: Int = 0, val hide: Boolean = false)

/** Backgrounds: two colours top to bottom and whether the text is light; -1 is the burrow picture. */
val BACKS = listOf(
    Triple(0xFFC8742F.toInt(), 0xFF7A3B17.toInt(), true), Triple(0xFF4F8A5B.toInt(), 0xFF1F3F2C.toInt(), true), Triple(0xFF2E3550.toInt(), 0xFF12152A.toInt(), true),
    Triple(0xFFF6EEDF.toInt(), 0xFFEADBC0.toInt(), false), Triple(0xFFF2D27A.toInt(), 0xFFE39A45.toInt(), false), Triple(-1, -1, true),
)

class Painter(private val ctx: Context, private val d: Data, private val look: Look) {
    private val W = 1080; private val H = if (look.story) 1920 else 1350
    private val bold: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_bold) ?: Typeface.DEFAULT_BOLD
    private val reg: Typeface = ResourcesCompat.getFont(ctx, R.font.nunito_regular) ?: Typeface.DEFAULT
    private val num: Typeface = ResourcesCompat.getFont(ctx, R.font.inter_semibold) ?: Typeface.DEFAULT_BOLD
    private val back = BACKS[look.bg.coerceIn(0, BACKS.lastIndex)]
    private val light = back.third
    private val ink = if (light) 0xFFFFFFFF.toInt() else 0xFF2E2218.toInt()
    private val sub = if (light) 0xC8FFFFFF.toInt() else 0xA02E2218.toInt()
    private val accent = if (light) 0xFFFFD36B.toInt() else 0xFFA65420.toInt()
    private val panel = if (light) 0x24FFFFFF else 0x142E2218
    private val top = if (look.story) 300f else 200f
    private val bottom get() = H - 150f

    private fun m(v: Long) = if (look.hide) "•••" else moneyR(v)
    private fun p(color: Int = ink) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun base(kicker: String): Pair<Bitmap, Canvas> {
        val b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val c = Canvas(b)
        if (back.first == -1) {
            Art.get(ctx, "burrow_bg")?.asAndroidBitmap()?.let { g -> val s = H.toFloat() / g.height; val w = g.width * s
                c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p(0xFF7A4E2A.toInt()))
                c.drawBitmap(g, Rect(0, 0, g.width, g.height), RectF((W - w) / 2, 0f, (W + w) / 2, H.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG)) }
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xA0140C08.toInt(), 0xE6140C08.toInt(), Shader.TileMode.CLAMP) })
        } else {
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, W * .4f, H.toFloat(), back.first, back.second, Shader.TileMode.CLAMP) })
            for ((x, y, r) in listOf(Triple(.92f, .08f, .42f), Triple(.05f, .62f, .36f), Triple(.8f, .95f, .3f)))
                c.drawCircle(W * x, H * y, W * r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(W * x, H * y, W * r, if (light) 0x30FFFFFF else 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP) })
        }
        val hy = if (look.story) 130f else 70f
        c.drawRoundRect(RectF(60f, hy, 60f + 270f, hy + 64f), 32f, 32f, p(panel))
        Art.prop(ctx, "acorn")?.asAndroidBitmap()?.let { img -> val s = 48f / max(img.width, img.height)
            c.drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(76f, hy + 8f, 76f + img.width * s, hy + 8f + img.height * s), Paint(Paint.FILTER_BITMAP_FLAG)) }
        c.t("Pennyling", 136f, hy + 44f, 32f, bold)
        c.t(kicker.uppercase(), W - 60f, hy + 44f, 30f, bold, sub, Paint.Align.RIGHT, maxW = W - 420f)
        return b to c
    }

    private fun Canvas.t(s: String, x: Float, y: Float, size: Float, face: Typeface = bold, color: Int = ink, align: Paint.Align = Paint.Align.LEFT, maxW: Float = when (align) { Paint.Align.CENTER -> W - 120f; Paint.Align.RIGHT -> x - 60f; else -> W - x - 60f }) {
        val pt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = face; this.color = color; textAlign = align }
        var str = s; if (pt.measureText(str) > maxW) { while (pt.measureText("$str…") > maxW && str.length > 2) str = str.dropLast(1); str += "…" }
        drawText(str, x, y, pt)
    }
    private fun Canvas.panel(x: Float, y: Float, w: Float, h: Float, r: Float = 36f, color: Int = panel) = drawRoundRect(RectF(x, y, x + w, y + h), r, r, p(color))
    private fun Canvas.facts(items: List<Pair<String, String>>, y: Float, h: Float = 170f) {
        val n = items.size; val gap = 18f; val w = (W - 120f - gap * (n - 1)) / n
        items.forEachIndexed { i, (v, l) -> val x = 60f + i * (w + gap); panel(x, y, w, h)
            t(v, x + w / 2, y + h * .52f, if (v.length > 8) 42f else 54f, num, ink, Paint.Align.CENTER, w - 24); t(l, x + w / 2, y + h * .8f, 28f, reg, sub, Paint.Align.CENTER, w - 24) }
    }
    private fun Canvas.bitmap(img: Bitmap, cx: Float, foot: Float, size: Float, shadow: Boolean = true) {
        val s = min(size / img.width, size / img.height); val w = img.width * s; val h = img.height * s
        if (shadow) drawOval(RectF(cx - w * .36f, foot - h * .04f, cx + w * .36f, foot + h * .04f), p(0x33000000))
        drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(cx - w / 2, foot - h, cx + w / 2, foot), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }
    private fun Canvas.pet(p: Pet?, pose: Pose, cx: Float, foot: Float, size: Float) {
        p ?: return
        val st = p.stage()
        val img = (if (st == Stage.EGG) Art.egg(ctx, p.species) else Art.pet(ctx, p.species, frameOf(st, pose)))?.asAndroidBitmap() ?: return
        val scale = when (st) { Stage.EGG -> .7f; Stage.BABY -> .62f; Stage.TEEN -> .8f; else -> .96f }
        bitmap(img, cx, foot, size * scale)
    }
    private fun Canvas.footer() = t(tr("Pennyling · норка, где деньги считают питомцы", "Pennyling · a burrow where pets keep the books"), W / 2f, H - 64f, 28f, reg, sub, Paint.Align.CENTER)

    /** The burrow in [r]: the room, the window, the things and the team. */
    private fun Canvas.burrow(r: RectF) {
        save(); clipPath(Path().apply { addRoundRect(r, 40f, 40f, Path.Direction.CW) })
        app.pennyling.ui.art.drawSky(this, r, app.pennyling.ui.art.Sky.of(d.s), .3f, d.ration.period.winter(d.today))
        Art.get(ctx, "burrow_bg")?.asAndroidBitmap()?.let { drawBitmap(it, Rect(0, 0, it.width, it.height), r, Paint(Paint.FILTER_BITMAP_FLAG)) }
        val items = d.s.items - d.s.hidden
        for (it in Shop.all.filter { it.id in items }.sortedBy { if (it.wall) 0 else 1 }) {
            val img = Art.item(ctx, it.id)?.asAndroidBitmap() ?: continue
            val w = r.width() * it.w; val h = w * img.height / img.width
            val x = r.left + r.width() * it.x - w / 2; val y = r.top + if (it.wall) r.height() * it.y - h / 2 else r.height() * it.y - h
            drawBitmap(img, Rect(0, 0, img.width, img.height), RectF(x, y, x + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val h = r.height(); val w = r.width()
        d.team.filter { it.slot in 1..2 }.forEach { pt -> val s = h * .3f; pet(pt, Pose.CHEER, r.left + w * (if (pt.slot == 1) .14f else .66f) + s / 2, r.top + h * .95f, s) }
        d.leader?.let { pet(it, Pose.IDLE, r.centerX(), r.top + h * .99f, h * .44f) }
        restore()
    }

    fun render(r: ShareReq): Pair<Bitmap, String> = when (r.kind) {
        CardKind.PERIOD -> period(if (r.id > 0) Period.of(r.id, d.s.payday) else d.ration.period)
        CardKind.PET -> pet(r.id); CardKind.GOAL -> goal(r.id); CardKind.YEAR -> year(if (r.id > 0) r.id.toInt() else LocalDate.now().year)
        CardKind.BURROW -> burrowCard(); CardKind.COLLECTION -> collection()
    }

    private fun period(p: Period): Pair<Bitmap, String> {
        val (b, c) = base("${fmtDayShort(p.start)} – ${fmtDayShort(p.end)}")
        val st = Stats(d.txs, Span(p.start, p.end, ""), d.catById, d.accounts, d.bills, d.s.budget, d.s.payday)
        val ok = d.s.budget > 0 && st.spent <= d.s.budget
        var y = top + 60
        c.t(if (p.end < d.today) (if (ok) tr("Перезимовали!", "We made it through winter!") else tr("Трудная зима позади", "A hard winter behind us")) else tr("Месяц в норке", "A month in the burrow"), 60f, y + 30, 76f)
        y += 90
        c.facts(listOf(m(st.spent) to tr("потрачено", "spent"), (if (d.s.budget > 0) "${st.spent * 100 / d.s.budget}%" else "–") to tr("бюджета", "of budget"),
            "${d.s.snow.count { p.has(it) }}" to tr("снежных дней", "snow days")), y, 160f)
        y += 200
        st.byCat().take(if (look.story) 6 else 4).forEach { cs ->
            c.panel(60f, y, W - 120f, 92f, 28f)
            cs.cat?.keeper?.takeIf { it.isNotEmpty() }?.let { k -> Art.pet(ctx, k, 2)?.asAndroidBitmap()?.let { c.bitmap(it, 120f, y + 84, 70f, false) } }
            c.t(cs.cat?.name ?: "–", 180f, y + 58, 38f, bold, ink, maxW = 520f)
            c.t(if (look.hide) "${(cs.share * 100).toInt()}%" else moneyR(cs.amount), W - 96f, y + 58, 38f, num, accent, Paint.Align.RIGHT)
            y += 108
        }
        val room = bottom - y
        if (room > 220) c.pet(d.leader, if (ok) Pose.CHEER else Pose.IDLE, W / 2f, bottom, min(room, 440f))
        c.footer()
        return b to tr("Мой месяц в Pennyling", "My month in Pennyling")
    }

    private fun pet(id: Long): Pair<Bitmap, String> {
        val pt = d.pets.firstOrNull { it.id == id } ?: d.leader; val (b, c) = base(tr("мой питомец", "my pet")); if (pt == null) return b to ""
        val sp = Pets.of(pt.species); val col = sp.color.toInt()
        val cy = top + (if (look.story) 450 else 290)
        c.drawCircle(W / 2f, cy, if (look.story) 400f else 320f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(W / 2f, cy, 420f, (col and 0xFFFFFF) or 0x88000000.toInt(), col and 0xFFFFFF, Shader.TileMode.CLAMP) })
        c.pet(pt, Pose.IDLE, W / 2f, cy + (if (look.story) 400 else 290), if (look.story) 820f else 560f)
        var y = cy + (if (look.story) 520 else 380)
        c.t(pt.name, W / 2f, y, 96f, bold, ink, Paint.Align.CENTER)
        c.t("${sp.title} · ${pt.stage().title()}" + if (pt.shiny) tr(" · редкий окрас", " · rare colours") else "", W / 2f, y + 64, 38f, reg, sub, Paint.Align.CENTER)
        y += 110
        c.facts(listOf("${pt.xp}" to tr("опыта", "XP"), "${pt.fed}" to tr("записей в категории", "records fed"), "${d.pets.count { it.hatched > 0 }}/${Pets.all.size}" to tr("в коллекции", "collected")), y, 150f)
        c.t(tr("В команде: ", "In the team: ") + app.pennyling.ui.pets.perkShort(sp), W / 2f, y + 200, 30f, reg, sub, Paint.Align.CENTER)
        c.footer()
        return b to "${pt.name} – ${sp.title}"
    }

    private fun goal(id: Long): Pair<Bitmap, String> {
        val g = d.goals.firstOrNull { it.id == id }; val (b, c) = base(tr("банка", "a jar")); if (g == null) return b to ""
        val full = g.done > 0
        Art.prop(ctx, if (full) "jar2" else "jar1")?.asAndroidBitmap()?.let { c.bitmap(it, W / 2f, top + (if (look.story) 760 else 560), if (look.story) 640f else 460f) }
        var y = top + (if (look.story) 860 else 650)
        c.t(if (full) tr("Банка полна!", "The jar is full!") else tr("Копится", "Filling up"), W / 2f, y, 52f, bold, sub, Paint.Align.CENTER)
        c.t(g.name, W / 2f, y + 100, 92f, bold, ink, Paint.Align.CENTER)
        y += 150
        c.facts(listOf(m(g.target) to tr("цель", "goal"), "${d.saved(g) * 100 / g.target.coerceAtLeast(1)}%" to tr("собрано", "saved"),
            (if (full) "${(g.done - g.created)}" else "${d.today - g.created}") to tr("дней", "days")), y, 150f)
        if (g.egg.isNotEmpty()) c.t(tr("Внутри нашлось яйцо: ${Pets.of(g.egg).ru.lowercase()}", "An egg was inside: a ${Pets.of(g.egg).en.lowercase()}"), W / 2f, y + 210, 32f, reg, sub, Paint.Align.CENTER)
        c.footer()
        return b to tr("Банка «${g.name}» в Pennyling", "The jar «${g.name}» in Pennyling")
    }

    private fun year(year: Int): Pair<Bitmap, String> {
        val (b, c) = base("$year")
        val from = LocalDate.of(year, 1, 1).toEpochDay(); val to = LocalDate.of(year, 12, 31).toEpochDay()
        val st = Stats(d.txs, Span(from, to, ""), d.catById, d.accounts, d.bills, d.s.budget, d.s.payday)
        var y = top + 60
        c.t(tr("Год в норке", "A year in the burrow"), 60f, y + 30, 80f); y += 90
        c.facts(listOf(m(st.spent) to tr("потрачено", "spent"), (if (st.income > 0) "${(st.saveRate * 100).toInt()}%" else "–") to tr("сбережено", "kept"),
            "${d.s.snow.count { it in from..to }}" to tr("снежных дней", "snow days")), y, 160f)
        y += 190
        c.facts(listOf("${st.txs.map { it.day }.toSet().size}" to tr("дней с записями", "days logged"), "${d.pets.count { it.hatched in from..to }}" to tr("вылупилось", "hatched"),
            "${d.goals.count { it.done in from..to }}" to tr("банок заполнено", "jars filled")), y, 160f)
        y += 190
        st.byCat().firstOrNull()?.let { cs -> c.t(tr("Главная статья: ${cs.cat?.name ?: "–"}", "Biggest line: ${cs.cat?.name ?: "–"}"), 60f, y + 40, 40f, bold, ink); y += 70 }
        val room = bottom - y
        if (room > 220) { val row = d.team.ifEmpty { listOfNotNull(d.leader) }; val s = min(room, 380f)
            row.forEachIndexed { i, pt -> c.pet(pt, Pose.CHEER, W / 2f + (i - (row.size - 1) / 2f) * s * .8f, bottom, s) } }
        c.footer()
        return b to tr("Мой $year в Pennyling", "My $year in Pennyling")
    }

    private fun burrowCard(): Pair<Bitmap, String> {
        val (b, c) = base(tr("моя норка", "my burrow"))
        val gh = (W - 120f) / 1.5f
        c.burrow(RectF(60f, top + 20, W - 60f, top + 20 + gh))
        var y = top + 70 + gh
        c.t(tr("Норка команды ${d.leader?.name ?: ""}", "${d.leader?.name ?: "The team"}'s burrow"), 60f, y + 40, 64f, maxW = W - 120f)
        c.t(d.team.joinToString(" · ") { "${it.name} (${Pets.of(it.species).title.lowercase()})" }, 60f, y + 100, 32f, reg, sub)
        y += 140
        c.facts(listOf("${(d.s.items - d.s.hidden).size}/${Shop.all.size}" to tr("вещей", "things"), "${d.pets.count { it.hatched > 0 }}" to tr("питомцев", "pets"),
            "${d.s.winters}" to tr("зим позади", "winters behind")), y, 150f)
        c.footer()
        return b to tr("Моя норка в Pennyling", "My burrow in Pennyling")
    }

    private fun collection(): Pair<Bitmap, String> {
        val (b, c) = base(tr("коллекция", "collection"))
        val cols = if (look.story) 4 else 6; val cell = (W - 120f) / cols
        var y = top + 20
        c.t(tr("${d.pets.count { it.hatched > 0 }} из ${Pets.all.size}", "${d.pets.count { it.hatched > 0 }} of ${Pets.all.size}"), 60f, y + 60, 76f); y += 100
        Pets.all.forEachIndexed { i, sp ->
            val x = 60f + (i % cols) * cell; val yy = y + (i / cols) * cell * .82f
            if (yy + cell * .8f > bottom) return@forEachIndexed
            val pt = d.pets.firstOrNull { it.species == sp.id }
            val img = when { pt == null -> null; pt.hatched == 0L -> Art.egg(ctx, sp.id); else -> Art.pet(ctx, sp.id, 2) }?.asAndroidBitmap()
            c.panel(x + 6, yy + 6, cell - 12, cell * .82f - 12, 28f)
            if (img != null) c.bitmap(img, x + cell / 2, yy + cell * .74f, cell * .66f, false) else c.t("?", x + cell / 2, yy + cell * .5f, 64f, bold, sub, Paint.Align.CENTER)
        }
        c.footer()
        return b to tr("Мои питомцы в Pennyling", "My pets in Pennyling")
    }
}

object Share {
    fun send(ctx: Context, bmp: Bitmap, title: String) {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }; val f = File(dir, "pennyling.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, tr("Поделиться открыткой", "Share the card")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Saves the card to Pictures/Pennyling. */
    fun save(ctx: Context, bmp: Bitmap): Boolean = runCatching {
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "pennyling-${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pennyling") }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!
        ctx.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; true }.getOrDefault(false)
}
