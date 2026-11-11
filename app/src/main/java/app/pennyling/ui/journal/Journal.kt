package app.pennyling.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pennyling.data.Tx
import app.pennyling.domain.cap
import app.pennyling.domain.dayShort
import app.pennyling.domain.fmtDay
import app.pennyling.domain.money
import app.pennyling.domain.moneyK
import app.pennyling.domain.monthName
import app.pennyling.domain.tr
import app.pennyling.domain.weekday
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.Screen
import app.pennyling.ui.components.Amount
import app.pennyling.ui.components.CatMark
import app.pennyling.ui.components.Chip
import app.pennyling.ui.components.Empty
import app.pennyling.ui.components.Field
import app.pennyling.ui.components.Icon
import app.pennyling.ui.components.Icons
import app.pennyling.ui.components.Segments
import app.pennyling.ui.components.card
import app.pennyling.ui.components.lerp
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import java.time.LocalDate

/** One record in a list: the category's keeper, what it was, the account, the amount. */
@Composable
fun TxRow(t: Tx, d: Data, onClick: () -> Unit) {
    val cat = d.catById[t.catId]
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 7.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (t.kind == Tx.MOVE) Box(Modifier.size(40.dp).clip(CircleShape).background(C.card2), contentAlignment = Alignment.Center) { Icon(Icons.SWAP, C.mute, 20.dp) }
        else CatMark(cat, 40.dp, d.pets)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(when { t.kind == Tx.MOVE -> "${d.accById[t.accountId]?.name.orEmpty()} → ${d.accById[t.toAccountId]?.name.orEmpty()}"; t.note.isNotBlank() -> t.note; else -> cat?.name.orEmpty() },
                style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = buildList {
                if (t.kind != Tx.MOVE && t.note.isNotBlank()) add(cat?.name.orEmpty())
                if (t.kind != Tx.MOVE) d.accById[t.accountId]?.let { add(it.name) }
                if (t.source == "bill") add(tr("регулярный", "regular"))
                if (t.source == "crow") add(tr("от вороны", "from the crow"))
                when (t.worth) { 1 -> add("♥"); 3 -> add(tr("не стоило", "not worth it")) }
            }
            if (sub.isNotEmpty()) Text(sub.joinToString(" · "), style = T.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Amount(t, Modifier.padding(start = 8.dp))
    }
}

@Composable
fun JournalScreen(d: Data, nav: Nav) {
    var back by rememberSaveable { mutableIntStateOf(0) }
    var view by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    val m = LocalDate.ofEpochDay(d.today).withDayOfMonth(1).minusMonths(back.toLong())
    val from = m.toEpochDay(); val to = m.plusMonths(1).toEpochDay() - 1
    val q = query.trim().lowercase()
    val list = d.txs.filter { (q.isNotEmpty() || it.day in from..to) && (nav.filterCat == 0L || it.catId == nav.filterCat) &&
        (q.isEmpty() || it.note.lowercase().contains(q) || d.catById[it.catId]?.name?.lowercase()?.contains(q) == true || app.pennyling.domain.plainMoney(it.amount) == q) }
    val spent = list.filter { it.kind == Tx.SPEND }.sumOf { it.amount }; val inc = list.filter { it.kind == Tx.INCOME }.sumOf { it.amount }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Журнал", "Journal"), style = T.title, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { searching = !searching; if (!searching) query = "" }, contentAlignment = Alignment.Center) { Icon(if (searching) Icons.CLOSE else Icons.SEARCH, C.ink, 22.dp) }
        }
        if (searching) Field(query, { query = it }, tr("Заметка, категория или сумма", "Note, category or amount"), Modifier.fillMaxWidth().padding(top = 8.dp))
        else Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).clickable { back++ }, contentAlignment = Alignment.Center) { Icon(Icons.BACK, C.ink, 20.dp) }
            Text("${monthName(m.monthValue).cap()} ${m.year}", style = T.h3, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Box(Modifier.size(36.dp).clip(CircleShape).clickable(enabled = back > 0) { back-- }, contentAlignment = Alignment.Center) { Icon(Icons.NEXT, if (back > 0) C.ink else C.line, 20.dp) }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.weight(1f)) { Text("−" + money(spent), style = T.numS); Text(tr("потрачено", "spent"), style = T.tiny) }
            Column(Modifier.weight(1f)) { Text(money(inc, plus = true), style = T.numS.copy(color = C.ok)); Text(tr("пришло", "came in"), style = T.tiny) }
            Column(Modifier.weight(1f)) { Text(money(inc - spent), style = T.numS); Text(tr("итог", "balance"), style = T.tiny) }
        }
        if (nav.filterCat != 0L) Row(Modifier.padding(bottom = 6.dp)) { Chip("${d.catById[nav.filterCat]?.name.orEmpty()}  ✕", true) { nav.filterCat = 0 } }
        if (!searching) Segments(listOf(tr("Список", "List"), tr("Календарь", "Calendar")), view, Modifier.padding(bottom = 8.dp)) { view = it }
        if (view == 1 && !searching) MonthGrid(d, m, list, nav)
        else if (list.isEmpty()) Empty(if (q.isNotEmpty()) tr("Ничего не нашлось", "Nothing found") else tr("В этом месяце записей нет. Нажмите «+», чтобы добавить.", "No records this month. Tap «+» to add one."))
        else LazyColumn(Modifier.fillMaxSize()) {
            val days = list.groupBy { it.day }.toSortedMap(compareByDescending { it })
            for ((day, g) in days) {
                item(key = "h$day") {
                    val sum = g.filter { it.kind == Tx.SPEND }.sumOf { it.amount }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${fmtDay(day)}, ${weekday(day)}", style = T.sec, modifier = Modifier.weight(1f))
                        if (day in d.s.snow) Text("❄ ", style = T.smallB.copy(color = C.snow))
                        if (sum > 0) Text("−" + money(sum), style = T.smallB.copy(color = C.mute))
                    }
                }
                items(g, key = { it.id }) { t -> Box(Modifier.fillMaxWidth().card(14.dp).padding(horizontal = 6.dp)) { TxRow(t, d) { nav.push(Screen.Add(t.id)) } }; Spacer(Modifier.height(6.dp)) }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

/** The month as a calendar: each day tinted by what it cost, snow days frosted. */
@Composable
private fun MonthGrid(d: Data, m: LocalDate, list: List<Tx>, nav: Nav) {
    val byDay = list.filter { it.kind == Tx.SPEND }.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.amount } }
    val max = (byDay.values.maxOrNull() ?: 1L).coerceAtLeast(1L)
    var open by rememberSaveable { mutableStateOf(-1L) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) { (0 until 7).forEach { Text(dayShort(it), style = T.tiny, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } }
        val first = m.toEpochDay(); val pad = m.dayOfWeek.value - 1; val n = m.lengthOfMonth()
        val cells = List(pad) { -1L } + (0 until n).map { first + it }
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { day ->
                    if (day < 0) Spacer(Modifier.weight(1f)) else {
                        val v = byDay[day] ?: 0
                        val bg = when { day in d.s.snow -> C.snow.copy(alpha = .3f); v == 0L -> C.card2; else -> lerp(C.heat[1], C.heat[4], (v.toFloat() / max).coerceIn(0f, 1f)) }
                        Column(Modifier.weight(1f).aspectRatio(.82f).clip(RoundedCornerShape(10.dp)).background(bg).clickable { open = if (open == day) -1 else day }.padding(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${LocalDate.ofEpochDay(day).dayOfMonth}", style = T.smallB.copy(color = if (day == d.today) C.acc else C.ink))
                            if (v > 0) Text(moneyK(v), style = T.tiny.copy(color = C.ink), maxLines = 1)
                            else if (day in d.s.snow) Text("❄", style = T.tiny)
                        }
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (open >= 0) {
            val g = list.filter { it.day == open }
            Text(fmtDay(open), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            if (g.isEmpty()) Empty(if (open in d.s.snow) tr("Снежный день – без трат", "A snow day – nothing spent") else tr("Записей нет", "No records"))
            g.forEach { t -> TxRow(t, d) { nav.push(Screen.Add(t.id)) } }
        }
    }
}
