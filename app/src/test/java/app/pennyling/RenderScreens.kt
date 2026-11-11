package app.pennyling

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.pennyling.data.DemoSeed
import app.pennyling.domain.Lang
import app.pennyling.domain.Money
import app.pennyling.ui.Onboarding
import app.pennyling.ui.Root
import app.pennyling.ui.Start
import app.pennyling.ui.share.CardKind
import app.pennyling.ui.share.Look
import app.pennyling.ui.share.Painter
import app.pennyling.ui.share.ShareReq
import app.pennyling.ui.theme.PennyTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every screen of the demo drawn to PNGs in app/build/art/screens (run with `-Prender`).
 * The screen is very tall, so whole scrolling pages fit in one picture.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h2400dp-xxhdpi")
class RenderScreens {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }
    private val dark = System.getProperty("render.dark") == "1"

    private fun shot(name: String) {
        repeat(6) { rule.mainClock.advanceTimeBy(400); Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "${if (dark) "d-" else ""}$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        Lang.ru = true
        val c = ApplicationProvider.getApplicationContext<PennylingApp>().container
        var i by mutableIntStateOf(-1)
        rule.mainClock.autoAdvance = false
        // onboarding first, on an empty burrow
        rule.setContent { PennyTheme(dark) { key(i) { if (i == -2) Onboarding() else { val s by c.settings.settings.collectAsState(null); s?.let { st -> if (i >= 0 && st.onboarded) Root(st) } } } } }
        rule.runOnUiThread { i = -2 }; shot("0-onboarding")
        runBlocking { DemoSeed.seedIfEmpty(c) }
        // the first-visit introductions: one drawn on its own, the rest put away for the screenshots
        runBlocking { c.settings.update { it.copy(seen = it.seen + listOf("i.journal", "i.pets", "i.stats", "i.add", "i.pet", "i.goals", "i.wishes", "i.cellar", "i.debts", "i.dam", "i.accounts", "i.cats", "i.heard", "i.review", "i.shop", "i.settings", "i.wrapped")) } }
        Money.set(runBlocking { c.settings.now().currency })
        val pets = runBlocking { c.dao.petList() }
        val tx = runBlocking { c.dao.txList().filter { it.kind == 0 && it.note.isNotBlank() }.maxBy { it.day } }
        val shots = listOf(
            "1-home" to (0 to null), "2-journal" to (1 to null), "3-pets" to (2 to null), "4-stats" to (3 to null),
            "5-pet" to (2 to "pet:${pets.first { it.slot == 1 }.id}"), "6-add" to (0 to "add"), "6b-edit" to (1 to "tx:${tx.id}"),
            "7-goals" to (0 to "goals"), "8-wishes" to (0 to "wishes"), "9-cellar" to (0 to "cellar"), "10-debts" to (0 to "debts"),
            "11-dam" to (0 to "dam"), "12-accounts" to (0 to "accounts"), "13-cats" to (0 to "cats"), "14-heard" to (0 to "heard"),
            "15-review" to (0 to "review"), "16-shop" to (0 to "shop"), "17-settings" to (0 to "settings"), "18-wrapped" to (3 to "wrapped"),
            "19-share" to (0 to "share:PERIOD"),
        )
        for ((n, sh) in shots.withIndex()) {
            Start.tab = sh.second.first; Start.open = sh.second.second
            rule.runOnUiThread { i = n }
            shot(sh.first)
        }
        // every piece of furniture at once, to check the layout
        runBlocking { c.settings.update { it.copy(items = app.pennyling.domain.Shop.all.map { i -> i.id }.toSet(), hidden = emptySet()) } }
        Start.tab = 0; Start.open = null; rule.runOnUiThread { i = 300 }; shot("1c-home-full")
        val data = runBlocking { app.pennyling.ui.Data(c.settings.now(), c.dao.txList().sortedByDescending { it.day }, c.dao.catList(), c.dao.accountList(), c.dao.billList(), c.dao.goalList(),
            c.dao.debtList(), c.dao.moveList(), c.dao.wishList(), c.dao.petList(), emptyList()) }
        val cards = File(out.parentFile, "cards").apply { mkdirs() }
        val reqs = listOf(ShareReq(CardKind.PERIOD), ShareReq(CardKind.PET, pets.first().id), ShareReq(CardKind.GOAL, data.goals.first { it.done > 0 }.id), ShareReq(CardKind.YEAR),
            ShareReq(CardKind.BURROW), ShareReq(CardKind.COLLECTION))
        reqs.forEachIndexed { k, r -> listOf(false, true).forEach { story ->
            val bmp = Painter(rule.activity, data, Look(story, k % 6, false)).render(r).first
            File(cards, "${k}-${r.kind.name.lowercase()}${if (story) "-story" else ""}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) } } }
    }
}
