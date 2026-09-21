package com.seagull.msreg

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), MsRegEngine.Listener {
    private lateinit var webView: WebView
    private lateinit var logView: TextView
    private lateinit var engine: MsRegEngine

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val btnStart = Button(this).apply {
            text = "开始全自动注册 (Start Auto-Reg)"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(btnStart)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                800
            )
        }
        layout.addView(webView)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        logView = TextView(this).apply {
            textSize = 12f
            setPadding(16, 16, 16, 16)
        }
        scrollView.addView(logView)
        layout.addView(scrollView)

        setContentView(layout)

        engine = MsRegEngine(this, webView)
        engine.listener = this

        btnStart.setOnClickListener {
            logView.text = ""
            engine.start()
        }
    }

    override fun onLog(msg: String) {
        runOnUiThread {
            logView.append("$msg\n")
        }
    }

    override fun onSuccess(email: String, password: String) {
        runOnUiThread {
            logView.append("\n[SUCCESS] 注册成功!\nEmail: $email\nPassword: $password\n")
        }
    }

    override fun onFailed(reason: String) {
        runOnUiThread {
            logView.append("\n[FAILED] $reason\n")
        }
    }
}