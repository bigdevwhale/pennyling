package app.pennyling.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.domain.Lang
import app.pennyling.domain.Money
import app.pennyling.domain.Stage
import app.pennyling.domain.money
import app.pennyling.domain.parseMoney
import app.pennyling.domain.tr
import app.pennyling.ui.art.Creature
import app.pennyling.ui.art.Mini
import app.pennyling.ui.art.Pose
import app.pennyling.ui.art.Prop
import app.pennyling.ui.components.Btn
import app.pennyling.ui.components.Chip
import app.pennyling.ui.components.Field
import app.pennyling.ui.components.Ghost
import app.pennyling.ui.components.Popup
import app.pennyling.ui.components.Row2
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import kotlinx.coroutines.launch

@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    var page by rememberSaveable { mutableIntStateOf(0) }
    var cur by rememberSaveable { mutableStateOf(Money.guess(java.util.Locale.getDefault().country, Lang.ru)) }
    var payday by rememberSaveable { mutableIntStateOf(1) }
    var budget by rememberSaveable { mutableStateOf("") }
    var income by rememberSaveable { mutableStateOf("") }
    var cash by rememberSaveable { mutableStateOf("") }
    var card by rememberSaveable { mutableStateOf("") }
    var pickCur by rememberSaveable { mutableStateOf(false) }
    Money.set(cur)
    BackHandler(page > 0) { page-- }
    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i -> Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (i <= page) C.acc else C.track)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 18.dp)) {
            when (page) {
                0 -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Creature("hamster", Stage.ADULT, Pose.IDLE, 210.dp) }
                    Text("Pennyling", style = T.title.copy(fontSize = T.title.fontSize * 1.3f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text(tr("Норка, где деньги считают питомцы", "A burrow where pets keep the books"), style = T.body.copy(color = C.mute), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Point("acorns", tr("Месяц до зарплаты – это зима", "The month to payday is a winter"), tr("Бюджет – запас в кладовой. Каждый день у вас свой паёк, а к концу месяца за окном падает снег.", "The budget is the pantry. Each day has its ration, and snow falls outside as the month runs out."))
                    Point("egg", tr("Много питомцев", "Lots of pets"), tr("У каждой категории трат – свой хранитель. Ещё десять питомцев появляются за полезные привычки: дни без трат, отложенные покупки, полные банки.", "Every spending category has a keeper. A dozen more turn up when you live kindly with money."))
                    Point("jar2", tr("Банки, чердак, погреб", "Jars, attic, cellar"), tr("Цели, желания на потом, подписки, долги и подушка безопасности – всё под одной крышей.", "Goals, wishes that need to cool, subscriptions, debts and a cushion – all under one roof."))
                    Point("crumbs", tr("Награда за честность", "Rewards for honesty"), tr("Крошки и рост питомцев – за записи, а не за экономию. Честно записанная лишняя трата тоже засчитывается.", "Crumbs and growth come from logging, not from saving. Logging a splurge is good too."))
                }
                1 -> {
                    Text(tr("Деньги и день зарплаты", "Money and payday"), style = T.title)
                    Text(tr("Валюта", "Currency"), style = T.sec, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
                    Ghost("${Money.of(cur).sign} · ${if (Lang.ru) Money.of(cur).ru else Money.of(cur).en}", Modifier.fillMaxWidth()) { pickCur = true }
                    Text(tr("С какого числа начинается ваш месяц?", "Which day does your month start?"), style = T.sec, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
                    Text(tr("Обычно это день зарплаты. Если зарплата дважды в месяц или её нет – выберите 1-е.", "Usually payday. If you're paid twice a month or not at all, pick the 1st."), style = T.small)
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 5, 10, 15, 20, 25).forEach { n -> Chip("$n", payday == n) { payday = n } }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Ghost("−", small = true) { payday = (payday - 1).coerceAtLeast(1) }
                        Text(tr("$payday-е число", "day $payday"), style = T.h3, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Ghost("+", small = true) { payday = (payday + 1).coerceAtMost(31) }
                    }
                }
                2 -> {
                    Text(tr("Запас на месяц", "The month's store"), style = T.title)
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Prop("sack", 70.dp); Spacer(Modifier.width(10.dp))
                        Text(tr("Сколько можно потратить за месяц – вместе с арендой, связью и прочими платежами? Из этого посчитается ежедневный паёк.", "How much can the month cost – including rent, phone and other bills? The daily ration is counted from it."), style = T.small, modifier = Modifier.weight(1f))
                    }
                    Text(tr("Доход в месяц (необязательно)", "Monthly income (optional)"), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                    Field(income, { income = it }, money(0), number = true)
                    val inc = parseMoney(income) ?: 0
                    if (inc > 0) Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(90, 80, 70).forEach { p -> Chip(tr("$p% дохода", "$p% of income"), parseMoney(budget) == inc * p / 100) { budget = app.pennyling.domain.plainMoney(inc * p / 100) } }
                    }
                    Text(tr("Бюджет на месяц", "The month's budget"), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                    Field(budget, { budget = it }, money(0), number = true)
                    val b = parseMoney(budget) ?: 0
                    if (b > 0) Text(tr("Это около ${money(b / 30)} в день", "That's about ${money(b / 30)} a day"), style = T.bodyB.copy(color = C.acc), modifier = Modifier.padding(top = 8.dp))
                    Text(tr("Можно пропустить и задать позже в настройках.", "You can skip this and set it later in settings."), style = T.small, modifier = Modifier.padding(top = 8.dp))
                }
                else -> {
                    Text(tr("Что в кошельке", "What's in the wallet"), style = T.title)
                    Text(tr("Сколько сейчас наличных и на карте – чтобы остатки сходились. Копилки и другие счета добавите потом.", "How much cash and how much on the card now – so the balances match. Savings and other accounts can come later."), style = T.small, modifier = Modifier.padding(top = 8.dp))
                    Text(tr("Наличные", "Cash"), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                    Field(cash, { cash = it }, money(0), number = true)
                    Text(tr("Карта", "Card"), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                    Field(card, { card = it }, money(0), number = true)
                    Row(Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Mini("hamster", 60.dp); Spacer(Modifier.width(10.dp))
                        Text(tr("Хомяк уже ждёт в норке. Первые записи – и появятся первые яйца.", "The hamster is waiting in the burrow. First records bring the first eggs."), style = T.body, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (page > 0) Ghost(tr("Назад", "Back"), Modifier.weight(1f)) { page-- }
            Btn(if (page < 3) tr("Дальше", "Next") else tr("В норку!", "Into the burrow!"), Modifier.weight(2f)) {
                if (page < 3) page++ else c.scope.launch { c.begin(cur, payday, parseMoney(budget) ?: 0, parseMoney(cash) ?: 0, parseMoney(card) ?: 0, emptyList()) }
            }
        }
    }
    if (pickCur) Popup({ pickCur = false }) {
        Text(tr("Валюта", "Currency"), style = T.h2)
        Money.all.forEach { m -> Row2("${m.sign}  ${if (Lang.ru) m.ru else m.en}", m.code) { cur = m.code; pickCur = false } }
    }
}

@Composable
private fun Point(prop: String, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Prop(prop, 44.dp); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title, style = T.bodyB); Text(text, style = T.small) }
    }
}
