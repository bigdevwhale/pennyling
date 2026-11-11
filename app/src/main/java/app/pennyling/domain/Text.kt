package app.pennyling.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** The language the app speaks right now (set from the activity's locale). */
object Lang { @Volatile var ru = true }


fun tr(ru: String, en: String) = if (Lang.ru) ru else en

fun plural(n: Long, one: String, few: String, many: String): String {
    val a = abs(n) % 100; val b = a % 10
    return if (a in 11..14) many else when (b) { 1L -> one; 2L, 3L, 4L -> few; else -> many }
}

/** «3 записи» / «3 records». */
fun count(n: Int, ru1: String, ru2: String, ru5: String, en1: String, enN: String) =
    if (Lang.ru) "${fmt(n.toLong())} ${plural(n.toLong(), ru1, ru2, ru5)}" else "${fmt(n.toLong())} ${if (n == 1) en1 else enN}"

/** 12 345 with a thin space in Russian, 12,345 in English. */
fun fmt(n: Long): String {
    val s = abs(n).toString().reversed().chunked(3).joinToString(if (Lang.ru) " " else ",").reversed()
    return if (n < 0) "−$s" else s
}
fun fmt(n: Int) = fmt(n.toLong())

/** One decimal at most, the decimal comma in Russian. */
fun fmt1(v: Double): String {
    val r = (v * 10).roundToLong()
    val s = if (r % 10 == 0L) fmt(r / 10) else fmt(r / 10) + (if (Lang.ru) "," else ".") + abs(r % 10)
    return if (v < 0 && r / 10 == 0L && r != 0L) "−$s" else s
}
fun fmt2(v: Double): String { val r = (v * 100).roundToLong(); return if (r % 100 == 0L) fmt(r / 100) else String.format(if (Lang.ru) Locale.forLanguageTag("ru") else Locale.UK, "%.2f", v).trimEnd('0') }

fun dur(sec: Int): String {
    val h = sec / 3600; val m = sec % 3600 / 60; val s = sec % 60
    return when {
        h > 0 -> tr("$h ч ${m} мин", "$h h ${m} min")
        m > 0 && s > 0 && m < 10 -> "$m:${s.toString().padStart(2, '0')}"
        m > 0 -> tr("$m мин", "$m min")
        else -> tr("$s с", "$s s")
    }
}
private val ruLoc = Locale.forLanguageTag("ru")
val loc: Locale get() = if (Lang.ru) ruLoc else Locale.UK
val zone: ZoneId get() = ZoneId.systemDefault()
fun dayOf(ms: Long): Long = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()
fun hourOf(ms: Long): Int = Instant.ofEpochMilli(ms).atZone(zone).hour
fun today(): Long = LocalDate.now().toEpochDay()
fun fmtDay(day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMMM", loc))
fun fmtDayShort(day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern(if (Lang.ru) "d MMM" else "d MMM", loc)).trimEnd('.')
fun fmtDayYear(day: Long): String {
    val d = LocalDate.ofEpochDay(day)
    return if (d.year == LocalDate.now().year) fmtDay(day) else d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", loc))
}
fun weekday(day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEEE", loc))
fun relDay(day: Long): String = when (today() - day) { 0L -> tr("сегодня", "today"); 1L -> tr("вчера", "yesterday"); else -> fmtDayYear(day) }
fun monthName(m: Int, nominative: Boolean = true): String =
    if (Lang.ru) (if (nominative) MONTHS_RU else MONTHS_RU_GEN)[m - 1] else MONTHS_EN[m - 1]
fun monthShort(m: Int): String = if (Lang.ru) MONTHS_RU_SHORT[m - 1] else MONTHS_EN[m - 1].take(3)
fun dayShort(i: Int): String = if (Lang.ru) listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")[i] else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")[i]

private val MONTHS_RU = listOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")
private val MONTHS_RU_GEN = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
private val MONTHS_RU_SHORT = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
private val MONTHS_EN = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

/** Monday of the week [day] falls in. */
fun weekStart(day: Long): Long = day - LocalDate.ofEpochDay(day).dayOfWeek.value + 1

/** FNV-1a: stable hashes for colours and the order of things. */
fun hash(s: String): Int { var x = -0x7ee3623b; for (c in s) { x = x xor c.code; x *= 16777619 }; return x }

/** A small seeded random (mulberry32), so made-up things look the same every time. */
class Rand(seed: Int) {
    private var a = seed
    fun next(): Float { a += 0x6D2B79F5; var t = (a xor (a ushr 15)) * (1 or a); t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t; return ((t xor (t ushr 14)).toLong() and 0xffffffffL).toFloat() / 4294967296f }
    fun range(a: Float, b: Float) = a + (b - a) * next()
    fun int(n: Int) = (next() * n).toInt().coerceAtMost(n - 1)
}

fun String.cap() = replaceFirstChar { it.uppercase() }
