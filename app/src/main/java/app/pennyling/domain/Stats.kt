package app.pennyling.domain

import app.pennyling.data.Account
import app.pennyling.data.Bill
import app.pennyling.data.Cat
import app.pennyling.data.Tx
import java.time.LocalDate
import kotlin.math.max

/** A stretch of days to look at. */
data class Span(val from: Long, val to: Long, val label: String) {
    val days get() = (to - from + 1).toInt().coerceAtLeast(1)
    fun has(d: Long) = d in from..to
    /** The same length right before. */
    fun before() = Span(from - days, from - 1, "")
}

enum class SpanKind { PERIOD, MONTH, YEAR, ALL }

object Spans {
    fun of(kind: SpanKind, back: Int, payday: Int, first: Long, today: Long = today()): Span = when (kind) {
        SpanKind.PERIOD -> { var p = Period.of(today, payday); repeat(back) { p = p.prev(payday) }
            Span(p.start, p.end, "${fmtDayShort(p.start)} – ${fmtDayShort(p.end)}") }
        SpanKind.MONTH -> { val m = LocalDate.ofEpochDay(today).withDayOfMonth(1).minusMonths(back.toLong())
            Span(m.toEpochDay(), m.plusMonths(1).toEpochDay() - 1, "${monthName(m.monthValue).cap()} ${m.year}") }
        SpanKind.YEAR -> { val y = LocalDate.ofEpochDay(today).year - back
            Span(LocalDate.of(y, 1, 1).toEpochDay(), LocalDate.of(y, 12, 31).toEpochDay(), "$y") }
        SpanKind.ALL -> Span(minOf(first, today), today, tr("За всё время", "All time"))
    }
}

data class CatSum(val cat: Cat?, val amount: Long, val count: Int, val share: Float, val before: Long)

/** Numbers about records in a span. [all] is every record, for comparisons and history. */
class Stats(val all: List<Tx>, val span: Span, val cats: Map<Long, Cat>, val accounts: List<Account>, val bills: List<Bill>, val budget: Long, val payday: Int) {
    val txs = all.filter { span.has(it.day) }
    val spends = txs.filter { it.kind == Tx.SPEND }
    val incomes = txs.filter { it.kind == Tx.INCOME }
    val spent = spends.sumOf { it.amount }
    val income = incomes.sumOf { it.amount }
    val net = income - spent
    val saveRate: Float = if (income > 0) (net.toFloat() / income) else 0f
    /** Days of the span that have passed. */
    val lived = (minOf(span.to, today()) - span.from + 1).toInt().coerceAtLeast(1)
    val perDay = spent / lived
    /** The span before, as far into it as this one has got (a half-lived month is compared with the first half of the last). */
    val prev = Span(span.from - span.days, span.from - 1 - (span.days - lived).coerceAtLeast(0), "")
    val spentBefore = all.filter { it.kind == Tx.SPEND && prev.has(it.day) }.sumOf { it.amount }

    fun byCat(income: Boolean = false): List<CatSum> {
        val l = if (income) incomes else spends
        val tot = max(1L, l.sumOf { it.amount })
        val before = all.filter { prev.has(it.day) && it.kind == (if (income) Tx.INCOME else Tx.SPEND) }.groupBy { it.catId }.mapValues { e -> e.value.sumOf { it.amount } }
        return l.groupBy { it.catId }.map { (id, g) -> val a = g.sumOf { it.amount }; CatSum(cats[id], a, g.size, a.toFloat() / tot, before[id] ?: 0) }
            .sortedByDescending { it.amount }
    }

    val byDay: Map<Long, Long> = spends.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.amount } }

    /** Average spend on each weekday (Mon..Sun). */
    val weekdays: List<Long> = run {
        val sums = LongArray(7); val n = IntArray(7)
        for (d in span.from..minOf(span.to, today())) { val w = LocalDate.ofEpochDay(d).dayOfWeek.value - 1; n[w]++; sums[w] += byDay[d] ?: 0 }
        (0 until 7).map { if (n[it] == 0) 0 else sums[it] / n[it] }
    }

    /** Spending by hour of day: (count, sum) in 8 three-hour slots. */
    val hours: List<Pair<Int, Long>> = (0 until 8).map { s -> val g = spends.filter { it.source != "bill" && hourOf(it.at) / 3 == s }; g.size to g.sumOf { it.amount } }

    /** Places: notes grouped by their first words. */
    val places: List<Triple<String, Int, Long>> = spends.filter { it.note.isNotBlank() && it.billId == 0L }.groupBy { Notice.key(it.note) }.filterKeys { it.isNotBlank() }
        .map { (_, g) -> Triple(g.first().note.trim(), g.size, g.sumOf { it.amount }) }.sortedByDescending { it.third }

    /** Small spends: no more than a third of an even day's share (or the currency's small sum). */
    val smallLimit: Long = max(if (budget > 0) budget / 30 / 3 else 0, if (Money.cur.minor) 30000 else 300)
    val small = spends.filter { it.amount <= smallLimit && it.billId == 0L }

    val biggest = spends.sortedByDescending { it.amount }.take(5)

    /** «Was it worth it?»: answered records by answer, and the sums. */
    val worthAll = all.filter { it.kind == Tx.SPEND && it.worth in 1..3 && span.has(it.day) }
    fun worthSum(a: Int) = worthAll.filter { it.worth == a }.sumOf { it.amount }
    /** Per category: share of «yes» among answered, by money. */
    fun joyByCat(): List<Triple<Cat?, Float, Long>> = worthAll.groupBy { it.catId }.filter { it.value.size >= 2 }
        .map { (id, g) -> val tot = max(1L, g.sumOf { it.amount }); Triple(cats[id], g.filter { it.worth == 1 }.sumOf { it.amount }.toFloat() / tot, g.filter { it.worth == 3 }.sumOf { it.amount }) }
        .sortedByDescending { it.second }

    /** Spent and received in each of the last [n] calendar months, oldest first. */
    fun months(n: Int = 12): List<Triple<String, Long, Long>> {
        val m0 = LocalDate.now().withDayOfMonth(1)
        return (n - 1 downTo 0).map { b -> val m = m0.minusMonths(b.toLong()); val a = m.toEpochDay(); val z = m.plusMonths(1).toEpochDay() - 1
            val g = all.filter { it.day in a..z }
            Triple(monthShort(m.monthValue), g.filter { it.kind == Tx.SPEND }.sumOf { it.amount }, g.filter { it.kind == Tx.INCOME }.sumOf { it.amount }) }
    }

    /** Personal inflation: each category's monthly average over the last 3 full months against the 3 before. */
    fun inflation(): List<Triple<Cat?, Long, Float>> {
        val m0 = LocalDate.now().withDayOfMonth(1)
        val a = m0.minusMonths(3).toEpochDay(); val b = m0.toEpochDay() - 1; val c = m0.minusMonths(6).toEpochDay()
        val now = all.filter { it.kind == Tx.SPEND && it.day in a..b }.groupBy { it.catId }.mapValues { e -> e.value.sumOf { it.amount } / 3 }
        val was = all.filter { it.kind == Tx.SPEND && it.day in c until a }.groupBy { it.catId }.mapValues { e -> e.value.sumOf { it.amount } / 3 }
        return now.filter { (id, v) -> (was[id] ?: 0) > 0 && v > 0 }.map { (id, v) -> Triple(cats[id], v, (v - was[id]!!).toFloat() / was[id]!!) }
            .sortedByDescending { it.third }
    }

    /** Balance of an account by the records. */
    fun balance(a: Account, upTo: Long = Long.MAX_VALUE): Long = a.start + all.filter { it.day <= upTo }.sumOf { t ->
        when {
            t.kind == Tx.SPEND && t.accountId == a.id -> -t.amount
            t.kind == Tx.INCOME && t.accountId == a.id -> t.amount
            t.kind == Tx.MOVE && t.accountId == a.id -> -t.amount
            t.kind == Tx.MOVE && t.toAccountId == a.id -> t.amount
            else -> 0L
        } }

    fun worth(upTo: Long = Long.MAX_VALUE) = accounts.filter { !it.archived }.sumOf { balance(it, upTo) }

    /** Net worth at the end of each of the last [n] months. */
    fun worthLine(n: Int = 12): List<Pair<Double, Double>> {
        val m0 = LocalDate.now().withDayOfMonth(1)
        return (n - 1 downTo 0).map { b -> val end = if (b == 0) today() else m0.minusMonths(b - 1L).toEpochDay() - 1
            (n - 1 - b).toDouble() to worth(end) / 100.0 }
    }

    /** Average monthly spending over the last 3 full months (or what there is). */
    val monthlySpend: Long = run {
        val m0 = LocalDate.now().withDayOfMonth(1)
        val from = m0.minusMonths(3).toEpochDay(); val first = all.minOfOrNull { it.day } ?: today()
        val months = ((m0.toEpochDay() - maxOf(from, first)) / 30.4).coerceAtLeast(1.0)
        val sum = all.filter { it.kind == Tx.SPEND && it.day in from until m0.toEpochDay() }.sumOf { it.amount }
        (if (sum > 0) (sum / months).toLong() else spent * 30 / lived) / 100 * 100
    }

    val subsYear = bills.filter { it.active && it.sub }.sumOf { Bills.yearly(it) }
    val billsMonth = bills.filter { it.active && !it.income }.sumOf { Bills.monthly(it) }

    /** Cumulative spending through the current period, day by day, against the budget line. */
    fun burn(): Pair<List<Pair<Double, Double>>, List<Pair<Double, Double>>> {
        val p = Period.of(today(), payday)
        var acc = 0L
        val line = (p.start..minOf(p.end, today())).map { d -> acc += all.filter { it.kind == Tx.SPEND && it.day == d }.sumOf { it.amount }; (d - p.start).toDouble() to acc / 100.0 }
        val plan = listOf(0.0 to 0.0, (p.days - 1).toDouble() to budget / 100.0)
        return line to plan
    }

    /** Days with at least one record, over everything. */
    val daysLogged = all.filter { it.kind != Tx.MOVE }.map { it.day }.toSet().size
}
