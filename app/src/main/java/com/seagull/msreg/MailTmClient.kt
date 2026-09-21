package com.seagull.msreg

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MailTmClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private var authToken = ""
    private var currentEmail = ""

    fun createAccount(): String {
        val domainsReq = Request.Builder().url("https://api.mail.tm/domains").build()
        val domainsResp = client.newCall(domainsReq).execute()
        val domainJson = JSONObject(domainsResp.body!!.string())
        val members = domainJson.optJSONArray("hydra:member") ?: domainJson.optJSONArray("member")
        val domain = members.getJSONObject(0).getString("domain")

        currentEmail = "seagull_${System.currentTimeMillis()}@$domain"
        val pwd = "password123"
        val json = JSONObject().put("address", currentEmail).put("password", pwd).toString()
        val createReq = Request.Builder()
            .url("https://api.mail.tm/accounts")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(createReq).execute()

        val loginJson = JSONObject().put("address", currentEmail).put("password", pwd).toString()
        val loginReq = Request.Builder()
            .url("https://api.mail.tm/token")
            .post(loginJson.toRequestBody("application/json".toMediaType()))
            .build()
        val loginResp = client.newCall(loginReq).execute()
        authToken = JSONObject(loginResp.body!!.string()).getString("token")
        return currentEmail
    }

    fun pollForCode(): String {
        val deadline = System.currentTimeMillis() + 180000
        val regex = Regex("\\b\\d{6}\\b|\\b\\d{7}\\b")
        while (System.currentTimeMillis() < deadline) {
            try {
                val req = Request.Builder()
                    .url("https://api.mail.tm/messages")
                    .addHeader("Authorization", "Bearer $authToken")
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body!!.string()
                    val json = JSONObject(bodyStr)
                    val messages = json.optJSONArray("hydra:member") ?: json.optJSONArray("member")
                    if (messages != null) {
                        for (i in 0 until messages.length()) {
                            val msg = messages.getJSONObject(i)
                            val subject = msg.optString("subject")
                            val intro = msg.optString("intro")
                            val text = msg.optString("text")
                            val combined = "$subject $intro $text"

                            if (combined.contains("Microsoft", ignoreCase = true) || combined.contains("verification", ignoreCase = true) || combined.contains("code", ignoreCase = true)) {
                                val match = regex.find(combined)
                                if (match != null) return match.value
                            } else {
                                val match = regex.find(combined)
                                if (match != null) return match.value
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
            Thread.sleep(4000)
        }
        throw Exception("接码超时 (Mail polling timeout)")
    }
}