package app.pennyling.domain

import kotlin.math.abs
import kotlin.math.roundToLong

/** A currency the app counts in: its sign and whether the sign goes before the number. */
data class Cur(val code: String, val sign: String, val before: Boolean, val ru: String, val en: String, val minor: Boolean = true)

object Money {
    val all = listOf(
        Cur("RUB", "₽", false, "Рубль", "Russian rouble"),
        Cur("USD", "$", true, "Доллар США", "US dollar"),
        Cur("EUR", "€", true, "Евро", "Euro"),
        Cur("GBP", "£", true, "Фунт стерлингов", "Pound sterling"),
        Cur("KZT", "₸", false, "Тенге", "Kazakhstani tenge"),
        Cur("UAH", "₴", false, "Гривна", "Ukrainian hryvnia"),
        Cur("BYN", "Br", false, "Белорусский рубль", "Belarusian rouble"),
        Cur("UZS", "сум", false, "Сум", "Uzbekistani som", minor = false),
        Cur("KGS", "сом", false, "Сом", "Kyrgyzstani som"),
        Cur("GEL", "₾", false, "Лари", "Georgian lari"),
        Cur("AMD", "֏", false, "Драм", "Armenian dram", minor = false),
        Cur("AZN", "₼", false, "Манат", "Azerbaijani manat"),
        Cur("RSD", "дин.", false, "Сербский динар", "Serbian dinar", minor = false),
        Cur("TRY", "₺", true, "Турецкая лира", "Turkish lira"),
        Cur("PLN", "zł", false, "Злотый", "Polish złoty"),
        Cur("CZK", "Kč", false, "Чешская крона", "Czech koruna"),
        Cur("ILS", "₪", true, "Шекель", "Israeli shekel"),
        Cur("AED", "AED", false, "Дирхам", "UAE dirham"),
        Cur("THB", "฿", true, "Бат", "Thai baht"),
        Cur("CNY", "¥", true, "Юань", "Chinese yuan"),
        Cur("JPY", "¥", true, "Иена", "Japanese yen", minor = false),
        Cur("INR", "₹", true, "Рупия", "Indian rupee"),
        Cur("CAD", "C$", true, "Канадский доллар", "Canadian dollar"),
        Cur("AUD", "A$", true, "Австралийский доллар", "Australian dollar"),
        Cur("CHF", "CHF", false, "Швейцарский франк", "Swiss franc"),
        Cur("SEK", "kr", false, "Шведская крона", "Swedish krona"),
        Cur("NOK", "kr", false, "Норвежская крона", "Norwegian krone"),
        Cur("BRL", "R$", true, "Реал", "Brazilian real"),
        Cur("MXN", "MX$", true, "Мексиканское песо", "Mexican peso"),
    )
    @Volatile var cur: Cur = all[0]
    fun set(code: String) { cur = all.firstOrNull { it.code == code } ?: all[0] }
    fun of(code: String) = all.firstOrNull { it.code == code } ?: all[0]

    /** The usual currency for the phone's country. */
    fun guess(country: String, ru: Boolean): String = when (country.uppercase()) {
        "RU" -> "RUB"; "US" -> "USD"; "GB" -> "GBP"; "KZ" -> "KZT"; "UA" -> "UAH"; "BY" -> "BYN"; "UZ" -> "UZS"; "KG" -> "KGS"
        "GE" -> "GEL"; "AM" -> "AMD"; "AZ" -> "AZN"; "RS" -> "RSD"; "TR" -> "TRY"; "PL" -> "PLN"; "CZ" -> "CZK"; "IL" -> "ILS"
        "AE" -> "AED"; "TH" -> "THB"; "CN" -> "CNY"; "JP" -> "JPY"; "IN" -> "INR"; "CA" -> "CAD"; "AU" -> "AUD"; "CH" -> "CHF"
        "SE" -> "SEK"; "NO" -> "NOK"; "BR" -> "BRL"; "MX" -> "MXN"
        "DE", "FR", "ES", "IT", "NL", "BE", "AT", "PT", "FI", "IE", "GR", "SK", "SI", "EE", "LV", "LT", "CY", "MT", "LU", "HR", "ME" -> "EUR"
        else -> if (ru) "RUB" else "USD"
    }
}

private fun wrap(num: String, neg: Boolean, plus: Boolean): String {
    val c = Money.cur
    val sgn = if (neg) "−" else if (plus) "+" else ""
    return if (c.before) "$sgn${c.sign}$num" else "$sgn$num ${c.sign}"
}

/** 1 250 ₽ · $12.50 · −300 ₽. Kopecks only when there are any. [plus] marks money coming in. */
fun money(minor: Long, plus: Boolean = false): String {
    val a = abs(minor)
    val whole = a / 100; val frac = a % 100
    val num = if (frac == 0L || !Money.cur.minor) fmt(if (Money.cur.minor) whole else (a + 50) / 100) else fmt(whole) + (if (Lang.ru) "," else ".") + frac.toString().padStart(2, '0')
    return wrap(num, minor < 0, plus && minor > 0)
}

/** Whole units, rounded: for tiles and charts. */
fun moneyR(minor: Long): String = wrap(fmt(abs((minor / 100.0).roundToLong())), minor < 0 && abs(minor) >= 50, false)

/** Short for chart labels: 12,4k · 1,2M. */
fun moneyK(minor: Long): String {
    val v = abs(minor) / 100.0
    val s = when {
        v >= 1_000_000 -> fmt1(v / 1_000_000) + tr(" млн", "M")
        v >= 10_000 -> fmt(Math.round(v / 1000)) + tr(" тыс", "k")
        v >= 1000 -> fmt1(v / 1000) + tr(" тыс", "k")
        else -> fmt(Math.round(v))
    }
    return if (minor < 0) "−$s" else s
}

/** «12 345,60» or «12.5» typed in → minor units. Null when it isn't a number. */
fun parseMoney(s: String): Long? {
    val t = s.replace(Regex("[\\s\\u00A0\\u202F]"), "").replace(',', '.')
    if (t.isEmpty()) return null
    val d = t.toDoubleOrNull() ?: return null
    return (d * 100).roundToLong()
}

/** Minor units as a plain number to edit: «1250» · «12.5». */
fun plainMoney(minor: Long): String = if (minor % 100 == 0L) (minor / 100).toString() else (minor / 100.0).toString().let { if (Lang.ru) it.replace('.', ',') else it }
