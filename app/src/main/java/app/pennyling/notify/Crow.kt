package app.pennyling.notify

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.pennyling.container
import app.pennyling.data.Heard
import app.pennyling.domain.Notice

/**
 * The crow: listens to the phone's notifications for bank messages. Apps marked as banks turn into records
 * waiting in the burrow; any other app that sent something money-like is only remembered by name,
 * so settings can offer it as a bank. Nothing is sent anywhere.
 */
class Crow : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val e = n.extras
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (e.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: e.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (text.isBlank()) return
        val read = Notice.read(title, text) ?: return
        val c = applicationContext.container
        c.write {
            val st = settings.now()
            if (sbn.packageName in st.banks) {
                // the same message twice (banks repeat themselves) is heard once
                val dup = dao.heardList().any { it.amount == read.amount && it.pkg == sbn.packageName && kotlin.math.abs(it.at - sbn.postTime) < 120_000 }
                if (!dup) hear(Heard(pkg = sbn.packageName, amount = read.amount, income = read.income, place = read.place, text = "$title. $text".take(300), at = sbn.postTime, cash = read.cash))
            } else if (sbn.packageName !in st.heardApps) {
                settings.update { it.copy(heardApps = (it.heardApps + sbn.packageName).toList().takeLast(20).toSet()) }
            }
        }
    }

    companion object {
        fun granted(ctx: Context): Boolean {
            val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(ctx, Crow::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
