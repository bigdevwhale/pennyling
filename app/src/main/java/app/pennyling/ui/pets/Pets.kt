package app.pennyling.ui.pets

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pennyling.container
import app.pennyling.data.Pet
import app.pennyling.data.Tx
import app.pennyling.domain.Pets
import app.pennyling.domain.Stage
import app.pennyling.domain.Species
import app.pennyling.domain.fmtDayYear
import app.pennyling.domain.money
import app.pennyling.domain.stage
import app.pennyling.domain.title
import app.pennyling.domain.toNext
import app.pennyling.domain.tr
import app.pennyling.ui.Data
import app.pennyling.ui.Nav
import app.pennyling.ui.Screen
import app.pennyling.ui.art.Creature
import app.pennyling.ui.art.Mini
import app.pennyling.ui.art.PetView
import app.pennyling.ui.art.Pose
import app.pennyling.ui.components.Btn
import app.pennyling.ui.components.Card
import app.pennyling.ui.components.Field
import app.pennyling.ui.components.Ghost
import app.pennyling.ui.components.Hint
import app.pennyling.ui.components.Icons
import app.pennyling.ui.components.Meter
import app.pennyling.ui.components.Page
import app.pennyling.ui.components.Popup
import app.pennyling.ui.components.Row2
import app.pennyling.ui.components.Section
import app.pennyling.ui.components.card
import app.pennyling.ui.share.CardKind
import app.pennyling.ui.share.ShareReq
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T

@Composable
fun PetsScreen(d: Data, nav: Nav) {
    var ask by remember { mutableStateOf<Species?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            Text(tr("Питомцы", "Pets"), style = T.title, modifier = Modifier.weight(1f))
            Text("${d.pets.count { it.hatched > 0 }} / ${Pets.all.size}", style = T.h3.copy(color = C.mute))
        }
        Section(tr("Команда", "The team"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (0..2).forEach { slot ->
                val p = d.pets.firstOrNull { it.slot == slot }
                Column(Modifier.weight(1f).card(18.dp).clickable(enabled = p != null) { p?.let { nav.push(Screen.PetInfo(it.id)) } }.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (slot == 0) tr("Вожак", "Leader") else tr("Спутник", "Companion"), style = T.tiny)
                    if (p != null) { PetView(p, 84.dp, Pose.IDLE); Text(p.name, style = T.smallB, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(perkShort(Pets.of(p.species)), style = T.tiny, maxLines = 2, textAlign = TextAlign.Center, minLines = 2) }
                    else { Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) { Text("?", style = T.title.copy(color = C.line)) }; Text(tr("свободно", "free"), style = T.small) }
                }
            }
        }
        Section(tr("Коллекция", "Collection"))
        Pets.all.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { sp ->
                    val p = d.pets.firstOrNull { it.species == sp.id }
                    Column(Modifier.weight(1f).card(16.dp, if (sp.rare && p == null) C.goldSoft else null).clickable { if (p != null) nav.push(Screen.PetInfo(p.id)) else ask = sp }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            p == null -> { Mini(sp.id, 58.dp, dim = true); Text(sp.title, style = T.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            p.hatched == 0L -> { PetView(p, 58.dp); Meter(p.growth.toFloat() / Pets.HATCH, Modifier.padding(top = 4.dp, bottom = 3.dp), h = 5.dp) }
                            else -> { PetView(p, 58.dp); Text(p.name, style = T.tiny.copy(color = C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
    ask?.let { sp -> Popup({ ask = null }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Mini(sp.id, 120.dp, dim = true) }
        Text(sp.title, style = T.h2, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
        Text(sp.perk, style = T.body.copy(color = C.mute), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
        Text(tr("Как найти", "How to find"), style = T.sec, modifier = Modifier.padding(top = 14.dp))
        Text(sp.find, style = T.bodyB, modifier = Modifier.padding(top = 4.dp))
        if (!sp.rare) Text(tr("А ещё яйцо может оказаться в полной банке или на ярмарке.", "An egg may also turn up in a full jar or at the fair."), style = T.small, modifier = Modifier.padding(top = 6.dp))
        Btn(tr("Понятно", "Got it"), Modifier.fillMaxWidth().padding(top = 14.dp)) { ask = null }
    } }
}

fun perkShort(sp: Species): String = when (Pets.perkOf(sp.id)) {
    app.pennyling.domain.Perk.SNOW -> tr("×2 за снежные дни", "×2 for snow days")
    app.pennyling.domain.Perk.WORTH -> tr("×2 за «Стоило того?»", "×2 for «Worth it?»")
    app.pennyling.domain.Perk.WISH -> tr("×2 за решения на чердаке", "×2 for attic decisions")
    app.pennyling.domain.Perk.GOAL -> tr("×2 за пополнение банок", "×2 for filling jars")
    app.pennyling.domain.Perk.DEBT -> tr("×2 за платежи по долгам", "×2 for debt payments")
    app.pennyling.domain.Perk.BILL -> tr("×2 за проверку погреба", "×2 for cellar reviews")
    app.pennyling.domain.Perk.REVIEW -> tr("×2 за разбор недели", "×2 for the weekly review")
    app.pennyling.domain.Perk.DAYS -> tr("×2 за первую запись дня", "×2 for the day's first record")
    app.pennyling.domain.Perk.ALL -> tr("+25% ко всему", "+25% to everything")
    app.pennyling.domain.Perk.KEEPER -> tr("+1 крошка за свои записи", "+1 crumb for its records")
    app.pennyling.domain.Perk.STASH -> tr("×2 за прожитую зиму", "×2 for a winter lived through")
    app.pennyling.domain.Perk.LONG -> tr("×2 за задания недели", "×2 for weekly tasks")
    app.pennyling.domain.Perk.HEARD -> tr("×2 за записи от вороны", "×2 for the crow's records")
}
