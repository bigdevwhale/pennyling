package app.pennyling.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.domain.Stage
import app.pennyling.domain.tr
import app.pennyling.ui.art.Creature
import app.pennyling.ui.art.Pose
import app.pennyling.ui.components.Btn
import app.pennyling.ui.components.tap
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T

/** A first-visit introduction to one screen: who explains it, the title and a few short points. */
class Intro(val id: String, val species: String, val title: String, val points: List<String>)

/** The introduction for what is on top now: the page over the tabs, or the tab itself. */
fun introFor(top: Screen?, tab: Int): Intro? = when (top) {
    null -> when (tab) {
        0 -> Intro("i.home", "hamster", tr("Это ваша норка", "This is your burrow"), listOf(
            tr("Бюджет на месяц – запас в кладовой. Чем меньше остаётся, тем меньше мешков в углу.", "The month's budget is the pantry. The sacks in the corner shrink with it."),
            tr("Паёк – сколько можно потратить сегодня. С каждой тратой желудей становится меньше.", "The ration is what today may cost. The acorns melt with every spend."),
            tr("За окном к концу месяца наступает зима, после зарплаты снова осень.", "Outside, winter comes as the month runs out; after payday it is autumn again."),
            tr("Кнопка «+» внизу – записать трату. Нажмите на питомца, чтобы открыть его.", "The «+» below logs a spend. Tap a pet to open it.")))
        1 -> Intro("i.journal", "mole", tr("Журнал", "The journal"), listOf(
            tr("Все записи по дням. Стрелками листайте месяцы.", "Every record by day. The arrows turn the months."),
            tr("«Календарь» показывает, какие дни вышли дорогими, а снежинка – дни без трат.", "«Calendar» shows the dear days; a snowflake marks days with no spending."),
            tr("Нажмите на запись, чтобы изменить или удалить её.", "Tap a record to change or delete it."),
            tr("Лупа ищет по заметке, категории и сумме.", "The magnifier searches notes, categories and sums.")))
        2 -> Intro("i.pets", "squirrel", tr("Питомцы", "Pets"), listOf(
            tr("У каждой категории трат есть хранитель. Он вылупляется из первых записей в ней.", "Every spending category has a keeper. It hatches from the first records in it."),
            tr("Ещё десять питомцев появляются за полезные привычки. Нажмите на серый силуэт – узнаете, за какие.", "Ten more turn up for good habits. Tap a grey shadow to see which."),
            tr("Вожак и два спутника живут в норке и приносят вдвое больше крошек за то, за что отвечают.", "The leader and two companions live in the burrow and double the crumbs for their thing."),
            tr("Питомцы растут от записей, а не от экономии.", "Pets grow on records, not on saving.")))
        else -> Intro("i.stats", "marmot", tr("Летопись", "The chronicle"), listOf(
            tr("Вверху выберите срок: до зарплаты, месяц, год или всё время.", "Pick the span at the top: to payday, a month, a year or all time."),
            tr("«Обзор» – главные цифры и прогноз: хватит ли запаса до зарплаты.", "«Overview» – the main numbers and whether the pantry lasts to payday."),
            tr("«Категории» – куда уходят деньги, «Привычки» – когда и на что вы тратите охотнее всего.", "«Categories» – where money goes; «Habits» – when and on what you spend most easily."),
            tr("«Капитал» – подписки, сбережения и калькулятор «что если».", "«Wealth» – subscriptions, savings and the «what if» calculator.")))
    }
    is Screen.Add -> if (top.id != 0L) null else Intro("i.add", "raccoon", tr("Новая запись", "A new record"), listOf(
        tr("Наберите сумму на клавиатуре внизу.", "Type the sum on the keypad below."),
        tr("Выберите категорию – её хранитель получит опыт, а вы крошки.", "Pick a category – its keeper gets XP and you get crumbs."),
        tr("«Как обычно» – частые траты в одно касание.", "«The usual» – frequent spends in one tap."),
        tr("Счёт и день меняются кнопками под категориями.", "The account and the day change with the buttons under the categories.")))
    is Screen.PetInfo -> Intro("i.pet", "hamster", tr("Страница питомца", "A pet's page"), listOf(
        tr("Кнопки под питомцем меняют его позу.", "The buttons under the pet show its poses."),
        tr("«Место»: вожак и спутники стоят в норке и дают бонус к крошкам.", "«Place»: the leader and companions stand in the burrow and add crumbs."),
        tr("Карандаш рядом с именем – чтобы дать новое имя.", "The pencil by the name renames it."),
        tr("Питомец проходит четыре ступени: от малыша до старейшины.", "A pet goes through four stages, from baby to elder.")))
    Screen.Goals -> Intro("i.goals", "tortoise", tr("Банки", "Jars"), listOf(
        tr("Каждая цель – банка варенья: на отпуск, велосипед, подушку.", "Each goal is a jar of jam: a holiday, a bike, a cushion."),
        tr("Кнопкой «Положить» отмечайте, что отложили. Если задать срок, приложение подскажет, сколько откладывать в месяц.", "«Put in» marks what you set aside. A date tells how much a month."),
        tr("В полной банке находится яйцо.", "A full jar holds an egg."),
        tr("Деньги между счетами при этом не переводятся – банка лишь отмечает отложенное.", "No money moves between accounts – a jar only marks what is set aside.")))
    Screen.Wishes -> Intro("i.wishes", "fox", tr("Чердак желаний", "The attic of wishes"), listOf(
        tr("Хочется что-то купить? Поднимите желание на чердак.", "Want to buy something? Take the wish up to the attic."),
        tr("Пока желание остывает, лиса его сторожит. Сколько ждать, решаете вы.", "While it cools the fox guards it. You choose how long."),
        tr("Расхотелось – сумма запишется в «не потрачено». Всё ещё хочется – покупайте спокойно.", "Let it go and the sum counts as «not spent». Still want it – buy it with peace of mind.")))
    Screen.Cellar -> Intro("i.cellar", "bat", tr("Погреб", "The cellar"), listOf(
        tr("Здесь регулярные платежи: подписки, аренда, зарплата.", "Regular payments live here: subscriptions, rent, salary."),
        tr("В свой день они записываются сами, а деньги на них заранее откладываются из пайка.", "They log themselves on their day and are kept aside from the ration in advance."),
        tr("Раз в три месяца погреб спросит про каждую подписку: пользуетесь?", "Every three months the cellar asks about each subscription: still using it?"),
        tr("Сорока находит траты, которые повторяются каждый месяц, и предлагает перенести их сюда.", "The magpie finds spends that repeat monthly and offers them here.")))
    Screen.Debts -> Intro("i.debts", "badger", tr("Валуны у входа", "Boulders at the door"), listOf(
        tr("Каждый долг – валун. Укажите сумму, ставку и минимальный платёж.", "Each debt is a boulder. Give the sum, the rate and the minimum payment."),
        tr("С каждым платежом валун становится меньше.", "«Make a payment» shrinks the boulder."),
        tr("Если долгов несколько, приложение сравнит «снежный ком» и «лавину».", "With several debts the app compares «snowball» and «avalanche».")))
    Screen.Dam -> Intro("i.dam", "beaver", tr("Плотина", "The dam"), listOf(
        tr("Плотина – подушка безопасности: на сколько месяцев хватит сбережений без дохода.", "The dam is your cushion: how many months your savings last without income."),
        tr("Плотина складывается из счетов с типом «Копилка».", "It is held by accounts of the «Savings» kind."),
        tr("Хорошая цель – от трёх до шести месяцев трат.", "A good aim is three to six months of spending.")))
    Screen.Accounts -> Intro("i.accounts", "hamster", tr("Счета", "Accounts"), listOf(
        tr("Наличные, карты и копилки – всё, где лежат деньги.", "Cash, cards and savings – everywhere money lives."),
        tr("Остаток считается по записям. Не сходится с банком – нажмите на счёт и поправьте сумму.", "Balances come from records. If one doesn't match the bank, tap it and fix the sum."),
        tr("Переводы между счетами не считаются тратами.", "Transfers between accounts don't count as spending.")))
    Screen.Cats -> Intro("i.cats", "squirrel", tr("Категории", "Categories"), listOf(
        tr("Нажмите на категорию, чтобы переименовать её или задать лимит на месяц.", "Tap a category to rename it or set a monthly limit."),
        tr("Можно добавить свои категории – хранителем станет найденный питомец.", "You can add your own – a found pet becomes its keeper."),
        tr("Ненужные категории можно скрыть: записи в них останутся.", "Categories you don't need can be hidden; their records stay.")))
    Screen.Heard -> Intro("i.heard", "crow", tr("Ворона", "The crow"), listOf(
        tr("Ворона читает уведомления банка и превращает их в готовые записи.", "The crow reads bank notifications and turns them into ready records."),
        tr("Дайте доступ к уведомлениям и отметьте приложение своего банка.", "Allow notification access and tick your bank's app."),
        tr("Останется выбрать категорию и нажать «Записать». Ничего не уходит с телефона.", "Then pick a category and tap «Log it». Nothing leaves the phone.")))
    Screen.Review -> Intro("i.review", "owl", tr("Разбор недели", "The week in review"), listOf(
        tr("По понедельникам сова показывает, куда ушли деньги за прошлую неделю.", "On Mondays the owl shows where last week's money went."),
        tr("Без оценок: просто посмотреть и сделать выводы.", "No judging: just look and draw your own conclusions."),
        tr("За разбор – крошки.", "The review brings crumbs.")))
    Screen.Shop -> Intro("i.shop", "chipmunk", tr("Ярмарка", "The fair"), listOf(
        tr("Крошки тратятся здесь: на мебель для норки и яйца-сюрпризы.", "Crumbs are spent here: on furniture and surprise eggs."),
        tr("Нажмите на вещь, чтобы примерить её в норке до покупки.", "Tap a thing to try it in the burrow before buying."),
        tr("Купленное можно убрать и достать снова.", "Bought things can be put away and brought back.")))
    Screen.Settings -> Intro("i.settings", "hamster", tr("Настройки", "Settings"), listOf(
        tr("День зарплаты и бюджет на месяц – из них считается паёк.", "Payday and the month's budget – the ration is counted from them."),
        tr("Здесь же категории, вечерний вопрос, ворона и резервная копия.", "Categories, the evening question, the crow and backups are here too."),
        tr("«Подсказки» вернут эти знакомства на всех экранах.", "«Hints» bring these introductions back on every screen.")))
    is Screen.Wrapped -> Intro("i.wrapped", "hamster", tr("Год в норке", "A year in the burrow"), listOf(
        tr("Итоги года собраны в карточки – листайте вниз.", "The year's story scrolls card by card."),
        tr("Внизу – открытка, чтобы поделиться годом.", "At the bottom is a card to share the year.")))
}

/** The introduction as a card sliding up over the screen; «Got it» stores its id in Settings.seen. */
@Composable
fun IntroSheet(intro: Intro) {
    val c = LocalContext.current.container
    fun done() = c.write { settings.update { it.copy(seen = it.seen + intro.id) } }
    BackHandler { done() }
    val a = remember(intro.id) { Animatable(0f) }
    LaunchedEffect(intro.id) { a.animateTo(1f, tween(320)) }
    Box(Modifier.fillMaxSize().background(Color(0x99000000).copy(alpha = .6f * a.value)).tap {}, contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().offset(y = ((1 - a.value) * 220).dp).alpha(a.value).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.bg)
            .navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Creature(intro.species, Stage.ADULT, Pose.CHEER, 96.dp)
                Column(Modifier.weight(1f).padding(start = 6.dp, bottom = 14.dp)) {
                    Text(intro.title, style = T.h2)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                intro.points.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(C.accSoft), contentAlignment = Alignment.Center) { Text("${i + 1}", style = T.smallB.copy(color = C.acc)) }
                        Spacer(Modifier.width(10.dp))
                        Text(s, style = T.body, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Btn(tr("Понятно", "Got it"), Modifier.fillMaxWidth()) { done() }
        }
    }
}
