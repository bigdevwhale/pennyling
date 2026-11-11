package app.pennyling.data

import app.pennyling.AppContainer
import app.pennyling.domain.Lang
import app.pennyling.domain.today
import java.time.LocalDate
import kotlin.random.Random

/**
 * Four months of an ordinary life with money, lived through the real engine day by day
 * (the clock is moved), so the pets, crumbs, quests and finds are what they would be. Demo builds only.
 */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.settings.now().onboarded || c.dao.txList().isNotEmpty()) return
        val ru = Lang.ru
        val k = if (ru) 1L else 1L  // amounts are written in whole units of the chosen currency
        val cur = if (ru) "RUB" else "USD"
        // in dollars the same life costs about 80 times less
        fun a(rub: Int): Long = if (ru) rub * 100L * k else maxOf(100L, (rub / 80.0 * 100).toLong() / 25 * 25)
        val t0 = today()
        val start = t0 - 125
        val r = Random(7)
        c.clock = { start }
        c.begin(cur, 10, a(68000), a(3000), a(42000), listOf(Account(name = if (ru) "Копилка" else "Savings", kind = Account.SAVE, start = a(120000))))
        // the screens wait until the whole history is in (ticks of a half-seeded burrow would race the seed)
        c.settings.update { it.copy(onboarded = false) }
        val cats = c.dao.catList().associateBy { it.key }
        val accs = c.dao.accountList(); val cash = accs.first { it.kind == Account.CASH }.id; val card = accs.first { it.kind == Account.CARD }.id
        fun cat(key: String) = cats[key]!!.id
        fun n(r1: String, e1: String) = if (ru) r1 else e1
        // regular payments
        val first10 = LocalDate.ofEpochDay(start).withDayOfMonth(10).let { if (it.toEpochDay() < start) it.plusMonths(1) else it }.toEpochDay()
        c.saveBill(Bill(title = n("Аренда", "Rent"), amount = a(25000), catId = cat("home"), accountId = card, next = first10 + 1))
        c.saveBill(Bill(title = n("Интернет", "Internet"), amount = a(650), catId = cat("connect"), accountId = card, next = first10 + 4))
        c.saveBill(Bill(title = n("Кинопоиск", "Streaming"), amount = a(299), catId = cat("connect"), accountId = card, next = first10 + 7, sub = true))
        c.saveBill(Bill(title = n("Музыка", "Music"), amount = a(199), catId = cat("connect"), accountId = card, next = first10 + 12, sub = true))
        c.saveBill(Bill(title = n("Облако для фото", "Photo cloud"), amount = a(149), catId = cat("connect"), accountId = card, next = first10 + 15, sub = true))
        c.saveBill(Bill(title = n("Спортзал", "Gym"), amount = a(2900), catId = cat("health"), accountId = card, next = first10 + 2, sub = true))
        c.saveBill(Bill(title = n("Зарплата", "Salary"), amount = a(98000), catId = cat("salary"), accountId = card, income = true, next = first10))
        val trip = c.saveGoal(Goal(name = n("Отпуск у моря", "A seaside holiday"), target = a(90000), created = start, until = LocalDate.ofEpochDay(start).plusMonths(7).toEpochDay()))
        val bike = c.saveGoal(Goal(name = n("Велосипед", "A bicycle"), target = a(32000), created = start))
        val debt = c.saveDebt(Debt(name = n("Кредитная карта", "Credit card"), total = a(46000), rate = 23.9, minPay = a(3500), created = start))

        val shops = listOf(n("Пятёрочка", "Tesco"), n("ВкусВилл", "Whole Foods"), n("Рынок", "Farmers market"), n("Перекрёсток", "Lidl"))
        for (day in start..t0) {
            c.clock = { day }
            c.tick()
            val wd = LocalDate.ofEpochDay(day).dayOfWeek.value
            val work = wd <= 5
            val list = ArrayList<Tx>()
            fun add(key: String, rub: Int, note: String, acc: Long = card) { list += Tx(amount = a(rub), catId = cat(key), accountId = acc, day = day, at = millis(day, 9 + r.nextInt(12)), note = note) }
            val snow = r.nextInt(100) < 6 && day < t0
            if (!snow) {
                if (r.nextInt(100) < 45) add("food", 600 + r.nextInt(2200), shops[r.nextInt(shops.size)])
                if (work && r.nextInt(100) < 70) add("cafe", 230 + r.nextInt(5) * 30, n("Кофейня у работы", "Coffee by the office"))
                if (work && r.nextInt(100) < 55) add("cafe", 420 + r.nextInt(250), n("Обед", "Lunch"))
                if (work && r.nextInt(100) < 80) add("transport", 66 * 2, n("Метро", "Metro"), cash)
                if (r.nextInt(100) < 8) add("transport", 380 + r.nextInt(600), n("Такси", "Taxi"))
                if (!work && r.nextInt(100) < 55) add("cafe", 1400 + r.nextInt(2400), n("Ужин с друзьями", "Dinner with friends"))
                if (!work && r.nextInt(100) < 18) add("fun", 600 + r.nextInt(900), n("Кино", "Cinema"))
                if (r.nextInt(100) < 5) add("shopping", 1800 + r.nextInt(5200), n("Одежда", "Clothes"))
                if (r.nextInt(100) < 4) add("health", 350 + r.nextInt(1400), n("Аптека", "Pharmacy"))
                if (r.nextInt(100) < 3) add("gifts", 1500 + r.nextInt(3000), n("Подарок другу", "A gift for a friend"))
                if (r.nextInt(100) < 3) add("learn", 600 + r.nextInt(900), n("Книги", "Books"))
                if (r.nextInt(100) < 3) add("family", 900 + r.nextInt(2000), n("Маме", "For Mum"))
                if (r.nextInt(100) < 4) add("fun", 400 + r.nextInt(800), n("Игры", "Games"))
                if (LocalDate.ofEpochDay(day).dayOfMonth == 18) add("care", 1600, n("Стрижка", "Haircut"))
                if (day == start + 52) add("travel", 14800, n("Билеты к бабушке", "Train to Grandma's"))
                if (r.nextInt(100) < 6) add("other", 200 + r.nextInt(800), n("Мелочи для дома", "Bits for home"), cash)
            }
            for (t in list.sortedBy { it.at }) c.addTx(t)
            if (snow) c.snowDay(day)
            // the evening: answering «worth it?» for last week, sometimes
            for (t in c.dao.txList().filter { it.worth == 0 && it.billId == 0L && it.kind == Tx.SPEND && day - it.day >= 7 && day - it.day < 12 }) {
                if (r.nextInt(100) < 70) c.answerWorth(t, when (t.catId) { cat("cafe") -> if (t.note.contains(n("Ужин", "Dinner"))) 1 else listOf(1, 2, 2, 3).random(r); cat("shopping") -> listOf(1, 3).random(r); cat("fun") -> 1; else -> listOf(1, 1, 2).random(r) })
            }
            if (LocalDate.ofEpochDay(day).dayOfMonth == 11) {
                c.dao.goalList().firstOrNull { it.id == trip }?.let { c.deposit(it, a(9000)) }
                c.dao.goalList().firstOrNull { it.id == bike && it.done == 0L }?.let { c.deposit(it, a(11000)) }
                c.dao.debtList().firstOrNull { it.id == debt }?.let { c.payDebt(it, a(6000)) }
                c.addTx(Tx(amount = a(6000), kind = Tx.MOVE, accountId = card, toAccountId = cash, day = day, at = millis(day, 11), note = n("Снял наличные", "Cash out")))
                c.addTx(Tx(amount = a(5000), kind = Tx.MOVE, accountId = card, toAccountId = accs.first { it.kind == Account.SAVE }.id, day = day, at = millis(day, 12)))
            }
            if (wd == 1 && day > start + 6) c.weeklyReview(day)
            if (day == start + 20) c.addWish(Wish(name = n("Беспроводные наушники", "Wireless headphones"), price = a(12900), added = day, days = 30))
            if (day == start + 30) c.addWish(Wish(name = n("Кофемашина", "Coffee machine"), price = a(34000), added = day, days = 30))
            if (day == t0 - 9) c.addWish(Wish(name = n("Плед из шерсти", "A wool blanket"), price = a(4900), added = day, days = 14))
            if (day == start + 52) c.dao.wishList().firstOrNull { it.price == a(12900) }?.let { c.decideWish(it, 2, null) }
            if (day == start + 66) c.dao.wishList().firstOrNull { it.price == a(34000) }?.let { c.decideWish(it, 1, Tx(amount = a(34000), catId = cat("shopping"), accountId = card, day = day, note = n("Кофемашина", "Coffee machine"))) }
            if (day == start + 40) c.dao.billList().filter { it.sub }.take(3).forEach { c.review(it, if (it.amount == a(149)) 2 else 1) }
            // furniture as crumbs allow
            val s = c.settings.now()
            app.pennyling.domain.Shop.all.sortedBy { it.price }.firstOrNull { it.id !in s.items && it.price <= s.crumbs - 30 }?.let { c.buy(it.id) }
        }
        // what the crow brought this morning
        c.clock = { t0 }
        c.settings.update { it.copy(banks = setOf("ru.sberbankmobile"), inbox = emptyList(), seen = setOf("home", "pets", "goals")) }
        c.hear(Heard(pkg = "ru.sberbankmobile", amount = a(1240), income = false, place = shops[1], text = n("Покупка 1 240 ₽ ВкусВилл. Баланс: 18 420 ₽", "Purchase of $15.50 at Whole Foods. Balance: $230.25"), at = System.currentTimeMillis() - 3600_000))
        c.hear(Heard(pkg = "ru.sberbankmobile", amount = a(320), income = false, place = n("Кофейня у работы", "Coffee by the office"), text = n("Оплата 320 ₽ Кофейня. Баланс: 18 100 ₽", "Payment of $4.00 at Coffee shop"), at = System.currentTimeMillis() - 1800_000))
        // a team: the hamster leads, two keepers with the most records come along
        val pets = c.dao.petList().filter { it.hatched > 0 }
        val busy = pets.filter { it.species != "hamster" }.sortedByDescending { it.xp }.take(2)
        for (p in pets) c.dao.update(p.copy(shiny = false, slot = when { p.species == "hamster" -> 0; p.id == busy.getOrNull(0)?.id -> 1; p.id == busy.getOrNull(1)?.id -> 2; else -> -1 }))
        c.clock = { today() }
        c.settings.update { it.copy(onboarded = true) }
    }

    private fun millis(day: Long, hour: Int) = LocalDate.ofEpochDay(day).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + hour * 3600_000L + Random.nextInt(3600_000)
}
