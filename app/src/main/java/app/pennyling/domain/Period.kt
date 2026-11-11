package app.pennyling.domain

import app.pennyling.data.Bill
import app.pennyling.data.Cat
import app.pennyling.data.Tx
import java.time.LocalDate
import kotlin.math.max

/** Built-in categories: the key, names, and for spending ones the keeper is found by the key. */
data class CatSeed(val key: String, val ru: String, val en: String, val income: Boolean = false, val icon: String = "")

object Cats {
    val seeds = listOf(
        CatSeed("food", "Продукты", "Groceries"), CatSeed("cafe", "Кафе и доставка", "Cafés & delivery"),
        CatSeed("home", "Дом и коммуналка", "Home & utilities"), CatSeed("transport", "Транспорт", "Transport"),
        CatSeed("health", "Здоровье", "Health"), CatSeed("shopping", "Покупки и одежда", "Shopping & clothes"),
        CatSeed("fun", "Развлечения", "Fun"), CatSeed("connect", "Связь и подписки", "Phone & subscriptions"),
        CatSeed("family", "Семья и дети", "Family & kids"), CatSeed("gifts", "Подарки", "Gifts"),
        CatSeed("travel", "Путешествия", "Travel"), CatSeed("care", "Уход за собой", "Self-care"),
        CatSeed("learn", "Учёба и книги", "Learning & books"), CatSeed("other", "Разное", "Other"),
        CatSeed("salary", "Зарплата", "Salary", true, "WALLET"), CatSeed("side", "Подработка", "Side work", true, "SPARK"),
        CatSeed("gift_in", "Подарили", "Gifts received", true, "GIFT"), CatSeed("back", "Возвраты и кешбэк", "Refunds & cashback", true, "SWAP"),
        CatSeed("other_in", "Другое", "Other income", true, "COIN"),
    )

    fun defaults(): List<Cat> = seeds.mapIndexed { i, s ->
        Cat(name = tr(s.ru, s.en), income = s.income, keeper = Pets.keeperOf(s.key)?.id.orEmpty(), icon = s.icon, order = i, key = s.key)
    }
}

/** The pay period: from payday to the day before the next one. Its days are «winter» counting down to spring. */
data class Period(val start: Long, val end: Long) {
    val days get() = (end - start + 1).toInt()
    fun has(day: Long) = day in start..end
    fun left(today: Long) = (end - today + 1).toInt().coerceAtLeast(1)
    /** 0 right after payday, 1 on its last day. */
    fun winter(today: Long) = ((today - start + .5f) / days).coerceIn(0f, 1f)
    fun prev(payday: Int) = of(start - 1, payday)
    fun next(payday: Int) = of(end + 1, payday)

    companion object {
        private fun payOn(year: Int, month: Int, payday: Int): LocalDate {
            val first = LocalDate.of(year, month, 1)
            return first.withDayOfMonth(minOf(payday.coerceIn(1, 31), first.lengthOfMonth()))
        }
        fun of(day: Long, payday: Int): Period {
            val d = LocalDate.ofEpochDay(day)
            var s = payOn(d.year, d.monthValue, payday)
            if (s.isAfter(d)) { val p = d.minusMonths(1); s = payOn(p.year, p.monthValue, payday) }
            val n = s.plusMonths(1).let { payOn(it.year, it.monthValue, payday) }
            return Period(s.toEpochDay(), n.toEpochDay() - 1)
        }
    }
}

/**
 * Today's ration: what the rest of the budget allows per day, after the regular payments still to come.
 * [left] is what's left of it today; [pantry] what's left of the whole period's budget.
 */
data class Ration(
    val period: Period, val budget: Long, val spent: Long, val spentToday: Long, val reserved: Long,
    val perDay: Long, val left: Long, val pantry: Long, val daysLeft: Int, val stash: Long, val forecast: Long,
) {
    val over get() = left < 0
    /** How full today's acorns are, 0..1. */
    val acorns get() = if (perDay <= 0) 0f else (left.toFloat() / perDay).coerceIn(0f, 1f)

    companion object {
        fun of(txs: List<Tx>, bills: List<Bill>, budget: Long, payday: Int, today: Long): Ration {
            val p = Period.of(today, payday)
            val spends = txs.filter { it.kind == Tx.SPEND && p.has(it.day) }
            val before = spends.filter { it.day < today }.sumOf { it.amount }
            val todayAll = spends.filter { it.day == today }
            val billToday = todayAll.filter { it.billId != 0L }.sumOf { it.amount }
            val spentToday = todayAll.filter { it.billId == 0L }.sumOf { it.amount }
            val reserved = bills.filter { it.active && !it.income }.sumOf { b -> Bills.dueIn(b, today, p.end).size * b.amount }
            val daysLeft = p.left(today)
            val perDay = max(0, budget - before - billToday - reserved) / daysLeft / 100 * 100
            val spent = before + billToday + spentToday
            val elapsed = (today - p.start).toInt()
            // what was set aside versus an even share of the budget, up to yesterday
            val even = if (p.days > 0) budget * elapsed / p.days else 0
            val stash = max(0, even - before)
            // where the period will end at the pace so far
            val pace = if (elapsed > 0) before.toDouble() / elapsed else spentToday.toDouble()
            val forecast = (before + spentToday + billToday + reserved + pace * (daysLeft - 1)).toLong()
            return Ration(p, budget, spent, spentToday, reserved, perDay, perDay - spentToday, budget - spent - reserved, daysLeft, stash, forecast)
        }
    }
}

/** Regular payments: when they fall. */
object Bills {
    fun step(b: Bill, day: Long): Long {
        val d = LocalDate.ofEpochDay(day)
        return when (b.every) { Bill.WEEK -> day + 7; Bill.YEAR -> d.plusYears(1).toEpochDay(); else -> d.plusMonths(1).toEpochDay() }
    }
    /** Days in from..to the bill falls on (from its next date on). */
    fun dueIn(b: Bill, from: Long, to: Long): List<Long> {
        val out = ArrayList<Long>(); var d = b.next; var guard = 0
        while (d <= to && guard++ < 60) { if (d >= from) out += d; d = step(b, d) }
        return out
    }
    /** What a bill costs a year. */
    fun yearly(b: Bill): Long = when (b.every) { Bill.WEEK -> b.amount * 52; Bill.YEAR -> b.amount; else -> b.amount * 12 }
    fun monthly(b: Bill): Long = yearly(b) / 12
}
