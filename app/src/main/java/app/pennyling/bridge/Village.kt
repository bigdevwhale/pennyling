package app.pennyling.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import app.pennyling.data.Backup
import app.pennyling.BuildConfig
import app.pennyling.PennylingApp
import app.pennyling.AppContainer
import app.pennyling.domain.Lang
import app.pennyling.domain.Pets
import app.pennyling.data.Tx
import app.pennyling.domain.money
import app.pennyling.domain.stage
import app.pennyling.domain.toNext
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The bridge to Lingvale, the village where the pets of all the -ling apps live together.
 *
 * Lingvale asks through ContentResolver.call() and nobody else gets an answer: the caller must be Lingvale signed
 * with its release key (a debug or demo build also answers a Lingvale built with the same debug key).
 * Protocol v2, the same in every -ling app:
 *  - "hello"    → {v}
 *  - "snapshot" → {json}: pets, species, days of activity, the last week's events
 *  - "art"      → {bytes}: one picture from res/drawable-nodpi (pet_*, egg_*, item_*, *_bg)
 *  - "backup"   (extras {fd}: a file open for writing) → {ok, name}: writes the same backup file the settings save
 *  - "restore"  (extras {fd}: a file open for reading) → {ok, error}: loads such a file, as the settings do,
 *    replacing what is on the phone; error is "newer" (a file from a newer version), "busy" or "bad"
 * Only "restore" changes anything here, and Lingvale asks the player before it calls it.
 */
class VillageProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (!trusted(ctx)) throw SecurityException("Only Lingvale may ask")
        return when (method) {
            "hello" -> Bundle().apply { putInt("v", VERSION) }
            "snapshot" -> Bundle().apply { putString("json", runBlocking { snapshot(ctx, container(ctx)) }.toString()) }
            "art" -> art(ctx, arg)
            "backup" -> backup(ctx, extras)
            "restore" -> restore(ctx, extras)
            else -> null
        }
    }

    private fun trusted(ctx: Context): Boolean {
        val pkg = callingPackage ?: return false
        if (pkg != HUB && !pkg.startsWith("$HUB.")) return false
        val pm = ctx.packageManager
        if (HUB_CERTS.any { pm.hasSigningCertificate(pkg, it, PackageManager.CERT_INPUT_SHA256) }) return true
        return BuildConfig.DEBUG && pm.checkSignatures(pkg, ctx.packageName) == PackageManager.SIGNATURE_MATCH
    }

    private fun art(ctx: Context, name: String?): Bundle? {
        if (name == null || !ART.matches(name)) return null
        @Suppress("DiscouragedApi")
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) return null
        return Bundle().apply { putByteArray("bytes", ctx.resources.openRawResource(id).use { it.readBytes() }) }
    }

    @Suppress("DEPRECATION")
    private fun fd(extras: Bundle?): ParcelFileDescriptor? = extras?.getParcelable("fd")

    /** Writes the settings' backup file into the file Lingvale opened. */
    private fun backup(ctx: Context, extras: Bundle?): Bundle {
        val fd = fd(extras) ?: return Bundle().apply { putBoolean("ok", false) }
        val ok = runCatching {
            val c = container(ctx)
            val text = runBlocking { Backup.export(c) }
            ParcelFileDescriptor.AutoCloseOutputStream(fd).use { it.write(text.toByteArray()) }
        }.onFailure { runCatching { fd.close() } }.isSuccess
        return Bundle().apply { putBoolean("ok", ok); putString("name", "pennyling.json") }
    }

    /** Loads a backup file from Lingvale, the way the settings do. */
    private fun restore(ctx: Context, extras: Bundle?): Bundle {
        val fd = fd(extras) ?: return Bundle().apply { putBoolean("ok", false); putString("error", "bad") }
        val ok = runCatching {
            val text = ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes().decodeToString() }
            val c = container(ctx)
            runBlocking { Backup.import(c, text) }
        }.getOrElse { runCatching { fd.close() }; false }
        return Bundle().apply { putBoolean("ok", ok); if (!ok) putString("error", "bad") }
    }

    /** The provider can be asked before the application has finished starting; wait for it a little. */
    private fun container(ctx: Context): AppContainer {
        repeat(100) {
            runCatching { return (ctx.applicationContext as PennylingApp).container }
            Thread.sleep(30)
        }
        error("not ready")
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val VERSION = 2
        const val HUB = "app.lingvale"
        /** SHA-256 of Lingvale's release certificate. */
        private val HUB_CERTS = listOf(hex("CEF40A1786896D829CFA53115693AC9A53FC0349EAB84BAF64EFA648592D1ABF"))
        private val ART = Regex("""(pet|egg|item|find)_[a-z0-9_]+|[a-z]+_bg""")
        private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        private fun color(c: Long) = "#%06X".format(c and 0xFFFFFF)
        private fun dayOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

        /** Everything Lingvale shows of Pennyling: records and pets; amounts only as the app itself writes them. */
        suspend fun snapshot(ctx: Context, c: AppContainer): JSONObject {
            val pets = c.dao.petList()
            val cats = c.dao.catList().associateBy { it.id }
            val txs = c.dao.txList().filter { it.kind != Tx.MOVE }
            val today = LocalDate.now().toEpochDay()

            val petsJson = JSONArray()
            pets.forEach { p ->
                val (have, need) = p.toNext()
                petsJson.put(JSONObject()
                    .put("id", p.id).put("species", p.species).put("name", p.name)
                    .put("stage", p.stage().ordinal).put("progress", if (need <= 0) 1.0 else (have.toDouble() / need).coerceIn(0.0, 1.0))
                    .put("slot", p.slot).put("shiny", p.shiny).put("found", p.found).put("hatched", p.hatched))
            }
            val speciesJson = JSONArray()
            Pets.all.forEach { s ->
                speciesJson.put(JSONObject().put("id", s.id).put("ru", s.ru).put("en", s.en).put("color", color(s.color))
                    .put("aboutRu", s.perkRu).put("aboutEn", s.perkEn).put("rare", s.rare))
            }
            val daysJson = JSONArray()
            txs.groupBy { it.day }.toSortedMap().forEach { (d, l) ->
                if (d > today - 400) daysJson.put(JSONObject().put("d", d).put("n", l.size).put("min", 0))
            }
            val eventsJson = JSONArray()
            txs.filter { it.day > today - 8 }.forEach { t ->
                val what = cats[t.catId]?.name.orEmpty()
                val line = listOf(what, money(t.amount, plus = t.kind == Tx.INCOME)).filter { it.isNotBlank() }.joinToString(" · ")
                eventsJson.put(JSONObject().put("at", t.at).put("min", 0).put("ru", line).put("en", line))
            }
            return JSONObject()
                .put("v", VERSION).put("app", "pennyling").put("version", BuildConfig.VERSION_NAME)
                .put("ru", Lang.ru).put("scene", "burrow_bg")
                .put("pets", petsJson).put("species", speciesJson).put("days", daysJson).put("events", eventsJson)
        }
    }
}
