package app.pennyling.ui.stores

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.currentLanguage
import app.pennyling.data.Account
import app.pennyling.data.Backup
import app.pennyling.data.Cat
import app.pennyling.data.Heard
import app.pennyling.data.Tx
import app.pennyling.domain.Lang
import app.pennyling.domain.Money
import app.pennyling.domain.Pets
import app.pennyling.domain.Shop
import app.pennyling.domain.Span
import app.pennyling.domain.Stats
import app.pennyling.domain.count
import app.pennyling.domain.fmtDay
import app.pennyling.domain.fmtDayShort
import app.pennyling.domain.money
import app.pennyling.domain.parseMoney
import app.pennyling.domain.plainMoney
import app.pennyling.domain.relDay
import app.pennyling.domain.tr
import app.pennyling.domain.weekStart
import app.pennyling.notify.Crow
import app.pennyling.setLanguage
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.Screen
import app.pennyling.ui.art.BurrowScene
import app.pennyling.ui.art.Creature
import app.pennyling.ui.art.EggView
import app.pennyling.ui.art.Mini
import app.pennyling.ui.art.Pose
import app.pennyling.ui.art.Sky
import app.pennyling.ui.components.Badge
import app.pennyling.ui.components.Btn
import app.pennyling.ui.components.Card
import app.pennyling.ui.components.CatMark
import app.pennyling.ui.components.Chip
import app.pennyling.ui.components.CrumbPill
import app.pennyling.ui.components.Empty
import app.pennyling.ui.components.Field
import app.pennyling.ui.components.Fold
import app.pennyling.ui.components.Ghost
import app.pennyling.ui.components.Icon
import app.pennyling.ui.components.Icons
import app.pennyling.ui.components.Page
import app.pennyling.ui.components.Popup
import app.pennyling.ui.components.Row2
import app.pennyling.ui.components.Section
import app.pennyling.ui.components.Segments
import app.pennyling.ui.components.card
import app.pennyling.ui.components.catColor
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import kotlinx.coroutines.launch

// ---------------- accounts ----------------

fun accIcon(a: Account) = when (a.kind) { Account.CASH -> Icons.WALLET; Account.SAVE -> Icons.PIGGY; else -> Icons.CARD }

@Composable
fun AccountsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var edit by remember { mutableStateOf<Account?>(null) }
    val st = Stats(d.txs, Span(d.first, d.today, ""), d.catById, d.accounts, d.bills, d.s.budget, d.s.payday)
    Page(tr("Счета", "Accounts"), { nav.pop() }, action = { Text(tr("Новый", "New"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { edit = Account(name = "", order = d.accounts.size) }.padding(10.dp)) }) {
        Text(money(st.worth()), style = T.big)
        Text(tr("всего на счетах", "across all accounts"), style = T.small)
        Btn(tr("Перевести между счетами", "Move between accounts"), Modifier.fillMaxWidth().padding(top = 12.dp), small = true) { nav.push(Screen.Add(kind = Tx.MOVE)) }
        Section(tr("Счета", "Accounts"))
        Card { d.accounts.filter { !it.archived }.forEach { a -> Row2(a.name, when (a.kind) { Account.CASH -> tr("наличные", "cash"); Account.SAVE -> tr("копилка, из неё строится плотина", "savings – holds the dam"); else -> tr("карта или счёт", "card or account") },
            money(st.balance(a)), lead = { Badge(accIcon(a), if (a.kind == Account.SAVE) C.ok else C.acc, 36.dp) }) { edit = a } } }
        val arch = d.accounts.filter { it.archived }
        if (arch.isNotEmpty()) { Section(tr("В архиве", "Archived")); Card { arch.forEach { a -> Row2(a.name, value = money(st.balance(a))) { edit = a } } } }
        Text(tr("Остаток считается так: начальная сумма плюс доходы и переводы на счёт, минус траты и переводы со счёта. Если не сходится с банком – поправьте начальную сумму.",
            "A balance is the starting sum plus income and transfers in, minus spending and transfers out. If it doesn't match the bank, adjust the starting sum."), style = T.small, modifier = Modifier.padding(top = 12.dp))
    }
    edit?.let { a -> var name by remember(a) { mutableStateOf(a.name) }; var kind by remember(a) { mutableStateOf(a.kind) }
        var real by remember(a) { mutableStateOf(if (a.id == 0L) "" else plainMoney(st.balance(a))) }; var arch by remember(a) { mutableStateOf(a.archived) }
        Popup({ edit = null }) {
            Text(if (a.id == 0L) tr("Новый счёт", "A new account") else a.name, style = T.h2)
            TextField(tr("Название", "Name"), name, { name = it.take(30) }, tr("Карта, кошелёк, копилка…", "Card, wallet, savings…"))
            Text(tr("Что это", "What it is"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            Segments(listOf(tr("Наличные", "Cash"), tr("Карта", "Card"), tr("Копилка", "Savings")), listOf(Account.CASH, Account.CARD, Account.SAVE).indexOf(kind).coerceAtLeast(0)) { kind = listOf(Account.CASH, Account.CARD, Account.SAVE)[it] }
            MoneyField(tr("Сколько на нём сейчас", "What it holds now"), real, { real = it })
            if (a.id != 0L) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) { androidx.compose.material3.Switch(arch, { arch = it }); Spacer(Modifier.width(8.dp)); Text(tr("В архиве", "Archived"), style = T.body) }
            Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(top = 16.dp), enabled = name.isNotBlank()) {
                val want = parseMoney(real) ?: 0
                c.write { if (a.id == 0L) dao.insert(a.copy(name = name.trim(), kind = kind, start = want))
                    else dao.update(a.copy(name = name.trim(), kind = kind, archived = arch, start = a.start + (want - st.balance(a)))) }; edit = null }
        } }
}

// ---------------- categories ----------------

@Composable
fun CatsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var edit by remember { mutableStateOf<Cat?>(null) }
    Page(tr("Категории", "Categories"), { nav.pop() }, action = { Text(tr("Новая", "New"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable { edit = Cat(name = "", order = d.cats.size) }.padding(10.dp)) }) {
        Text(tr("У каждой категории трат свой хранитель. Можно задать месячный лимит – он покажется в летописи.", "Each spending category has its keeper. Set a monthly limit and the chronicle shows it."), style = T.small)
        Section(tr("Траты", "Spending"))
        Card { d.cats.filter { !it.income && !it.archived }.forEach { ct -> Row2(ct.name, ct.keeper.takeIf { it.isNotEmpty() }?.let { Pets.of(it).title }, if (ct.limit > 0) tr("лимит ${money(ct.limit)}", "limit ${money(ct.limit)}") else null, lead = { CatMark(ct, 36.dp, d.pets) }) { edit = ct } } }
        Section(tr("Доходы", "Income"))
        Card { d.cats.filter { it.income && !it.archived }.forEach { ct -> Row2(ct.name, lead = { CatMark(ct, 36.dp, d.pets) }) { edit = ct } } }
        val arch = d.cats.filter { it.archived }
        if (arch.isNotEmpty()) { Section(tr("Скрытые", "Hidden")); Card { arch.forEach { ct -> Row2(ct.name, lead = { CatMark(ct, 36.dp, d.pets) }) { edit = ct } } } }
    }
    edit?.let { ct -> var name by remember(ct) { mutableStateOf(ct.name) }; var income by remember(ct) { mutableStateOf(ct.income) }
        var limit by remember(ct) { mutableStateOf(if (ct.limit > 0) plainMoney(ct.limit) else "") }; var keeper by remember(ct) { mutableStateOf(ct.keeper) }
        var arch by remember(ct) { mutableStateOf(ct.archived) }
        Popup({ edit = null }) {
            Text(if (ct.id == 0L) tr("Новая категория", "A new category") else ct.name, style = T.h2)
            if (ct.id == 0L) Segments(listOf(tr("Трата", "Spending"), tr("Доход", "Income")), if (income) 1 else 0, Modifier.padding(top = 10.dp)) { income = it == 1 }
            TextField(tr("Название", "Name"), name, { name = it.take(30) })
            if (!income) {
                MoneyField(tr("Лимит в месяц (необязательно)", "Monthly limit (optional)"), limit, { limit = it })
                if (ct.key.isEmpty()) {
                    Text(tr("Хранитель", "Keeper"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    val free = d.pets.filter { it.hatched > 0 && Pets.of(it.species).cat.isEmpty() }.map { it.species }.distinct()
                    if (free.isEmpty()) Text(tr("Хранителем может стать найденный питомец без своей категории. Таких пока нет.", "A found pet without its own category can keep this one – they come later."), style = T.small)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        free.forEach { sp -> Box(Modifier.clip(RoundedCornerShape(50)).background(if (sp == keeper) C.accSoft else C.card2).clickable { keeper = if (keeper == sp) "" else sp }.padding(4.dp)) { Mini(sp, 40.dp) } }
                    }
                }
            }
            if (ct.id != 0L) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) { androidx.compose.material3.Switch(arch, { arch = it }); Spacer(Modifier.width(8.dp)); Text(tr("Скрыть", "Hide"), style = T.body) }
            Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(top = 16.dp), enabled = name.isNotBlank()) {
                c.write { val n = ct.copy(name = name.trim(), income = income, limit = parseMoney(limit) ?: 0, keeper = if (ct.key.isEmpty()) keeper else ct.keeper, archived = arch)
                    if (ct.id == 0L) dao.insert(n) else dao.update(n) }; edit = null }
        } }
}

// ---------------- the crow ----------------

@Composable
fun HeardPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val ctx = LocalContext.current
    val granted = remember { Crow.granted(ctx) }
    Page(tr("Ворона", "The crow"), { nav.pop() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Creature("crow", app.pennyling.domain.Stage.ADULT, Pose.WORK, 90.dp)
            Text(tr("Ворона слушает уведомления только тех приложений, которые вы отметили как банк, и превращает их в готовые записи. Остаётся выбрать категорию. Ничего не уходит с телефона.",
                "The crow listens only to the notifications of apps you mark as a bank and turns them into ready records. You just pick the category. Nothing leaves the phone."), style = T.small, modifier = Modifier.weight(1f).padding(start = 8.dp))
        }
        if (!granted) Btn(tr("Разрешить читать уведомления", "Allow reading notifications"), Modifier.fillMaxWidth().padding(top = 12.dp)) {
            runCatching { ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
        Section(tr("Ждут решения", "Waiting"))
        if (d.heard.isEmpty()) Empty(tr("Пока ничего не принесла", "Nothing brought yet"))
        d.heard.forEach { h -> HeardCard(h, d) }
        Section(tr("Банки", "Banks"))
        val pm = ctx.packageManager
        fun label(p: String) = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString() }.getOrDefault(p)
        Card {
            val all = (d.s.banks + d.s.heardApps).toList()
            if (all.isEmpty()) Text(tr("Когда банк пришлёт уведомление о покупке, его приложение появится здесь – отметьте его, и ворона начнёт слушать.", "When a bank sends a purchase notification its app shows up here – tick it and the crow starts listening."), style = T.small)
            all.forEach { p -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label(p), style = T.body, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(p in d.s.banks, { on -> c.write { settings.update { s -> s.copy(banks = if (on) s.banks + p else s.banks - p, heardApps = s.heardApps - p + if (!on) setOf(p) else emptySet()) } } })
            } }
        }
    }
}

@Composable
private fun HeardCard(h: Heard, d: Data) {
    val c = LocalContext.current.container
    val cats = d.activeCats.filter { it.income == h.income }
    // the category used last time for this place
    val guess = d.txs.firstOrNull { it.note.isNotBlank() && app.pennyling.domain.Notice.key(it.note) == app.pennyling.domain.Notice.key(h.place) }?.catId
    var cat by remember(h.id) { mutableLongStateOf(guess ?: 0L) }
    var note by remember(h.id) { mutableStateOf(h.place) }
    val acc = d.accounts.firstOrNull { if (h.cash) it.kind == Account.CASH else it.kind == Account.CARD }?.id ?: d.accounts.firstOrNull()?.id ?: 0
    Card(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text((if (h.income) "+" else "−") + money(h.amount), style = T.num.copy(color = if (h.income) C.ok else C.ink))
                Text(relDay(app.pennyling.domain.dayOf(h.at)), style = T.small)
            }
            Text(tr("Не то", "Not this"), style = T.smallB.copy(color = C.mute), modifier = Modifier.clickable { c.write { drop(h) } }.padding(8.dp))
        }
        Text(h.text, style = T.tiny, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Field(note, { note = it }, tr("Заметка", "Note"), Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cats.forEach { ct -> Box(Modifier.clip(RoundedCornerShape(50)).background(if (ct.id == cat) catColor(ct).copy(alpha = .25f) else C.card2).clickable { cat = ct.id }.padding(3.dp)) { CatMark(ct, 36.dp, d.pets) } }
        }
        Text(d.catById[cat]?.name ?: tr("Выберите категорию", "Pick a category"), style = T.tiny, modifier = Modifier.padding(top = 2.dp))
        Btn(tr("Записать", "Log it"), Modifier.fillMaxWidth().padding(top = 8.dp), small = true, enabled = cat != 0L) { c.write { take(h, cat, acc, note.trim()) } }
    }
}

// ---------------- the owl's weekly review ----------------

@Composable
fun ReviewPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val monday = weekStart(d.today); val from = monday - 7; val to = monday - 1
    val st = Stats(d.txs, Span(from, to, ""), d.catById, d.accounts, d.bills, d.s.budget, d.s.payday)
    Page(tr("Разбор недели", "The week in review"), { nav.pop() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Creature("owl", app.pennyling.domain.Stage.ADULT, Pose.WORK, 96.dp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("${fmtDayShort(from)} – ${fmtDayShort(to)}", style = T.h3)
                Text(tr("Спокойно посмотрим, куда ушли деньги за неделю. Без оценок – просто чтобы знать.", "Let's calmly see where the week went. No judging – just seeing."), style = T.small)
            }
        }
        Card(Modifier.padding(top = 12.dp)) {
            Row2(tr("Потрачено", "Spent"), st.spentBefore.takeIf { it > 0 }?.let { tr("неделей раньше ${money(it)}", "the week before ${money(it)}") }, money(st.spent))
            Row2(tr("Пришло", "Came in"), value = money(st.income))
            Row2(tr("Дней с записями", "Days with records"), value = "${st.txs.map { it.day }.toSet().size} / 7")
            Row2(tr("Снежных дней", "Snow days"), value = "${d.s.snow.count { it in from..to }}")
        }
        Section(tr("Куда ушла", "Where it went"))
        Card { st.byCat().take(4).forEach { cs -> Row2(cs.cat?.name ?: "–", "${(cs.share * 100).toInt()}%", money(cs.amount), lead = { CatMark(cs.cat, 34.dp, d.pets) }) } }
        st.biggest.firstOrNull()?.let { t -> Section(tr("Самая крупная", "The biggest")); Card { app.pennyling.ui.journal.TxRow(t, d) { nav.push(Screen.Add(t.id)) } } }
        val regrets = st.worthAll.filter { it.worth == 3 }
        if (regrets.isNotEmpty()) Text(tr("О ${count(regrets.size, "трате", "тратах", "тратах", "spend", "spends")} вы пожалели – на ${money(regrets.sumOf { it.amount })}.", "You regretted ${count(regrets.size, "spend", "spends", "spends", "spend", "spends")} – ${money(regrets.sumOf { it.amount })}."), style = T.body, modifier = Modifier.padding(top = 12.dp))
        val done = d.s.reviewed >= monday
        Btn(if (done) tr("Разобрано", "Reviewed") else tr("Разобрали – спасибо, сова", "Done – thank you, owl"), Modifier.fillMaxWidth().padding(top = 18.dp), enabled = !done) {
            c.write { weeklyReview(monday); say(tr("Сова довольна. Крошки в кладовой", "The owl is pleased. Crumbs in the pantry")) }; nav.pop() }
    }
}

// ---------------- the fair ----------------

@Composable
fun ShopPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    var preview by remember { mutableStateOf<String?>(null) }
    Page(tr("Ярмарка", "The fair"), { nav.pop() }, action = { CrumbPill(d.s.crumbs, Modifier.padding(end = 10.dp)) }) {
        Box(Modifier.fillMaxWidth().card(20.dp)) { BurrowScene(d.team, d.s.items - d.s.hidden, Pose.IDLE, preview = preview, sky = Sky.of(d.s), winter = d.ration.period.winter(d.today)) }
        Text(tr("Крошки даются за честный учёт: записи, снежные дни, ответы, решения, задания. Деньги здесь не тратятся.", "Crumbs come from honest books: records, snow days, answers, decisions, tasks. No real money here."), style = T.small, modifier = Modifier.padding(top = 10.dp))
        Section(tr("Яйцо с ярмарки", "An egg from the fair"))
        val left = Pets.all.count { sp -> !sp.rare && d.pets.none { it.species == sp.id } }
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EggView("hamster", 64.dp, .3f); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(tr("Яйцо-сюрприз", "A surprise egg"), style = T.bodyB); Text(if (left == 0) tr("Все уже живут в норке", "Everyone already lives here") else tr("Кто-то, кого ещё нет в норке", "Someone not yet in the burrow"), style = T.small) }
                Btn("${Shop.EGG}", small = true, enabled = d.s.crumbs >= Shop.EGG && left > 0) { c.write { if (buyEgg()) say(tr("Яйцо в норке!", "An egg in the burrow!")) } }
            }
        }
        Section(tr("Для норки", "For the burrow"))
        Shop.all.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { it0 ->
                val have = it0.id in d.s.items; val hidden = it0.id in d.s.hidden
                Column(Modifier.weight(1f).card(16.dp).clickable { preview = if (preview == it0.id) null else it0.id }.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ItemPic(it0.id)
                    Text(it0.title, style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    when {
                        have -> Text(if (hidden) tr("Достать", "Bring out") else tr("Убрать", "Put away"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable {
                            c.write { settings.update { s -> s.copy(hidden = if (hidden) s.hidden - it0.id else s.hidden + it0.id) } } }.padding(4.dp))
                        else -> Btn("${it0.price}", Modifier.padding(top = 4.dp), small = true, enabled = d.s.crumbs >= it0.price) { c.write { if (buy(it0.id)) say(tr("Куплено: ${it0.title}", "Bought: ${it0.title}")) }; preview = null }
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        } }
    }
}

@Composable
private fun ItemPic(id: String) {
    val ctx = LocalContext.current
    val img = app.pennyling.ui.art.Art.item(ctx, id)
    if (img != null) androidx.compose.foundation.Image(img, null, Modifier.size(72.dp), filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium)
    else Box(Modifier.size(72.dp))
}

// ---------------- settings ----------------

object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "pennyling"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
    const val SITE = "https://bigdevwhale.github.io/stillcraft/"
    const val CODE = "https://github.com/bigdevwhale/pennyling"
}

@Composable
private fun LinkRow(icon: Icons, tint: androidx.compose.ui.graphics.Color, title: String, sub: String?, url: String) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Badge(icon, tint, 36.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        Icon(Icons.NEXT, C.mute, 18.dp)
    }
}

@Composable
private fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Ghost("−", small = true, onClick = onMinus)
        Text(value, style = T.bodyB, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Ghost("+", small = true, onClick = onPlus)
    }
}

@Composable
fun SettingsPage(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = d.s
    var cur by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { val text = Backup.export(c); runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            c.say(tr("Копия сохранена", "Backup saved")) } }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { val text = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull().orEmpty()
            c.say(if (Backup.import(c, text)) tr("Норка восстановлена", "The burrow is restored") else tr("Это не копия Pennyling", "That's not a Pennyling backup")) } }
    Page(tr("Настройки", "Settings"), { nav.pop() }) {
        Fold(Icons.ACORN, C.acc, tr("Месяц и бюджет", "The month and budget"), tr("зарплата ${s.payday}-го · ${money(s.budget)}", "payday on the ${s.payday} · ${money(s.budget)}")) {
            Text(tr("День зарплаты – с него начинается месяц норки", "Payday – the burrow's month starts on it"), style = T.small)
            Stepper(tr("${s.payday}-е число", "day ${s.payday}"), { c.write { settings.update { it.copy(payday = (it.payday - 1).coerceAtLeast(1)) } } }, { c.write { settings.update { it.copy(payday = (it.payday + 1).coerceAtMost(31)) } } })
            var b by remember { mutableStateOf(if (s.budget > 0) plainMoney(s.budget) else "") }
            MoneyField(tr("Бюджет на месяц – все траты вместе с платежами", "The month's budget – all spending with bills"), b, { b = it; parseMoney(it)?.let { v -> c.write { settings.update { st -> st.copy(budget = v) } } } })
            val st = Stats(d.txs, Span(d.first, d.today, ""), d.catById, d.accounts, d.bills, s.budget, s.payday)
            if (st.monthlySpend > 0) Text(tr("Обычно уходит ${money(st.monthlySpend)} в месяц", "Usually ${money(st.monthlySpend)} goes a month"), style = T.small, modifier = Modifier.padding(top = 6.dp))
            Text(tr("Валюта", "Currency"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Ghost("${Money.cur.sign} · ${if (Lang.ru) Money.cur.ru else Money.cur.en}", Modifier.fillMaxWidth(), small = true) { cur = true }
        }
        Fold(Icons.LIST, C.acc, tr("Категории и лимиты", "Categories and limits"), tr("хранители, месячные лимиты", "keepers, monthly limits")) {
            Ghost(tr("Открыть", "Open"), Modifier.fillMaxWidth(), small = true) { nav.push(Screen.Cats) } }
        Fold(Icons.PAW, C.gold, tr("Норка", "The burrow"), tr("вопросы, чердак, муравей", "questions, attic, the ant")) {
            Text(tr("Спрашивать «Стоило того?» через", "Ask «Worth it?» after"), style = T.small)
            Stepper(tr("${s.worthDays} дн.", "${s.worthDays} days"), { c.write { settings.update { it.copy(worthDays = (it.worthDays - 1).coerceAtLeast(1)) } } }, { c.write { settings.update { it.copy(worthDays = (it.worthDays + 1).coerceAtMost(30)) } } })
            Text(tr("Желания остывают на чердаке", "Wishes cool in the attic for"), style = T.small, modifier = Modifier.padding(top = 10.dp))
            Stepper(tr("${s.coolDays} дн.", "${s.coolDays} days"), { c.write { settings.update { it.copy(coolDays = (it.coolDays - 1).coerceAtLeast(1)) } } }, { c.write { settings.update { it.copy(coolDays = (it.coolDays + 1).coerceAtMost(90)) } } })
            var r by remember { mutableStateOf(plainMoney(s.roundTo)) }
            MoneyField(tr("Муравей округляет траты до", "The ant rounds spends up to"), r, { r = it; parseMoney(it)?.takeIf { v -> v > 0 }?.let { v -> c.write { settings.update { st -> st.copy(roundTo = v) } } } })
            var h by remember { mutableStateOf(if (s.hourly > 0) plainMoney(s.hourly) else "") }
            MoneyField(tr("Сколько вы зарабатываете в час (для «цены в часах»)", "What an hour of work brings (for «cost in hours»)"), h, { h = it; c.write { settings.update { st -> st.copy(hourly = parseMoney(it) ?: 0) } } })
        }
        Fold(Icons.BELL, C.ok, tr("Вечерний вопрос", "The evening question"), if (s.evening < 0) tr("выключен", "off") else tr("в ${s.evening}:00", "at ${s.evening}:00")) {
            Text(tr("Если за день ничего не записано, вожак спросит, на что сегодня ушли деньги.", "If nothing was logged today, the leader asks what the day cost."), style = T.small)
            Stepper(if (s.evening < 0) tr("выключен", "off") else "${s.evening}:00", { c.write { settings.update { it.copy(evening = if (it.evening <= 17) -1 else it.evening - 1) } } }, { c.write { settings.update { it.copy(evening = if (it.evening < 0) 18 else (it.evening + 1).coerceAtMost(23)) } } })
            if (android.os.Build.VERSION.SDK_INT >= 33) { val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                Ghost(tr("Разрешить уведомления", "Allow notifications"), Modifier.fillMaxWidth().padding(top = 8.dp), small = true) { ask.launch(android.Manifest.permission.POST_NOTIFICATIONS) } }
        }
        Fold(Icons.BELL, C.ink, tr("Ворона", "The crow"), tr("записи из уведомлений банка", "records from bank notifications")) { Ghost(tr("Открыть", "Open"), Modifier.fillMaxWidth(), small = true) { nav.push(Screen.Heard) } }
        Fold(Icons.PALETTE, C.acc, tr("Вид и язык", "Look and language"), null) {
            Segments(listOf(tr("Светлая", "Light"), tr("Тёмная", "Dark"), tr("Как в системе", "System")), when (s.theme) { "light" -> 0; "dark" -> 1; else -> 2 }) { i -> c.write { settings.update { it.copy(theme = listOf("light", "dark", "system")[i]) } } }
            Spacer(Modifier.height(10.dp))
            val lang = currentLanguage()
            Segments(listOf("Русский", "English", tr("Как в системе", "System")), when (lang) { "ru" -> 0; "en" -> 1; else -> 2 }) { i -> setLanguage(listOf("ru", "en", "")[i]) }
            Text(tr("За окном – погода в городе: ${s.town.ifBlank { "не определён" }}", "Out of the window – the weather in: ${s.town.ifBlank { "unknown" }}"), style = T.small, modifier = Modifier.padding(top = 10.dp))
        }
        Fold(Icons.SAVE, C.ok, tr("Резервная копия", "Backup"), tr("файл со всей норкой", "one file with the whole burrow")) {
            Btn(tr("Сохранить копию", "Save a backup"), Modifier.fillMaxWidth(), small = true) { save.launch("pennyling-${java.time.LocalDate.now()}.json") }
            Ghost(tr("Восстановить из файла", "Restore from a file"), Modifier.fillMaxWidth().padding(top = 8.dp), small = true) { open.launch(arrayOf("application/json", "*/*")) }
            Text(tr("Восстановление заменит всё, что есть сейчас.", "Restoring replaces everything there is now."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }
        Fold(Icons.HEART, C.bad, tr("Поддержать", "Support"), tr("StillCraft держится на поддержке пользователей", "StillCraft lives on goodwill")) { LinkRow(Icons.HEART, C.bad, tr("Поддержать разработку", "Support development"), null, Community.SUPPORT) }
        Fold(Icons.GLOBE, C.ok, "StillCraft", tr("Pennyling – одно из небольших приложений StillCraft", "Pennyling is one of the small StillCraft apps")) {
            LinkRow(Icons.HEART, C.acc, tr("Сообщество в Telegram", "Telegram community"), tr("новости, обсуждения, другие приложения", "news, talk and the other apps"), Community.CHAT)
            LinkRow(Icons.CLOSE, C.bad, tr("Сообщить об ошибке", "Report a bug"), tr("через бота StillCraft", "through the StillCraft bot"), Community.BUG)
            LinkRow(Icons.SPARK, C.gold, tr("Предложить идею", "Suggest an idea"), null, Community.IDEA)
            LinkRow(Icons.STAR, C.ok, tr("Следить за обновлениями", "Follow updates"), tr("бот напишет, когда выйдет новая версия", "the bot writes when a new version is out"), Community.FOLLOW)
            LinkRow(Icons.HOME, C.acc, tr("Все приложения StillCraft", "All StillCraft apps"), null, Community.SITE)
            LinkRow(Icons.LIST, C.mute, tr("Исходный код", "Source code"), "GitHub", Community.CODE)
        }
        Fold(Icons.SPARK, C.acc, tr("Подсказки", "Hints"), tr("короткие пояснения на экранах", "short notes on the screens")) {
            Ghost(tr("Показать заново", "Show again"), Modifier.fillMaxWidth(), small = true) { c.write { settings.update { it.copy(seen = emptySet()) } }; c.say(tr("Подсказки покажутся снова", "Hints will show again")) } }
        Text(tr("Pennyling работает без аккаунта. Все записи и питомцы хранятся только на телефоне; в интернет уходят лишь координаты города для погоды за окном. Погода – Open-Meteo и MET Norway.",
            "Pennyling needs no account. Records and pets stay on the phone; only the town's coordinates go online, for the weather outside the window. Weather: Open-Meteo and MET Norway."), style = T.tiny, modifier = Modifier.padding(top = 14.dp))
    }
    if (cur) Popup({ cur = false }) {
        Text(tr("Валюта", "Currency"), style = T.h2)
        Money.all.forEach { m -> Row2("${m.sign}  ${if (Lang.ru) m.ru else m.en}", m.code) { c.write { settings.update { it.copy(currency = m.code) } }; Money.set(m.code); cur = false } }
    }
}
