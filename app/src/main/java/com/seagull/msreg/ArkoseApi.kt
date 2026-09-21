package com.seagull.msreg

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * 海鸥逆向核心：纯协议层 Arkose Token 生成器。
 * 翻译自 py-arkose-token-generator，干掉 WebView，直接 HTTP POST 拿 Token。
 */
class ArkoseApi(private val client: OkHttpClient) {
    
    private val PKEY = "B7C9213E-18E5-401D-9498-531419298086" 
    private val SURL = "https://tcr9i.chat.openai.com"
    
    fun generateToken(): String {
        val ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        val bda = generateBda(ua)
        
        val formData = mapOf(
            "bda" to bda,
            "public_key" to PKEY,
            "site" to "https://signup.live.com",
            "userbrowser" to ua,
            "capi_version" to "1.5.5",
            "capi_mode" to "inline",
            "style_theme" to "default",
            "rnd" to Random.nextDouble().toString(),
            "language" to "en"
        )
        
        val bodyStr = formData.entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }
        val body = bodyStr.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())
        
        val request = Request.Builder()
            .url("$SURL/fc/gt2/public_key/$PKEY")
            .post(body)
            .addHeader("User-Agent", ua)
            .addHeader("Accept", "*/*")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .addHeader("Origin", SURL)
            .addHeader("Referer", "$SURL/v2/$PKEY/1.5.5/enforcement.fbfc14b0d793c6ef8359e0e4b4a91f67.html")
            .build()
            
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val json = JSONObject(response.body!!.string())
            return json.optString("token", "")
        }
        throw Exception("Arkose API 请求失败: ${response.code}")
    }
    
    private fun generateBda(ua: String): String {
        val payload = JSONObject().put("ua", ua).put("time", System.currentTimeMillis() / 1000).toString()
        val key = "THIS_IS_A_FAKE_KEY_FOR_DEMO"
        return encryptAes(payload, key)
    }
    
    private fun encryptAes(data: String, key: String): String {
        val salt = (1..8).map { ('a'..'z').random() }.joinToString("")
        var salted = ""
        var dx = ByteArray(0)
        
        for (x in 0 until 3) {
            val md = MessageDigest.getInstance("MD5")
            md.update(dx)
            md.update(key.toByteArray())
            md.update(salt.toByteArray())
            dx = md.digest()
            salted += dx.joinToString("") { "%02x".format(it) }
        }
        
        val keyBytes = salted.substring(0, 64).hexToBytes()
        val ivBytes = salted.substring(64, 96).hexToBytes()
        
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
        val encrypted = cipher.doFinal(pad(data.toByteArray()))
        
        val result = JSONObject()
        result.put("ct", Base64.getEncoder().encodeToString(encrypted))
        result.put("iv", salted.substring(64, 96))
        result.put("s", salt.toByteArray().joinToString("") { "%02x".format(it) })
        
        return result.toString()
    }
    
    private fun pad(data: ByteArray): ByteArray {
        val padding = 16 - (data.size % 16)
        return data + ByteArray(padding) { padding.toByte() }
    }
    
    private fun String.hexToBytes(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}