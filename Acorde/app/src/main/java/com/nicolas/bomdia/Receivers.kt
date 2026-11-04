package com.nicolas.bomdia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val s = Intent(c, AlarmService::class.java)
            .putExtra("l", i.getStringExtra("l"))
            .putExtra("t", i.getStringExtra("t"))
        c.startForegroundService(s)
        Scheduler.scheduleNext(c) // agenda o próximo
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Scheduler.scheduleNext(c)
    }
}
