package app.pennyling.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.pennyling.MainActivity
import app.pennyling.R
import app.pennyling.container
import app.pennyling.data.Tx
import app.pennyling.domain.Lang
import app.pennyling.domain.Pets
import app.pennyling.domain.today
import app.pennyling.domain.tr
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * The evening nudge: once a day at the chosen hour, if nothing was written today, the leader asks
 * what the day cost. Runs hourly and speaks only in its hour.
 */
class Evening(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.now()
        if (!s.onboarded || s.evening < 0 || LocalDateTime.now().hour != s.evening) return Result.success()
        val t = today()
        if (t in s.snow || c.dao.txList().any { it.day == t && it.kind != Tx.MOVE && it.source != "bill" }) return Result.success()
        Lang.ru = applicationContext.resources.configuration.locales[0].language == "ru"
        val lead = c.dao.petList().firstOrNull { it.slot == 0 }
        val name = lead?.name ?: Pets.of("hamster").title
        show(applicationContext, tr("$name ждёт у кладовой", "$name is waiting by the pantry"),
            tr("На что сегодня ушли деньги? А если ни на что – отметьте снежный день.", "What did today cost? And if nothing – mark a snow day."))
        return Result.success()
    }

    companion object {
        private const val CH = "evening"
        fun schedule(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("evening", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<Evening>(1, TimeUnit.HOURS).build())
        }
        fun show(ctx: Context, title: String, text: String) {
            if (android.os.Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH, tr("Вечерний вопрос", "Evening question"), NotificationManager.IMPORTANCE_DEFAULT))
            val pi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
            val n = NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(pi).setAutoCancel(true).build()
            NotificationManagerCompat.from(ctx).notify(1, n)
        }
    }
}
