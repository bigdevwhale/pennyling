package app.pennyling.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.domain.Pets
import app.pennyling.domain.Quests
import app.pennyling.domain.Stage
import app.pennyling.domain.fmtDayShort
import app.pennyling.domain.money
import app.pennyling.domain.title
import app.pennyling.domain.tr
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.Screen
import app.pennyling.ui.art.Creature
import app.pennyling.ui.art.EggView
import app.pennyling.ui.art.Pose
import app.pennyling.ui.art.Prop
import app.pennyling.ui.components.Btn
import app.pennyling.ui.components.Centered
import app.pennyling.ui.components.Popup
import app.pennyling.ui.share.CardKind
import app.pennyling.ui.share.ShareReq
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T

/** News waiting in Settings.inbox, one at a time: eggs, hatchings, growth, finished tasks, the end of a winter. */
@Composable
fun Inbox(d: Data, nav: Nav) {
    val item = d.s.inbox.firstOrNull() ?: return
    val c = LocalContext.current.container
    fun done() = c.write { settings.update { it.copy(inbox = it.inbox.drop(1)) } }
    val parts = item.split(':')
    var title = ""; var text = ""; var pic: @Composable () -> Unit = {}; var action: Pair<String, () -> Unit>? = null
    when (parts[0]) {
        "egg" -> { val sp = Pets.of(parts[1]); val why = parts.getOrNull(2)
            title = tr("Новое яйцо!", "A new egg!")
            text = when (why) {
                "jar" -> tr("Банка полна – и в ней нашлось яйцо. Кто там? Узнаете, когда вылупится.", "The jar is full – and there's an egg in it. Who's inside? You'll see when it hatches.")
                "fair" -> tr("Яйцо с ярмарки. Записи его согреют, и скоро оно проклюнется.", "An egg from the fair. Records will warm it, and soon it cracks.")
                else -> if (sp.cat.isNotEmpty()) tr("В норке появилось яйцо – ещё пара записей в этой категории, и оно вылупится.", "An egg turned up – a couple more records in its category and it hatches.")
                    else tr("Это награда за ваши привычки. Кто внутри? Узнаете, когда вылупится.", "A reward for your habits. Who is inside? You will see when it hatches.")
            }
            pic = { EggView(sp.id, 150.dp, .6f) } }
        "hatch" -> { val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            val sp = Pets.of(p.species)
            title = tr("В норке новый жилец!", "A new lodger in the burrow!")
            text = tr("Знакомьтесь: ${p.name}, ${sp.ru.lowercase()}. ${sp.perkRu}.", "Meet ${p.name}, a ${sp.en.lowercase()}. ${sp.perkEn}.")
            pic = { Creature(p.species, Stage.BABY, Pose.CHEER, 160.dp, shiny = p.shiny) }
            action = tr("Взять в команду", "Take into the team") to { c.write { val free = (0..2).firstOrNull { sl -> d.pets.none { it.slot == sl } } ?: 1; setSlot(p, free) } } }
        "stage" -> { val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            val st = Stage.entries[parts[2].toInt()]
            title = tr("Питомец подрос!", "${p.name} grew up!")
            text = tr("${p.name} теперь «${st.title().lowercase()}». Это ваши записи помогли.", "${p.name} is now a «${st.title().lowercase()}». Your records did it.")
            pic = { Creature(p.species, st, Pose.CHEER, 160.dp, shiny = p.shiny) } }
        "quest" -> { val q = Quests.byId[parts[1]] ?: run { done(); return }
            title = tr("Задание выполнено", "Task done"); text = "${q.title}\n+${q.crumbs} ${tr("крошек", "crumbs")}"
            pic = { Prop("medal", 110.dp) } }
        "won", "hard" -> { val start = parts[1].toLong()
            val ok = parts[0] == "won"
            title = if (ok) tr("Перезимовали!", "We made it through winter!") else tr("Зима была трудной", "It was a hard winter")
            text = if (ok) tr("Месяц с ${fmtDayShort(start)} уложился в бюджет. Весна, зарплата и полная кладовая!", "The month from ${fmtDayShort(start)} stayed within the budget. Spring, payday and a full pantry!")
                else tr("Месяц с ${fmtDayShort(start)} вышел за бюджет. Ничего страшного: новая зарплата – новый запас.", "The month from ${fmtDayShort(start)} went over budget. That's alright – new payday, new pantry. The burrow is still standing.")
            pic = { Prop(if (ok) "treasure" else "snowflake", 120.dp) }
            action = tr("Поделиться", "Share") to { nav.share = ShareReq(CardKind.PERIOD, start) } }
        "goal" -> { val g = d.goals.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            title = tr("Банка полна!", "The jar is full!"); text = "${g.name} – ${money(g.target)}"
            pic = { Prop("jar2", 120.dp) }
            action = tr("Поделиться", "Share") to { nav.share = ShareReq(CardKind.GOAL, g.id) } }
        "debt" -> { val g = d.debts.firstOrNull { it.id == parts[1].toLongOrNull() } ?: run { done(); return }
            title = tr("Валун откачен!", "The boulder is rolled away!"); text = tr("Долг «${g.name}» закрыт. Вход в норку свободен.", "The debt «${g.name}» is closed. The way in is clear.")
            pic = { Prop("boulder", 110.dp) } }
        else -> { done(); return }
    }
    Popup(::done) {
        Centered { pic() }
        Spacer(Modifier.height(10.dp))
        Text(title, style = T.h2, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(text, style = T.body.copy(color = C.mute), modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        action?.let { (l, f) -> Btn(l, Modifier.fillMaxWidth()) { f(); done() }; Spacer(Modifier.height(8.dp)) }
        Btn(tr("Отлично", "Lovely"), Modifier.fillMaxWidth(), color = if (action != null) C.card2 else C.acc, ink = if (action != null) C.ink else C.accInk) { done() }
    }
}

