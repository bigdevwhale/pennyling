package app.pennyling.data

import app.pennyling.AppContainer
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** The whole burrow as one JSON file: every table and the settings. Import replaces everything. */
object Backup {
    private fun arr(l: List<JSONObject>) = JSONArray().apply { l.forEach { put(it) } }
    private fun JSONObject.objs(k: String): List<JSONObject> = optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()

    suspend fun export(c: AppContainer): String {
        val d = c.dao
        return JSONObject().apply {
            put("app", "pennyling"); put("version", 1); put("at", System.currentTimeMillis())
            put("settings", c.settings.now().json())
            put("accounts", arr(d.accountList().map { JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind).put("start", it.start).put("archived", it.archived).put("order", it.order) }))
            put("cats", arr(d.catList().map { JSONObject().put("id", it.id).put("name", it.name).put("income", it.income).put("keeper", it.keeper).put("icon", it.icon).put("limit", it.limit).put("archived", it.archived).put("order", it.order).put("key", it.key) }))
            put("txs", arr(d.txList().map { JSONObject().put("id", it.id).put("amount", it.amount).put("kind", it.kind).put("catId", it.catId).put("accountId", it.accountId).put("toAccountId", it.toAccountId)
                .put("day", it.day).put("at", it.at).put("note", it.note).put("worth", it.worth).put("billId", it.billId).put("source", it.source) }))
            put("bills", arr(d.billList().map { JSONObject().put("id", it.id).put("title", it.title).put("amount", it.amount).put("catId", it.catId).put("accountId", it.accountId).put("income", it.income)
                .put("every", it.every).put("next", it.next).put("sub", it.sub).put("active", it.active).put("checked", it.checked).put("keep", it.keep) }))
            put("goals", arr(d.goalList().map { JSONObject().put("id", it.id).put("name", it.name).put("target", it.target).put("created", it.created).put("until", it.until).put("done", it.done).put("egg", it.egg).put("color", it.color).put("archived", it.archived) }))
            put("debts", arr(d.debtList().map { JSONObject().put("id", it.id).put("name", it.name).put("total", it.total).put("rate", it.rate).put("minPay", it.minPay).put("created", it.created).put("done", it.done) }))
            put("moves", arr(d.moveList().map { JSONObject().put("id", it.id).put("kind", it.kind).put("refId", it.refId).put("amount", it.amount).put("day", it.day).put("at", it.at) }))
            put("wishes", arr(d.wishList().map { JSONObject().put("id", it.id).put("name", it.name).put("price", it.price).put("added", it.added).put("days", it.days).put("state", it.state).put("decided", it.decided).put("note", it.note) }))
            put("pets", arr(d.petList().map { JSONObject().put("id", it.id).put("species", it.species).put("name", it.name).put("xp", it.xp).put("growth", it.growth).put("found", it.found).put("hatched", it.hatched).put("slot", it.slot).put("shiny", it.shiny).put("fed", it.fed) }))
        }.toString(1)
    }

    /** Reads a backup; false when the file isn't one. */
    suspend fun import(c: AppContainer, text: String): Boolean {
        val j = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (j.optString("app") != "pennyling") return false
        val d = c.dao
        c.db.runInTransactionSuspend {
            d.wipeTx(); d.wipeAccounts(); d.wipeCats(); d.wipeBills(); d.wipeGoals(); d.wipeDebts(); d.wipeMoves(); d.wipeWishes(); d.wipePets(); d.wipeHeard()
            j.objs("accounts").forEach { d.insert(Account(it.getLong("id"), it.getString("name"), it.optString("kind", Account.CARD), it.optLong("start"), it.optBoolean("archived"), it.optInt("order"))) }
            d.insertCats(j.objs("cats").map { Cat(it.getLong("id"), it.getString("name"), it.optBoolean("income"), it.optString("keeper"), it.optString("icon"), it.optLong("limit"), it.optBoolean("archived"), it.optInt("order"), it.optString("key")) })
            d.insertTxs(j.objs("txs").map { Tx(it.getLong("id"), it.getLong("amount"), it.optInt("kind"), it.optLong("catId"), it.optLong("accountId"), it.optLong("toAccountId"), it.getLong("day"), it.optLong("at"),
                it.optString("note"), it.optInt("worth"), it.optLong("billId"), it.optString("source")) })
            j.objs("bills").forEach { d.insert(Bill(it.getLong("id"), it.getString("title"), it.getLong("amount"), it.optLong("catId"), it.optLong("accountId"), it.optBoolean("income"), it.optString("every", Bill.MONTH),
                it.getLong("next"), it.optBoolean("sub"), it.optBoolean("active", true), it.optLong("checked"), it.optInt("keep"))) }
            j.objs("goals").forEach { d.insert(Goal(it.getLong("id"), it.getString("name"), it.getLong("target"), it.optLong("created"), it.optLong("until"), it.optLong("done"), it.optString("egg"), it.optInt("color"), it.optBoolean("archived"))) }
            j.objs("debts").forEach { d.insert(Debt(it.getLong("id"), it.getString("name"), it.getLong("total"), it.optDouble("rate", 0.0), it.optLong("minPay"), it.optLong("created"), it.optLong("done"))) }
            d.insertMoves(j.objs("moves").map { Move(it.getLong("id"), it.getString("kind"), it.getLong("refId"), it.getLong("amount"), it.getLong("day"), it.optLong("at")) })
            j.objs("wishes").forEach { d.insert(Wish(it.getLong("id"), it.getString("name"), it.getLong("price"), it.getLong("added"), it.optInt("days", 30), it.optInt("state"), it.optLong("decided"), it.optString("note"))) }
            j.objs("pets").forEach { d.insert(Pet(it.getLong("id"), it.getString("species"), it.getString("name"), it.optInt("xp"), it.optInt("growth"), it.optLong("found"), it.optLong("hatched"), it.optInt("slot", -1), it.optBoolean("shiny"), it.optInt("fed"))) }
        }
        j.optJSONObject("settings")?.let { s -> val st = Settings.of(s); c.settings.update { st.copy(onboarded = true) } }
        return true
    }
}

private suspend fun androidx.room.RoomDatabase.runInTransactionSuspend(block: suspend () -> Unit) = withTransaction { block() }
