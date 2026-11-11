package app.pennyling.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pennyling.data.Tx
import app.pennyling.domain.CatSum
import app.pennyling.domain.Period
import app.pennyling.domain.SpanKind
import app.pennyling.domain.Spans
import app.pennyling.domain.Stats
import app.pennyling.domain.count
import app.pennyling.domain.dayShort
import app.pennyling.domain.fmt
import app.pennyling.domain.fmt1
import app.pennyling.domain.fmtDayShort
import app.pennyling.domain.money
import app.pennyling.domain.moneyK
import app.pennyling.domain.moneyR
import app.pennyling.domain.today
import app.pennyling.domain.tr
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.Screen
import app.pennyling.ui.art.Mini
import app.pennyling.ui.components.Bars
import app.pennyling.ui.components.Card
import app.pennyling.ui.components.CatMark
import app.pennyling.ui.components.ChipRow
import app.pennyling.ui.components.Empty
import app.pennyling.ui.components.Fold
import app.pennyling.ui.components.Heat
import app.pennyling.ui.components.Icon
import app.pennyling.ui.components.Icons
import app.pennyling.ui.components.Line
import app.pennyling.ui.components.Meter
import app.pennyling.ui.components.Row2
import app.pennyling.ui.components.Section
import app.pennyling.ui.components.Split
import app.pennyling.ui.components.Segments
import app.pennyling.ui.components.TileGrid
import app.pennyling.ui.components.catColor
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import kotlin.math.abs
import kotlin.math.roundToInt

private fun delta(now: Long, was: Long): String? {
    if (was <= 0) return null
    val p = ((now - was) * 100.0 / was).roundToInt()
    return if (p >= 0) "+$p% ${tr("к прошлому", "vs before")}" else "−${abs(p)}% ${tr("к прошлому", "vs before")}"
}

@Composable
fun StatsScreen(d: Data, nav: Nav) {
    var kind by rememberSaveable { mutableIntStateOf(0) }
    var back by rememberSaveable { mutableIntStateOf(0) }
    val sk = SpanKind.entries[kind]
    val span = Spans.of(sk, back, d.s.payday, d.first, d.today)
    val st = remember(d, kind, back) { Stats(d.txs, span, d.catById, d.accounts, d.bills, d.s.budget, d.s.payday) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Text(tr("Летопись", "Chronicle"), style = T.title, modifier = Modifier.padding(top = 10.dp))
        ChipRow(listOf(tr("До зарплаты", "Pay period"), tr("Месяц", "Month"), tr("Год", "Year"), tr("Всё время", "All time")), kind, Modifier.padding(top = 8.dp)) { kind = it; back = 0 }
        if (sk != SpanKind.ALL) Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).clickable { back++ }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, C.ink, 20.dp) }
            Text(span.label, style = T.h3, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Box(Modifier.size(36.dp).clip(CircleShape).clickable(enabled = back > 0) { back-- }, contentAlignment = Alignment.Center) { Icon(Icons.NEXT, if (back > 0) C.ink else C.line, 20.dp) }
        }
        Spacer(Modifier.height(10.dp))
        if (st.txs.isEmpty()) { Empty(tr("Здесь пока пусто. Записи появятся – появится и летопись.", "Nothing here yet. Records will bring the chronicle.")); return@Column }
        var view by rememberSaveable { mutableIntStateOf(0) }
        Segments(listOf(tr("Обзор", "Overview"), tr("Категории", "Categories"), tr("Привычки", "Habits"), tr("Капитал", "Wealth")), view, Modifier.padding(bottom = 12.dp)) { view = it }
        val infl = st.inflation()
        when (view) {
        0 -> {
        TileGrid(listOf(
            Triple(moneyR(st.spent), tr("потрачено", "spent"), delta(st.spent, st.spentBefore)),
            Triple(moneyR(st.income), tr("пришло", "came in"), null),
            Triple(if (st.income > 0) "${(st.saveRate * 100).roundToInt()}%" else "–", tr("осталось от дохода", "of income kept"), null),
            Triple(moneyR(st.perDay), tr("в среднем за день", "a day on average"), null),
        ), icons = listOf(Icons.WALLET to C.acc, Icons.COIN to C.ok, Icons.PIGGY to C.gold, Icons.CAL to C.mute))

        if (sk == SpanKind.PERIOD && back == 0 && d.s.budget > 0) {
            val (line, plan) = st.burn()
            Card(Modifier.padding(top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mini("marmot", 40.dp); Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Прогноз сурка", "The marmot's forecast"), style = T.bodyB)
                        val f = d.ration.forecast
                        Text(if (f <= d.s.budget) tr("Если тратить так же, к зарплате уйдёт ${money(f)} и ещё останется ${money(d.s.budget - f)}", "At this pace ${money(f)} goes by payday – the pantry lasts, ${money(d.s.budget - f)} to spare")
                            else tr("Если тратить так же, к зарплате уйдёт ${money(f)} – на ${money(f - d.s.budget)} больше бюджета", "At this pace ${money(f)} goes by payday – ${money(f - d.s.budget)} over the budget"), style = T.small)
                    }
                }
                Burn(line, plan.last().first, plan.last().second, Modifier.padding(top = 10.dp))
                Text(tr("Траты с зарплаты; пунктир – бюджет ${money(d.s.budget)}", "Spending since payday; budget ${money(d.s.budget)}"), style = T.tiny)
            }
        }

        Section(tr("По месяцам", "Month by month"))
        Card {
            val ms = st.months(12)
            Bars(ms.map { it.first to it.second / 100.0 }, color = C.acc, hi = ms.lastIndex, h = 130.dp)
            Split(ms.sumOf { it.second }.toDouble(), ms.sumOf { it.third }.coerceAtLeast(0).toDouble().let { (it - ms.sumOf { m -> m.second }).coerceAtLeast(0.0) },
                tr("потрачено", "spent"), tr("осталось", "kept"), C.acc, C.ok)
            Text(tr("за 12 месяцев: потрачено ${moneyR(ms.sumOf { it.second })}, пришло ${moneyR(ms.sumOf { it.third })}", "12 months: spent ${moneyR(ms.sumOf { it.second })}, came in ${moneyR(ms.sumOf { it.third })}"), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }

        Card(Modifier.padding(top = 14.dp), onClick = { nav.push(Screen.Wrapped(java.time.LocalDate.ofEpochDay(d.today).year)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mini("hamster", 48.dp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(tr("Год в норке", "A year in the burrow"), style = T.bodyB); Text(tr("Итоги года – страница за страницей", "The year's story, page by page"), style = T.small) }
                Icon(Icons.NEXT, C.mute, 20.dp)
            }
        }
        }
        1 -> {
        Section(tr("Куда уходит", "Where it goes"))
        val cats = st.byCat()
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Donut(cats.take(8).map { catColor(it.cat) to it.amount }, 120.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    cats.take(5).forEach { cs -> Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(catColor(cs.cat))); Spacer(Modifier.width(6.dp))
                        Text(cs.cat?.name ?: "–", style = T.small.copy(color = C.ink), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${(cs.share * 100).roundToInt()}%", style = T.smallB) } }
                }
            }
            Spacer(Modifier.height(10.dp))
            cats.forEach { cs -> CatLine(cs, d, sk) { nav.filterCat = cs.cat?.id ?: 0; nav.tab = 1 } }
        }
        val inc = st.byCat(income = true)
        if (inc.isNotEmpty()) Fold(Icons.COIN, C.ok, tr("Откуда приходит", "Where it comes from"), inc.firstOrNull()?.cat?.name) {
            inc.forEach { cs -> Row2(cs.cat?.name ?: "–", "${(cs.share * 100).roundToInt()}%", money(cs.amount), lead = { CatMark(cs.cat, 34.dp, d.pets) }) }
        }

        if (infl.isNotEmpty()) Fold(Icons.UP, C.bad, tr("Личная инфляция", "Personal inflation"), tr("последние 3 месяца и 3 до них", "3 months against the 3 before")) {
            infl.take(8).forEach { (cat, v, ch) -> Row2(cat?.name ?: "–", tr("в месяц ${money(v)}", "${money(v)} a month"), (if (ch >= 0) "+" else "−") + "${abs((ch * 100).roundToInt())}%", lead = { CatMark(cat, 30.dp, d.pets) }) }
        }
        }
        2 -> {
        Section(tr("Календарь трат", "Spending calendar"))
        Card {
            val from = maxOf(minOf(span.from, d.today - 118), d.today - 364)
            Heat(st.byDay.mapValues { (it.value / 100).toInt().coerceAtLeast(1) }, from, minOf(span.to, d.today))
            val zero = (from..minOf(span.to, d.today)).count { it !in st.byDay }
            Text(tr("Дней без трат: $zero, из них снежных: ${d.s.snow.count { it in from..span.to }}", "Days with no spending: $zero, snow days among them: ${d.s.snow.count { it in from..span.to }}"), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }

        Fold(Icons.CAL, C.acc, tr("Дни недели и часы", "Weekdays and hours"), tr("когда тратится больше всего", "when money leaves most easily")) {
            Bars((0 until 7).map { dayShort(it) to st.weekdays[it] / 100.0 }, hi = st.weekdays.indexOf(st.weekdays.max()), h = 110.dp)
            Text(tr("Средняя трата по дням недели", "Average spending by weekday"), style = T.tiny, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            Bars(st.hours.mapIndexed { i, h -> "${i * 3}" to h.second / 100.0 }, color = C.gold, h = 100.dp)
            Text(tr("Траты по времени суток (часы)", "Spending by time of day (hours)"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
            val night = st.hours.drop(7).sumOf { it.second } + st.hours.take(2).sumOf { it.second }
            if (night > 0) Text(tr("После 21:00 и до 6 утра: ${money(night)}", "After 9 pm and before 6 am: ${money(night)}"), style = T.small, modifier = Modifier.padding(top = 6.dp))
        }

        Fold(Icons.HEART, C.ok, tr("Радость на рубль", "Joy per coin"), if (st.worthAll.isEmpty()) tr("ответьте на «Стоило того?»", "answer «Worth it?»") else count(st.worthAll.size, "ответ", "ответа", "ответов", "answer", "answers")) {
            if (st.worthAll.isEmpty()) Text(tr("Через неделю после траты выдра спросит, стоила ли она того. Здесь соберутся ответы.", "A week after a spend the otter asks if it was worth it. The answers gather here."), style = T.small)
            else {
                val y = st.worthSum(1); val m = st.worthSum(2); val n = st.worthSum(3)
                Row2(tr("Стоило", "Worth it"), value = money(y), lead = { Mini("otter", 32.dp) })
                Row2(tr("Так себе", "So-so"), value = money(m))
                Row2(tr("Не стоило", "Not worth it"), value = money(n))
                Text(tr("Больше всего радости", "Most joy"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                st.joyByCat().take(5).forEach { (cat, share, regret) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        CatMark(cat, 30.dp, d.pets); Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) { Text(cat?.name ?: "–", style = T.smallB); Meter(share, color = C.ok, h = 6.dp) }
                        Text("${(share * 100).roundToInt()}%", style = T.smallB, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (n > 0) Text(tr("Сожалений набралось на ${money(n)}. Это не упрёк – это подсказка, на чём можно сэкономить без потерь.", "Regrets add up to ${money(n)}. Not a reproach – a hint where to save without losing anything."), style = T.small, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Fold(Icons.ACORN, C.gold, tr("Мелочи", "Little things"), tr("${st.small.size} мелких трат – ${moneyR(st.small.sumOf { it.amount })}", "${st.small.size} small spends – ${moneyR(st.small.sumOf { it.amount })}")) {
            val sum = st.small.sumOf { it.amount }
            Text(tr("Траты до ${money(st.smallLimit)} по отдельности не заметны, а вместе – ${money(sum)}, или ${if (st.spent > 0) (sum * 100 / st.spent) else 0}% всех трат.",
                "Spends up to ${money(st.smallLimit)}: unnoticed one by one, together ${money(sum)}, ${if (st.spent > 0) (sum * 100 / st.spent) else 0}% of all spending."), style = T.body)
            if (st.lived >= 7) Text(tr("За год в таком темпе: ${moneyR(sum * 365 / st.lived)}", "A year at this pace: ${moneyR(sum * 365 / st.lived)}"), style = T.bodyB, modifier = Modifier.padding(top = 6.dp))
            val ant = d.pets.any { it.species == "ant" && it.hatched > 0 }
            val round = st.spends.sumOf { t -> val r = d.s.roundTo.coerceAtLeast(100); (r - t.amount % r) % r }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Mini("ant", 34.dp, dim = !ant); Spacer(Modifier.width(8.dp))
                Text(tr("Муравей округлил бы каждую трату до ${money(d.s.roundTo)} и отложил разницу: ${money(round)}", "The ant would round each spend up to ${money(d.s.roundTo)} and keep the difference: ${money(round)}"), style = T.small, modifier = Modifier.weight(1f))
            }
        }

        if (st.places.isNotEmpty()) Fold(Icons.TARGET, C.acc, tr("Любимые места", "Favourite places"), st.places.first().first) {
            st.places.take(8).forEach { (n, k, s) -> Row2(n, count(k, "раз", "раза", "раз", "time", "times"), money(s)) }
        }
        Fold(Icons.BOLT, C.bad, tr("Самые крупные", "The biggest"), st.biggest.firstOrNull()?.let { money(it.amount) }) {
            st.biggest.forEach { t -> app.pennyling.ui.journal.TxRow(t, d) { nav.push(Screen.Add(t.id)) } }
        }
        Fold(Icons.CLOCK, C.mute, tr("Цена в часах", "Cost in hours"), if (d.s.hourly > 0) tr("${fmt1(st.spent.toDouble() / d.s.hourly)} ч работы", "${fmt1(st.spent.toDouble() / d.s.hourly)} h of work") else tr("укажите, сколько зарабатываете в час", "set what an hour earns")) {
            if (d.s.hourly <= 0) Text(tr("Укажите в настройках, сколько вы зарабатываете в час, и каждая трата будет видна ещё и в часах работы.", "In settings you can say what an hour of work brings, and every spend will also show in hours of life."), style = T.small)
            else {
                Text(tr("Потрачено – ${fmt1(st.spent.toDouble() / d.s.hourly)} ч работы, это ${fmt1(st.spent.toDouble() / d.s.hourly / 8)} рабочих дней.", "Spent in this span: ${fmt1(st.spent.toDouble() / d.s.hourly)} h of work, ${fmt1(st.spent.toDouble() / d.s.hourly / 8)} working days."), style = T.body)
                st.byCat().take(5).forEach { cs -> Row2(cs.cat?.name ?: "–", value = tr("${fmt1(cs.amount.toDouble() / d.s.hourly)} ч", "${fmt1(cs.amount.toDouble() / d.s.hourly)} h"), lead = { CatMark(cs.cat, 30.dp, d.pets) }) }
            }
        }
        }
        else -> {
        Fold(Icons.BARREL, C.mute, tr("Подписки и платежи", "Subscriptions and bills"), tr("${moneyR(st.subsYear)} в год на подписки", "${moneyR(st.subsYear)} a year on subscriptions")) {
            Row2(tr("Регулярные платежи в месяц", "Regular bills a month"), value = money(st.billsMonth))
            Row2(tr("Подписки в год", "Subscriptions a year"), value = money(st.subsYear))
            if (d.s.hourly > 0 && st.subsYear > 0) Row2(tr("В часах работы", "That's hours of work"), value = fmt1(st.subsYear.toDouble() / d.s.hourly))
            Text(tr("Открыть погреб", "Open the cellar"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { nav.push(Screen.Cellar) }.padding(vertical = 8.dp))
        }
        Fold(Icons.BANK, C.ok, tr("Капитал", "Net worth"), money(st.worth())) {
            Line(st.worthLine(12), color = C.ok, h = 120.dp)
            Text(tr("Сумма на всех счетах в конце каждого месяца", "All accounts together at the end of each month"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
        }
        WhatIf(st, d)
        }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun CatLine(cs: CatSum, d: Data, sk: SpanKind, onClick: () -> Unit) {
    val lim = cs.cat?.limit ?: 0
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        CatMark(cs.cat, 36.dp, d.pets); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row { Text(cs.cat?.name ?: "–", style = T.smallB, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text(money(cs.amount), style = T.smallB) }
            if (lim > 0 && sk != SpanKind.YEAR && sk != SpanKind.ALL) {
                Meter(cs.amount.toFloat() / lim, Modifier.padding(top = 4.dp), color = if (cs.amount > lim) C.bad else catColor(cs.cat), h = 6.dp)
                Text(if (cs.amount > lim) tr("сверх лимита на ${money(cs.amount - lim)}", "${money(cs.amount - lim)} over the limit") else tr("из ${money(lim)}", "of ${money(lim)}"), style = T.tiny)
            } else {
                Meter(cs.share, Modifier.padding(top = 4.dp), color = catColor(cs.cat), h = 6.dp)
                val dl = if (cs.before > 0) ((cs.amount - cs.before) * 100 / cs.before) else null
                Text("${cs.count} · ${(cs.share * 100).roundToInt()}%" + (dl?.let { " · " + (if (it >= 0) "+$it%" else "−${-it}%") } ?: ""), style = T.tiny)
            }
        }
    }
}

/** A ring of coloured parts. */
@Composable
fun Donut(parts: List<Pair<Color, Long>>, size: Dp) {
    val tot = parts.sumOf { it.second }.coerceAtLeast(1)
    val track = C.track
    Canvas(Modifier.size(size)) {
        val w = this.size.width * .16f
        val r = Size(this.size.width - w, this.size.height - w); val o = Offset(w / 2, w / 2)
        drawArc(track, 0f, 360f, false, o, r, style = Stroke(w))
        var a = -90f
        parts.forEach { (c, v) -> val sw = 360f * v / tot; drawArc(c, a, (sw - 1.5f).coerceAtLeast(.5f), false, o, r, style = Stroke(w)); a += sw }
    }
}

/** «What if»: cut a category by a share and see what a month and a year save, and when a jar fills. */
@Composable
private fun WhatIf(st: Stats, d: Data) {
    val cats = st.byCat().filter { it.cat != null }
    if (cats.isEmpty()) return
    var pick by rememberSaveable { mutableLongStateOf(cats.first().cat!!.id) }
    var cut by rememberSaveable { mutableFloatStateOf(.3f) }
    Fold(Icons.SPARK, C.acc, tr("Что если…", "What if…"), tr("если тратить меньше на что-то одно", "trim one category")) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cats.take(4).forEach { cs -> Box(Modifier.weight(1f).clip(CircleShape).background(if (cs.cat!!.id == pick) catColor(cs.cat).copy(alpha = .25f) else C.card2).clickable { pick = cs.cat.id }.padding(6.dp), contentAlignment = Alignment.Center) {
                CatMark(cs.cat, 34.dp, d.pets) } }
        }
        val c = cats.firstOrNull { it.cat!!.id == pick } ?: cats.first()
        val month = c.amount * 30 / st.lived.coerceAtLeast(1)
        Text(tr("${c.cat!!.name}: около ${money(month)} в месяц", "${c.cat!!.name}: about ${money(month)} a month"), style = T.bodyB)
        androidx.compose.material3.Slider(cut, { cut = (it * 20).roundToInt() / 20f }, valueRange = .05f..1f)
        val save = (month * cut).toLong()
        Text(tr("Если тратить на ${(cut * 100).roundToInt()}% меньше, выйдет ${money(save)} в месяц и ${moneyR(save * 12)} в год.", "Trim by ${(cut * 100).roundToInt()}% – that's ${money(save)} a month and ${moneyR(save * 12)} a year."), style = T.body)
        d.goals.firstOrNull { it.done == 0L && !it.archived }?.let { g -> val left = g.target - d.saved(g)
            if (save > 0 && left > 0) Text(tr("Банка «${g.name}» наполнилась бы за ${fmt1(left.toDouble() / save)} мес.", "The jar «${g.name}» would fill in ${fmt1(left.toDouble() / save)} months."), style = T.small, modifier = Modifier.padding(top = 6.dp)) }
    }
}

/** Spending so far through the period against the straight budget line from nothing to the whole budget. */
@Composable
fun Burn(line: List<Pair<Double, Double>>, days: Double, budget: Double, modifier: Modifier = Modifier) {
    val acc = C.acc; val grid = C.line; val bad = C.bad
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val ymax = maxOf(budget, line.maxOfOrNull { it.second } ?: 0.0) * 1.08
        fun o(x: Double, y: Double) = Offset((x / days.coerceAtLeast(1.0) * size.width).toFloat(), (size.height - y / ymax * size.height).toFloat())
        for (i in 0..3) { val y = size.height * i / 3; drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f) }
        drawLine(grid, o(0.0, 0.0), o(days, budget), 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        drawLine(bad.copy(alpha = .5f), o(0.0, budget), o(days, budget), 1.dp.toPx())
        val path = androidx.compose.ui.graphics.Path(); line.forEachIndexed { i, (x, y) -> val q = o(x, y); if (i == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y) }
        drawPath(path, acc, style = Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        line.lastOrNull()?.let { drawCircle(acc, 5.dp.toPx(), o(it.first, it.second)) }
    }
}
