package com.eazytagg.agent.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Result of one call to the EazyTagg PHP API. */
sealed class ApiResult {
    /** Server answered with JSON. [status] is the "status" field (success / error / exists). */
    data class Reply(val status: String, val message: String, val json: JSONObject) : ApiResult()
    /** Could not reach the server (offline, wrong address, timeout, not JSON). */
    data class NoConnection(val reason: String) : ApiResult()
}

object Api {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Drop pooled sockets, which go stale when Wi-Fi drops and returns. */
    fun resetConnections() { client.connectionPool.evictAll() }

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /** "192.168.1.5/eazytagg_test" -> "http://192.168.1.5/eazytagg_test/" */
    fun normalizeBase(input: String): String {
        var s = input.trim()
        if (s.isEmpty()) return ""
        if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) {
            val host = s.substringBefore('/').substringBefore(':')
            val local = host == "localhost" || host == "10.0.2.2" ||
                Regex("^\\d{1,3}(\\.\\d{1,3}){3}$").matches(host)
            s = (if (local) "http://" else "https://") + s
        }
        s = s.replace(Regex("(?i)(/api)?(/[a-z.]+\\.php)?/*$"), "")
        return "$s/"
    }

    private fun parse(body: String): ApiResult {
        val start = body.indexOf('{')
        if (start < 0) return ApiResult.NoConnection("The server did not answer correctly. Check the server address.")
        return try {
            val o = JSONObject(body.substring(start))
            ApiResult.Reply(o.optString("status"), o.optString("message"), o)
        } catch (e: Exception) {
            ApiResult.NoConnection("The server reply could not be read.")
        }
    }

    suspend fun call(base: String, endpoint: String, payload: JSONObject? = null): ApiResult =
        withContext(Dispatchers.IO) {
            try {
                val url = base + "api/" + endpoint
                val req = Request.Builder().url(url).apply {
                    if (payload == null) get() else post(payload.toString().toRequestBody(JSON))
                }.build()
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (resp.code >= 500 && !text.contains("{")) {
                        ApiResult.NoConnection("Server error (${resp.code})")
                    } else parse(text)
                }
            } catch (e: Exception) {
                ApiResult.NoConnection(e.message ?: "No connection")
            }
        }

    suspend fun ping(base: String) = call(base, "ping.php")

    suspend fun login(base: String, user: String, pass: String) =
        call(base, "agent.auth.php", JSONObject().put("username", user).put("password", pass))

    suspend fun getEvent(base: String, code: String) =
        call(base, "get.event.php", JSONObject().put("event_code", code))

    suspend fun getSessions(base: String, code: String) =
        call(base, "get.sessions.php", JSONObject().put("event_code", code))

    suspend fun count(base: String, code: String, detailed: Boolean): ApiResult {
        val p = JSONObject().put("event_code", code)
        if (detailed) p.put("detailed", "1")
        return call(base, "attendee.count.php", p)
    }

    /** Adds the real scan time (phone clock, epoch ms) for queued scans; live scans omit it. */
    private fun JSONObject.scanned(at: Long?): JSONObject {
        if (at != null && at > 0) put("scanned_at", at)
        return this
    }

    suspend fun checkin(base: String, code: String, uuid: String, at: Long? = null) =
        call(base, "log.checkin.php", JSONObject().put("event_code", code).put("attendee_uuid", uuid).scanned(at))

    suspend fun checkout(base: String, code: String, uuid: String, at: Long? = null) =
        call(base, "log.checkout.php", JSONObject().put("event_code", code).put("attendee_uuid", uuid).scanned(at))

    suspend fun sessionLog(base: String, code: String, sessionUuid: String, uuid: String, at: Long? = null) =
        call(
            base, "log.sessions.php",
            JSONObject().put("event_code", code).put("session_uuid", sessionUuid)
                .put("type", "session").put("attendee_uuid", uuid).scanned(at)
        )

    suspend fun pin(base: String, code: String, pin: String, at: Long? = null) =
        call(base, "pin.entry.php", JSONObject().put("event_code", code).put("pin_code", pin).scanned(at))

    suspend fun search(base: String, code: String, term: String) =
        call(base, "attendee.search.php", JSONObject().put("event_code", code).put("term", term))

    suspend fun load(base: String, uuid: String) =
        call(base, "attendee.load.php", JSONObject().put("attendee_uuid", uuid))

    suspend fun update(base: String, code: String, uuid: String, entryData: String) =
        call(
            base, "update.entry.php",
            JSONObject().put("event_code", code).put("attendee_uuid", uuid).put("entry_data", entryData)
        )

    /** Find events by name. Needs the search.events.php endpoint on the server. */
    suspend fun searchEvents(base: String, term: String) =
        call(base, "search.events.php", JSONObject().put("term", term))
}
