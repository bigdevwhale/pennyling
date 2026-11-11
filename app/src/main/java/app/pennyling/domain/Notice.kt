package app.pennyling.domain

/**
 * Reads a bank's notification: how much, which way, where. Banks write it a hundred ways
 * («Покупка 612р Пятёрочка Баланс: 12 345р», «Оплата 612 ₽ в PYATEROCHKA, карта *1234»,
 * «Purchase of $12.40 at Blue Bottle»), so this looks for the first money amount that isn't a
 * balance, the words around it that say spent or received, and what's left as the place.
 */
object Notice {
    data class Read(val income: Boolean, val amount: Long, val place: String, val cash: Boolean)

    private const val CUR = "₽|рубл[а-яё]*|руб\\.?|р\\.|р|rub|rur|\\$|usd|€|eur|£|gbp|₸|kzt|₴|uah|byn|br|zł|pln|₺|try|gel|₾|amd|֏|uzs|сум"
    private const val NUM = "\\d{1,3}(?:[ \\u00A0\\u202F]\\d{3})+(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?"
    /** Groups: 1–2 currency then number, 3–4 number then currency; never inside a word. */
    private val AMOUNT = Regex("(?iu)(?:(?<![\\p{L}\\d])($CUR)\\s?($NUM)|(?<![\\d.,])($NUM)\\s?($CUR)(?![\\p{L}]))")

    /** Words after which an amount is a balance, not the payment. */
    private val BALANCE = Regex("(?iu)(баланс|доступно|остаток|ост\\.|available|balance|лимит|limit)[: ]*$")
    private val SPEND = Regex("(?iu)покупк|оплат|списан|платёж|платеж|перевод (на|по)|выдач|снят|purchase|payment|paid|spent|debit|charged|withdraw")
    private val FEE = Regex("(?iu)зачислен|поступлен|пополнен|перевод от|возврат|refund|received|credited|deposit|incoming|salary|зарплат")
    private val CASH = Regex("(?iu)выдач|снят|банкомат|atm|cash|наличн")
    private val CARD = Regex("(?iu)(карт[аеыу]?|card|счёт|счет|acct)?\\s*[*•·]+\\s?\\d{2,4}")
    private val NOISE = Regex("(?iu)\\b(покупка|оплата|списание|платёж|платеж|успешно|на сумму|сумма|purchase|payment|of|at|in|в|на|по|с|карта|card|mir|visa|mastercard|мир|operation|операция|одобрена|approved)\\b")
    private val TIME = Regex("\\b\\d{1,2}[:.]\\d{2}(?:[:.]\\d{2})?\\b|\\b\\d{1,2}[./]\\d{1,2}(?:[./]\\d{2,4})?\\b")

    fun read(title: String, text: String): Read? {
        val all = "$title. $text".replace('\n', ' ')
        val m = AMOUNT.findAll(all).firstOrNull { !BALANCE.containsMatchIn(all.substring(0, it.range.first).takeLast(24)) } ?: return null
        val raw = m.groups[2]?.value ?: m.groups[3]?.value ?: return null
        val amount = minor(raw) ?: return null
        if (amount <= 0) return null
        val before = all.substring(0, m.range.first)
        val fee = FEE.containsMatchIn(all) && !SPEND.containsMatchIn(before.takeLast(40))
        val spend = SPEND.containsMatchIn(all)
        if (!fee && !spend) return null
        val cash = !fee && CASH.containsMatchIn(all)
        return Read(fee, amount, place(all, m.range), cash)
    }

    /** «12 345,60» → 1234560. */
    fun minor(raw: String): Long? {
        val s = raw.replace(Regex("[ \\u00A0\\u202F]"), "")
        val sep = s.lastIndexOfAny(charArrayOf('.', ','))
        return if (sep >= 0 && s.length - sep - 1 in 1..2) {
            val whole = s.substring(0, sep).filter(Char::isDigit).toLongOrNull() ?: return null
            val frac = s.substring(sep + 1).padEnd(2, '0').toLongOrNull() ?: return null
            whole * 100 + frac
        } else s.filter(Char::isDigit).toLongOrNull()?.times(100)
    }

    /** The place: what's left of the text after the amount, with balances, cards, times and the bank's words taken out. */
    private fun place(all: String, amount: IntRange): String {
        var rest = all.substring(amount.last + 1)
        // Cut at the balance part.
        Regex("(?iu)(баланс|доступно|остаток|ост\\.|available|balance)").find(rest)?.let { rest = rest.substring(0, it.range.first) }
        var p = rest.replace(CARD, " ").replace(AMOUNT, " ").replace(TIME, " ").replace(NOISE, " ")
            .replace(Regex("[,;:()\\[\\]\"«»]"), " ").replace(Regex("\\s+"), " ").trim().trim('.', '-', '—', ' ')
        if (p.length < 2) {
            // Some banks name the place first: «Пятёрочка: покупка 612 ₽».
            p = all.substring(0, amount.first).replace(CARD, " ").replace(TIME, " ").replace(SPEND, " ").replace(NOISE, " ")
                .replace(Regex("[,;:()\\[\\]\"«»]"), " ").replace(Regex("\\s+"), " ").trim().trim('.', '-', '—', ' ')
                .split(". ").lastOrNull()?.trim().orEmpty()
        }
        return p.split(' ').take(4).joinToString(" ").take(40)
    }

    /** A place as it is recognised again: letters only, the first two words. */
    fun key(place: String): String = place.lowercase().replace('ё', 'е').split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length > 1 }.take(2).joinToString(" ")
}
