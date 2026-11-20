package app.pennyling

import app.pennyling.data.Bill
import app.pennyling.data.Cat
import app.pennyling.data.Debt
import app.pennyling.data.Pet
import app.pennyling.data.Tx
import app.pennyling.domain.Cats
import app.pennyling.domain.Engine
import app.pennyling.domain.Money
import app.pennyling.domain.Notice
import app.pennyling.domain.Period
import app.pennyling.domain.Pets
import app.pennyling.domain.Ration
import app.pennyling.domain.Stage
import app.pennyling.domain.money
import app.pennyling.domain.parseMoney
import app.pennyling.domain.stage
import app.pennyling.ui.stores.plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EngineTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test fun periodRunsFromPaydayToTheDayBefore() {
        val p = Period.of(day(2026, 11, 11), 10)
        assertEquals(day(2026, 11, 10), p.start); assertEquals(day(2026, 12, 9), p.end)
        val q = Period.of(day(2026, 11, 3), 10)
        assertEquals(day(2026, 10, 10), q.start); assertEquals(day(2026, 11, 9), q.end)
        // payday on the 31st falls on the last day of shorter months
        val f = Period.of(day(2027, 2, 28), 31)
        assertEquals(day(2027, 2, 28), f.start); assertEquals(day(2027, 3, 30), f.end)
        assertEquals(1, Period.of(day(2026, 11, 30), 1).left(day(2026, 11, 30)))
    }

    @Test fun rationKeepsMoneyForBillsStillToCome() {
        val today = day(2026, 11, 11)
        val txs = listOf(Tx(amount = 300_00, day = day(2026, 11, 10)), Tx(amount = 100_00, day = today))
        val rent = Bill(title = "rent", amount = 1000_00, next = day(2026, 11, 20))
        val r = Ration.of(txs, listOf(rent), 3000_00, 10, today)
        // 3000 − 300 spent before − 1000 rent, over 29 days left
        assertEquals((1700_00L / 29) / 100 * 100, r.perDay)
        assertEquals(r.perDay - 100_00, r.left)
        assertEquals(1000_00L, r.reserved)
        assertEquals(3000_00L - 400_00 - 1000_00, r.pantry)
    }

    @Test fun firstRecordLaysTheKeepersEggAndThreeHatchIt() {
        var pets = listOf(Pet(id = 1, species = "hamster", name = "h", found = 0, hatched = 1, slot = 0))
        val f1 = Engine.feed(pets, "squirrel", 5)
        assertEquals(listOf("squirrel"), f1.eggs)
        pets = f1.pets + Pet(id = 2, species = "squirrel", name = "s", found = 5)
        repeat(3) { pets = Engine.feed(pets, "squirrel", 6).pets }
        assertEquals(Stage.BABY, pets.first { it.species == "squirrel" }.stage())
        assertTrue(pets.first { it.species == "hamster" }.xp > 0)
    }

    @Test fun aKeepersEggGrowsOnlyOnItsOwnCategory() {
        // The chipmunk's egg (fun) is one warm-up from hatching; a transport record must not hatch it (#8).
        var pets = listOf(Pet(id = 1, species = "hamster", name = "h", found = 0, hatched = 1, slot = 0),
            Pet(id = 2, species = "chipmunk", name = "c", found = 3, growth = 8), Pet(id = 3, species = "ant", name = "a", found = 3, growth = 8))
        val byTransport = Engine.feed(pets, "mole", 6)
        assertEquals(8, byTransport.pets.first { it.species == "chipmunk" }.growth)
        assertEquals(listOf("ant"), byTransport.hatched.map { it.species })
        // A surprise chipmunk egg from a full jar still warms on any record.
        val open = Engine.feed(pets, "mole", 6, open = setOf(2L))
        assertTrue(open.hatched.any { it.species == "chipmunk" })
        // Its own category hatches it.
        pets = byTransport.pets
        assertTrue(Engine.feed(pets, "chipmunk", 7).hatched.any { it.species == "chipmunk" })
    }

    @Test fun teamPerksDoubleTheirOwnThing() {
        val jay = listOf(Pet(species = "jay", name = "j", found = 0, hatched = 1, slot = 1))
        assertEquals(Engine.SNOW * 2, Engine.bonus(Engine.SNOW, app.pennyling.domain.Perk.SNOW, jay))
        assertEquals(Engine.WORTH, Engine.bonus(Engine.WORTH, app.pennyling.domain.Perk.WORTH, jay))
    }

    @Test fun everySpeciesHasSixFramesNamed() {
        assertEquals(35, Pets.all.size)
        assertEquals(10, Pets.own.size)
        assertEquals(Pets.all.size, Pets.all.map { it.id }.toSet().size)
        assertEquals(15, Pets.all.count { it.cat.isNotEmpty() && !it.own })
        // keepers of the player's own categories never come out of a jar or the fair
        repeat(50) { assertNotNull(Engine.anyEgg(emptySet(), it)); assertFalse(Pets.of(Engine.anyEgg(emptySet(), it)!!).own) }
        // every spending built-in has its own keeper
        Cats.seeds.filter { !it.income }.forEach { if (it.key != "other") assertNotNull(it.key, Pets.keeperOf(it.key)) }
    }

    @Test fun anOldBurrowGetsTheNewBuiltInCategoryAfterItsNeighbour() {
        val old = Cats.defaults().filter { it.key != "hobby" }.mapIndexed { i, c -> c.copy(id = i + 1L, order = i) }
        val all = Cats.missing(old)
        val hobby = all.single { it.key == "hobby" }
        assertEquals(0L, hobby.id)
        assertEquals("ferret", hobby.keeper)
        assertEquals(all.indexOfFirst { it.key == "fun" } + 1, all.indexOf(hobby))
        assertEquals(all.indices.toList(), all.map { it.order })
        assertTrue(Cats.missing(all.map { if (it.id == 0L) it.copy(id = 99) else it }).isEmpty())
    }

    @Test fun anOwnHobbyCategoryBecomesTheBuiltInOne() {
        val old = Cats.defaults().filter { it.key != "hobby" }.mapIndexed { i, c -> c.copy(id = i + 1L, order = i) } +
            Cat(id = 50, name = "хобби ", order = 40)
        val all = Cats.missing(old)
        assertEquals(old.size, all.size)
        val mine = all.single { it.id == 50L }
        assertEquals("hobby", mine.key); assertEquals("ferret", mine.keeper); assertEquals("хобби ", mine.name)
    }

    @Test fun reorderingMovesOnlyTheShownCategories() {
        val cats = listOf(Cat(1, "a", order = 0), Cat(2, "in", income = true, order = 1), Cat(3, "b", order = 2), Cat(4, "c", order = 3))
        val out = Cats.reorder(cats, listOf(4L, 1L, 3L))
        assertEquals(listOf(4L, 2L, 1L, 3L), out.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), out.map { it.order })
    }

    @Test fun bankNotificationsAreRead() {
        val a = Notice.read("Сбербанк", "Покупка 1 240,50 ₽ ВкусВилл. Баланс: 18 420 ₽")
        assertNotNull(a); assertEquals(1240_50L, a!!.amount); assertTrue(!a.income); assertTrue(a.place.contains("ВкусВилл"))
        val b = Notice.read("Bank", "Salary credited: $2,500.00")
        assertNotNull(b); assertTrue(b!!.income)
        assertNull(Notice.read("Chat", "See you at 7"))
    }

    @Test fun moneyIsWrittenTheLocalWay() {
        app.pennyling.domain.Lang.ru = true; Money.set("RUB")
        assertEquals("1 250 ₽", money(1250_00).replace(' ', ' ').replace(' ', ' ').replace(' ', ' '))
        assertEquals("12,50 ₽", money(12_50))
        app.pennyling.domain.Lang.ru = false; Money.set("USD")
        assertEquals("$12.50", money(12_50)); assertEquals("+$3", money(3_00, plus = true))
        assertEquals(1250_00L, parseMoney("1 250")); assertEquals(12_50L, parseMoney("12,5"))
    }

    @Test fun avalancheNeverCostsMoreInterest() {
        val debts = listOf(Debt(name = "card", total = 0, rate = 30.0, minPay = 50_00, created = 0) to 3000_00L,
            Debt(name = "friend", total = 0, rate = 0.0, minPay = 0, created = 0) to 500_00L)
        val snow = plan(debts, 400_00, false)!!; val ava = plan(debts, 400_00, true)!!
        assertTrue(ava.second <= snow.second)
        assertNull(plan(debts, 10_00, true))
    }
}
