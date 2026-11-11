package app.pennyling.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** What the window shows. */
enum class Weather { CLEAR, CLOUDY, OVERCAST, RAIN, SNOW, FOG, STORM }

/** The weather outside right now, and today's sunrise and sunset (0 when unknown). */
data class Outside(val weather: Weather, val cloud: Int, val temp: Double, val sunrise: Long, val sunset: Long, val at: Long) {
    fun json(): String = JSONObject().put("w", weather.name).put("c", cloud).put("t", temp).put("r", sunrise).put("s", sunset).put("at", at).toString()

    companion object {
        fun of(s: String?): Outside? = runCatching {
            val o = JSONObject(s ?: return null)
            Outside(Weather.valueOf(o.getString("w")), o.getInt("c"), o.optDouble("t"), o.optLong("r"), o.optLong("s"), o.getLong("at"))
        }.getOrNull()
    }
}

/** A town to take the weather from. */
data class Town(val name: String, val region: String, val lat: Double, val lon: Double)

/**
 * The real weather for the gym window: Open-Meteo, or MET Norway when Open-Meteo can't be reached.
 * Nothing but the town's coordinates is sent.
 */
object OutsideSource {
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 7000; c.readTimeout = 9000
        c.setRequestProperty("User-Agent", "Pumpling/1.0 (https://github.com/bigdevwhale/pumpling)")
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    fun fetch(lat: Double, lon: Double): Outside = runCatching { openMeteo(lat, lon) }.getOrElse { metNo(lat, lon) }

    /** WMO weather codes, folded. */
    private fun ofCode(code: Int, cloud: Int): Weather = when (code) {
        0, 1 -> if (cloud > 60) Weather.CLOUDY else Weather.CLEAR
        2 -> Weather.CLOUDY
        3 -> Weather.OVERCAST
        45, 48 -> Weather.FOG
        in 51..67, in 80..82 -> Weather.RAIN
        in 71..77, 85, 86 -> Weather.SNOW
        in 95..99 -> Weather.STORM
        else -> if (cloud > 70) Weather.OVERCAST else Weather.CLOUDY
    }

    private fun openMeteo(lat: Double, lon: Double): Outside {
        val j = JSONObject(get(String.format(Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,cloud_cover&daily=sunrise,sunset&forecast_days=1&timezone=auto&timeformat=unixtime", lat, lon)))
        val c = j.getJSONObject("current")
        val d = j.getJSONObject("daily")
        val cloud = c.optInt("cloud_cover", 50)
        return Outside(ofCode(c.optInt("weather_code"), cloud), cloud, c.optDouble("temperature_2m"),
            d.getJSONArray("sunrise").optLong(0) * 1000, d.getJSONArray("sunset").optLong(0) * 1000, System.currentTimeMillis())
    }

    private fun metNo(lat: Double, lon: Double): Outside {
        val j = JSONObject(get(String.format(Locale.US, "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.3f&lon=%.3f", lat, lon)))
        val first = j.getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
        val inst = first.getJSONObject("instant").getJSONObject("details")
        val cloud = inst.optDouble("cloud_area_fraction", 50.0).toInt()
        val sym = first.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code").orEmpty()
        val w = when {
            "thunder" in sym -> Weather.STORM
            "snow" in sym || "sleet" in sym -> Weather.SNOW
            "rain" in sym -> Weather.RAIN
            "fog" in sym -> Weather.FOG
            "clearsky" in sym || "fair" in sym -> Weather.CLEAR
            cloud > 85 -> Weather.OVERCAST
            cloud > 30 -> Weather.CLOUDY
            else -> Weather.CLEAR
        }
        val (rise, set) = Sun.riseSet(lat, lon, System.currentTimeMillis())
        return Outside(w, cloud, inst.optDouble("air_temperature"), rise, set, System.currentTimeMillis())
    }

    /** Towns matching a name, for the settings search (Open-Meteo geocoding). */
    fun towns(q: String, ru: Boolean): List<Town> {
        val j = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?count=8&language=${if (ru) "ru" else "en"}&name=" + URLEncoder.encode(q, "UTF-8")))
        val a = j.optJSONArray("results") ?: return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
            Town(o.optString("name"), listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "), o.getDouble("latitude"), o.getDouble("longitude"))
        }
    }

    /** A first guess at the town, from the phone's time zone («Europe/Moscow» → Moscow). */
    fun guess(ru: Boolean, zone: ZoneId = ZoneId.systemDefault()): Town? {
        val city = zone.id.substringAfterLast('/').replace('_', ' ')
        if (city.isBlank() || zone.id.startsWith("Etc") || zone.id == "UTC") return null
        return runCatching { towns(city, ru).firstOrNull() }.getOrNull()
    }
}

/** Sunrise and sunset by the usual NOAA approximation, for when the weather service doesn't say. */
object Sun {
    fun riseSet(lat: Double, lon: Double, now: Long): Pair<Long, Long> {
        val day = Instant.ofEpochMilli(now).atZone(ZoneId.of("UTC")).toLocalDate()
        val n = day.dayOfYear
        val g = 2 * Math.PI / 365 * (n - 1)
        val decl = 0.006918 - 0.399912 * Math.cos(g) + 0.070257 * Math.sin(g) - 0.006758 * Math.cos(2 * g) + 0.000907 * Math.sin(2 * g)
        val eq = 229.18 * (0.000075 + 0.001868 * Math.cos(g) - 0.032077 * Math.sin(g) - 0.014615 * Math.cos(2 * g) - 0.040849 * Math.sin(2 * g))
        val cosH = (Math.cos(Math.toRadians(90.833)) / (Math.cos(Math.toRadians(lat)) * Math.cos(decl)) - Math.tan(Math.toRadians(lat)) * Math.tan(decl)).coerceIn(-1.0, 1.0)
        val ha = Math.toDegrees(Math.acos(cosH))
        val noon = 720 - 4 * lon - eq
        val start = day.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        return (start + ((noon - 4 * ha) * 60_000).toLong()) to (start + ((noon + 4 * ha) * 60_000).toLong())
    }
}
