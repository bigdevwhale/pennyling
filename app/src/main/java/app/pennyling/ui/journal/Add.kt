package app.pennyling.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import app.pennyling.container
import app.pennyling.data.Tx
import app.pennyling.domain.Lang
import app.pennyling.domain.Money
import app.pennyling.domain.Pets
import app.pennyling.domain.fmtDayShort
import app.pennyling.domain.money
import app.pennyling.domain.parseMoney
import app.pennyling.domain.plainMoney
import app.pennyling.domain.tr
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.Screen
import app.pennyling.ui.components.CatMark
import app.pennyling.ui.components.Chip
import app.pennyling.ui.components.Field
import app.pennyling.ui.components.Icon
import app.pennyling.ui.components.Icons
import app.pennyling.ui.components.Segments
import app.pennyling.ui.components.catColor
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A new record or one to change: the amount on a keypad, the category (its keeper looks on), the account, the day, a note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPage(scr: Screen.Add, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val old = d.txs.firstOrNull { it.id == scr.id }
    var kind by rememberSaveable { mutableStateOf(old?.kind ?: scr.kind) }
    var amount by rememberSaveable { mutableStateOf(old?.let { plainMoney(it.amount) } ?: if (scr.amount > 0) plainMoney(scr.amount) else "") }
    var catId by rememberSaveable { mutableLongStateOf(old?.catId ?: scr.catId) }
    val accs = d.accounts.filter { !it.archived }
    var acc by rememberSaveable { mutableLongStateOf(old?.accountId ?: (accs.firstOrNull { it.kind == app.pennyling.data.Account.CARD } ?: accs.firstOrNull())?.id ?: 0L) }
    var toAcc by rememberSaveable { mutableLongStateOf(old?.toAccountId ?: accs.firstOrNull { it.id != acc }?.id ?: 0L) }
    var day by rememberSaveable { mutableLongStateOf(old?.day ?: d.today) }
    var note by rememberSaveable { mutableStateOf(old?.note ?: scr.note) }
    var pickDay by remember { mutableStateOf(false) }
    var pickAcc by remember { mutableStateOf(false) }
    // opened before the accounts loaded: pick the card (or the first one) once they are here
    androidx.compose.runtime.LaunchedEffect(accs) { if (accs.none { it.id == acc }) acc = (accs.firstOrNull { it.kind == app.pennyling.data.Account.CARD } ?: accs.firstOrNull())?.id ?: 0L
        if (accs.none { it.id == toAcc }) toAcc = accs.firstOrNull { it.id != acc }?.id ?: 0L }
    var askDelete by remember { mutableStateOf(false) }
    val cats = d.activeCats.filter { it.income == (kind == Tx.INCOME) }
    val value = parseMoney(amount) ?: 0L
    val ok = value > 0 && (kind == Tx.MOVE && acc != toAcc && toAcc != 0L || kind != Tx.MOVE && catId != 0L && cats.any { it.id == catId })
    BackHandler { nav.pop() }

    fun save() {
        if (!ok) return
        val t = Tx(id = old?.id ?: 0, amount = value, kind = kind, catId = if (kind == Tx.MOVE) 0 else catId, accountId = acc, toAccountId = if (kind == Tx.MOVE) toAcc else 0,
            day = day, at = old?.at ?: System.currentTimeMillis(), note = note.trim(), worth = old?.worth ?: if (kind == Tx.SPEND) 0 else -1, billId = old?.billId ?: 0, source = old?.source ?: "")
        c.write {
            if (old == null) {
                addTx(t)
                if (kind == Tx.SPEND && day in d.s.snow) unSnow(day)
                val keeper = d.catById[catId]?.keeper.orEmpty()
                val p = d.pets.firstOrNull { it.species == keeper }
                say(when {
                    kind == Tx.MOVE -> tr("Переведено", "Moved")
                    p == null && keeper.isNotEmpty() -> tr("Записано. В норке что-то появилось…", "Logged. Something turned up in the burrow…")
                    p != null && p.hatched == 0L -> tr("Записано. Яйцо потеплело", "Logged. The egg got warmer")
                    p != null -> tr("Записано. ${p.name} благодарит", "Logged. ${p.name} says thanks")
                    else -> tr("Записано", "Logged")
                })
            } else updateTx(t)
        }
        nav.pop()
    }

    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.pop() }, contentAlignment = Alignment.Center) { Icon(Icons.CLOSE, C.ink, 22.dp) }
            Text(if (old == null) tr("Новая запись", "New record") else tr("Запись", "Record"), style = T.h2, modifier = Modifier.weight(1f).padding(start = 4.dp))
            if (old != null) Box(Modifier.size(44.dp).clip(CircleShape).clickable { askDelete = true }, contentAlignment = Alignment.Center) { Icon(Icons.TRASH, C.bad, 21.dp) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Segments(listOf(tr("Трата", "Spent"), tr("Доход", "Income"), tr("Перевод", "Transfer")), kind) { kind = it; if (cats.none { c0 -> c0.id == catId }) catId = 0 }
            // the amount
            val col = when (kind) { Tx.INCOME -> C.ok; Tx.MOVE -> C.mute; else -> C.ink }
            Text(if (amount.isEmpty()) money(0) else money(value), style = T.big.copy(color = if (amount.isEmpty()) C.line else col), modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), textAlign = TextAlign.Center, maxLines = 1)
            // usual records in one tap
            if (old == null && kind != Tx.MOVE) {
                val usual = d.txs.filter { it.kind == kind && it.billId == 0L && d.today - it.day < 120 }.groupBy { Triple(it.catId, it.amount, it.note.trim()) }
                    .filter { it.value.size >= 2 }.entries.sortedByDescending { it.value.size }.take(6).map { it.key }
                if (usual.isNotEmpty()) {
                    Text(tr("Как обычно", "The usual"), style = T.sec, modifier = Modifier.padding(bottom = 6.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        usual.forEach { (cid, a, n) ->
                            Row(Modifier.clip(RoundedCornerShape(50)).background(C.card).clickable { catId = cid; amount = plainMoney(a); note = n }.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                CatMark(d.catById[cid], 26.dp, d.pets); Spacer(Modifier.width(6.dp))
                                Text("${n.ifBlank { d.catById[cid]?.name.orEmpty() }} · ${money(a)}", style = T.smallB, maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (kind == Tx.MOVE) {
                Text(tr("Откуда", "From"), style = T.sec, modifier = Modifier.padding(bottom = 6.dp))
                AccountChips(d, acc) { acc = it }
                Text(tr("Куда", "To"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                AccountChips(d, toAcc) { toAcc = it }
            } else {
                Text(tr("Категория", "Category"), style = T.sec, modifier = Modifier.padding(bottom = 6.dp))
                cats.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { ct ->
                            val on = ct.id == catId
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) catColor(ct).copy(alpha = .18f) else androidx.compose.ui.graphics.Color.Transparent)
                                .border(1.5.dp, if (on) catColor(ct) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(14.dp))
                                .clickable { catId = ct.id }.padding(vertical = 6.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CatMark(ct, 44.dp, d.pets)
                                Text(ct.name, style = T.tiny.copy(color = if (on) C.ink else C.mute), maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            // where from and which day: one row of two chips; the note right under
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (kind != Tx.MOVE) Choice(Icons.CARD, d.accById[acc]?.name ?: "–", Modifier.weight(1f)) { pickAcc = true }
                Choice(Icons.CAL, when (day) { d.today -> tr("Сегодня", "Today"); d.today - 1 -> tr("Вчера", "Yesterday"); else -> fmtDayShort(day) }, Modifier.weight(1f)) { pickDay = true }
            }
            Spacer(Modifier.height(8.dp))
            Field(note, { note = it }, tr("Где, что, зачем", "Where, what, why"), single = true)
            val notes = d.txs.filter { it.catId == catId && it.note.isNotBlank() && it.note != note && it.kind == kind }.map { it.note.trim() }.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(6).map { it.key }
            if (notes.isNotEmpty() && kind != Tx.MOVE) Row(Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                notes.forEach { n -> Chip(n, false) { note = n } }
            }
            if (old != null && old.kind == Tx.SPEND && old.worth in 1..3) {
                Text(tr("Стоило того: ", "Worth it: ") + when (old.worth) { 1 -> tr("да", "yes"); 2 -> tr("так себе", "so-so"); else -> tr("нет", "no") },
                    style = T.small, modifier = Modifier.padding(top = 12.dp))
            }
            if (old?.source == "bill") Text(tr("Записано по регулярному платежу", "Logged by a regular payment"), style = T.small, modifier = Modifier.padding(top = 8.dp))
            if (old?.source == "crow") Text(tr("Принесла ворона из уведомления банка", "Brought by the crow from a bank notification"), style = T.small, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(12.dp))
        }
        Keypad(amount, { amount = it }, ok, if (old == null) tr("Записать", "Log it") else tr("Сохранить", "Save")) { save() }
    }

    var calendar by remember { mutableStateOf(false) }
    if (pickDay) app.pennyling.ui.components.Popup({ pickDay = false }) {
        Text(tr("Какой день", "Which day"), style = T.h2)
        listOf(d.today to tr("Сегодня", "Today"), d.today - 1 to tr("Вчера", "Yesterday"), d.today - 2 to tr("Позавчера", "2 days ago")).forEach { (dd, l) ->
            app.pennyling.ui.components.Row2(l, fmtDayShort(dd)) { day = dd; pickDay = false } }
        app.pennyling.ui.components.Row2(tr("Другой день…", "Another day…")) { pickDay = false; calendar = true }
    }
    if (pickAcc) app.pennyling.ui.components.Popup({ pickAcc = false }) {
        Text(if (kind == Tx.INCOME) tr("На какой счёт", "Into which account") else tr("С какого счёта", "From which account"), style = T.h2)
        d.accounts.filter { !it.archived }.forEach { a -> app.pennyling.ui.components.Row2(a.name, if (a.id == acc) "✓" else null) { acc = a.id; pickAcc = false } }
    }
    if (calendar) {
        val st = rememberDatePickerState(initialSelectedDateMillis = LocalDate.ofEpochDay(day).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(onDismissRequest = { calendar = false }, confirmButton = {
            TextButton({ st.selectedDateMillis?.let { day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }; calendar = false }) { Text("OK") }
        }) { DatePicker(st) }
    }
    if (askDelete && old != null) app.pennyling.ui.components.Popup({ askDelete = false }) {
        Text(tr("Удалить запись?", "Delete the record?"), style = T.h2)
        Text(tr("Питомцы не обидятся – крошки останутся.", "The pets won't mind – the crumbs stay."), style = T.small, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            app.pennyling.ui.components.Ghost(tr("Отмена", "Cancel"), Modifier.weight(1f)) { askDelete = false }
            app.pennyling.ui.components.Btn(tr("Удалить", "Delete"), Modifier.weight(1f), color = C.bad) { c.write { deleteTx(old.id) }; askDelete = false; nav.pop() }
        }
    }
}

@Composable
fun AccountChips(d: Data, sel: Long, onSel: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        d.accounts.filter { !it.archived }.forEach { a -> Chip(a.name, a.id == sel) { onSel(a.id) } }
    }
}

/** A calculator-like keypad that edits [text] (a plain number) and a big button under it. */
@Composable
fun Keypad(text: String, onText: (String) -> Unit, ok: Boolean, label: String, onDone: () -> Unit) {
    val sep = if (Lang.ru) ',' else '.'
    fun press(k: String) {
        when (k) {
            "⌫" -> onText(text.dropLast(1))
            "sep" -> if (Money.cur.minor && !text.contains(sep)) onText(if (text.isEmpty()) "0$sep" else text + sep)
            else -> { val frac = text.substringAfter(sep, ""); if (text.contains(sep) && frac.length >= 2) return
                if (text.replace(sep.toString(), "").length >= 10) return
                onText(if (text == "0") k else text + k) }
        }
    }
    Column(Modifier.fillMaxWidth().background(C.card).padding(horizontal = 12.dp, vertical = 8.dp)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("sep", "0", "⌫")).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).clickable { press(k) }, contentAlignment = Alignment.Center) {
                        when (k) {
                            "⌫" -> Icon(Icons.BACK, C.ink, 24.dp)
                            "sep" -> Text(if (Money.cur.minor) sep.toString() else "", style = T.num)
                            else -> Text(k, style = T.num)
                        }
                    }
                }
            }
        }
        app.pennyling.ui.components.Btn(label, Modifier.fillMaxWidth().padding(top = 6.dp), enabled = ok, onClick = onDone)
    }
}

/** Kept for symmetry with the keepers: the species that looks after a category, if any. */
fun keeperName(d: Data, catId: Long) = d.catById[catId]?.keeper?.takeIf { it.isNotEmpty() }?.let { Pets.of(it).title }

/** A pill that shows a choice and opens it. */
@Composable
private fun Choice(icon: Icons, text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(C.card).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, C.mute, 18.dp); Spacer(Modifier.width(8.dp))
        Text(text, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.DOWN, C.mute, 16.dp)
    }
}
