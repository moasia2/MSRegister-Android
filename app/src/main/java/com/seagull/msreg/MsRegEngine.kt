package com.seagull.msreg

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.*
import java.util.*

class MsRegEngine(private val activity: Activity, private val webView: WebView) {
    interface Listener {
        fun onLog(msg: String)
        fun onSuccess(email: String, password: String)
        fun onFailed(reason: String)
    }

    var listener: Listener? = null
    private val mailClient = MailTmClient()

    @SuppressLint("SetJavaScriptEnabled")
    fun start() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        webView.addJavascriptInterface(JsBridge(), "AndroidBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("signup.live.com") == true || url?.contains("account.live.com") == true) {
                    view?.evaluateJavascript(PxBypassJs.BYPASS_PAYLOAD, null)
                }
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()
                if (url.contains("arkoselabs") || url.contains("funcaptcha") || url.contains("px2") || url.contains("client-api.arkoselabs.com")) {
                    listener?.onLog("[Bypass] 网络层拦截: $url")
                    val fakeJson = """{"token":"bypass_${System.currentTimeMillis()}","success":true}""
                    return WebResourceResponse("application/json", "UTF-8", fakeJson.byteInputStream())
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        runStateMachine()
    }

    private fun runStateMachine() {
        Thread {
            try {
                listener?.onLog("[*] 状态机启动，准备造邮箱...")
                val email = mailClient.createAccount()
                val password = "Seagull@${Random().nextInt(99999)}!"
                listener?.onLog("[+] 邮箱: $email")

                activity.runOnUiThread {
                    webView.loadUrl("https://signup.live.com/signup?lic=1&mkt=en-US&uaid=${UUID.randomUUID()}")
                }
                Thread.sleep(6000)

                listener?.onLog("[*] 填写邮箱...")
                evaluateJs(PxBypassJs.fillEmail(email))
                Thread.sleep(1500)
                evaluateJs(PxBypassJs.clickNext())
                Thread.sleep(5000)

                listener?.onLog("[*] 填写密码...")
                evaluateJs(PxBypassJs.fillPassword(password))
                Thread.sleep(1500)
                evaluateJs(PxBypassJs.clickNext())
                Thread.sleep(5000)

                listener?.onLog("[*] 填写个人信息...")
                evaluateJs(PxBypassJs.fillDetails("Seagull", "Operator"))
                Thread.sleep(1500)
                evaluateJs(PxBypassJs.clickNext())
                Thread.sleep(6000)

                listener?.onLog("[*] 等待微软验证码...")
                val code = mailClient.pollForCode()
                listener?.onLog("[+] 拿到验证码: $code")

                evaluateJs(PxBypassJs.fillOtp(code))
                Thread.sleep(2000)
                evaluateJs(PxBypassJs.clickNext())
                Thread.sleep(6000)

                listener?.onLog("[*] 检查注册状态...")
                activity.runOnUiThread {
                    webView.evaluateJavascript("window.location.href") { url ->
                        val u = url?.replace(""", "") ?: ""
                        if (u.contains("account.microsoft.com") || u.contains("outlook.live.com") || u.contains("login.live.com/oauth") || u.contains("signup.live.com") && u.contains("success")) {
                            listener?.onSuccess(email, password)
                        } else {
                            listener?.onFailed("未检测到成功跳转，当前URL: $u")
                        }
                    }
                }
            } catch (e: Exception) {
                listener?.onFailed(e.message ?: "Unknown Error")
            }
        }.start()
    }

    private fun evaluateJs(js: String) {
        activity.runOnUiThread { webView.evaluateJavascript(js, null) }
    }

    inner class JsBridge {
        @JavascriptInterface
        fun log(msg: String) { listener?.onLog("[JS] $msg") }
    }
}