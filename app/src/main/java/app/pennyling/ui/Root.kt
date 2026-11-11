package app.pennyling.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.data.Account
import app.pennyling.data.Bill
import app.pennyling.data.Cat
import app.pennyling.data.Debt
import app.pennyling.data.Goal
import app.pennyling.data.Heard
import app.pennyling.data.Move
import app.pennyling.data.Pet
import app.pennyling.data.Settings
import app.pennyling.data.Tx
import app.pennyling.data.Wish
import app.pennyling.domain.Engine
import app.pennyling.domain.Ration
import app.pennyling.domain.today
import app.pennyling.domain.tr
import app.pennyling.ui.components.Icon
import app.pennyling.ui.components.Icons
import app.pennyling.ui.home.HomeScreen
import app.pennyling.ui.home.Inbox
import app.pennyling.ui.journal.AddPage
import app.pennyling.ui.journal.JournalScreen
import app.pennyling.ui.pets.PetPage
import app.pennyling.ui.pets.PetsScreen
import app.pennyling.ui.share.ShareReq
import app.pennyling.ui.share.ShareSheet
import app.pennyling.ui.stats.StatsScreen
import app.pennyling.ui.stats.WrappedPage
import app.pennyling.ui.stores.AccountsPage
import app.pennyling.ui.stores.CatsPage
import app.pennyling.ui.stores.CellarPage
import app.pennyling.ui.stores.DamPage
import app.pennyling.ui.stores.DebtsPage
import app.pennyling.ui.stores.GoalsPage
import app.pennyling.ui.stores.HeardPage
import app.pennyling.ui.stores.ReviewPage
import app.pennyling.ui.stores.SettingsPage
import app.pennyling.ui.stores.ShopPage
import app.pennyling.ui.stores.WishesPage
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T
import kotlinx.coroutines.delay

/** Where a demo/screenshot launch opens (set from the intent in demo builds only). */
object Start { var tab = 0; var open: String? = null }

/** A page over the tabs. */
sealed class Screen {
    /** A record: new ([id] 0) of [kind], or an existing one to edit. */
    data class Add(val id: Long = 0, val kind: Int = Tx.SPEND, val catId: Long = 0, val amount: Long = 0, val note: String = "") : Screen()
    data class PetInfo(val id: Long) : Screen()
    data object Shop : Screen()
    data object Settings : Screen()
    data object Goals : Screen()
    data object Wishes : Screen()
    data object Cellar : Screen()
    data object Debts : Screen()
    data object Dam : Screen()
    data object Accounts : Screen()
    data object Cats : Screen()
    data object Heard : Screen()
    data object Review : Screen()
    data class Wrapped(val year: Int) : Screen()
}

class Nav {
    var tab by mutableIntStateOf(0)
    val stack = mutableStateListOf<Screen>()
    var share by mutableStateOf<ShareReq?>(null)
    /** Journal filter set from elsewhere (a category tapped in stats). */
    var filterCat by mutableStateOf(0L)
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
}

/** Everything the screens read, collected once. */
class Data(val s: Settings, val txs: List<Tx>, val cats: List<Cat>, val accounts: List<Account>, val bills: List<Bill>, val goals: List<Goal>,
           val debts: List<Debt>, val moves: List<Move>, val wishes: List<Wish>, val pets: List<Pet>, val heard: List<Heard>, val today: Long = today()) {
    val catById = cats.associateBy { it.id }
    val accById = accounts.associateBy { it.id }
    val team = Engine.team(pets)
    val leader = team.firstOrNull { it.slot == 0 } ?: pets.firstOrNull { it.hatched > 0 }
    val ration = Ration.of(txs, bills, s.budget, s.payday, today)
    val first = txs.minOfOrNull { it.day } ?: today
    fun saved(g: Goal) = moves.filter { it.kind == "goal" && it.refId == g.id }.sumOf { it.amount }
    fun paid(d: Debt) = moves.filter { it.kind == "debt" && it.refId == d.id }.sumOf { it.amount }
    /** Records old enough to ask «was it worth it?» about. */
    val toAsk = txs.filter { it.kind == Tx.SPEND && it.worth == 0 && it.billId == 0L && today - it.day >= s.worthDays && today - it.day <= 45 }
    val activeCats = cats.filter { !it.archived }
}

@Composable
fun Root(s: Settings) {
    val c = LocalContext.current.container
    val txs by c.dao.txs().collectAsState(initial = emptyList())
    val cats by c.dao.cats().collectAsState(initial = emptyList())
    val accounts by c.dao.accounts().collectAsState(initial = emptyList())
    val bills by c.dao.bills().collectAsState(initial = emptyList())
    val goals by c.dao.goals().collectAsState(initial = emptyList())
    val debts by c.dao.debts().collectAsState(initial = emptyList())
    val moves by c.dao.moves().collectAsState(initial = emptyList())
    val wishes by c.dao.wishes().collectAsState(initial = emptyList())
    val pets by c.dao.pets().collectAsState(initial = emptyList())
    val heard by c.dao.heard().collectAsState(initial = emptyList())
    val data = remember(s, txs, cats, accounts, bills, goals, debts, moves, wishes, pets, heard) { Data(s, txs, cats, accounts, bills, goals, debts, moves, wishes, pets, heard, c.clock()) }
    val nav = remember { Nav().apply { tab = Start.tab } }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.news.collect { note = it; delay(2600); note = null } }
    LaunchedEffect(Unit) { c.tick() }
    LaunchedEffect(Unit) { if (!app.pennyling.BuildConfig.SHOTS) while (true) { c.refreshOutside(); delay(30 * 60_000L) } }
    LaunchedEffect(Unit) {
        when (val o = Start.open) {
            null -> {}
            "add" -> nav.push(Screen.Add()); "shop" -> nav.push(Screen.Shop); "settings" -> nav.push(Screen.Settings); "goals" -> nav.push(Screen.Goals)
            "wishes" -> nav.push(Screen.Wishes); "cellar" -> nav.push(Screen.Cellar); "debts" -> nav.push(Screen.Debts); "dam" -> nav.push(Screen.Dam)
            "accounts" -> nav.push(Screen.Accounts); "cats" -> nav.push(Screen.Cats); "review" -> nav.push(Screen.Review); "heard" -> nav.push(Screen.Heard)
            "wrapped" -> nav.push(Screen.Wrapped(java.time.LocalDate.now().year))
            else -> when {
                o.startsWith("pet:") -> nav.push(Screen.PetInfo(o.drop(4).toLongOrNull() ?: -1))
                o.startsWith("tx:") -> nav.push(Screen.Add(o.drop(3).toLong()))
                o.startsWith("share:") -> nav.share = ShareReq(app.pennyling.ui.share.CardKind.valueOf(o.drop(6).substringBefore(':')), o.drop(6).substringAfter(':', "0").toLong())
            }
        }
        Start.open = null
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                when (nav.tab) {
                    0 -> HomeScreen(data, nav)
                    1 -> JournalScreen(data, nav)
                    2 -> PetsScreen(data, nav)
                    else -> StatsScreen(data, nav)
                }
            }
            Dock(nav.tab, { nav.tab = it }) { nav.push(Screen.Add()) }
        }
        for (scr in nav.stack) Box(Modifier.fillMaxSize()) {
            when (scr) {
                is Screen.Add -> AddPage(scr, data, nav)
                is Screen.PetInfo -> PetPage(scr.id, data, nav)
                Screen.Shop -> ShopPage(data, nav)
                Screen.Settings -> SettingsPage(data, nav)
                Screen.Goals -> GoalsPage(data, nav)
                Screen.Wishes -> WishesPage(data, nav)
                Screen.Cellar -> CellarPage(data, nav)
                Screen.Debts -> DebtsPage(data, nav)
                Screen.Dam -> DamPage(data, nav)
                Screen.Accounts -> AccountsPage(data, nav)
                Screen.Cats -> CatsPage(data, nav)
                Screen.Heard -> HeardPage(data, nav)
                Screen.Review -> ReviewPage(data, nav)
                is Screen.Wrapped -> WrappedPage(scr.year, data, nav)
            }
        }
        if (nav.stack.isEmpty() && !app.pennyling.BuildConfig.SHOTS) Inbox(data, nav)
        // a first-visit introduction to whatever is on top (after any news)
        if (!app.pennyling.BuildConfig.SHOTS && s.inbox.isEmpty() && nav.share == null) introFor(nav.stack.lastOrNull(), nav.tab)?.takeIf { it.id !in s.seen }?.let { IntroSheet(it) }
        nav.share?.let { ShareSheet(it, data) { nav.share = null } }
        AnimatedVisibility(note != null, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 18.dp, end = 18.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(note ?: "", style = T.bodyB.copy(color = C.bg), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.ink).padding(horizontal = 16.dp, vertical = 11.dp))
        }
    }
}

@Composable
private fun Dock(sel: Int, onSel: (Int) -> Unit, onAdd: () -> Unit) {
    val tabs = listOf(tr("Норка", "Burrow") to Icons.HOME, tr("Журнал", "Journal") to Icons.LOG, tr("Питомцы", "Pets") to Icons.PAW, tr("Летопись", "Chronicle") to Icons.STATS)
    Row(Modifier.fillMaxWidth().shadow(12.dp, ambientColor = C.shadow, spotColor = C.shadow).background(C.nav).navigationBarsPadding().padding(top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { i, (label, ic) ->
            if (i == 2) Box(Modifier.weight(1.1f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(C.acc).clickable(onClick = onAdd), contentAlignment = Alignment.Center) { Icon(Icons.PLUS, C.accInk, 28.dp) }
            }
            val col = if (i == sel) C.acc else C.mute
            Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSel(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(ic, col, 24.dp)
                Text(label, style = T.tiny.copy(color = col), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
