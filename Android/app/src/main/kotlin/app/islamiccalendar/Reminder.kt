package app.islamiccalendar
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Lesson shown today. Shared by Home and the notification so both always agree. */
fun lessonOfDay(list: List<Lesson>, day: LocalDate = LocalDate.now()): Lesson = list[(day.toEpochDay() % list.size).toInt()]

object Reminder {
    private const val CHANNEL = "daily_wisdom"
    private const val ID = 1001

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(ctx, ID, Intent(ctx, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** Schedules the next occurrence of hour:minute. Inexact-while-idle: may arrive a few minutes late, needs no special permission. */
    fun schedule(ctx: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var at = now.toLocalDate().atTime(hour, minute)
        if (!at.isAfter(now)) at = at.plusDays(1)
        val ms = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        ctx.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pending(ctx))
    }

    fun cancel(ctx: Context) = ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx))

    fun show(ctx: Context) {
        val p = ctx.getSharedPreferences("s", 0)
        val lang = p.getString("lang", "en")!!
        val adj = p.getInt("adj", 0)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, tr(lang, "notif_channel"), NotificationManager.IMPORTANCE_HIGH)
            .apply { description = tr(lang, "notif_channel_desc") })
        val lesson = lessonOfDay(Content(ctx).lessons(lang))
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif_moon)
            .setColor(Color.parseColor("#5563E8"))
            .setContentTitle("🌙 " + Hijri.text(Hijri.of(LocalDate.now(), adj), lang))
            .setContentText(lesson.takeaway)
            .setSubText(tr(lang, "notif_channel"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(lesson.takeaway + "\n\n" + tr(lang, "practice") + lesson.action))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .addAction(0, tr(lang, "notif_read"), open)
            .setAutoCancel(true)
            .build()
        nm.notify(ID, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val p = ctx.getSharedPreferences("s", 0)
        if (!p.getBoolean("remind", false)) return
        Reminder.show(ctx)
        Reminder.schedule(ctx, p.getInt("hour", 8), p.getInt("minute", 0))
    }
}

/** Re-arms alarms after reboot, app update, time-zone change or an exact-alarm permission change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val p = ctx.getSharedPreferences("s", 0)
        if (p.getBoolean("remind", false)) Reminder.schedule(ctx, p.getInt("hour", 8), p.getInt("minute", 0))
        PrayerAlerts.schedule(ctx)
    }
}
