package app.pennyling.ui.pets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.data.Tx
import app.pennyling.domain.Pets
import app.pennyling.domain.Stage
import app.pennyling.domain.count
import app.pennyling.domain.fmtDayYear
import app.pennyling.domain.money
import app.pennyling.domain.monthShort
import app.pennyling.domain.stage
import app.pennyling.domain.title
import app.pennyling.domain.toNext
import app.pennyling.domain.tr
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.art.Creature
import app.pennyling.ui.art.PetView
import app.pennyling.ui.art.Pose
import app.pennyling.ui.components.Bars
import app.pennyling.ui.components.Btn
import app.pennyling.ui.components.Field
import app.pennyling.ui.components.Icon
import app.pennyling.ui.components.Icons
import app.pennyling.ui.components.Meter
import app.pennyling.ui.components.Popup
import app.pennyling.ui.components.Segments
import app.pennyling.ui.components.card
import app.pennyling.ui.share.CardKind
import app.pennyling.ui.share.ShareReq
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import java.time.LocalDate

/**
 * A pet's own page: a big stage in its colour where it shows its poses, its name and growth,
 * its place in the team as one switch, what it does, the stages it grows through and, for a keeper, its category.
 */
@Composable
fun PetPage(id: Long, d: Data, nav: Nav) {
    BackHandler { nav.pop() }
    val c = LocalContext.current.container
    // opened before the pets loaded (a deep link): wait, don't close
    val p = (if (id == -1L) d.pets.firstOrNull { it.slot == 1 } else d.pets.firstOrNull { it.id == id }) ?: run { if (d.pets.isNotEmpty()) nav.pop(); return }
    val sp = Pets.of(p.species)
    val col = Color(sp.color)
    val st = p.stage()
    var pose by remember { mutableStateOf(Pose.IDLE) }
    var rename by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(C.bg).clickable(enabled = false) {}) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // the stage
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(col.copy(alpha = .38f), col.copy(alpha = .1f), C.bg))).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Round(Icons.BACK) { nav.pop() }
                    Spacer(Modifier.weight(1f))
                    if (p.hatched > 0) Round(Icons.SHARE) { nav.share = ShareReq(CardKind.PET, p.id) }
                }
                Column(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.padding(bottom = 10.dp).size(180.dp, 26.dp).clip(RoundedCornerShape(50)).background(col.copy(alpha = .18f)))
                        if (p.hatched == 0L) PetView(p, 230.dp) else Creature(p.species, st, pose, 230.dp, shiny = p.shiny)
                    }
                    if (p.hatched > 0 && st >= Stage.ADULT) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Pose.IDLE to tr("Стоит", "Idle"), Pose.WORK to tr("За делом", "At work"), Pose.CHEER to tr("Радуется", "Cheers"), Pose.SLEEP to tr("Спит", "Sleeps")).forEach { (ps, l) ->
                            Text(l, style = T.smallB.copy(color = if (ps == pose) C.accInk else C.ink), modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(if (ps == pose) col else C.card.copy(alpha = .7f)).clickable { pose = ps }.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 18.dp)) {
                // name and growth
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (p.hatched == 0L) tr("Яйцо", "An egg") else p.name, style = T.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (p.hatched > 0) Box(Modifier.padding(start = 6.dp).size(32.dp).clip(CircleShape).clickable { rename = true }, contentAlignment = Alignment.Center) { Icon(Icons.EDIT, C.mute, 17.dp) }
                        }
                        Text(sp.title + (if (p.hatched > 0) " · ${st.title()}" else "") + if (p.shiny) tr(" · редкий окрас", " · rare colours") else "", style = T.small)
                    }
                }
                val (have, need) = p.toNext()
                if (st != Stage.MASTER) {
                    Meter(have.toFloat() / need, Modifier.padding(top = 10.dp), color = col, h = 10.dp)
                    val next = Stage.entries[(st.ordinal + 1).coerceAtMost(Stage.MASTER.ordinal)]
                    Text(if (p.hatched == 0L) tr("Ещё ${need - have} – и вылупится. Яйцо греют записи.", "${need - have} more and it hatches. Records keep the egg warm.")
                        else tr("До ступени «${next.title().lowercase()}» – ${need - have} опыта", "${need - have} XP to «${next.title().lowercase()}»"), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
                } else Text(tr("Высшая ступень", "The highest stage"), style = T.tiny.copy(color = C.gold), modifier = Modifier.padding(top = 6.dp))

                // the place in the team
                if (p.hatched > 0) {
                    Label(tr("Место", "Place"))
                    val slot = when (p.slot) { 0 -> 0; 1, 2 -> 1; else -> 2 }
                    Segments(listOf(tr("Вожак", "Leader"), tr("Спутник", "Companion"), tr("Дома", "At home")), slot) { i ->
                        if (i == slot) return@Segments
                        c.write { when (i) { 0 -> setSlot(p, 0); 1 -> setSlot(p, (1..2).firstOrNull { sl -> d.pets.none { it.slot == sl } } ?: 2); else -> setSlot(p, -1) } }
                    }
                }

                // what it does
                Label(tr("Что умеет", "What it does"))
                Column(Modifier.fillMaxWidth().card(18.dp).padding(14.dp)) {
                    Text(sp.perk, style = T.body)
                    Text(tr("В команде: ", "In the team: ") + perkShort(sp), style = T.smallB.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp)
                        .clip(RoundedCornerShape(50)).background(C.accSoft).padding(horizontal = 10.dp, vertical = 5.dp))
                    if (p.hatched == 0L && sp.find.isNotEmpty()) Text(sp.find, style = T.small, modifier = Modifier.padding(top = 8.dp))
                }

                // its category, for a keeper
                val cat = d.cats.firstOrNull { it.keeper == sp.id }
                if (cat != null && p.hatched > 0) {
                    Label(tr("Его категория", "Its category"))
                    val m0 = LocalDate.ofEpochDay(d.today).withDayOfMonth(1)
                    val months = (5 downTo 0).map { b -> val m = m0.minusMonths(b.toLong()); val a = m.toEpochDay(); val z = m.plusMonths(1).toEpochDay() - 1
                        monthShort(m.monthValue) to d.txs.filter { it.catId == cat.id && it.kind == Tx.SPEND && it.day in a..z }.sumOf { it.amount } / 100.0 }
                    val now = d.txs.filter { it.catId == cat.id && it.kind == Tx.SPEND && d.ration.period.has(it.day) }
                    Column(Modifier.fillMaxWidth().card(18.dp).clickable { nav.filterCat = cat.id; nav.tab = 1; nav.stack.clear() }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text(cat.name, style = T.bodyB)
                                Text(tr("с зарплаты: ", "since payday: ") + count(now.size, "запись", "записи", "записей", "record", "records"), style = T.tiny)
                            }
                            Text(money(now.sumOf { it.amount }), style = T.num)
                        }
                        if (cat.limit > 0) Meter(now.sumOf { it.amount }.toFloat() / cat.limit, Modifier.padding(top = 8.dp), color = if (now.sumOf { it.amount } > cat.limit) C.bad else col, h = 6.dp)
                        Bars(months, Modifier.padding(top = 10.dp), color = col, hi = months.lastIndex, h = 80.dp)
                    }
                }

                // the stages
                if (p.hatched > 0) {
                    Label(tr("Ступени", "Stages"))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Stage.BABY, Stage.TEEN, Stage.ADULT, Stage.MASTER).forEach { s ->
                            val reached = st >= s
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (s == st) col.copy(alpha = .2f) else C.card)
                                .then(if (s == st) Modifier.border(1.5.dp, col, RoundedCornerShape(16.dp)) else Modifier).padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.alpha(if (reached) 1f else .25f)) { Creature(p.species, s, Pose.IDLE, 62.dp, shiny = p.shiny) }
                                Text(s.title(), style = T.tiny.copy(color = if (reached) C.ink else C.mute), maxLines = 1, textAlign = TextAlign.Center)
                                Text(when (s) { Stage.BABY -> "0"; Stage.TEEN -> "${Pets.TEEN}"; Stage.ADULT -> "${Pets.ADULT}"; else -> "${Pets.MASTER}" }, style = T.tiny)
                            }
                        }
                    }
                }

                // facts
                Label(tr("История", "Story"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fact("${p.xp}", tr("опыта", "XP"), Modifier.weight(1f))
                    Fact("${(d.today - p.found).coerceAtLeast(0)}", tr("дней в норке", "days here"), Modifier.weight(1f))
                    if (sp.cat.isNotEmpty()) Fact("${p.fed}", tr("записей", "records"), Modifier.weight(1f))
                }
                Text(tr("Появился ", "Found ") + fmtDayYear(p.found) + (if (p.hatched > 0) tr(", вылупился ", ", hatched ") + fmtDayYear(p.hatched) else ""), style = T.tiny, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(30.dp))
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
    if (rename) { var name by remember { mutableStateOf(p.name) }
        Popup({ rename = false }) {
            Text(tr("Как зовут?", "What's its name?"), style = T.h2)
            Field(name, { name = it.take(20) }, sp.title, Modifier.padding(top = 10.dp))
            Btn(tr("Готово", "Done"), Modifier.fillMaxWidth().padding(top = 14.dp), enabled = name.isNotBlank()) { c.write { dao.update(p.copy(name = name.trim())) }; rename = false }
        } }
}

@Composable
private fun Round(icon: Icons, onClick: () -> Unit) =
    Box(Modifier.size(42.dp).clip(CircleShape).background(C.card.copy(alpha = .75f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, C.ink, 21.dp) }

@Composable
private fun Label(text: String) = Text(text.uppercase(), style = T.sec, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))

@Composable
private fun Fact(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.card(16.dp).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.num)
        Text(label, style = T.tiny)
    }
}
