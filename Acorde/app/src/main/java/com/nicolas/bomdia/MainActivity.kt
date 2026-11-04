package com.nicolas.bomdia

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.parseColor("#0b1020")
        window.navigationBarColor = Color.parseColor("#0b1020")
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Android")
        web.webViewClient = WebViewClient()
        web.loadUrl("file:///android_asset/bom-dia.html")
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() {
        super.onResume()
        web.evaluateJavascript("window.refresh && refresh()", null)
        // Android 14+: permissão pra alarme aparecer em cima da tela de bloqueio
        if (Build.VERSION.SDK_INT >= 34) {
            val sp = getSharedPreferences("bomdia", 0)
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent() && !sp.getBoolean("fsiAsked", false)) {
                sp.edit().putBoolean("fsiAsked", true).apply()
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
            }
        }
    }

    inner class Bridge {
        @JavascriptInterface fun setConfig(json: String) { Scheduler.save(this@MainActivity, json) }

        @JavascriptInterface fun get(id: String, url: String) {
            Thread {
                val r = if (!url.startsWith("https://")) "" else try {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.connectTimeout = 8000; c.readTimeout = 8000
                    c.setRequestProperty("User-Agent", "Mozilla/5.0 BomDia")
                    c.inputStream.bufferedReader().use { it.readText() }
                } catch (e: Exception) { "" }
                runOnUiThread {
                    web.evaluateJavascript("window.__r(" + JSONObject.quote(id) + "," + JSONObject.quote(r) + ")", null)
                }
            }.start()
        }

        @JavascriptInterface fun open(url: String) {
            if (url.startsWith("https://") || url.startsWith("http://"))
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        @JavascriptInterface fun test() {
            startForegroundService(Intent(this@MainActivity, AlarmService::class.java)
                .putExtra("l", "Teste do alarme").putExtra("t", "agora"))
            startActivity(Intent(this@MainActivity, AlarmActivity::class.java)
                .putExtra("l", "Teste do alarme").putExtra("t", "agora"))
        }
    }
}
