package com.nicolas.bomdia

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import kotlin.random.Random

class AlarmActivity : Activity() {
    private var ans = 0

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val label = intent.getStringExtra("l") ?: "Acordar!"
        val time = intent.getStringExtra("t") ?: ""
        val chal = Scheduler.cfg(this).optBoolean("chal", true)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(48, 48, 48, 48)
        root.setBackgroundColor(Color.rgb(200, 20, 40))

        fun tv(t: String, sp: Float) = TextView(this).apply {
            text = t; textSize = sp; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        }
        root.addView(tv(label, 28f))
        root.addView(tv(time, 72f))

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.gravity = Gravity.CENTER
        input.textSize = 28f
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.LTGRAY)

        if (chal) {
            val x = Random.nextInt(7, 19)
            val y = Random.nextInt(6, 14)
            ans = x * y
            root.addView(tv("$x × $y = ?", 36f))
            root.addView(input)
        }

        val off = Button(this)
        off.text = "Desligar"
        off.textSize = 22f
        off.setOnClickListener {
            if (chal && input.text.toString().toIntOrNull() != ans) {
                input.setText("")
                input.hint = "errou! tenta de novo"
            } else dismiss()
        }
        root.addView(off)

        if (!chal) {
            val sn = Button(this)
            sn.text = "Soneca 5 min"
            sn.setOnClickListener {
                Scheduler.snooze(this, "$label (soneca)")
                stopService(Intent(this, AlarmService::class.java))
                finish()
            }
            root.addView(sn)
        }
        setContentView(root)
    }

    private fun dismiss() {
        stopService(Intent(this, AlarmService::class.java))
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    @Deprecated("sem voltar")
    override fun onBackPressed() {}
}
