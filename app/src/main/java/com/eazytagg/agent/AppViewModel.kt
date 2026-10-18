package com.eazytagg.agent

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eazytagg.agent.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class Screen { SignIn, EventCode, Dashboard, Sessions, Scan, Search, Edit, Sync, Activity, Settings }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    // ---- navigation ----
    val stack = mutableStateListOf<Screen>()
    val screen: Screen get() = stack.lastOrNull() ?: Screen.SignIn

    fun go(s: Screen) { stack.add(s) }
    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.size - 1)
        return true
    }
    private fun reset(s: Screen) { stack.clear(); stack.add(s) }

    // ---- settings / session ----
    var theme by mutableStateOf(store.theme)
    var serverUrl by mutableStateOf(store.baseUrl)
    var email by mutableStateOf(store.agentEmail)
    var password by mutableStateOf("")
    var agentName by mutableStateOf(store.agentName)
    var busy by mutableStateOf(false)
    var message by mutableStateOf("")       // inline info / error on the current screen
    var messageIsError by mutableStateOf(true)
    var online by mutableStateOf(true)

    // ---- event ----
    var eventCodeInput by mutableStateOf("")
    var event by mutableStateOf<EventInfo?>(null)
    var sessions by mutableStateOf<List<SessionInfo>>(emptyList())
    var counts by mutableStateOf(Counts())
    var mode by mutableStateOf(Mode.CHECKIN)
    var selectedSession by mutableStateOf<SessionInfo?>(null)

    // ---- scanning ----
    var result by mutableStateOf<ScanResult?>(null)
    var pinInput by mutableStateOf("")
    private var lastCode = ""
    private var lastCodeAt = 0L
    private var resultJob: Job? = null

    // ---- search / edit ----
    var searchTerm by mutableStateOf("")
    var hits by mutableStateOf<List<SearchHit>>(emptyList())
    var searching by mutableStateOf(false)
    private var searchJob: Job? = null
    var editUuid by mutableStateOf("")
    var editName by mutableStateOf("")
    val editValues = mutableStateMapOf<String, String>()
    val editMulti = mutableStateMapOf<String, List<String>>()
    var editSaved by mutableStateOf(false)

    // ---- queue / activity ----
    val queue = mutableStateListOf<QueuedOp>()
    val activity = mutableStateListOf<ActivityItem>()
    var syncing by mutableStateOf(false)

    init {
        queue.addAll(store.loadQueue())
        activity.addAll(store.loadActivity())
        event = EventInfo.fromJson(store.eventJson)
        sessions = store.loadSessions()
        if (store.signedIn && agentName.isNotBlank()) {
            reset(if (event != null) Screen.Dashboard else Screen.EventCode)
            if (event != null) refreshCounts()
        } else reset(Screen.SignIn)

        viewModelScope.launch {
            while (true) {
                delay(15_000)
                if (queue.any { !it.failed }) flushQueue()
            }
        }
    }

    // ================= sign in =================
    fun testConnection() {
        val base = Api.normalizeBase(serverUrl)
        if (base.isEmpty()) { setMsg("Enter the server address first.", true); return }
        serverUrl = base
        viewModelScope.launch {
            busy = true
            when (val r = Api.ping(base)) {
                is ApiResult.Reply -> { online = true; setMsg(if (r.status == "success") "Connected to the server." else "Server replied: ${r.message}", r.status != "success") }
                is ApiResult.NoConnection -> setMsg("Cannot reach the server: ${r.reason}", true)
            }
            busy = false
        }
    }

    fun signIn() {
        val base = Api.normalizeBase(serverUrl)
        if (base.isEmpty()) { setMsg("Enter the server address.", true); return }
        if (email.isBlank() || password.isBlank()) { setMsg("Enter your email and password.", true); return }
        viewModelScope.launch {
            busy = true; message = ""
            when (val r = Api.login(base, email.trim(), password)) {
                is ApiResult.Reply -> {
                    online = true
                    if (r.status == "success") {
                        serverUrl = base
                        store.baseUrl = base
                        agentName = r.json.optString("name").ifBlank { email.trim() }
                        store.agentName = agentName
                        store.agentEmail = email.trim()
                        store.signedIn = true
                        password = ""
                        log("Signed in", "", "ok")
                        reset(if (event != null) Screen.Dashboard else Screen.EventCode)
                        if (event != null) refreshCounts()
                    } else setMsg(r.message.ifBlank { "Sign in failed." }, true)
                }
                is ApiResult.NoConnection -> setMsg("Cannot reach the server: ${r.reason}", true)
            }
            busy = false
        }
    }

    fun signOut() {
        store.signedIn = false
        password = ""
        reset(Screen.SignIn)
    }

    // ================= event =================
    fun openEvent() {
        val code = eventCodeInput.trim()
        if (code.isEmpty()) { setMsg("Enter the event code.", true); return }
        viewModelScope.launch {
            busy = true; message = ""
            when (val r = Api.getEvent(store.baseUrl, code)) {
                is ApiResult.Reply -> {
                    online = true
                    if (r.status == "success") {
                        val ev = EventInfo.fromServer(code, r.json)
                        event = ev
                        store.eventJson = ev.toJson()
                        loadSessions(code)
                        selectedSession = null
                        mode = Mode.CHECKIN
                        eventCodeInput = ""
                        log("Opened event", code, "ok")
                        reset(Screen.Dashboard)
                        refreshCounts()
                    } else setMsg(r.message.ifBlank { "Invalid event code." }, true)
                }
                is ApiResult.NoConnection -> setMsg("Cannot reach the server: ${r.reason}", true)
            }
            busy = false
        }
    }

    private suspend fun loadSessions(code: String) {
        val r = Api.getSessions(store.baseUrl, code)
        if (r is ApiResult.Reply && r.status == "success") {
            val arr = r.json.optJSONArray("sessions") ?: JSONArray()
            sessions = (0 until arr.length()).map {
                val o = arr.getJSONObject(it); SessionInfo(o.optString("uuid"), o.optString("name"))
            }
            store.saveSessions(sessions)
            val days = r.json.optJSONArray("days")
            val title = days?.optJSONObject(0)?.optString("title").orEmpty()
            if (title.isNotBlank()) {
                event = event?.copy(title = title)
                event?.let { store.eventJson = it.toJson() }
            }
        }
    }

    fun refreshCounts() {
        val ev = event ?: return
        viewModelScope.launch {
            val simple = Api.count(store.baseUrl, ev.code, false)
            val detailed = Api.count(store.baseUrl, ev.code, true)
            var today = counts.today
            if (simple is ApiResult.Reply) { online = true; today = simple.json.optInt("count", today) }
            else if (simple is ApiResult.NoConnection) online = false
            var days = counts.days
            var sess = counts.sessions
            if (detailed is ApiResult.Reply) {
                val c = detailed.json.optJSONObject("count")
                val d = c?.optJSONObject("days")
                if (d != null) days = d.keys().asSequence().map { it to d.optInt(it) }.toList()
                val s = c?.optJSONObject("sessions")
                sess = if (s != null) s.keys().asSequence().map { it to s.optInt(it) }.toList() else emptyList()
            }
            counts = Counts(today, days, sess)
        }
    }

    fun changeEvent() { reset(Screen.EventCode) }

    // ================= scanning =================
    fun startScan(m: Mode, session: SessionInfo? = null) {
        mode = m; selectedSession = session; result = null; pinInput = ""
        go(Screen.Scan)
    }

    /** Called by the camera for every barcode it sees. */
    fun onScanned(text: String) {
        val now = System.currentTimeMillis()
        if (result != null) return
        if (text == lastCode && now - lastCodeAt < 4000) return
        lastCode = text; lastCodeAt = now
        val uuid = UUID_RE.find(text)?.value
        when {
            uuid != null -> perform(uuid, "")
            PIN_RE.matches(text.trim()) -> perform("", text.trim())
            else -> showResult(ScanResult(ResultKind.ERROR, "Unrecognised code", detail = "This is not an EazyTagg code."))
        }
    }

    fun submitPin() {
        val p = pinInput.trim()
        if (p.isEmpty()) return
        pinInput = ""
        perform("", p)
    }

    private fun perform(uuid: String, pin: String) {
        val ev = event ?: return
        val base = store.baseUrl
        val m = mode
        val sess = selectedSession
        if (m == Mode.SESSION && sess == null) { showResult(ScanResult(ResultKind.ERROR, "Choose a session first")); return }
        viewModelScope.launch {
            busy = true
            val r: ApiResult = when {
                pin.isNotEmpty() -> Api.pin(base, ev.code, pin)
                m == Mode.CHECKIN -> Api.checkin(base, ev.code, uuid)
                m == Mode.CHECKOUT -> Api.checkout(base, ev.code, uuid)
                else -> Api.sessionLog(base, ev.code, sess!!.uuid, uuid)
            }
            busy = false
            val action = if (pin.isNotEmpty()) "PIN check-in" else m.label + (sess?.let { " · ${it.name}" } ?: "")
            val ref = if (pin.isNotEmpty()) "PIN $pin" else uuid.take(8)
            when (r) {
                is ApiResult.NoConnection -> {
                    online = false
                    enqueue(
                        QueuedOp(
                            id = System.nanoTime(),
                            kind = if (pin.isNotEmpty()) "pin" else when (m) { Mode.CHECKIN -> "checkin"; Mode.CHECKOUT -> "checkout"; else -> "session" },
                            eventCode = ev.code, attendeeUuid = uuid, sessionUuid = sess?.uuid.orEmpty(), pin = pin,
                            agent = agentName, at = System.currentTimeMillis(), label = "$action · $ref"
                        )
                    )
                    log(action, ref, "queued", "No connection")
                    showResult(ScanResult(ResultKind.QUEUED, "Saved offline", detail = "It will be sent when the connection is back."))
                }
                is ApiResult.Reply -> {
                    online = true
                    val name = r.json.optString("name")
                    val msg = r.message
                    val already = r.status == "exists" || msg.contains("already", true)
                    when {
                        r.status == "success" && !already -> {
                            log(action, name.ifBlank { ref }, "ok")
                            showResult(ScanResult(ResultKind.OK, okTitle(m, pin), name, r.json.optString("package")))
                            refreshCounts()
                        }
                        already -> {
                            log(action, name.ifBlank { ref }, "already", msg)
                            showResult(ScanResult(ResultKind.ALREADY, "Already done", name, msg))
                        }
                        else -> {
                            log(action, ref, "error", msg)
                            showResult(ScanResult(ResultKind.ERROR, msg.ifBlank { "Failed" }))
                        }
                    }
                }
            }
        }
    }

    private fun okTitle(m: Mode, pin: String) = when {
        pin.isNotEmpty() -> "Checked in"
        m == Mode.CHECKIN -> "Checked in"
        m == Mode.CHECKOUT -> "Checked out"
        else -> "Session recorded"
    }

    private fun showResult(r: ScanResult) {
        result = r
        resultJob?.cancel()
        resultJob = viewModelScope.launch {
            delay(if (r.kind == ResultKind.ERROR) 4000 else 2500)
            result = null
        }
    }

    fun dismissResult() { resultJob?.cancel(); result = null }

    // ================= search / edit =================
    fun onSearchChange(t: String) {
        searchTerm = t
        searchJob?.cancel()
        if (t.trim().length < 2) { hits = emptyList(); searching = false; return }
        searchJob = viewModelScope.launch {
            delay(400)
            runSearch()
        }
    }

    private suspend fun runSearch() {
        val ev = event ?: return
        searching = true; message = ""
        when (val r = Api.search(store.baseUrl, ev.code, searchTerm.trim())) {
            is ApiResult.Reply -> {
                online = true
                val uuids = r.json.optJSONArray("data") ?: JSONArray()
                val disp = r.json.optJSONArray("raw_search") ?: JSONArray()
                val raw = r.json.optJSONArray("raw_data") ?: JSONArray()
                hits = (0 until uuids.length()).map { i ->
                    val o = raw.optJSONObject(i) ?: JSONObject()
                    val m = o.keys().asSequence().associateWith { k -> o.optString(k) }
                    SearchHit(uuids.optString(i), disp.optString(i).ifBlank { "(no name)" }, m)
                }.take(100)
                if (hits.isEmpty()) setMsg("Nobody matches \"${searchTerm.trim()}\".", false)
            }
            is ApiResult.NoConnection -> { online = false; hits = emptyList(); setMsg("Search needs a connection: ${r.reason}", true) }
        }
        searching = false
    }

    fun checkInHit(h: SearchHit) {
        val ev = event ?: return
        viewModelScope.launch {
            busy = true
            val r = Api.checkin(store.baseUrl, ev.code, h.uuid)
            busy = false
            when (r) {
                is ApiResult.NoConnection -> {
                    enqueue(QueuedOp(System.nanoTime(), "checkin", ev.code, attendeeUuid = h.uuid, agent = agentName, at = System.currentTimeMillis(), label = "Check in · ${h.display}"))
                    log("Check in", h.display, "queued", "No connection")
                    setMsg("Saved offline: ${h.display}", false)
                }
                is ApiResult.Reply -> {
                    val already = r.status == "exists" || r.message.contains("already", true)
                    if (r.status == "success" && !already) { log("Check in", h.display, "ok"); setMsg("Checked in: ${h.display}", false); refreshCounts() }
                    else if (already) { log("Check in", h.display, "already", r.message); setMsg("Already checked in: ${h.display}", false) }
                    else { log("Check in", h.display, "error", r.message); setMsg(r.message.ifBlank { "Failed" }, true) }
                }
            }
        }
    }

    fun startEdit(h: SearchHit) {
        val ev = event ?: return
        editUuid = h.uuid
        editName = h.display
        editSaved = false
        editValues.clear(); editMulti.clear()
        message = ""
        go(Screen.Edit)
        viewModelScope.launch {
            busy = true
            val r = Api.load(store.baseUrl, h.uuid)
            if (r is ApiResult.Reply) {
                val d = r.json.optJSONObject("data")
                if (d != null) {
                    ev.fields.filter { it.editable }.forEach { f ->
                        if (f.isMulti) {
                            val a = d.optJSONArray(f.id)
                            editMulti[f.id] = if (a == null) emptyList() else (0 until a.length()).map { a.optString(it) }
                        } else editValues[f.id] = d.optString(f.id, "").let { if (it == "null") "" else it }
                    }
                } else setMsg("Could not load this person.", true)
            } else setMsg("Could not load: ${(r as ApiResult.NoConnection).reason}", true)
            busy = false
        }
    }

    fun saveEdit() {
        val ev = event ?: return
        val missing = ev.fields.filter { it.editable && it.required && !it.isMulti && editValues[it.id].orEmpty().isBlank() }
        if (missing.isNotEmpty()) { setMsg("Fill in: ${missing.joinToString { it.name }}", true); return }
        val data = JSONObject()
        ev.fields.filter { it.editable }.forEach { f ->
            if (f.isMulti) data.put(f.id, JSONArray(editMulti[f.id].orEmpty()))
            else data.put(f.id, editValues[f.id].orEmpty().trim())
        }
        viewModelScope.launch {
            busy = true; message = ""
            when (val r = Api.update(store.baseUrl, ev.code, editUuid, data.toString())) {
                is ApiResult.NoConnection -> {
                    enqueue(QueuedOp(System.nanoTime(), "update", ev.code, attendeeUuid = editUuid, entryData = data.toString(), agent = agentName, at = System.currentTimeMillis(), label = "Edit · $editName"))
                    log("Edit details", editName, "queued", "No connection")
                    setMsg("Saved offline. It will be sent when the connection is back.", false)
                    editSaved = true
                }
                is ApiResult.Reply -> when (r.status) {
                    "success" -> { log("Edit details", editName, "ok"); editSaved = true; setMsg("Saved.", false) }
                    "exists" -> { log("Edit details", editName, "error", r.message); setMsg(r.message, true) }
                    else -> { log("Edit details", editName, "error", r.message); setMsg(r.message.ifBlank { "Could not save." }, true) }
                }
            }
            busy = false
        }
    }

    // ================= queue =================
    private fun enqueue(op: QueuedOp) { queue.add(op); store.saveQueue(queue.toList()) }

    fun flushQueue() {
        if (syncing) return
        viewModelScope.launch {
            syncing = true
            val base = store.baseUrl
            for (op in queue.toList()) {
                if (op.failed) continue
                val r: ApiResult = when (op.kind) {
                    "checkin" -> Api.checkin(base, op.eventCode, op.attendeeUuid)
                    "checkout" -> Api.checkout(base, op.eventCode, op.attendeeUuid)
                    "session" -> Api.sessionLog(base, op.eventCode, op.sessionUuid, op.attendeeUuid)
                    "pin" -> Api.pin(base, op.eventCode, op.pin)
                    else -> Api.update(base, op.eventCode, op.attendeeUuid, op.entryData)
                }
                if (r is ApiResult.NoConnection) { online = false; break }
                online = true
                val rep = r as ApiResult.Reply
                val idx = queue.indexOfFirst { it.id == op.id }
                if (idx < 0) continue
                if (rep.status == "success" || rep.status == "exists" || rep.message.contains("already", true)) {
                    queue.removeAt(idx)
                    log(op.label, "", "synced", "Sent by ${op.agent}")
                } else {
                    queue[idx] = op.copy(attempts = op.attempts + 1, failed = true, error = rep.message)
                }
                store.saveQueue(queue.toList())
            }
            syncing = false
            refreshCounts()
        }
    }

    fun retryFailed(op: QueuedOp) {
        val i = queue.indexOfFirst { it.id == op.id }
        if (i >= 0) { queue[i] = op.copy(failed = false, error = ""); store.saveQueue(queue.toList()); flushQueue() }
    }

    fun discard(op: QueuedOp) {
        queue.removeAll { it.id == op.id }
        store.saveQueue(queue.toList())
    }

    // ================= misc =================
    fun chooseTheme(t: String) { theme = t; store.theme = t }

    fun clearActivity() { activity.clear(); store.saveActivity(emptyList()) }

    private fun log(action: String, who: String, result: String, detail: String = "") {
        activity.add(ActivityItem(System.currentTimeMillis(), agentName, action, who, result, detail))
        store.saveActivity(activity.toList())
    }

    fun setMsg(m: String, error: Boolean) { message = m; messageIsError = error }
    fun clearMsg() { message = "" }

    companion object {
        val UUID_RE = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        val PIN_RE = Regex("^\\d{3,8}$")
    }
}
