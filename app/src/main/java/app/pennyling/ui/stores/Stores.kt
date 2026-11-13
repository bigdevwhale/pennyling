package app.pennyling.ui.stores

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.data.Account
import app.pennyling.data.Bill
import app.pennyling.data.Debt
import app.pennyling.data.Goal
import app.pennyling.data.Tx
import app.pennyling.data.Wish
import app.pennyling.domain.Bills
import app.pennyling.domain.Notice
import app.pennyling.domain.Span
import app.pennyling.domain.Stats
import app.pennyling.domain.count
import app.pennyling.domain.fmt1
import app.pennyling.domain.fmtDay
import app.pennyling.domain.fmtDayShort
import app.pennyling.domain.money
import app.pennyling.domain.parseMoney
import app.pennyling.domain.plainMoney
import app.pennyling.domain.plural
import app.pennyling.domain.tr
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.Screen
import app.pennyling.ui.art.Creature
import app.pennyling.ui.art.Mini
import app.pennyling.ui.art.Pose
import app.pennyling.ui.art.Prop
import app.pennyling.ui.components.Btn
import app.pennyling.ui.components.Card
import app.pennyling.ui.components.CatMark
import app.pennyling.ui.components.Chip
import app.pennyling.ui.components.Empty
import app.pennyling.ui.components.Field
import app.pennyling.ui.components.Ghost
import app.pennyling.ui.components.Hint
import app.pennyling.ui.components.Icons
import app.pennyling.ui.components.Meter
import app.pennyling.ui.components.Page
import app.pennyling.ui.components.Popup
import app.pennyling.ui.components.Row2
import app.pennyling.ui.components.Section
import app.pennyling.ui.components.Segments
import app.pennyling.ui.components.catColor
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import java.time.LocalDate
import kotlin.math.max

/** A labelled money field. */
@Composable
fun MoneyField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        Field(value, onChange, money(0), number = true)
    }
}

@Composable
fun TextField(label: String, value: String, onChange: (String) -> Unit, hint: String = "") {
    Text(label, style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
    Field(value, onChange, hint)
}

// ---------------- jars ----------------

@Composable
fun GoalsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var edit by remember { mutableStateOf<Goal?>(null) }
    var put by remember { mutableStateOf<Pair<Goal, Int>?>(null) }
    Page(tr("Банки", "Jars"), { nav.pop() }, action = { Text(tr("Новая", "New"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { edit = Goal(name = "", target = 0, created = d.today) }.padding(10.dp)) }) {
        val list = d.goals.filter { !it.archived }.sortedBy { if (it.done > 0) 1 else 0 }
        if (list.isEmpty()) Empty(tr("На что копим? Нажмите «Новая».", "What are we saving for? Tap «New»."))
        list.forEach { g ->
            val saved = d.saved(g); val f = saved.toFloat() / g.target.coerceAtLeast(1)
            Card(Modifier.padding(bottom = 10.dp), onClick = { edit = g }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Prop(when { g.done > 0 -> "jar2"; f < .34f -> "jar0"; else -> "jar1" }, 64.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, style = T.h3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${money(saved)} ${tr("из", "of")} ${money(g.target)}", style = T.small)
                        Meter(f, Modifier.padding(top = 6.dp), color = if (g.done > 0) C.ok else C.acc)
                        if (g.done > 0) Text(tr("Полна с ${fmtDay(g.done)}", "Full since ${fmtDay(g.done)}"), style = T.tiny.copy(color = C.ok))
                        else if (g.until > d.today) { val months = max(1.0, (g.until - d.today) / 30.4)
                            Text(tr("к ${fmtDayShort(g.until)} – по ${money(((g.target - saved) / months).toLong())} в месяц", "by ${fmtDayShort(g.until)} – ${money(((g.target - saved) / months).toLong())} a month"), style = T.tiny) }
                    }
                }
                if (g.done == 0L) Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(tr("Положить", "Put in"), Modifier.weight(1f), small = true) { put = g to 1 }
                    if (saved > 0) Ghost(tr("Взять", "Take out"), Modifier.weight(1f), small = true) { put = g to -1 }
                }
            }
        }
        val done = d.goals.count { it.done > 0 }
        if (done > 0) Text(tr("Наполнено банок: $done", "Jars filled: $done"), style = T.small, modifier = Modifier.padding(top = 6.dp))
    }
    edit?.let { g -> var name by remember(g) { mutableStateOf(g.name) }; var target by remember(g) { mutableStateOf(if (g.target > 0) plainMoney(g.target) else "") }
        var until by remember(g) { mutableLongStateOf(g.until) }
        Popup({ edit = null }) {
            Text(if (g.id == 0L) tr("Новая банка", "A new jar") else g.name, style = T.h2)
            TextField(tr("На что", "For what"), name, { name = it.take(40) }, tr("Велосипед, отпуск, подушка…", "A bike, a trip, a cushion…"))
            MoneyField(tr("Сколько нужно", "How much"), target, { target = it })
            Text(tr("К какому сроку", "By when"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0L to tr("Без срока", "No date"), 3L to tr("3 мес.", "3 mo"), 6L to tr("полгода", "6 mo"), 12L to tr("год", "a year")).forEach { (m, l) ->
                    val day = if (m == 0L) 0L else LocalDate.ofEpochDay(d.today).plusMonths(m).toEpochDay()
                    Chip(l, if (m == 0L) until == 0L else kotlin.math.abs(until - day) < 3) { until = day } }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (g.id != 0L) Ghost(tr("Удалить", "Delete"), Modifier.weight(1f), color = C.bad) { c.write { deleteGoal(g.id) }; edit = null }
                Btn(tr("Сохранить", "Save"), Modifier.weight(1f), enabled = name.isNotBlank() && (parseMoney(target) ?: 0) > 0) {
                    c.write { saveGoal(g.copy(name = name.trim(), target = parseMoney(target) ?: 0, until = until)) }; edit = null }
            }
        } }
    put?.let { (g, sign) -> var v by remember { mutableStateOf("") }
        Popup({ put = null }) {
            Text(if (sign > 0) tr("Положить в «${g.name}»", "Put into «${g.name}»") else tr("Взять из «${g.name}»", "Take from «${g.name}»"), style = T.h2)
            MoneyField(tr("Сумма", "Amount"), v, { v = it })
            val left = g.target - d.saved(g)
            if (sign > 0 && left > 0) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(left / 10, left / 4, left).filter { it > 0 }.distinct().forEach { a -> Chip(money(a), false) { v = plainMoney(a) } } }
            Btn(tr("Готово", "Done"), Modifier.fillMaxWidth().padding(top = 16.dp), enabled = (parseMoney(v) ?: 0) > 0) {
                c.write { deposit(g, (parseMoney(v) ?: 0) * sign) }; put = null }
        } }
}

// ---------------- the attic ----------------

@Composable
fun WishesPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var add by remember { mutableStateOf(false) }
    Page(tr("Чердак желаний", "The attic of wishes"), { nav.pop() }, action = { Text(tr("Желание", "Wish"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { add = true }.padding(10.dp)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Creature("fox", app.pennyling.domain.Stage.ADULT, Pose.WORK, 92.dp)
            Text(tr("Хочется купить? Поднимите желание на чердак. Лиса посторожит его, пока оно остывает. Если через месяц всё ещё хочется – покупайте с чистой совестью.",
                "Want to buy something? Take the wish up to the attic. The fox guards it while it cools. If you still want it in a month – buy it with a clear conscience."), style = T.small, modifier = Modifier.weight(1f).padding(start = 8.dp))
        }
        val waiting = d.wishes.filter { it.state == 0 }
        Section(tr("Остывают", "Cooling"))
        if (waiting.isEmpty()) Empty(tr("Чердак пуст", "The attic is empty"))
        waiting.forEach { w ->
            val past = (d.today - w.added).toInt(); val ready = past >= w.days
            Card(Modifier.padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Prop(if (ready) "gift" else "snowflake", 44.dp); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.name, style = T.h3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(money(w.price) + if (d.s.hourly > 0) tr(" · ${fmt1(w.price.toDouble() / d.s.hourly)} ч работы", " · ${fmt1(w.price.toDouble() / d.s.hourly)} h of work") else "", style = T.small)
                    }
                }
                Meter(past.toFloat() / w.days, Modifier.padding(top = 8.dp), color = if (ready) C.ok else C.snow)
                Text(if (ready) tr("Остыло. Всё ещё хочется?", "Cooled. Still want it?") else tr("Ещё ${w.days - past} ${plural((w.days - past).toLong(), "день", "дня", "дней")}", "${w.days - past} more days"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Ghost(tr("Расхотелось", "Let it go"), Modifier.weight(1f), small = true) { c.write { decideWish(w, 2, null); say(tr("Не потрачено: ${money(w.price)}. Лиса довольна", "Not spent: ${money(w.price)}. The fox is pleased")) } }
                    Btn(if (ready) tr("Купить", "Buy") else tr("Купить сейчас", "Buy now"), Modifier.weight(1f), small = true, color = if (ready) C.acc else C.card2, ink = if (ready) C.accInk else C.ink) {
                        c.write { decideWish(w, 1, null) }; nav.push(Screen.Add(amount = w.price, note = w.name)) }
                }
            }
        }
        val let = d.wishes.filter { it.state == 2 }; val bought = d.wishes.filter { it.state == 1 }
        if (let.isNotEmpty() || bought.isNotEmpty()) {
            Section(tr("Решено", "Decided"))
            Card {
                Row2(tr("Не потрачено", "Not spent"), count(let.size, "желание отпущено", "желания отпущено", "желаний отпущено", "wish let go", "wishes let go"), money(let.sumOf { it.price }))
                Row2(tr("Куплено после раздумий", "Bought after thinking"), count(bought.size, "покупка", "покупки", "покупок", "purchase", "purchases"), money(bought.sumOf { it.price }))
                (let + bought).sortedByDescending { it.decided }.take(8).forEach { w -> Row2(w.name, if (w.state == 2) tr("отпущено", "let go") else tr("куплено", "bought"), money(w.price)) }
            }
        }
    }
    if (add) { var name by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var days by remember { mutableIntStateOf(d.s.coolDays) }
        Popup({ add = false }) {
            Text(tr("Поднять на чердак", "Up to the attic"), style = T.h2)
            TextField(tr("Что хочется", "What you want"), name, { name = it.take(40) }, tr("Наушники, куртка…", "Headphones, a jacket…"))
            MoneyField(tr("Сколько стоит", "Price"), price, { price = it })
            Text(tr("Сколько остывать", "Cool for"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(3, 7, 14, 30, 60).forEach { n -> Chip(tr("$n дн.", "$n d"), n == days) { days = n } } }
            Btn(tr("Поднять", "Take up"), Modifier.fillMaxWidth().padding(top = 16.dp), enabled = name.isNotBlank() && (parseMoney(price) ?: 0) > 0) {
                c.write { addWish(Wish(name = name.trim(), price = parseMoney(price) ?: 0, added = d.today, days = days)) }; add = false }
        } }
}

// ---------------- the cellar ----------------

/** Spends that repeat like a subscription: the same place, about the same sum, about a month apart, three times or more. */
fun repeating(d: Data): List<Triple<String, Long, Long>> {
    val known = d.bills.map { Notice.key(it.title) }.toSet()
    return d.txs.filter { it.kind == Tx.SPEND && it.billId == 0L && it.note.isNotBlank() && d.today - it.day < 200 }.groupBy { Notice.key(it.note) }
        .filter { (k, g) -> k.isNotBlank() && k !in known && g.size >= 3 }
        .mapNotNull { (_, g) -> val s = g.sortedBy { it.day }
            val gaps = s.zipWithNext { a, b -> b.day - a.day }
            val amounts = s.map { it.amount }
            if (gaps.all { it in 25..35 } && amounts.max() - amounts.min() <= amounts.max() / 10) Triple(s.last().note.trim(), s.last().amount, s.last().catId) else null }
}

@Composable
fun CellarPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var edit by remember { mutableStateOf<Bill?>(null) }
    Page(tr("Погреб", "The cellar"), { nav.pop() }, action = { Text(tr("Добавить", "Add"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { edit = Bill(title = "", amount = 0, next = d.today, sub = true) }.padding(10.dp)) }) {
        val subs = d.bills.filter { it.sub && !it.income }; val other = d.bills.filter { !it.sub }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Creature("bat", app.pennyling.domain.Stage.ADULT, Pose.WORK, 88.dp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(money(subs.filter { it.active }.sumOf { Bills.yearly(it) }), style = T.title)
                Text(tr("в год уходит на подписки – по бочке на каждую", "a year goes on subscriptions – a barrel each"), style = T.small)
            }
        }
        Text(tr("Регулярные платежи записываются сами в свой день и заранее откладываются из пайка.", "Regular payments log themselves on their day and are kept aside from the ration in advance."), style = T.small, modifier = Modifier.padding(top = 8.dp))
        val stale = subs.filter { it.active && d.today - it.checked > 90 }
        stale.firstOrNull()?.let { b -> Card(Modifier.padding(top = 12.dp)) {
            Text(tr("Ревизия погреба", "Cellar review"), style = T.sec)
            Text(tr("«${b.title}» – ${money(Bills.yearly(b))} в год. Пользуетесь?", "«${b.title}» – ${money(Bills.yearly(b))} a year. Do you use it?"), style = T.bodyB, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Btn(tr("Нужна", "Keep"), Modifier.weight(1f), small = true, color = C.ok) { c.write { review(b, 1) } }
                Ghost(tr("Сомневаюсь", "Not sure"), Modifier.weight(1f), small = true) { c.write { review(b, 2) } }
                Ghost(tr("Отписываюсь", "Cancelled"), Modifier.weight(1f), small = true, color = C.bad) { c.write { review(b, 3); say(tr("Одной бочкой меньше: ${money(Bills.yearly(b))} в год останутся в кладовой", "One barrel less: ${money(Bills.yearly(b))} a year stays in the pantry")) } }
            }
            if (stale.size > 1) Text(tr("Ещё ${stale.size - 1} на проверку", "${stale.size - 1} more to check"), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        } }
        val rep = repeating(d)
        if (rep.isNotEmpty()) {
            Section(tr("Сорока нашла", "The magpie found"))
            rep.forEach { (n, a, cid) -> Card(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mini("magpie", 40.dp); Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) { Text(n, style = T.bodyB); Text(tr("${money(a)} каждый месяц – похоже на подписку", "${money(a)} every month – looks like a subscription"), style = T.small) }
                    Text(tr("В погреб", "Add"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable {
                        val last = d.txs.filter { Notice.key(it.note) == Notice.key(n) }.maxOf { it.day }
                        c.write { saveBill(Bill(title = n, amount = a, catId = cid, accountId = d.accounts.firstOrNull()?.id ?: 0, next = LocalDate.ofEpochDay(last).plusMonths(1).toEpochDay(), sub = true)) } }.padding(8.dp))
                }
            } }
        }
        Section(tr("Подписки", "Subscriptions"))
        if (subs.isEmpty()) Empty(tr("Подписок нет – или они ещё не найдены", "No subscriptions – or not found yet"))
        Card { subs.forEach { b -> BillRow(b, d) { edit = b } } }
        Section(tr("Другие регулярные", "Other regular ones"))
        if (other.isEmpty()) Empty(tr("Аренда, коммуналка, зарплата – всё, что повторяется", "Rent, utilities, salary – anything that repeats"))
        else Card { other.forEach { b -> BillRow(b, d) { edit = b } } }
    }
    edit?.let { b -> BillEditor(b, d) { edit = null } }
}

@Composable
private fun BillRow(b: Bill, d: Data, onClick: () -> Unit) {
    val every = everyLabel(b.every)
    Row2(b.title, if (!b.active) tr("остановлен", "stopped") else tr("следующий ${fmtDayShort(b.next)}", "next ${fmtDayShort(b.next)}") + if (b.keep == 2) tr(" · под вопросом", " · in doubt") else "",
        (if (b.income) "+" else "") + money(b.amount) + " " + every, lead = { if (b.sub) Prop("barrel", 36.dp) else CatMark(d.catById[b.catId], 36.dp, d.pets) }, onClick = onClick)
}

/** "a month", "a week"… or "every 2 months", "every 10 days". */
fun everyLabel(code: String): String {
    val e = Bills.every(code)
    val n = e.n.toLong()
    if (e.n == 1) return when (e.unit) { 'd' -> tr("в день", "a day"); 'w' -> tr("в неделю", "a week"); 'y' -> tr("в год", "a year"); else -> tr("в месяц", "a month") }
    return when (e.unit) {
        'd' -> tr("раз в $n ${plural(n, "день", "дня", "дней")}", "every $n days")
        'w' -> tr("раз в $n ${plural(n, "неделю", "недели", "недель")}", "every $n weeks")
        'y' -> tr("раз в $n ${plural(n, "год", "года", "лет")}", "every $n years")
        else -> tr("раз в $n ${plural(n, "месяц", "месяца", "месяцев")}", "every $n months")
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BillEditor(b: Bill, d: Data, onClose: () -> Unit) {
    val c = LocalContext.current.container
    var title by remember(b) { mutableStateOf(b.title) }; var amount by remember(b) { mutableStateOf(if (b.amount > 0) plainMoney(b.amount) else "") }
    var every by remember(b) { mutableStateOf(b.every) }; var sub by remember(b) { mutableStateOf(b.sub) }; var income by remember(b) { mutableStateOf(b.income) }
    var next by remember(b) { mutableLongStateOf(b.next) }; var cat by remember(b) { mutableLongStateOf(b.catId) }; var acc by remember(b) { mutableLongStateOf(if (b.accountId != 0L) b.accountId else d.accounts.firstOrNull()?.id ?: 0) }
    var active by remember(b) { mutableStateOf(b.active) }
    Popup(onClose) {
        Text(if (b.id == 0L) tr("Регулярный платёж", "A regular payment") else b.title, style = T.h2)
        Segments(listOf(tr("Подписка", "Subscription"), tr("Платёж", "Bill"), tr("Доход", "Income")), if (income) 2 else if (sub) 0 else 1, Modifier.padding(top = 10.dp)) { sub = it == 0; income = it == 2 }
        TextField(tr("Название", "Name"), title, { title = it.take(40) }, tr("Кино, аренда, спортзал…", "Streaming, rent, gym…"))
        MoneyField(tr("Сумма", "Amount"), amount, { amount = it })
        Text(tr("Как часто", "How often"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        // Wraps on narrow screens (#1); "own" opens any period: every N days, weeks, months or years.
        val presets = listOf(Bill.MONTH to tr("Каждый месяц", "Monthly"), Bill.WEEK to tr("Каждую неделю", "Weekly"), Bill.YEAR to tr("Каждый год", "Yearly"))
        var own by remember(b) { mutableStateOf(presets.none { it.first == b.every }) }
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { (k, l) -> Chip(l, !own && every == k) { own = false; every = k } }
            Chip(tr("Свой период", "Custom"), own) {
                if (!own) { own = true; val e = Bills.every(every); every = Bills.Every(maxOf(2, e.n), e.unit).code }
            }
        }
        if (own) {
            val e = Bills.every(every)
            val n = e.n.toLong()
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Раз в", "Every"), style = T.body)
                Ghost("−", Modifier.padding(start = 8.dp), small = true) { every = e.copy(n = (e.n - 1).coerceAtLeast(1)).code }
                Text(e.n.toString(), style = T.bodyB, modifier = Modifier.padding(horizontal = 10.dp))
                Ghost("+", small = true) { every = e.copy(n = (e.n + 1).coerceAtMost(999)).code }
            }
            androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    'd' to tr(plural(n, "день", "дня", "дней"), if (n == 1L) "day" else "days"),
                    'w' to tr(plural(n, "неделю", "недели", "недель"), if (n == 1L) "week" else "weeks"),
                    'm' to tr(plural(n, "месяц", "месяца", "месяцев"), if (n == 1L) "month" else "months"),
                    'y' to tr(plural(n, "год", "года", "лет"), if (n == 1L) "year" else "years"),
                ).forEach { (u, l) -> Chip(l, e.unit == u) { every = e.copy(unit = u).code } }
            }
        }
        Text(tr("Следующий раз", "Next time"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ghost("−", small = true) { next-- }; Text(fmtDay(next), style = T.bodyB, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)); Ghost("+", small = true) { next++ }
        }
        Text(tr("Категория", "Category"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        val cats = d.activeCats.filter { it.income == income }
        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cats.forEach { ct -> Box(Modifier.clip(RoundedCornerShape(50)).background(if (ct.id == cat) catColor(ct).copy(alpha = .25f) else C.card2).clickable { cat = ct.id }.padding(3.dp)) { CatMark(ct, 36.dp, d.pets) } }
        }
        Text(d.catById[cat]?.name ?: tr("не выбрана", "not chosen"), style = T.tiny, modifier = Modifier.padding(top = 2.dp))
        Text(tr("Счёт", "Account"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        app.pennyling.ui.journal.AccountChips(d, acc) { acc = it }
        if (b.id != 0L) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Switch(active, { active = it }); Spacer(Modifier.width(8.dp)); Text(tr("Действует", "Active"), style = T.body) }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (b.id != 0L) Ghost(tr("Удалить", "Delete"), Modifier.weight(1f), color = C.bad) { c.write { deleteBill(b.id) }; onClose() }
            Btn(tr("Сохранить", "Save"), Modifier.weight(1f), enabled = title.isNotBlank() && (parseMoney(amount) ?: 0) > 0 && cat != 0L) {
                c.write { saveBill(b.copy(title = title.trim(), amount = parseMoney(amount) ?: 0, every = every, sub = sub && !income, income = income, next = next, catId = cat, accountId = acc, active = active)) }; onClose() }
        }
    }
}


// ---------------- boulders ----------------

/** Months and interest to clear all debts paying [budget] a month, smallest first (snowball) or dearest first (avalanche). */
fun plan(debts: List<Pair<Debt, Long>>, budget: Long, avalanche: Boolean): Pair<Int, Long>? {
    if (debts.isEmpty() || budget <= 0) return null
    val left = debts.map { it.second.toDouble() }.toMutableList()
    val order = debts.indices.sortedWith(if (avalanche) compareByDescending { debts[it].first.rate } else compareBy { debts[it].second })
    var months = 0; var interest = 0.0
    while (left.any { it > 0.5 } && months < 600) {
        months++
        for (i in left.indices) if (left[i] > 0) { val add = left[i] * debts[i].first.rate / 100 / 12; interest += add; left[i] += add }
        var money = budget.toDouble()
        for (i in left.indices) if (left[i] > 0) { val m = minOf(left[i], debts[i].first.minPay.toDouble()); left[i] -= m; money -= m }
        for (i in order) if (left[i] > 0 && money > 0) { val m = minOf(left[i], money); left[i] -= m; money -= m }
        if (money >= budget.toDouble() - .5 && left.any { it > 0.5 }) return null
    }
    return if (months >= 600) null else months to interest.toLong()
}

@Composable
fun DebtsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var edit by remember { mutableStateOf<Debt?>(null) }
    var pay by remember { mutableStateOf<Debt?>(null) }
    var extra by remember { mutableStateOf("") }
    Page(tr("Валуны у входа", "Boulders at the door"), { nav.pop() }, action = { Text(tr("Долг", "Debt"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { edit = Debt(name = "", total = 0, created = d.today) }.padding(10.dp)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Creature("badger", app.pennyling.domain.Stage.ADULT, Pose.WORK, 92.dp)
            Text(tr("Каждый долг – валун у входа. Барсук откатывает его платёж за платежом. Отмечайте здесь каждый платёж, и валун будет уменьшаться.",
                "Each debt is a boulder at the door. The badger rolls it away payment by payment. Mark here what you paid and the boulder shrinks."), style = T.small, modifier = Modifier.weight(1f).padding(start = 8.dp))
        }
        val open = d.debts.filter { it.done == 0L }
        if (open.isEmpty()) Empty(tr("Вход свободен – долгов нет", "The way is clear – no debts"))
        open.forEach { db -> val paid = d.paid(db); val left = (db.total - paid).coerceAtLeast(0)
            Card(Modifier.padding(top = 10.dp), onClick = { edit = db }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Prop("boulder", (36 + 40 * left.toFloat() / db.total.coerceAtLeast(1)).dp); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(db.name, style = T.h3); Text(tr("осталось ${money(left)} из ${money(db.total)}", "${money(left)} left of ${money(db.total)}"), style = T.small)
                        if (db.rate > 0 || db.minPay > 0) Text(listOfNotNull(db.rate.takeIf { it > 0 }?.let { "${fmt1(it)}%" }, db.minPay.takeIf { it > 0 }?.let { tr("мин. ${money(it)}", "min ${money(it)}") }).joinToString(" · "), style = T.tiny)
                    }
                }
                Meter(paid.toFloat() / db.total.coerceAtLeast(1), Modifier.padding(top = 8.dp), color = C.ok)
                Btn(tr("Внести платёж", "Make a payment"), Modifier.fillMaxWidth().padding(top = 10.dp), small = true) { pay = db }
            } }
        if (open.size >= 2) {
            Section(tr("Как гасить быстрее", "How to roll them"))
            Card {
                val mins = open.sumOf { it.minPay }
                MoneyField(tr("Сколько в месяц на все долги", "A month for all debts"), extra.ifEmpty { if (mins > 0) plainMoney(mins) else "" }, { extra = it })
                val per = parseMoney(extra) ?: mins
                val list = open.map { it to (it.total - d.paid(it)).coerceAtLeast(0) }
                val snow = plan(list, per, false); val ava = plan(list, per, true)
                Row2(tr("«Снежный ком»", "«Snowball»"), tr("сначала самый маленький", "smallest first"), snow?.let { tr("${it.first} мес., переплата ${money(it.second)}", "${it.first} mo, interest ${money(it.second)}") } ?: "–")
                Row2(tr("«Лавина»", "«Avalanche»"), tr("сначала самый дорогой", "dearest first"), ava?.let { tr("${it.first} мес., переплата ${money(it.second)}", "${it.first} mo, interest ${money(it.second)}") } ?: "–")
                if (snow == null || ava == null) Text(tr("Этой суммы не хватает даже на проценты – попробуйте больше", "This doesn't cover even the interest – try more"), style = T.small.copy(color = C.bad))
                else Text(tr("Снежный ком быстрее радует, лавина дешевле обходится. Выбирайте, что вам ближе.", "The snowball cheers you sooner, the avalanche costs less. Pick what suits you."), style = T.small, modifier = Modifier.padding(top = 6.dp))
            }
        }
        val closed = d.debts.filter { it.done > 0 }
        if (closed.isNotEmpty()) { Section(tr("Откачены", "Rolled away")); Card { closed.forEach { Row2(it.name, fmtDay(it.done), money(it.total)) } } }
    }
    edit?.let { db -> var name by remember(db) { mutableStateOf(db.name) }; var total by remember(db) { mutableStateOf(if (db.total > 0) plainMoney(db.total) else "") }
        var rate by remember(db) { mutableStateOf(if (db.rate > 0) fmt1(db.rate) else "") }; var min by remember(db) { mutableStateOf(if (db.minPay > 0) plainMoney(db.minPay) else "") }
        Popup({ edit = null }) {
            Text(if (db.id == 0L) tr("Новый валун", "A new boulder") else db.name, style = T.h2)
            TextField(tr("Кому или за что", "To whom or what"), name, { name = it.take(40) }, tr("Кредитка, друг, рассрочка…", "Credit card, a friend, instalments…"))
            MoneyField(tr("Сумма долга", "The debt"), total, { total = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { Text(tr("Ставка, % годовых", "Rate, % a year"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)); Field(rate, { rate = it }, "0", number = true) }
                MoneyField(tr("Мин. платёж", "Min payment"), min, { min = it }, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (db.id != 0L) Ghost(tr("Удалить", "Delete"), Modifier.weight(1f), color = C.bad) { c.write { deleteDebt(db.id) }; edit = null }
                Btn(tr("Сохранить", "Save"), Modifier.weight(1f), enabled = name.isNotBlank() && (parseMoney(total) ?: 0) > 0) {
                    c.write { saveDebt(db.copy(name = name.trim(), total = parseMoney(total) ?: 0, rate = rate.replace(',', '.').toDoubleOrNull() ?: 0.0, minPay = parseMoney(min) ?: 0)) }; edit = null }
            }
        } }
    pay?.let { db -> var v by remember { mutableStateOf(if (db.minPay > 0) plainMoney(db.minPay) else "") }
        Popup({ pay = null }) {
            Text(tr("Платёж: ${db.name}", "Payment: ${db.name}"), style = T.h2)
            MoneyField(tr("Сколько внесли", "How much"), v, { v = it })
            Text(tr("Если платили с карты, запишите платёж ещё и как трату – так паёк будет точнее.", "If you paid by card, log it as a spend too – keeps the ration honest."), style = T.small, modifier = Modifier.padding(top = 8.dp))
            Btn(tr("Откатить", "Roll it"), Modifier.fillMaxWidth().padding(top = 14.dp), enabled = (parseMoney(v) ?: 0) > 0) { c.write { payDebt(db, parseMoney(v) ?: 0) }; pay = null }
        } }
}

// ---------------- the dam ----------------

@Composable
fun DamPage(d: Data, nav: Nav) {
    val st = Stats(d.txs, Span(d.first, d.today, ""), d.catById, d.accounts, d.bills, d.s.budget, d.s.payday)
    val saves = d.accounts.filter { it.kind == Account.SAVE && !it.archived }
    val held = saves.sumOf { st.balance(it) }
    val month = max(st.monthlySpend, 1)
    val months = held.toDouble() / month
    Page(tr("Плотина", "The dam"), { nav.pop() }) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.snow.copy(alpha = .18f)).padding(14.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Creature("beaver", app.pennyling.domain.Stage.ADULT, if (months >= 3) Pose.CHEER else Pose.WORK, 140.dp)
                Text(tr("${fmt1(months)} мес.", "${fmt1(months)} months"), style = T.big)
                Text(tr("столько продержится норка без дохода", "the burrow would last without income"), style = T.small)
            }
        }
        Meter((months / 6).toFloat(), Modifier.padding(top = 14.dp), color = if (months >= 3) C.ok else C.acc, h = 12.dp)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) { Text("0", style = T.tiny, modifier = Modifier.weight(1f)); Text(tr("3 мес.", "3 mo"), style = T.tiny, modifier = Modifier.weight(1f)); Text(tr("6 мес.", "6 mo"), style = T.tiny) }
        Card(Modifier.padding(top = 14.dp)) {
            Row2(tr("В плотине", "Held by the dam"), tr("на сберегательных счетах", "in savings accounts"), app.pennyling.domain.moneyR(held))
            Row2(tr("Месяц жизни", "A month of life"), tr("средние траты за 3 месяца", "average spending over 3 months"), app.pennyling.domain.moneyR(st.monthlySpend))
            if (months < 3) Row2(tr("До трёх месяцев", "To three months"), value = app.pennyling.domain.moneyR((month * 3 - held).coerceAtLeast(0)))
            else if (months < 6) Row2(tr("До шести месяцев", "To six months"), value = app.pennyling.domain.moneyR((month * 6 - held).coerceAtLeast(0)))
        }
        Text(tr("Бобр советует: подушка на 3–6 месяцев трат превращает любую бурю в обычный дождь. Плотину складывают сберегательные счета: отметьте их в «Счетах» типом «Копилка».",
            "The beaver's advice: a cushion of 3–6 months of spending turns any storm into mere rain. The dam is held by savings accounts – mark them as «Savings» in Accounts."), style = T.small, modifier = Modifier.padding(top = 12.dp))
        if (saves.isEmpty()) Btn(tr("Открыть счета", "Open accounts"), Modifier.fillMaxWidth().padding(top = 12.dp)) { nav.push(Screen.Accounts) }
    }
}
