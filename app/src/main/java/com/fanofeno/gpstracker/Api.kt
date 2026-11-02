package com.fanofeno.gpstracker

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Api {
    private fun request(path: String, method: String, body: String? = null, token: String? = null,
                        extraHeaders: Map<String, String> = emptyMap()): Pair<Int, String> {
        val conn = (URL(BuildConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection)
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
        conn.setRequestProperty("Content-Type", "application/json")
        if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
        extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        return code to response
    }

    fun signUp(email: String, password: String): String {
        val (code, response) = request("/auth/v1/signup", "POST",
            JSONObject().put("email", email).put("password", password).toString())
        if (code !in 200..299) throw Exception(parseError(response, "Tsy nahomby ny fisoratana anarana"))
        return "Vita ny fangatahana. Raha mitaky fanamarinana mailaka ny Supabase, jereo ny mailakao aloha vao miditra."
    }

    fun signIn(email: String, password: String): JSONObject {
        val (code, response) = request("/auth/v1/token?grant_type=password", "POST",
            JSONObject().put("email", email).put("password", password).toString())
        if (code !in 200..299) throw Exception(parseError(response, "Tsy nety ny fidirana"))
        return JSONObject(response)
    }

    fun refresh(refreshToken: String): JSONObject {
        val (code, response) = request("/auth/v1/token?grant_type=refresh_token", "POST",
            JSONObject().put("refresh_token", refreshToken).toString())
        if (code !in 200..299) throw Exception("Tsy afaka nanavao session")
        return JSONObject(response)
    }

    fun upload(context: Context, point: JSONObject): Boolean {
        val prefs = context.getSharedPreferences("gps_session", Context.MODE_PRIVATE)
        var access = prefs.getString("access_token", null) ?: return false
        val refresh = prefs.getString("refresh_token", null)
        fun send(token: String): Int {
            return request("/rest/v1/gps_locations", "POST", point.toString(), token,
                mapOf("Prefer" to "return=minimal")).first
        }
        var code = send(access)
        if ((code == 401 || code == 403) && refresh != null) {
            try {
                val newSession = refresh(refresh)
                access = newSession.getString("access_token")
                prefs.edit().putString("access_token", access)
                    .putString("refresh_token", newSession.optString("refresh_token", refresh)).apply()
                code = send(access)
            } catch (_: Exception) { return false }
        }
        return code in 200..299
    }

    private fun parseError(body: String, fallback: String): String = try {
        val j = JSONObject(body)
        j.optString("msg", j.optString("message", j.optString("error_description", fallback)))
    } catch (_: Exception) { fallback }
}
