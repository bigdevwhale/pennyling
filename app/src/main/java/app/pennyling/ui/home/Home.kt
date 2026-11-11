package app.pennyling.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.data.Tx
import app.pennyling.domain.Quests
import app.pennyling.domain.money
import app.pennyling.domain.plural
import app.pennyling.domain.relDay
import app.pennyling.domain.tr
import app.pennyling.domain.weekStart
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.Screen
import app.pennyling.ui.art.BurrowScene
import app.pennyling.ui.art.Mini
import app.pennyling.ui.art.Pose
import app.pennyling.ui.art.Prop
import app.pennyling.ui.art.Sky
import app.pennyling.ui.components.Acorns
import app.pennyling.ui.components.CatMark
import app.pennyling.ui.components.CrumbPill
import app.pennyling.ui.components.Hint
import app.pennyling.ui.components.Icon
import app.pennyling.ui.components.Icons
import app.pennyling.ui.components.Meter
import app.pennyling.ui.components.Row2
import app.pennyling.ui.components.Sheet
import app.pennyling.ui.components.WorthRow
import app.pennyling.ui.components.card
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T

/**
 * The burrow on one screen: the scene, today's ration, the one thing waiting (if any),
 * the pantry as a row of doors and the week's tasks in one line. Details open on tap.
 */
@Composable
fun HomeScreen(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val r = d.ration
    val snow = d.today in d.s.snow
    val spentToday = d.txs.any { it.day == d.today && it.kind == Tx.SPEND && it.billId == 0L }
    var ration by remember { mutableStateOf(false) }
    var quests by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Норка", "The burrow"), style = T.title)
                val left = r.daysLeft
                Text(if (left <= 1) tr("Завтра – зарплата", "Payday tomorrow") else tr("До зарплаты $left ${plural(left.toLong(), "день", "дня", "дней")}", "$left days to payday"), style = T.small)
            }
            CrumbPill(d.s.crumbs) { nav.push(Screen.Shop) }
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { nav.push(Screen.Settings) }, contentAlignment = Alignment.Center) { Icon(Icons.GEAR, C.ink, 22.dp) }
        }
        Box(Modifier.fillMaxWidth().card(22.dp)) {
            val pose = when { snow -> Pose.SLEEP; r.left > 0 && spentToday -> Pose.WORK; else -> Pose.IDLE }
            BurrowScene(d.team, d.s.items - d.s.hidden, pose, sky = Sky.of(d.s), winter = r.period.winter(d.today),
                stock = if (d.s.budget > 0) r.pantry.toFloat() / d.s.budget else 1f, snowDay = snow) { nav.push(Screen.PetInfo(it.id)) }
        }

        // today's ration: one number, the acorns, one line; the rest on tap
        Row(Modifier.padding(top = 12.dp).fillMaxWidth().card(20.dp).clickable { ration = true }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Паёк на сегодня", "Today's ration"), style = T.small)
                Text(if (d.s.budget <= 0) "–" else if (r.over) "−" + money(-r.left) else money(r.left), style = T.num.copy(fontSize = T.num.fontSize * 1.25f, color = if (r.over) C.bad else C.ink), maxLines = 1)
                Text(if (d.s.budget <= 0) tr("Нажмите, чтобы задать бюджет", "Set a budget – tap") else tr("в кладовой ${money(r.pantry.coerceAtLeast(0))}", "${money(r.pantry.coerceAtLeast(0))} in the pantry"), style = T.tiny)
            }
            Acorns(r.acorns, 24.dp)
            Icon(Icons.NEXT, C.mute, 20.dp, Modifier.padding(start = 4.dp))
        }
        if (d.s.budget > 0) Column(Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
            val used = (r.spent + r.reserved).toFloat() / d.s.budget
            val time = r.period.winter(d.today)
            Box(Modifier.fillMaxWidth()) {
                Meter(used, color = if (used > 1f) C.bad else if (used > time + .1f) C.gold else C.ok, h = 6.dp)
                // a tick where the month is now: the money line should stay left of it
                Box(Modifier.fillMaxWidth(time.coerceIn(0.01f, 1f)).height(6.dp), contentAlignment = Alignment.CenterEnd) { Box(Modifier.size(2.dp, 10.dp).background(C.ink.copy(alpha = .5f))) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(tr("за месяц ${money(r.spent)} из ${money(d.s.budget)}", "${money(r.spent)} of ${money(d.s.budget)} this month"), style = T.tiny, modifier = Modifier.weight(1f))
                Text(tr("день ${d.today - r.period.start + 1} из ${r.period.days}", "day ${d.today - r.period.start + 1} of ${r.period.days}"), style = T.tiny)
            }
        }
        if (snow) Pill(tr("Снежный день – нажмите, чтобы отменить", "Snow day – tap to undo"), "snowflake", C.snow, Modifier.fillMaxWidth().padding(top = 8.dp)) { c.write { unSnow(d.today) } }
        else if (!spentToday) Pill(tr("Сегодня без трат? Отметить снежный день", "Nothing spent today? Mark a snow day"), "snowflake", C.snow, Modifier.fillMaxWidth().padding(top = 8.dp)) { c.write { snowDay(d.today); say(tr("Снежный день! Норка в инее", "A snow day! Frost in the burrow")) } }

        // the week's tasks: what's next, the dots of progress and an arrow that says it opens
        val qs = d.s.quests.mapNotNull { Quests.byId[it] }
        if (qs.isNotEmpty()) {
            val next = qs.firstOrNull { it.id !in d.s.questsDone }
            Row(Modifier.padding(top = 8.dp).fillMaxWidth().card(18.dp).clickable { quests = true }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Prop("medal", 34.dp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Задания недели", "This week's tasks"), style = T.bodyB)
                    Text(next?.let { "${it.title} · +${it.crumbs}" } ?: tr("Все выполнены – новые в понедельник", "All done – new ones on Monday"), style = T.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                    qs.forEach { q -> Box(Modifier.size(9.dp).clip(CircleShape).background(if (q.id in d.s.questsDone) C.ok else C.track)) }
                }
                Icon(Icons.NEXT, C.mute, 20.dp)
            }
        }

        // the one thing waiting, if any
        Waiting(d, nav)

        // the pantry: six doors in one row, each with its one number
        val all = app.pennyling.domain.Stats(d.txs, app.pennyling.domain.Span(d.first, d.today, ""), d.catById, d.accounts, d.bills, d.s.budget, d.s.payday)
        val goals = d.goals.filter { !it.archived && it.done == 0L }
        val debts = d.debts.filter { it.done == 0L }
        val save = d.accounts.filter { it.kind == app.pennyling.data.Account.SAVE && !it.archived }.sumOf { all.balance(it) }
        Text(tr("Кладовая", "The pantry"), style = T.sec, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Door("jar1", tr("Банки", "Jars"), Modifier.weight(1f), value = if (goals.isEmpty()) "–" else "${goals.sumOf { d.saved(it) } * 100 / goals.sumOf { it.target }.coerceAtLeast(1)}%") { nav.push(Screen.Goals) }
            Door("gift", tr("Чердак", "Attic"), Modifier.weight(1f), value = "${d.wishes.count { it.state == 0 }}") { nav.push(Screen.Wishes) }
            Door("barrel", tr("Погреб", "Cellar"), Modifier.weight(1f), value = app.pennyling.domain.moneyK(all.subsYear) + tr("/г", "/y")) { nav.push(Screen.Cellar) }
            Door("boulder", tr("Долги", "Debts"), Modifier.weight(1f), value = if (debts.isEmpty()) "0" else app.pennyling.domain.moneyK(debts.sumOf { it.total - d.paid(it) })) { nav.push(Screen.Debts) }
            Door("", tr("Плотина", "Dam"), Modifier.weight(1f), species = "beaver", value = if (all.monthlySpend > 0) tr("${app.pennyling.domain.fmt1(save.toDouble() / all.monthlySpend)} мес", "${app.pennyling.domain.fmt1(save.toDouble() / all.monthlySpend)} mo") else "–") { nav.push(Screen.Dam) }
            Door("coin", tr("Счета", "Accounts"), Modifier.weight(1f), value = app.pennyling.domain.moneyK(all.worth())) { nav.push(Screen.Accounts) }
        }

        // the week: seven days against the ration
        Text(tr("Эта неделя", "This week"), style = T.sec, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        Week(d)

        // today's records, or the latest ones
        val todays = d.txs.filter { it.day == d.today && it.kind != Tx.MOVE }
        val recent = todays.ifEmpty { d.txs.filter { it.kind != Tx.MOVE }.take(3) }
        if (recent.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (todays.isNotEmpty()) tr("Сегодня", "Today") else tr("Последние записи", "Latest records"), style = T.sec, modifier = Modifier.weight(1f))
                Text(tr("Журнал", "Journal"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { nav.tab = 1 }.padding(4.dp))
            }
            Column(Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
                recent.take(4).forEach { t -> app.pennyling.ui.journal.TxRow(t, d) { nav.push(Screen.Add(t.id)) } }
                if (todays.size > 4) Text(tr("и ещё ${todays.size - 4}", "and ${todays.size - 4} more"), style = T.tiny, modifier = Modifier.padding(8.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (ration) Sheet({ ration = false }) {
        Text(tr("Паёк и кладовая", "Ration and pantry"), style = T.h2)
        Text(tr("Остаток бюджета делится на дни до зарплаты. Деньги на будущие платежи откладываются заранее.", "What's left of the budget is divided by the days to payday. Money for bills still to come is kept aside first."), style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Row2(tr("Паёк на день", "Ration a day"), value = money(r.perDay))
        Row2(tr("Потрачено сегодня", "Spent today"), value = money(r.spentToday))
        Row2(tr("В кладовой до зарплаты", "In the pantry to payday"), value = money(r.pantry.coerceAtLeast(0)))
        if (r.reserved > 0) Row2(tr("Отложено на платежи", "Kept for bills"), value = money(r.reserved))
        Row2(tr("Хомяк припрятал", "The hamster tucked away"), tr("что осталось от прошлых пайков", "left over from past rations"), money(r.stash))
        Row2(tr("Бюджет на месяц", "The month's budget"), value = money(d.s.budget)) { ration = false; nav.push(Screen.Settings) }
        if (r.over) Text(tr("Сегодня взято из запаса – паёк на следующие дни станет чуть меньше.", "Taken from the store today – the next days' ration gets a little smaller."), style = T.small.copy(color = C.bad), modifier = Modifier.padding(top = 8.dp))
    }
    if (quests) Sheet({ quests = false }) {
        Text(tr("Задания недели", "This week's tasks"), style = T.h2)
        Text(tr("Новые – каждый понедельник. За них дают крошки.", "New ones every Monday. They bring crumbs."), style = T.small, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        d.s.quests.mapNotNull { Quests.byId[it] }.forEach { q ->
            val done = q.id in d.s.questsDone
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(if (done) C.ok else C.card2), contentAlignment = Alignment.Center) { if (done) Icon(Icons.CHECK, C.accInk, 16.dp) }
                Spacer(Modifier.width(10.dp))
                Text(q.title, style = if (done) T.body.copy(color = C.mute) else T.body, modifier = Modifier.weight(1f))
                Text("+${q.crumbs}", style = T.smallB.copy(color = C.gold))
            }
        }
    }
}

/** A small rounded button with a picture. */
@Composable
private fun Pill(text: String, prop: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = .16f)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Prop(prop, 22.dp); Spacer(Modifier.width(6.dp))
        Text(text, style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A door of the pantry: a picture over a word. */
@Composable
private fun Door(prop: String, label: String, modifier: Modifier = Modifier, species: String? = null, value: String? = null, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(C.card).clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (species != null) Mini(species, 34.dp) else Prop(prop, 34.dp)
        Text(label, style = T.tiny.copy(color = C.ink), maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
        if (value != null) Text(value, style = T.tiny.copy(fontSize = T.tiny.fontSize * .92f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** At most one card asks for attention: the crow's news, then «worth it?», then the owl. */
@Composable
private fun Waiting(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val monday = weekStart(d.today)
    when {
        d.heard.isNotEmpty() -> Row(Modifier.padding(top = 12.dp).fillMaxWidth().card(18.dp).clickable { nav.push(Screen.Heard) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Mini("crow", 40.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Ворона принесла вести", "The crow brought news"), style = T.bodyB)
                Text(app.pennyling.domain.count(d.heard.size, "запись ждёт", "записи ждут", "записей ждут", "record waits", "records wait"), style = T.small)
            }
            Icon(Icons.NEXT, C.mute, 20.dp)
        }
        d.toAsk.isNotEmpty() -> { val t = d.toAsk.first()
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().card(18.dp).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CatMark(d.catById[t.catId], 40.dp, d.pets); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Стоило того?", "Was it worth it?"), style = T.tiny)
                        Text("${t.note.ifBlank { d.catById[t.catId]?.name.orEmpty() }} · ${money(t.amount)}", style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(relDay(t.day), style = T.tiny)
                    }
                    Text(tr("Позже", "Later"), style = T.smallB.copy(color = C.mute), modifier = Modifier.clickable { c.write { answerWorth(t, -1) } }.padding(6.dp))
                }
                Spacer(Modifier.height(8.dp))
                WorthRow { a -> c.write { answerWorth(t, a) } }
            }
        }
        d.s.reviewed < monday && d.txs.any { it.day in (monday - 7) until monday } ->
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().card(18.dp).clickable { nav.push(Screen.Review) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Mini("owl", 40.dp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Сова зовёт на разбор недели", "The owl calls you to go over the week"), style = T.bodyB)
                    Text(tr("Пять минут – и ясно, куда ушли деньги", "Five minutes to see where the money went"), style = T.small)
                }
                Icon(Icons.NEXT, C.mute, 20.dp)
            }
    }
}


/** Monday to Sunday: what each day cost against an even share of the budget; snow days frosted, days to come faint. */
@Composable
private fun Week(d: Data) {
    val monday = weekStart(d.today)
    val share = if (d.s.budget > 0) d.s.budget / d.ration.period.days.coerceAtLeast(1) else 0L
    val byDay = d.txs.filter { it.kind == Tx.SPEND && it.billId == 0L && it.day in monday..(monday + 6) }.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.amount } }
    Row(Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 10.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 7) {
            val day = monday + i; val v = byDay[day] ?: 0L
            val future = day > d.today
            val f = if (share > 0) (v.toFloat() / share).coerceIn(0f, 1.6f) / 1.6f else if (v > 0) .5f else 0f
            val col = when { share > 0 && v > share -> C.gold; else -> C.ok }
            Column(Modifier.weight(1f).alpha(if (future) .35f else 1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(52.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    if (day in d.s.snow) Prop("snowflake", 22.dp)
                    else Box(Modifier.width(14.dp).height((4 + 48 * f).dp).clip(RoundedCornerShape(50)).background(if (v == 0L) C.track else col))
                }
                Text(app.pennyling.domain.dayShort(i), style = T.tiny.copy(color = if (day == d.today) C.acc else C.mute), modifier = Modifier.padding(top = 4.dp))
                Text(if (v > 0) app.pennyling.domain.moneyK(v) else " ", style = T.tiny.copy(fontSize = T.tiny.fontSize * .85f), maxLines = 1)
            }
        }
    }
}
