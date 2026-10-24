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

enum class Screen { Splash, SignIn, EventCode, Dashboard, Sessions, Scan, Search, Edit, Sync, Activity, Settings }

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
    var serverUrl by mutableStateOf(if (Brand.isPd) store.baseUrl.removePrefix("http://").removePrefix("https://").substringBefore('/') else store.baseUrl)
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

    // ---- Popular Dynamics extras ----
    /** Practice mode: scans are answered locally, nothing is sent or queued. */
    var testMode by mutableStateOf(false)
    var menuView by mutableStateOf(store.menuView)
    var torchOn by mutableStateOf(false)
    var frontCamera by mutableStateOf(false)
    var eventQuery by mutableStateOf("")
    var eventHits by mutableStateOf<List<EventHit>>(emptyList())
    var eventSearching by mutableStateOf(false)
    private var eventJob: Job? = null
    private var bootTarget = Screen.SignIn

    fun chooseMenuView(v: String) { menuView = v; store.menuView = v }

    /** Called when the splash animation has played. */
    fun finishSplash() { if (screen == Screen.Splash) reset(bootTarget) }

    fun onEventQueryChange(t: String) {
        eventQuery = t
        eventJob?.cancel()
        if (t.trim().length < 2) { eventHits = emptyList(); eventSearching = false; return }
        eventJob = viewModelScope.launch {
            delay(350)
            eventSearching = true
            when (val r = Api.searchEvents(store.baseUrl, t.trim())) {
                is ApiResult.Reply -> {
                    online = true
                    val arr = r.json.optJSONArray("events") ?: JSONArray()
                    eventHits = (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        EventHit(o.optString("code"), o.optString("title"), o.optString("city"), o.optString("date"))
                    }.filter { it.code.isNotBlank() }
                    if (r.status != "success") setMsg(r.message.ifBlank { "Event search is not available on this server yet." }, true)
                    else if (eventHits.isEmpty()) setMsg("No event matches that name.", false) else clearMsg()
                }
                is ApiResult.NoConnection -> { eventHits = emptyList(); setMsg("Cannot search while offline. Type the event code instead.", true) }
            }
            eventSearching = false
        }
    }

    fun pickEvent(h: EventHit) { eventCodeInput = h.code; clearMsg() }

    // ---- queue / activity ----
    val queue = mutableStateListOf<QueuedOp>()
    val activity = mutableStateListOf<ActivityItem>()
    var syncing by mutableStateOf(false)
    var syncNote by mutableStateOf("")

    init {
        queue.addAll(store.loadQueue())
        activity.addAll(store.loadActivity())
        event = EventInfo.fromJson(store.eventJson)
        sessions = store.loadSessions()
        if (store.signedIn && agentName.isNotBlank()) {
            bootTarget = if (event != null) Screen.Dashboard else Screen.EventCode
            if (event != null) refreshCounts()
        } else bootTarget = Screen.SignIn
        reset(if (Brand.isPd) Screen.Splash else bootTarget)

        viewModelScope.launch {
            while (true) {
                delay(5_000)
                if (queue.any { !it.failed }) flushQueue()
            }
        }
    }

    private val ipRe = Regex("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$")
    fun isValidIp(s: String) = ipRe.matches(s.trim())

    /** The Popular Dynamics app asks for the IP only; the server folder is fixed in the build. */
    private fun baseOf(input: String): String {
        val t = input.trim()
        return Api.normalizeBase(if (Brand.isPd && t.isNotEmpty() && '/' !in t && BuildConfig.SERVER_PATH.isNotEmpty()) "$t/${BuildConfig.SERVER_PATH}" else t)
    }

    // ================= sign in =================
    fun testConnection() {
        if (Brand.isPd && !isValidIp(serverUrl)) { setMsg("Enter a valid server IP address, for example 192.168.0.120.", true); return }
        val base = baseOf(serverUrl)
        if (base.isEmpty()) { setMsg("Enter the server address first.", true); return }
        if (!Brand.isPd) serverUrl = base
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
        if (Brand.isPd && !isValidIp(serverUrl)) { setMsg("Enter a valid server IP address, for example 192.168.0.120.", true); return }
        val base = baseOf(serverUrl)
        if (base.isEmpty()) { setMsg("Enter the server address.", true); return }
        if (email.isBlank() || password.isBlank()) { setMsg("Enter your email and password.", true); return }
        viewModelScope.launch {
            busy = true; message = ""
            when (val r = Api.login(base, email.trim(), password)) {
                is ApiResult.Reply -> {
                    online = true
                    if (r.status == "success") {
                        if (!Brand.isPd) serverUrl = base
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
        if (testMode) {
            showResult(ScanResult(ResultKind.OK, "Practice " + okTitle(m, pin).lowercase(), "Test attendee", "Test mode: nothing was saved or sent"))
            return
        }
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
                    showResult(ScanResult(ResultKind.QUEUED, "Saved offline", detail = "It will be sent when the connection is back. The tag prints then, if this is a first check-in."))
                }
                is ApiResult.Reply -> {
                    online = true
                    val name = r.json.optString("name")
                    val msg = r.message
                    val already = r.status == "exists" || msg.contains("already", true)
                    when {
                        r.status == "success" && !already -> {
                            log(action, name.ifBlank { ref }, "ok")
                            val pk = r.json.optString("package")
                            val printNote = if (r.json.optBoolean("printed", false)) "Tag sent to the printer" else ""
                            showResult(ScanResult(ResultKind.OK, okTitle(m, pin), name, listOf(pk, printNote).filter { it.isNotBlank() }.joinToString(" · ")))
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
                    // name shown: the event's search setting, else the name field(s), else the first filled values
                    val nameIds = ev.successField.split(' ').filter { it.isNotBlank() }
                    val fromName = nameIds.map { m[it].orEmpty().trim() }.filter { it.isNotEmpty() }.joinToString(" ")
                    val firstValues = m.values.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("(") }.take(2).joinToString(", ")
                    val shown = disp.optString(i).replace(Regex("^[,\\s]+|[,\\s]+$"), "").ifBlank { fromName }.ifBlank { firstValues }.ifBlank { "(no name)" }
                    SearchHit(uuids.optString(i), shown, m)
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
                    if (r.status == "success" && !already) { log("Check in", h.display, "ok"); setMsg("Checked in: ${h.display}" + (if (r.json.optBoolean("printed", false)) " · tag sent to the printer" else ""), false); refreshCounts() }
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

    private fun clock(): String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

    /** Called when the phone gets a network again or the app comes back to the front. */
    fun networkBack() {
        Api.resetConnections()
        flushQueue()
    }

    fun flushQueue() {
        if (syncing) return
        if (queue.none { !it.failed }) return
        viewModelScope.launch {
            syncing = true
            var sent = 0
            try {
                val base = store.baseUrl
                for (op in queue.toList()) {
                    if (op.failed) continue
                    val r: ApiResult = when (op.kind) {
                        "checkin" -> Api.checkin(base, op.eventCode, op.attendeeUuid, op.at)
                        "checkout" -> Api.checkout(base, op.eventCode, op.attendeeUuid, op.at)
                        "session" -> Api.sessionLog(base, op.eventCode, op.sessionUuid, op.attendeeUuid, op.at)
                        "pin" -> Api.pin(base, op.eventCode, op.pin, op.at)
                        else -> Api.update(base, op.eventCode, op.attendeeUuid, op.entryData)
                    }
                    if (r is ApiResult.NoConnection) {
                        online = false
                        syncNote = "${clock()}  Could not reach the server (${r.reason}). Will try again."
                        break
                    }
                    online = true
                    val rep = r as ApiResult.Reply
                    val idx = queue.indexOfFirst { it.id == op.id }
                    if (idx < 0) continue
                    if (rep.status == "success" || rep.status == "exists" || rep.message.contains("already", true)) {
                        queue.removeAt(idx)
                        sent++
                        log(op.label, "", "synced", "Sent by ${op.agent}")
                    } else {
                        queue[idx] = op.copy(attempts = op.attempts + 1, failed = true, error = rep.message)
                        syncNote = "${clock()}  The server refused one item: ${rep.message}"
                    }
                    store.saveQueue(queue.toList())
                }
                if (sent > 0) syncNote = "${clock()}  Sent $sent item${if (sent == 1) "" else "s"}."
            } catch (e: Exception) {
                syncNote = "${clock()}  Sync problem: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                syncing = false
            }
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
