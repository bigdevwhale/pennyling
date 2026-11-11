package app.pennyling.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Everything that isn't a record or a pet: the period, the budget, crumbs, the burrow, quests. */
data class Settings(
    val onboarded: Boolean = false,
    val currency: String = "RUB",
    val payday: Int = 1,                         // day of the month the period starts (1 = calendar month)
    val budget: Long = 0,                        // what a period may cost, minor units
    val crumbs: Int = 0,                         // the burrow's money (never real money)
    val items: Set<String> = emptySet(),         // furniture bought
    val hidden: Set<String> = emptySet(),        // bought and put away
    val theme: String = "system",                // light · dark · system
    val inbox: List<String> = emptyList(),       // news to show once: «egg:jay», «hatch:12», «stage:12:2», «won:20250»
    val seen: Set<String> = emptySet(),          // hints read
    val questWeek: Long = 0,
    val quests: List<String> = emptyList(),
    val questsDone: Set<String> = emptySet(),
    val snow: Set<Long> = emptySet(),            // days marked «spent nothing»
    val banks: Set<String> = emptySet(),         // packages the crow listens to
    val heardApps: Set<String> = emptySet(),     // packages that sent money-like notifications, to offer as banks
    val hourly: Long = 0,                        // what an hour of work brings, for «cost in hours»; 0 = off
    val roundTo: Long = 10000,                   // the ant rounds every spend up to this
    val worthDays: Int = 7,                      // ask «was it worth it?» after this many days
    val coolDays: Int = 30,                      // a wish cools in the attic this long
    val evening: Int = 21,                       // hour of the evening nudge, -1 off
    val period: Long = 0,                        // start day of the period last looked at (to notice a new one)
    val won: Int = 0,                            // periods that ended within the budget
    val winters: Int = 0,                        // periods lived through
    val reviewed: Long = 0,                      // Monday of the last weekly review with the owl
    val crumbDay: Long = 0, val crumbsToday: Int = 0,   // crumbs for records are capped per day
    val town: String = "", val lat: Double = Double.NaN, val lon: Double = Double.NaN,
    val guessed: Boolean = false,
    val outside: String = "",
    val cardBg: Int = 0,
    val hideSums: Boolean = false,               // on shared cards
) {
    fun json(): JSONObject = JSONObject().apply {
        put("onboarded", onboarded); put("currency", currency); put("payday", payday); put("budget", budget); put("crumbs", crumbs)
        put("items", JSONArray(items.toList())); put("hidden", JSONArray(hidden.toList())); put("theme", theme); put("inbox", JSONArray(inbox))
        put("seen", JSONArray(seen.toList())); put("questWeek", questWeek); put("quests", JSONArray(quests)); put("questsDone", JSONArray(questsDone.toList()))
        put("snow", JSONArray(snow.toList())); put("banks", JSONArray(banks.toList())); put("heardApps", JSONArray(heardApps.toList()))
        put("hourly", hourly); put("roundTo", roundTo); put("worthDays", worthDays); put("coolDays", coolDays); put("evening", evening)
        put("period", period); put("won", won); put("winters", winters); put("reviewed", reviewed); put("crumbDay", crumbDay); put("crumbsToday", crumbsToday)
        put("town", town); if (!lat.isNaN()) { put("lat", lat); put("lon", lon) }; put("guessed", guessed); put("outside", outside)
        put("cardBg", cardBg); put("hideSums", hideSums)
    }

    companion object {
        fun of(j: JSONObject): Settings {
            fun list(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            fun set(k: String) = list(k).toSet()
            return Settings(j.optBoolean("onboarded"), j.optString("currency", "RUB"), j.optInt("payday", 1), j.optLong("budget"), j.optInt("crumbs"),
                set("items"), set("hidden"), j.optString("theme", "system"), list("inbox"), set("seen"), j.optLong("questWeek"), list("quests"), set("questsDone"),
                list("snow").mapNotNull { it.toLongOrNull() }.toSet(), set("banks"), set("heardApps"), j.optLong("hourly"), j.optLong("roundTo", 10000),
                j.optInt("worthDays", 7), j.optInt("coolDays", 30), j.optInt("evening", 21), j.optLong("period"), j.optInt("won"), j.optInt("winters"),
                j.optLong("reviewed"), j.optLong("crumbDay"), j.optInt("crumbsToday"),
                j.optString("town"), j.optDouble("lat", Double.NaN), j.optDouble("lon", Double.NaN), j.optBoolean("guessed"), j.optString("outside"),
                j.optInt("cardBg"), j.optBoolean("hideSums"))
        }
    }
}

private val Context.store by preferencesDataStore("pennyling")
private val KEY = stringPreferencesKey("settings")

class SettingsStore(private val ctx: Context) {
    val settings: Flow<Settings> = ctx.store.data.map { p -> p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings() }
    suspend fun now(): Settings = settings.first()
    suspend fun update(f: (Settings) -> Settings) { ctx.store.edit { p ->
        val cur = p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings()
        p[KEY] = f(cur).json().toString() } }
}
