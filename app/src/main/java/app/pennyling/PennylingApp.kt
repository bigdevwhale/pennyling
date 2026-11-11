package app.pennyling

import android.app.Application
import android.content.Context
import app.pennyling.data.Account
import app.pennyling.data.Bill
import app.pennyling.data.Db
import app.pennyling.data.Debt
import app.pennyling.data.Goal
import app.pennyling.data.Heard
import app.pennyling.data.Move
import app.pennyling.data.OutsideSource
import app.pennyling.data.Outside
import app.pennyling.data.Pet
import app.pennyling.data.SettingsStore
import app.pennyling.data.Tx
import app.pennyling.data.Wish
import app.pennyling.domain.Bills
import app.pennyling.domain.Cats
import app.pennyling.domain.Engine
import app.pennyling.domain.Lang
import app.pennyling.domain.Money
import app.pennyling.domain.Period
import app.pennyling.domain.Perk
import app.pennyling.domain.Pets
import app.pennyling.domain.Quests
import app.pennyling.domain.Shop
import app.pennyling.domain.WeekFacts
import app.pennyling.domain.stage
import app.pennyling.domain.today
import app.pennyling.domain.tr
import app.pennyling.domain.dayOf
import app.pennyling.domain.weekStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = Db.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val news = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private val lock = Mutex()
    /** The clock the burrow lives by; the demo moves it while seeding. */
    @Volatile var clock: () -> Long = { today() }

    fun write(block: suspend AppContainer.() -> Unit) { scope.launch { block() } }
    fun say(s: String) { news.tryEmit(s) }

    // ---------- the start ----------

    /** First launch: the categories, two accounts, the hamster and the settings from the questions. */
    suspend fun begin(currency: String, payday: Int, budget: Long, cash: Long, card: Long, extra: List<Account>) = lock.withLock {
        if (dao.catList().isEmpty()) dao.insertCats(Cats.defaults())
        if (dao.accountList().isEmpty()) {
            dao.insert(Account(name = tr("Наличные", "Cash"), kind = Account.CASH, start = cash, order = 0))
            dao.insert(Account(name = tr("Карта", "Card"), kind = Account.CARD, start = card, order = 1))
            extra.forEachIndexed { i, a -> dao.insert(a.copy(order = 2 + i)) }
        }
        if (dao.petList().none { it.species == "hamster" }) dao.insert(newPet("hamster", 0).copy(hatched = clock(), growth = Pets.HATCH))
        val t = clock()
        Money.set(currency)
        settings.update { it.copy(onboarded = true, currency = currency, payday = payday, budget = budget, period = Period.of(t, payday).start,
            questWeek = weekStart(t), quests = Quests.deal(weekStart(t), false)) }
    }

    fun newPet(species: String, slot: Int): Pet {
        val sp = Pets.of(species); val names = if (Lang.ru) sp.names else sp.namesEn
        return Pet(species = species, name = names.random(), found = clock(), slot = slot, shiny = Random.nextInt(20) == 0)
    }

    // ---------- crumbs ----------

    /** Adds crumbs; [record] ones are capped per day. Returns what was given. */
    private suspend fun crumbs(n: Int, record: Boolean = false): Int {
        if (n <= 0) return 0
        val t = clock(); var given = n
        settings.update { s ->
            val todayGot = if (s.crumbDay == t) s.crumbsToday else 0
            if (record) given = n.coerceAtMost((Engine.DAILY_CAP - todayGot).coerceAtLeast(0))
            s.copy(crumbs = s.crumbs + given, crumbDay = t, crumbsToday = todayGot + if (record) given else 0)
        }
        return given
    }

    private suspend fun news(items: List<String>) { if (items.isNotEmpty()) settings.update { it.copy(inbox = it.inbox + items) } }

    /** Lays eggs (into free team slots first) and writes the news. */
    private suspend fun lay(species: List<String>, why: String = "") {
        if (species.isEmpty()) return
        val pets = dao.petList()
        val free = (1..2).filter { sl -> pets.none { it.slot == sl } }.toMutableList()
        for (sp in species.distinct()) { if (dao.petList().any { it.species == sp }) continue
            dao.insert(newPet(sp, free.removeFirstOrNull() ?: -1)) }
        news(species.distinct().map { "egg:$it$why" })
    }

    // ---------- records ----------

    suspend fun addTx(t: Tx): Long = lock.withLock {
        val id = dao.insert(t)
        if (t.kind != Tx.MOVE) reward(t)
        id
    }

    private suspend fun reward(t: Tx) {
        val pets = dao.petList()
        val keeper = if (t.kind == Tx.SPEND) dao.catList().firstOrNull { it.id == t.catId }?.keeper.orEmpty() else ""
        val fed = Engine.feed(pets, keeper, clock())
        dao.updatePets(fed.pets)
        val first = dao.txList().none { it.id != t.id && it.day == t.day && it.kind != Tx.MOVE && it.source != "bill" }
        val keeperInTeam = Engine.team(pets).any { it.species == keeper }
        var c = Engine.bonus(Engine.PER_RECORD, Perk.KEEPER, pets) + if (keeperInTeam) 1 else 0
        if (first && t.source != "bill") c += Engine.bonus(Engine.FIRST_OF_DAY, Perk.DAYS, pets)
        if (t.source != "bill") crumbs(c, record = true)
        lay(fed.eggs)
        news(fed.hatched.map { "hatch:${it.id}" } + fed.grown.map { "stage:${it.first.id}:${it.second.ordinal}" })
        afterAct()
    }

    suspend fun updateTx(t: Tx) = lock.withLock { dao.update(t) }
    suspend fun deleteTx(id: Long) = lock.withLock { dao.deleteTx(id) }

    /** «Spent nothing today»: a snow day. */
    suspend fun snowDay(day: Long) = lock.withLock {
        val s = settings.now(); if (day in s.snow) return@withLock
        val pets = dao.petList()
        settings.update { it.copy(snow = it.snow + day) }
        val bonus = if (Engine.team(pets).any { it.species == "dormouse" }) 2 else 1
        crumbs(Engine.bonus(Engine.SNOW, Perk.SNOW, pets) * bonus)
        dao.updatePets(Engine.cheer(pets, 4, "jay"))
        afterAct()
    }
    suspend fun unSnow(day: Long) = lock.withLock { settings.update { it.copy(snow = it.snow - day) } }

    suspend fun answerWorth(t: Tx, a: Int) = lock.withLock {
        dao.update(t.copy(worth = a))
        if (a in 1..3) { val pets = dao.petList(); crumbs(Engine.bonus(Engine.WORTH, Perk.WORTH, pets)); dao.updatePets(Engine.cheer(pets, 1, "otter")) }
        afterAct()
    }

    // ---------- wishes, jars, boulders, barrels ----------

    suspend fun addWish(w: Wish) = lock.withLock { if (w.id == 0L) dao.insert(w) else dao.update(w); afterAct() }
    suspend fun deleteWish(id: Long) = lock.withLock { dao.deleteWish(id) }

    /** Let go ([state] 2) or bought after cooling ([state] 1; [tx] records the buy). */
    suspend fun decideWish(w: Wish, state: Int, tx: Tx?) {
        lock.withLock {
            dao.update(w.copy(state = state, decided = clock()))
            val pets = dao.petList()
            val cooled = clock() - w.added >= w.days
            crumbs(if (state == 2 || cooled) Engine.bonus(Engine.WISH, Perk.WISH, pets) else 1)
            dao.updatePets(Engine.cheer(pets, 3, "fox"))
        }
        if (tx != null) addTx(tx) else lock.withLock { afterAct() }
    }

    suspend fun saveGoal(g: Goal): Long = lock.withLock { if (g.id == 0L) dao.insert(g) else { dao.update(g); g.id } }
    suspend fun deleteGoal(id: Long) = lock.withLock { dao.deleteGoal(id); dao.deleteMovesOf("goal", id) }

    /** Puts money into a jar (or takes it out). A jar that fills turns into an egg. */
    suspend fun deposit(g: Goal, amount: Long) = lock.withLock {
        dao.insert(Move(kind = "goal", refId = g.id, amount = amount, day = clock()))
        val pets = dao.petList()
        if (amount > 0) { crumbs(Engine.bonus(Engine.GOAL, Perk.GOAL, pets)); dao.updatePets(Engine.cheer(pets, 2, "tortoise")) }
        val saved = dao.moveList().filter { it.kind == "goal" && it.refId == g.id }.sumOf { it.amount }
        if (g.done == 0L && saved >= g.target) {
            val sp = Engine.anyEgg(dao.petList().map { it.species }.toSet(), (g.id * 31 + clock()).toInt())
            dao.update(g.copy(done = clock(), egg = sp.orEmpty()))
            news(listOf("goal:${g.id}"))
            if (sp != null) lay(listOf(sp), ":jar") else crumbs(60)
        }
        afterAct()
    }

    suspend fun saveDebt(d: Debt): Long = lock.withLock { if (d.id == 0L) dao.insert(d) else { dao.update(d); d.id } }
    suspend fun deleteDebt(id: Long) = lock.withLock { dao.deleteDebt(id); dao.deleteMovesOf("debt", id) }

    suspend fun payDebt(d: Debt, amount: Long) = lock.withLock {
        dao.insert(Move(kind = "debt", refId = d.id, amount = amount, day = clock()))
        val pets = dao.petList()
        crumbs(Engine.bonus(Engine.DEBT, Perk.DEBT, pets)); dao.updatePets(Engine.cheer(pets, 2, "badger"))
        val paid = dao.moveList().filter { it.kind == "debt" && it.refId == d.id }.sumOf { it.amount }
        if (d.done == 0L && paid >= d.total) { dao.update(d.copy(done = clock())); news(listOf("debt:${d.id}")); crumbs(40) }
        afterAct()
    }

    suspend fun saveBill(b: Bill) = lock.withLock { if (b.id == 0L) dao.insert(b) else dao.update(b); postBills() }
    suspend fun deleteBill(id: Long) = lock.withLock { dao.deleteBill(id) }

    /** A subscription looked at: keep it (1), doubt it (2) or cancel it (stops). */
    suspend fun review(b: Bill, verdict: Int) = lock.withLock {
        dao.update(b.copy(checked = clock(), keep = verdict, active = verdict != 3 && b.active))
        val pets = dao.petList(); crumbs(Engine.bonus(Engine.BILL, Perk.BILL, pets)); dao.updatePets(Engine.cheer(pets, 2, "bat"))
        afterAct()
    }

    /** Regular payments due by today become records. */
    private suspend fun postBills() {
        val t = clock()
        for (b in dao.billList().filter { it.active && it.next <= t }) {
            var next = b.next
            var n = 0
            while (next <= t && n++ < 24) {
                dao.insert(Tx(amount = b.amount, kind = if (b.income) Tx.INCOME else Tx.SPEND, catId = b.catId, accountId = b.accountId, day = next,
                    at = System.currentTimeMillis(), note = b.title, billId = b.id, source = "bill", worth = -1))
                next = Bills.step(b, next)
                // a regular payment feeds its keeper too (no crumbs: nothing was done by hand)
                val keeper = if (b.income) "" else dao.catList().firstOrNull { it.id == b.catId }?.keeper.orEmpty()
                if (keeper.isNotEmpty()) { val fed = Engine.feed(dao.petList(), keeper, clock(), xp = 2); dao.updatePets(fed.pets); lay(fed.eggs)
                    news(fed.hatched.map { "hatch:${it.id}" } + fed.grown.map { "stage:${it.first.id}:${it.second.ordinal}" }) }
            }
            dao.update(b.copy(next = next))
        }
    }

    // ---------- the crow ----------

    suspend fun hear(h: Heard) = lock.withLock { dao.insert(h) }
    suspend fun take(h: Heard, catId: Long, accountId: Long, note: String) {
        lock.withLock { dao.update(h.copy(state = 1)); crumbs(Engine.bonus(2, Perk.HEARD, dao.petList())) }
        addTx(Tx(amount = h.amount, kind = if (h.income) Tx.INCOME else Tx.SPEND, catId = catId, accountId = accountId, day = dayOf(h.at),
            at = h.at, note = note, source = "crow"))
    }
    suspend fun drop(h: Heard) = lock.withLock { dao.update(h.copy(state = 2)) }

    // ---------- the burrow ----------

    suspend fun buy(id: String): Boolean = lock.withLock {
        val it = Shop.byId[id] ?: return@withLock false; val s = settings.now()
        if (s.crumbs < it.price || id in s.items) return@withLock false
        settings.update { st -> st.copy(crumbs = st.crumbs - it.price, items = st.items + id) }
        true
    }

    suspend fun buyEgg(): Boolean = lock.withLock {
        val s = settings.now(); if (s.crumbs < Shop.EGG) return@withLock false
        val sp = Engine.anyEgg(dao.petList().map { it.species }.toSet(), clock().toInt() + s.crumbs) ?: return@withLock false
        settings.update { it.copy(crumbs = it.crumbs - Shop.EGG) }
        lay(listOf(sp), ":fair"); true
    }

    suspend fun setSlot(p: Pet, slot: Int) = lock.withLock {
        val pets = dao.petList()
        if (slot >= 0) pets.firstOrNull { it.slot == slot && it.id != p.id }?.let { dao.update(it.copy(slot = p.slot)) }
        dao.update(dao.petList().first { it.id == p.id }.copy(slot = slot))
    }

    suspend fun weeklyReview(monday: Long) = lock.withLock {
        val s = settings.now(); if (s.reviewed >= monday) return@withLock
        settings.update { it.copy(reviewed = monday) }
        val pets = dao.petList(); crumbs(Engine.bonus(Engine.REVIEW, Perk.REVIEW, pets)); dao.updatePets(Engine.cheer(pets, 4, "owl"))
        afterAct()
    }

    // ---------- the day ----------

    /** After any act: finds, then quests. */
    private suspend fun afterAct() {
        val s = settings.now()
        val txs = dao.txList(); val moves = dao.moveList(); val wishes = dao.wishList(); val bills = dao.billList()
        val owned = dao.petList().map { it.species }.toSet()
        lay(Engine.finds(Engine.Life(txs, moves, wishes, bills, s, dao.heardList().count { it.state == 1 }), owned))
        val t = clock(); val monday = weekStart(t)
        val period = Period.of(t, s.payday)
        val facts = WeekFacts(monday, txs, moves, wishes, bills, s.snow, dao.accountList(), s.budget, period.days)
        facts.worth = txs.count { it.worth in 1..3 && it.at >= java.time.LocalDate.ofEpochDay(monday).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
        val ids = if (s.questWeek == monday) s.quests else Quests.deal(monday, dao.debtList().any { it.done == 0L })
        val doneBefore = if (s.questWeek == monday) s.questsDone else emptySet()
        val newly = ids.mapNotNull { Quests.byId[it] }.filter { it.id !in doneBefore && it.progress(facts) >= it.need }
        if (s.questWeek != monday || newly.isNotEmpty()) {
            settings.update { it.copy(questWeek = monday, quests = ids, questsDone = doneBefore + newly.map { q -> q.id }, inbox = it.inbox + newly.map { q -> "quest:${q.id}" }) }
            val pets = dao.petList(); newly.forEach { crumbs(Engine.bonus(it.crumbs, Perk.LONG, pets)) }
        }
    }

    /** Daily housekeeping: regular payments, a new period (and how the winter went), quests. */
    suspend fun tick() = lock.withLock {
        postBills()
        val s = settings.now(); val t = clock()
        val p = Period.of(t, s.payday)
        if (s.period != 0L && s.period < p.start) {
            // one or more periods ended: judge the last one that had any records
            val last = Period.of(p.start - 1, s.payday)
            val spent = dao.txList().filter { it.kind == Tx.SPEND && last.has(it.day) }.sumOf { it.amount }
            val any = dao.txList().any { last.has(it.day) }
            if (any) {
                val ok = s.budget > 0 && spent <= s.budget
                settings.update { it.copy(winters = it.winters + 1, won = if (ok) it.won + 1 else 0, inbox = it.inbox + "${if (ok) "won" else "hard"}:${last.start}") }
                crumbs(Engine.bonus(if (ok) Engine.WINTER else Engine.WINTER_HARD, Perk.STASH, dao.petList()))
                dao.updatePets(Engine.cheer(dao.petList(), if (ok) 12 else 4, "marmot"))
            }
            settings.update { it.copy(period = p.start) }
        } else if (s.period == 0L) settings.update { it.copy(period = p.start) }
        afterAct()
    }

    /** Fetches the weather for the window when it is older than half an hour; guesses the town from the time zone once. */
    suspend fun refreshOutside(force: Boolean = false) = kotlinx.coroutines.withContext(Dispatchers.IO) { runCatching {
        var s = settings.now()
        if (s.lat.isNaN() && !s.guessed) {
            val t = OutsideSource.guess(Lang.ru)
            settings.update { it.copy(guessed = true, town = t?.name ?: it.town, lat = t?.lat ?: it.lat, lon = t?.lon ?: it.lon) }
            s = settings.now()
        }
        if (s.lat.isNaN()) return@runCatching
        val old = Outside.of(s.outside)
        if (!force && old != null && System.currentTimeMillis() - old.at < 30 * 60_000L) return@runCatching
        val o = OutsideSource.fetch(s.lat, s.lon)
        settings.update { it.copy(outside = o.json()) }
    } }

    suspend fun pets() = dao.petList()
    fun stageOf(p: Pet) = p.stage()
}

class PennylingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Lang.ru = resources.configuration.locales[0].language == "ru"
        container = AppContainer(this)
        container.scope.launch { Money.set(container.settings.now().currency) }
    }
}

val Context.container: AppContainer get() = (applicationContext as PennylingApp).container
