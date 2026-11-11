package app.pennyling.domain

import app.pennyling.data.Bill
import app.pennyling.data.Move
import app.pennyling.data.Pet
import app.pennyling.data.Settings
import app.pennyling.data.Tx
import app.pennyling.data.Wish

/**
 * The rules of the burrow. Crumbs and growth come from keeping honest books – a record, a day looked after,
 * an answer, a decision – and never from how much or how little was spent.
 */
object Engine {
    const val PER_RECORD = 1
    const val FIRST_OF_DAY = 2
    const val DAILY_CAP = 14
    const val SNOW = 6
    const val WORTH = 1
    const val WISH = 6
    const val GOAL = 3
    const val DEBT = 3
    const val BILL = 2
    const val REVIEW = 8
    const val WINTER = 30
    const val WINTER_HARD = 10

    fun team(pets: List<Pet>) = pets.filter { it.slot in 0..2 && it.hatched > 0 }.sortedBy { it.slot }

    /** Crumbs for an act, doubled when a pet with the matching perk is in the team (and ×1.25 with the pangolin). */
    fun bonus(base: Int, perk: Perk, pets: List<Pet>): Int {
        val t = team(pets)
        var v = base
        if (perk != Perk.KEEPER && t.any { Pets.perkOf(it.species) == perk }) v *= 2
        if (t.any { it.species == "pangolin" }) v = (v * 1.25).toInt().coerceAtLeast(v + 1)
        return v
    }

    /**
     * A new record feeds the keeper of its category (an egg of it appears with the first one) and gives the team a little.
     * Returns the pets changed, new eggs to lay and pets that hatched.
     */
    data class Fed(val pets: List<Pet>, val eggs: List<String>, val hatched: List<Pet>, val grown: List<Pair<Pet, Stage>>)

    fun feed(pets: List<Pet>, keeper: String, today: Long, xp: Int = 3): Fed {
        val eggs = ArrayList<String>()
        if (keeper.isNotEmpty() && pets.none { it.species == keeper }) eggs += keeper
        val team = team(pets).map { it.id }.toSet()
        val hatched = ArrayList<Pet>(); val grown = ArrayList<Pair<Pet, Stage>>()
        val out = pets.map { p ->
            val before = p.stage()
            var q = p
            if (p.hatched == 0L) {
                val g = p.growth + (if (p.species == keeper) 3 else 1)
                q = if (g >= Pets.HATCH) p.copy(growth = Pets.HATCH, hatched = today, xp = 0).also { hatched += it } else p.copy(growth = g)
            } else {
                var add = 0
                if (p.species == keeper) add += xp
                if (p.id in team) add += 1
                if (add > 0) q = p.copy(xp = p.xp + add, fed = p.fed + if (p.species == keeper) 1 else 0)
            }
            if (q.hatched > 0 && before != Stage.EGG && q.stage() != before) grown += q to q.stage()
            q
        }
        return Fed(out, eggs, hatched, grown)
    }

    /** XP for the whole team, for acts that aren't records (a snow day, an answer, a decision). */
    fun cheer(pets: List<Pet>, xp: Int, also: String = ""): List<Pet> {
        val team = team(pets).map { it.id }.toSet()
        return pets.map { p -> if (p.hatched > 0 && (p.id in team || p.species == also)) p.copy(xp = p.xp + xp) else p }
    }

    /** Facts the finds are judged on. */
    class Life(val txs: List<Tx>, val moves: List<Move>, val wishes: List<Wish>, val bills: List<Bill>, val s: Settings, val heardTaken: Int) {
        val days = txs.filter { it.kind != Tx.MOVE }.map { it.day }.toSet().size
        val worth = txs.count { it.worth in 1..3 }
    }

    /** Pets found by how money is lived with, not yet owned. */
    fun finds(l: Life, owned: Set<String>): List<String> {
        val out = ArrayList<String>()
        fun f(id: String, ok: Boolean) { if (ok && id !in owned) out += id }
        f("ant", l.days >= 7)
        f("jay", l.s.snow.isNotEmpty())
        f("dormouse", l.s.snow.size >= 5)
        f("otter", l.worth >= 15)
        f("fox", l.wishes.any { it.state != 0 })
        f("badger", l.moves.any { it.kind == "debt" && it.amount > 0 })
        f("crow", l.heardTaken > 0 || l.bills.count { it.checked > 0 } >= 3)
        f("marmot", l.s.winters >= 1)
        f("tortoise", l.days >= 60)
        f("pangolin", l.s.won >= 6)
        return out
    }

    /** A species for an egg from a full jar or the fair: one not owned yet, the rare one last. */
    fun anyEgg(owned: Set<String>, seed: Int): String? {
        val pool = Pets.all.filter { it.id !in owned && !it.rare }
        if (pool.isEmpty()) return null
        return pool[Rand(seed).int(pool.size)].id
    }
}
