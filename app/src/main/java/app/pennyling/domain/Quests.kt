package app.pennyling.domain

import app.pennyling.data.Account
import app.pennyling.data.Bill
import app.pennyling.data.Move
import app.pennyling.data.Tx
import app.pennyling.data.Wish

/** What happened this week, for the quests. */
class WeekFacts(val monday: Long, txs: List<Tx>, moves: List<Move>, wishes: List<Wish>, bills: List<Bill>, snow: Set<Long>, accounts: List<Account>, budget: Long, periodDays: Int) {
    private val week = monday..(monday + 6)
    private val mine = txs.filter { it.day in week && it.kind != Tx.MOVE }
    val days = (mine.map { it.day } + snow.filter { it in week }).toSet().size
    val snowDays = snow.count { it in week }
    val notes = mine.count { it.note.isNotBlank() }
    val cats = mine.filter { it.kind == Tx.SPEND }.map { it.catId }.toSet().size
    val income = mine.count { it.kind == Tx.INCOME }
    private val cash = accounts.filter { it.kind == Account.CASH }.map { it.id }.toSet()
    val cashRecs = mine.count { it.accountId in cash }
    val wishes = wishes.count { it.added in week }
    val deposits = moves.count { it.kind == "goal" && it.amount > 0 && it.day in week }
    val debtPays = moves.count { it.kind == "debt" && it.amount > 0 && it.day in week }
    val reviewed = bills.count { it.checked in week }
    /** Days this week that cost no more than an even share of the budget. */
    val calm: Int = run {
        if (budget <= 0) return@run 0
        val share = budget / periodDays.coerceAtLeast(1)
        val byDay = mine.filter { it.kind == Tx.SPEND && it.billId == 0L }.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.amount } }
        (monday..minOf(monday + 6, today())).count { d -> (byDay[d] ?: 0) <= share && (d in byDay || d in snow) }
    }
    var worth = 0
}

data class Quest(val id: String, val ru: String, val en: String, val need: Int, val crumbs: Int, val progress: (WeekFacts) -> Int) {
    val title get() = tr(ru, en)
}

object Quests {
    val all = listOf(
        Quest("days5", "Записывать 5 дней из 7", "Log on 5 days of 7", 5, 25) { it.days },
        Quest("snow2", "Два снежных дня – без трат", "Two snow days – no spending", 2, 25) { it.snowDays },
        Quest("worth4", "Ответить на 4 «Стоило того?»", "Answer 4 «Worth it?»", 4, 15) { it.worth },
        Quest("notes5", "5 записей с заметкой", "5 records with a note", 5, 15) { it.notes },
        Quest("wish1", "Отнести желание на чердак", "Put a wish in the attic", 1, 15) { it.wishes },
        Quest("goal1", "Пополнить банку", "Add to a jar", 1, 20) { it.deposits },
        Quest("calm4", "4 спокойных дня – в пределах пайка", "4 calm days – within the ration", 4, 25) { it.calm },
        Quest("cash3", "3 траты наличными", "3 cash records", 3, 15) { it.cashRecs },
        Quest("review2", "Проверить 2 подписки в погребе", "Review 2 subscriptions in the cellar", 2, 15) { it.reviewed },
        Quest("cats4", "Траты в 4 разных категориях", "Spending in 4 different categories", 4, 10) { it.cats },
        Quest("income1", "Записать доход", "Log an income", 1, 10) { it.income },
        Quest("debt1", "Откатить валун – платёж по долгу", "Roll a boulder – a debt payment", 1, 20) { it.debtPays },
    )
    val byId = all.associateBy { it.id }

    /** Three quests for a week, the same for the same week. */
    fun deal(monday: Long, hasDebt: Boolean): List<String> {
        val r = Rand(monday.toInt() * 7919)
        val pool = all.filter { hasDebt || it.id != "debt1" }.toMutableList()
        return (0 until 3).map { pool.removeAt(r.int(pool.size)).id }
    }
}

/** Furniture for the burrow, bought with crumbs. Placed by [x], [y] (parts of the picture) and drawn [w] wide. */
data class Item(val id: String, val ru: String, val en: String, val price: Int, val x: Float, val y: Float, val w: Float, val wall: Boolean) {
    val title get() = tr(ru, en)
}

object Shop {
    val all = listOf(
        Item("shelf", "Полка с вареньем", "Jam shelf", 100, .46f, .3f, .15f, true),
        Item("fern", "Папоротник", "Fern", 120, .965f, .99f, .07f, false),
        Item("clock", "Часы", "Wall clock", 140, .63f, .24f, .065f, true),
        Item("lantern", "Фонарь", "Lantern", 160, .875f, .22f, .05f, true),
        Item("barrels", "Бочки", "Barrels", 180, .245f, .71f, .07f, false),
        Item("garland", "Сушёные грибы", "Dried mushrooms", 200, .3f, .13f, .14f, true),
        Item("painting", "Картина с дубом", "Oak painting", 230, .76f, .32f, .085f, true),
        Item("sacks", "Мешки с зерном", "Grain sacks", 260, .565f, .71f, .085f, false),
        Item("sled", "Санки", "Sled", 280, .8f, .47f, .09f, true),
        Item("abacus", "Счёты", "Abacus", 300, .315f, .71f, .055f, false),
        Item("stove", "Печка", "Stove", 340, .125f, .71f, .09f, false),
        Item("armchair", "Кресло", "Armchair", 380, .885f, .99f, .1f, false),
        Item("books", "Книжный шкаф", "Bookcase", 420, .69f, .71f, .085f, false),
        Item("hammock", "Гамак", "Hammock", 460, .3f, .44f, .12f, true),
        Item("desk", "Конторка", "Writing desk", 520, .8f, .71f, .085f, false),
        Item("chest", "Сундук", "Chest", 600, .385f, .71f, .07f, false),
    )
    val byId = all.associateBy { it.id }
    const val EGG = 400
}
