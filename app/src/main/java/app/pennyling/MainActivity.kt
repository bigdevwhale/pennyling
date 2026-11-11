package app.pennyling

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pennyling.data.DemoSeed
import app.pennyling.data.Settings
import app.pennyling.domain.Lang
import app.pennyling.domain.Money
import app.pennyling.notify.Evening
import app.pennyling.ui.Onboarding
import app.pennyling.ui.Root
import app.pennyling.ui.Start
import app.pennyling.ui.theme.PennyTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEMO && BuildConfig.DEMO_LANG.isNotEmpty() && AppCompatDelegate.getApplicationLocales().toLanguageTags() != BuildConfig.DEMO_LANG) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(BuildConfig.DEMO_LANG)); return }
        Lang.ru = resources.configuration.locales[0].language == "ru"
        if (BuildConfig.DEMO) {
            container.scope.launch { DemoSeed.seedIfEmpty(container) }
            Start.tab = intent.getIntExtra("tab", 0); Start.open = intent.getStringExtra("open")
        }
        Evening.schedule(this)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val sys = isSystemInDarkTheme()
            val dark = when (settings?.theme) { "dark" -> true; "light" -> false; else -> sys }
            LaunchedEffect(dark) {
                val t = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t), navigationBarStyle = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t))
            }
            PennyTheme(dark) {
                settings?.let { s -> Money.set(s.currency); if (s.onboarded) Root(s) else Onboarding() }
            }
        }
    }
}

/** Switches the app language: "" follows the phone, "ru" or "en" pins it. The activity restarts in the new language. */
fun setLanguage(tag: String) = AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
fun currentLanguage(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
