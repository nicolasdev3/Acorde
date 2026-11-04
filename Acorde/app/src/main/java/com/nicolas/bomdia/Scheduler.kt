package com.nicolas.bomdia

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.util.Calendar

object Scheduler {
    private const val PREFS = "bomdia"

    fun save(ctx: Context, json: String) {
        ctx.getSharedPreferences(PREFS, 0).edit().putString("cfg", json).apply()
        scheduleNext(ctx)
    }

    fun cfg(ctx: Context): JSONObject = try {
        JSONObject(ctx.getSharedPreferences(PREFS, 0).getString("cfg", "{}")!!)
    } catch (e: Exception) { JSONObject() }

    fun snooze(ctx: Context, label: String) {
        val at = System.currentTimeMillis() + 5 * 60 * 1000
        ctx.getSharedPreferences(PREFS, 0).edit().putLong("snAt", at).putString("snL", label).apply()
        scheduleNext(ctx)
    }

    private fun op(ctx: Context, label: String, time: String): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).putExtra("l", label).putExtra("t", time)
        return PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun scheduleNext(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        var best = Long.MAX_VALUE
        var bl = ""
        var bt = ""
        val arr = cfg(ctx).optJSONArray("alarms")
        if (arr != null) for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            if (!a.optBoolean("on", true)) continue
            val p = a.getString("t").split(":")
            val days = a.optJSONArray("d")
            val n = days?.length() ?: 0
            for (off in 0..7) {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, off)
                c.set(Calendar.HOUR_OF_DAY, p[0].toInt())
                c.set(Calendar.MINUTE, p[1].toInt())
                c.set(Calendar.SECOND, 0)
                c.set(Calendar.MILLISECOND, 0)
                val dow = c.get(Calendar.DAY_OF_WEEK) - 1 // 0 = domingo, igual ao JS
                var ok = n == 0
                for (k in 0 until n) if (days!!.getInt(k) == dow) ok = true
                if (ok && c.timeInMillis > now && c.timeInMillis < best) {
                    best = c.timeInMillis
                    bl = a.optString("l", "Acordar!")
                    bt = a.getString("t")
                }
            }
        }
        val sp = ctx.getSharedPreferences(PREFS, 0)
        val sn = sp.getLong("snAt", 0)
        if (sn > now && sn < best) {
            best = sn
            bl = sp.getString("snL", "Soneca") ?: "Soneca"
            val c = Calendar.getInstance(); c.timeInMillis = sn
            bt = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        }
        if (best == Long.MAX_VALUE) {
            am.cancel(op(ctx, "", ""))
            return
        }
        val show = PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(best, show), op(ctx, bl, bt))
    }
}
