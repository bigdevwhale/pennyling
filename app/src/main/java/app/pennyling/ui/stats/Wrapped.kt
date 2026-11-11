package app.pennyling.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pennyling.data.Tx
import app.pennyling.domain.Pets
import app.pennyling.domain.Span
import app.pennyling.domain.Stats
import app.pennyling.domain.cap
import app.pennyling.domain.count
import app.pennyling.domain.monthName
import app.pennyling.domain.moneyR
import app.pennyling.domain.stage
import app.pennyling.domain.tr
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.art.Creature
import app.pennyling.ui.art.Pose
import app.pennyling.ui.art.Prop
import app.pennyling.ui.components.Btn
import app.pennyling.ui.components.CatMark
import app.pennyling.ui.components.Empty
import app.pennyling.ui.components.Page
import app.pennyling.ui.share.CardKind
import app.pennyling.ui.share.ShareReq
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import java.time.LocalDate

/** «A year in the burrow»: the year's story told card by card. */
@Composable
fun WrappedPage(year: Int, d: Data, nav: Nav) {
    val from = LocalDate.of(year, 1, 1).toEpochDay(); val to = LocalDate.of(year, 12, 31).toEpochDay()
    val st = Stats(d.txs, Span(from, to, "$year"), d.catById, d.accounts, d.bills, d.s.budget, d.s.payday)
    Page(tr("Год в норке · $year", "A year in the burrow · $year"), { nav.pop() }) {
        if (st.txs.isEmpty()) { Empty(tr("В этом году записей нет", "No records this year")); return@Page }
        @Composable fun Leaf(bg: Color, content: @Composable () -> Unit) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(24.dp)).background(bg).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() }
        }
        val days = st.txs.filter { it.kind != Tx.MOVE }.map { it.day }.toSet().size
        Leaf(C.accSoft) {
            Prop("acorns", 90.dp)
            Text(count(days, "день", "дня", "дней", "day", "days"), style = T.big, modifier = Modifier.padding(top = 8.dp))
            Text(tr("вы вели учёт – ${count(st.txs.size, "запись", "записи", "записей", "record", "records")}", "you kept the burrow's books – ${count(st.txs.size, "record", "records", "records", "record", "records")}"), style = T.body, textAlign = TextAlign.Center)
        }
        Leaf(C.card) {
            Text(tr("Прошло через руки", "Passed through your hands"), style = T.sec)
            Text(moneyR(st.spent), style = T.big, modifier = Modifier.padding(top = 6.dp))
            Text(tr("потрачено, а пришло ${moneyR(st.income)}", "spent, and ${moneyR(st.income)} came in"), style = T.body)
            if (st.income > 0) Text(tr("Осталось ${(st.saveRate * 100).toInt()}% дохода", "${(st.saveRate * 100).toInt()}% of income kept"), style = T.bodyB.copy(color = C.ok), modifier = Modifier.padding(top = 6.dp))
        }
        st.byCat().firstOrNull()?.let { top -> Leaf(C.goldSoft) {
            val k = top.cat?.keeper.orEmpty()
            if (k.isNotEmpty()) Creature(k, app.pennyling.domain.Stage.ADULT, Pose.WORK, 150.dp) else CatMark(top.cat, 90.dp, d.pets)
            Text(tr("Главная статья расходов", "The year's biggest line"), style = T.sec, modifier = Modifier.padding(top = 6.dp))
            Text(top.cat?.name ?: "–", style = T.title)
            Text(tr("${moneyR(top.amount)} – ${(top.share * 100).toInt()}% всех трат", "${moneyR(top.amount)} – ${(top.share * 100).toInt()}% of all spending"), style = T.body)
            if (k.isNotEmpty()) Text(tr("Её хранитель не знал выходных", "The ${Pets.of(k).en.lowercase()} worked without days off"), style = T.small)
        } }
        val ms = (1..12).map { m -> val a = LocalDate.of(year, m, 1).toEpochDay(); val z = LocalDate.of(year, m, 1).plusMonths(1).toEpochDay() - 1
            m to (if (z >= d.today) 0L else st.spends.filter { it.day in a..z }.sumOf { it.amount }) }.filter { it.second > 0 }
        if (ms.size >= 2) Leaf(C.card) {
            val lo = ms.minBy { it.second }; val hi = ms.maxBy { it.second }
            Text(tr("Самый бережливый месяц", "The thriftiest month"), style = T.sec)
            Text(monthName(lo.first).cap(), style = T.title); Text(moneyR(lo.second), style = T.body)
            Spacer(Modifier.height(12.dp))
            Text(tr("Самый дорогой", "The most generous"), style = T.sec)
            Text(monthName(hi.first).cap(), style = T.title); Text(moneyR(hi.second), style = T.body)
        }
        val snow = d.s.snow.count { it in from..to }
        Leaf(C.snow.copy(alpha = .25f)) {
            Prop("snowflake", 80.dp)
            Text(count(snow, "снежный день", "снежных дня", "снежных дней", "snow day", "snow days"), style = T.title, modifier = Modifier.padding(top = 6.dp))
            Text(tr("дней, когда не потрачено ни копейки", "days without spending a penny"), style = T.body)
        }
        if (st.worthAll.isNotEmpty()) Leaf(C.okSoft) {
            Creature("otter", app.pennyling.domain.Stage.ADULT, Pose.CHEER, 130.dp)
            Text(tr("Стоило того", "Worth it"), style = T.sec)
            Text(moneyR(st.worthSum(1)), style = T.title)
            Text(tr("а о ${moneyR(st.worthSum(3))} вы пожалели", "and you regretted ${moneyR(st.worthSum(3))}"), style = T.body)
        }
        val born = d.pets.filter { it.hatched in from..to }
        if (born.isNotEmpty()) Leaf(C.card) {
            Text(tr("Вылупились в этом году", "Hatched this year"), style = T.sec)
            Row(Modifier.padding(top = 8.dp)) { born.take(5).forEach { p -> Creature(p.species, p.stage(), Pose.IDLE, 58.dp, shiny = p.shiny); Spacer(Modifier.width(2.dp)) } }
            Text(count(born.size, "питомец", "питомца", "питомцев", "pet", "pets"), style = T.title)
        }
        val goals = d.goals.filter { it.done in from..to }
        if (goals.isNotEmpty()) Leaf(C.accSoft) {
            Prop("jar2", 80.dp)
            Text(count(goals.size, "банка наполнена", "банки наполнены", "банок наполнено", "jar filled", "jars filled"), style = T.title)
            Text(goals.joinToString(", ") { it.name }, style = T.body, textAlign = TextAlign.Center)
        }
        Btn(tr("Поделиться годом", "Share the year"), Modifier.fillMaxWidth().padding(top = 16.dp)) { nav.share = ShareReq(CardKind.YEAR, year.toLong()) }
    }
}
