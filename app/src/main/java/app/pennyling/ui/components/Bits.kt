package app.pennyling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pennyling.data.Cat
import app.pennyling.data.Pet
import app.pennyling.data.Tx
import app.pennyling.domain.Pets
import app.pennyling.domain.money
import app.pennyling.domain.tr
import app.pennyling.ui.art.Mini
import app.pennyling.ui.art.Prop
import app.pennyling.ui.theme.C
import app.pennyling.ui.theme.T

/** A category's colour: its keeper's, or a calm one for income and own categories. */
fun catColor(c: Cat?): Color = when {
    c == null -> C.mute
    c.income -> C.ok
    c.keeper.isNotEmpty() -> Color(Pets.of(c.keeper).color)
    else -> Color(0xFF000000 or (app.pennyling.domain.hash(c.name).toLong() and 0x7F7F7F) or 0x404040)
}

/** A category's mark: its keeper in a soft circle (an egg until it hatches, a grey shadow if not met yet), or an icon. */
@Composable
fun CatMark(c: Cat?, size: Dp = 40.dp, pets: List<Pet> = emptyList()) {
    val col = catColor(c)
    Box(Modifier.size(size).clip(CircleShape).background(col.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
        val keeper = c?.keeper.orEmpty()
        if (keeper.isNotEmpty()) {
            val p = pets.firstOrNull { it.species == keeper }
            Mini(keeper, size * .86f, egg = p != null && p.hatched == 0L, dim = p == null && pets.isNotEmpty())
        } else {
            val ic = runCatching { Icons.valueOf(c?.icon ?: "") }.getOrNull() ?: if (c?.income == true) Icons.COIN else Icons.TARGET
            Icon(ic, col, size * .5f)
        }
    }
}

/** An amount coloured by its way: spent in ink, received in green with a plus, moved in grey. */
@Composable
fun Amount(t: Tx, modifier: Modifier = Modifier, big: Boolean = false) {
    val st = if (big) T.num else T.numS
    when (t.kind) {
        Tx.INCOME -> Text(money(t.amount, plus = true), style = st.copy(color = C.ok), modifier = modifier, maxLines = 1)
        Tx.MOVE -> Text(money(t.amount), style = st.copy(color = C.mute), modifier = modifier, maxLines = 1)
        else -> Text("−" + money(t.amount), style = st, modifier = modifier, maxLines = 1)
    }
}

/** Five acorns: today's ration; [fill] 0..1 of it still left. Eaten ones fade. */
@Composable
fun Acorns(fill: Float, size: Dp = 30.dp, over: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 5) {
            val part = (fill * 5 - i).coerceIn(0f, 1f)
            Box(contentAlignment = Alignment.Center) {
                Prop("acorn", size, alpha = .18f)
                if (part > 0) Prop("acorn", size * (.55f + .45f * part), alpha = .4f + .6f * part)
            }
        }
        if (over) { Spacer(Modifier.width(4.dp)); Prop("crumbs", size * .8f) }
    }
}

/** A tile of the pantry: a picture, a title and a line about it. */
@Composable
fun StoreTile(prop: String, title: String, line: String, modifier: Modifier = Modifier, species: String? = null, onClick: () -> Unit) {
    Column(modifier.card(18.dp).clickable(onClick = onClick).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (species != null) Mini(species, 36.dp) else Prop(prop, 36.dp)
            Spacer(Modifier.width(8.dp))
            Text(title, style = T.bodyB, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Text(line, style = T.small, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Crumbs as a little pill: the crumbs picture and the number. */
@Composable
fun CrumbPill(n: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(modifier.clip(RoundedCornerShape(50)).background(C.goldSoft).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Prop("crumbs", 22.dp); Spacer(Modifier.width(4.dp))
        Text(app.pennyling.domain.fmt(n), style = T.smallB.copy(color = C.ink))
    }
}

/** Three answers to «was it worth it?». */
@Composable
fun WorthRow(onAnswer: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1 to tr("Да", "Yes"), 2 to tr("Так себе", "So-so"), 3 to tr("Нет", "No")).forEach { (a, l) ->
            val col = when (a) { 1 -> C.ok; 2 -> C.gold; else -> C.bad }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(col.copy(alpha = .15f)).clickable { onAnswer(a) }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                Text(l, style = T.bodyB.copy(color = col))
            }
        }
    }
}

@Composable
fun Dim(on: Boolean, content: @Composable () -> Unit) = Box(Modifier.alpha(if (on) .45f else 1f)) { content() }
