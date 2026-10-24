package com.eazytagg.agent.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eazytagg.agent.AppViewModel
import com.eazytagg.agent.Screen
import com.eazytagg.agent.data.Field
import com.eazytagg.agent.data.Mode
import com.eazytagg.agent.data.ResultKind

@Composable
fun Root(vm: AppViewModel) {
    val pd = com.eazytagg.agent.Brand.isPd
    when (vm.screen) {
        Screen.Splash -> if (pd) PdSplash(vm) else SignInScreen(vm)
        Screen.SignIn -> if (pd) PdSignIn(vm) else SignInScreen(vm)
        Screen.EventCode -> if (pd) PdFindEvent(vm) else EventCodeScreen(vm)
        Screen.Dashboard -> if (pd) PdMenu(vm) else DashboardScreen(vm)
        Screen.Sessions -> SessionsScreen(vm)
        Screen.Scan -> ScanScreen(vm)
        Screen.Search -> SearchScreen(vm)
        Screen.Edit -> EditScreen(vm)
        Screen.Sync -> SyncScreen(vm)
        Screen.Activity -> ActivityScreen(vm)
        Screen.Settings -> SettingsScreen(vm)
    }
}

private fun pendingCount(vm: AppViewModel) = vm.queue.count { !it.failed }
internal fun offlineBanner(vm: AppViewModel): String? {
    val n = pendingCount(vm)
    return when {
        vm.testMode && !vm.online -> "Test mode on. Offline. Nothing is saved."
        vm.testMode -> "Test mode on. Nothing is saved or sent."
        !vm.online && n > 0 -> "Offline - $n waiting to sync"
        !vm.online -> "Offline - actions are saved and sent later"
        else -> null
    }
}

// ---------------------------------------------------------------- sign in
@Composable
fun SignInScreen(vm: AppViewModel) {
    Page(title = "EazyTagg Agent", subtitle = "Door staff sign in") {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.serverUrl, onValueChange = { vm.serverUrl = it; vm.clearMsg() },
            label = { Text("Server address") },
            placeholder = { Text("e.g. 192.168.1.20/eazytagg") },
            supportingText = { Text("The address of your EazyTagg server, from the admin.") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        OutlinedTextField(
            value = vm.email, onValueChange = { vm.email = it; vm.clearMsg() },
            label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = vm.password, onValueChange = { vm.password = it; vm.clearMsg() },
            label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        MessageText(vm.message, vm.messageIsError)
        Button(onClick = { vm.signIn() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Sign in", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = { vm.testConnection() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Test connection")
        }
    }
}

// ---------------------------------------------------------------- event code
@Composable
fun EventCodeScreen(vm: AppViewModel) {
    Page(
        title = "Event", subtitle = "Signed in as ${vm.agentName}",
        onBack = if (vm.stack.size > 1) ({ vm.back() }) else null,
        banner = offlineBanner(vm),
        actions = { TextButton(onClick = { vm.signOut() }) { Text("Sign out") } }
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Enter the event code", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("You get the code from the event organizer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = vm.eventCodeInput, onValueChange = { vm.eventCodeInput = it; vm.clearMsg() },
            label = { Text("Event code") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        MessageText(vm.message, vm.messageIsError)
        Button(onClick = { vm.openEvent() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Open event", fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------- dashboard
@Composable
fun DashboardScreen(vm: AppViewModel) {
    val ev = vm.event
    val pending = pendingCount(vm)
    Page(
        title = if (ev == null) "Event" else ev.title.ifBlank { "Event ${ev.code}" },
        subtitle = if (ev == null) null else "Code ${ev.code} · ${vm.agentName}",
        banner = offlineBanner(vm),
        actions = { TextButton(onClick = { vm.go(Screen.Settings) }) { Text("Settings") } }
    ) {
        val wide = isWide()
        val countsCard: @Composable () -> Unit = {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Checked in today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text("${vm.counts.today}", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    vm.counts.days.forEach { (k, v) -> SpaceBetween { Text(k); Text("$v", fontWeight = FontWeight.Bold) } }
                    if (vm.counts.sessions.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        vm.counts.sessions.forEach { (k, v) -> SpaceBetween { Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("$v", fontWeight = FontWeight.Bold) } }
                    }
                    TextButton(onClick = { vm.refreshCounts() }) { Text("Refresh counts") }
                }
            }
        }
        val actions: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigAction("Check in", "Scan a QR code or enter a PIN") { vm.startScan(Mode.CHECKIN) }
                BigAction("Check out", "Record someone leaving") { vm.startScan(Mode.CHECKOUT) }
                if (vm.sessions.isNotEmpty()) BigAction("Sessions", "${vm.sessions.size} session(s)") { vm.go(Screen.Sessions) }
                BigAction("Search & edit", "Find by name, fix details") { vm.searchTerm = ""; vm.hits = emptyList(); vm.clearMsg(); vm.go(Screen.Search) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { vm.go(Screen.Sync) }, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text(if (pending > 0) "Sync ($pending)" else "Sync")
                    }
                    OutlinedButton(onClick = { vm.go(Screen.Activity) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Activity") }
                    OutlinedButton(onClick = { vm.changeEvent() }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Event") }
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { actions() }
                Box(Modifier.weight(1f)) { countsCard() }
            }
        } else {
            countsCard(); actions()
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BigAction(title: String, sub: String, onClick: () -> Unit) {
    Card(
        onClick = onClick, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- sessions
@Composable
fun SessionsScreen(vm: AppViewModel) {
    Page(title = "Choose a session", subtitle = "Scans are recorded against it", onBack = { vm.back() }, banner = offlineBanner(vm)) {
        if (vm.sessions.isEmpty()) Text("This event has no sessions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        vm.sessions.forEach { s -> BigAction(s.name, "Tap to start scanning") { vm.startScan(Mode.SESSION, s) } }
    }
}

// ---------------------------------------------------------------- scan
@Composable
fun ScanScreen(vm: AppViewModel) {
    val title = when (vm.mode) {
        Mode.SESSION -> vm.selectedSession?.name ?: "Session"
        else -> vm.mode.label
    }
    Page(title = title, subtitle = "Point the camera at a QR code", onBack = { vm.back() }, banner = offlineBanner(vm), scroll = false) {
        val wide = isWide()
        val camera: @Composable (Modifier) -> Unit = { m ->
            Box(m.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)).clip20()) {
                var hasTorch by remember { mutableStateOf(false) }
                CameraScanner(Modifier.fillMaxSize(), torch = vm.torchOn, front = vm.frontCamera, onTorchSupport = { hasTorch = it }) { vm.onScanned(it) }
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = { vm.frontCamera = !vm.frontCamera; vm.torchOn = false },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0x8C0A0B24), contentColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0x73FFFFFF))
                    ) { Text(if (vm.frontCamera) "Front" else "Back") }
                    if (hasTorch && !vm.frontCamera) OutlinedButton(
                        onClick = { vm.torchOn = !vm.torchOn },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (vm.torchOn) Color(0xFF199BF1) else Color(0x8C0A0B24), contentColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0x73FFFFFF))
                    ) { Text(if (vm.torchOn) "Light on" else "Light") }
                }
                ResultOverlay(vm)
            }
        }
        val pin: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vm.pinInput, onValueChange = { vm.pinInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Or enter the PIN") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(onClick = { vm.submitPin() }, enabled = !vm.busy && vm.pinInput.isNotBlank(), modifier = Modifier.height(56.dp)) { Text("Go") }
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                camera(Modifier.weight(1.4f).fillMaxSize())
                Column(Modifier.weight(1f)) { pin() }
            }
        } else {
            camera(Modifier.weight(1f).fillMaxWidth())
            pin()
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun Modifier.clip20() = this.then(Modifier.clip(RoundedCornerShape(20.dp)))

@Composable
private fun ResultOverlay(vm: AppViewModel) {
    val r = vm.result ?: return
    val st = LocalStatus.current
    val (bg, fg) = when (r.kind) {
        ResultKind.OK -> st.okBg to st.okFg
        ResultKind.ALREADY, ResultKind.QUEUED -> st.warnBg to st.warnFg
        ResultKind.ERROR -> st.errBg to st.errFg
    }
    Box(Modifier.fillMaxSize().background(bg).clickable { vm.dismissResult() }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(r.title, color = fg, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            if (r.name.isNotBlank()) Text(r.name, color = fg, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (r.detail.isNotBlank()) Text(r.detail, color = fg, fontSize = 16.sp, textAlign = TextAlign.Center)
            Text("Tap to continue", color = fg.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------- search
@Composable
fun SearchScreen(vm: AppViewModel) {
    Page(title = "Search & edit", subtitle = "Type at least 2 letters", onBack = { vm.back() }, banner = offlineBanner(vm), scroll = false) {
        OutlinedTextField(
            value = vm.searchTerm, onValueChange = { vm.onSearchChange(it) },
            label = { Text("Name, phone or email") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        if (vm.searching) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        MessageText(vm.message, vm.messageIsError)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(vm.hits, key = { it.uuid }) { h ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(h.display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.checkInHit(h) }, enabled = !vm.busy, modifier = Modifier.weight(1f)) { Text("Check in") }
                            OutlinedButton(onClick = { vm.startEdit(h) }, modifier = Modifier.weight(1f)) { Text("Edit") }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- edit
@Composable
fun EditScreen(vm: AppViewModel) {
    val ev = vm.event
    Page(title = "Edit details", subtitle = vm.editName, onBack = { vm.back() }, banner = offlineBanner(vm)) {
        if (ev == null) { Text("No event open."); return@Page }
        val fields = ev.fields.filter { it.editable }
        if (vm.busy && vm.editValues.isEmpty() && vm.editMulti.isEmpty()) CircularProgressIndicator()
        fields.forEach { f -> FieldEditor(vm, f) }
        MessageText(vm.message, vm.messageIsError)
        Button(onClick = { vm.saveEdit() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Save changes", fontWeight = FontWeight.Bold)
        }
        if (vm.editSaved) OutlinedButton(onClick = { vm.back() }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Done") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FieldEditor(vm: AppViewModel, f: Field) {
    val label = f.name + if (f.required) " *" else ""
    when {
        f.isMulti -> {
            Text(label, fontWeight = FontWeight.SemiBold)
            val chosen = vm.editMulti[f.id].orEmpty()
            f.options.forEach { opt ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                    vm.editMulti[f.id] = if (opt in chosen) chosen - opt else chosen + opt
                }) {
                    Checkbox(checked = opt in chosen, onCheckedChange = { vm.editMulti[f.id] = if (it) chosen + opt else chosen - opt })
                    Text(opt)
                }
            }
        }
        f.isChoice -> {
            var open by remember { mutableStateOf(false) }
            Text(label, fontWeight = FontWeight.SemiBold)
            Box {
                OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(vm.editValues[f.id].orEmpty().ifBlank { "Choose..." }, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    f.options.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt) }, onClick = { vm.editValues[f.id] = opt; open = false })
                    }
                }
            }
        }
        else -> OutlinedTextField(
            value = vm.editValues[f.id].orEmpty(), onValueChange = { vm.editValues[f.id] = it; vm.clearMsg() },
            label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
            singleLine = f.type != "textarea",
            keyboardOptions = KeyboardOptions(
                keyboardType = when (f.type) {
                    "email" -> KeyboardType.Email
                    "phone" -> KeyboardType.Phone
                    "number" -> KeyboardType.Number
                    else -> KeyboardType.Text
                }
            )
        )
    }
}

// ---------------------------------------------------------------- sync
@Composable
fun SyncScreen(vm: AppViewModel) {
    Page(title = "Sync", subtitle = if (vm.online) "Online" else "Offline", onBack = { vm.back() }) {
        val pending = vm.queue.count { !it.failed }
        val failed = vm.queue.count { it.failed }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Waiting to send", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text("$pending", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                if (failed > 0) Text("$failed need attention", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
        if (vm.syncNote.isNotBlank()) Text(vm.syncNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { vm.networkBack() }, enabled = !vm.syncing && pending > 0, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (vm.syncing) "Sending..." else "Send now", fontWeight = FontWeight.Bold)
        }
        if (vm.queue.isEmpty()) Text("Everything has been sent.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        vm.queue.forEach { op ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, if (op.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(op.label, fontWeight = FontWeight.SemiBold)
                    Text("${timeText(op.at)} · ${op.agent}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (op.failed) {
                        Text(op.error.ifBlank { "The server refused this." }, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.retryFailed(op) }) { Text("Try again") }
                            TextButton(onClick = { vm.discard(op) }) { Text("Discard") }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- activity
@Composable
fun ActivityScreen(vm: AppViewModel) {
    Page(
        title = "Activity", subtitle = "Everything done on this device", onBack = { vm.back() },
        actions = { TextButton(onClick = { vm.clearActivity() }) { Text("Clear") } }
    ) {
        val st = LocalStatus.current
        if (vm.activity.isEmpty()) Text("Nothing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        vm.activity.asReversed().take(200).forEach { a ->
            val (bg, fg) = when (a.result) {
                "ok", "synced" -> st.okBg to st.okFg
                "error" -> st.errBg to st.errFg
                else -> st.warnBg to st.warnFg
            }
            Column(Modifier.fillMaxWidth()) {
                SpaceBetween {
                    Text(a.action, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Pill(a.result, bg, fg)
                }
                if (a.who.isNotBlank()) Text(a.who)
                Text("${timeText(a.at)} · ${a.agent}" + if (a.detail.isNotBlank()) " · ${a.detail}" else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(top = 8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- settings
@Composable
fun SettingsScreen(vm: AppViewModel) {
    Page(title = "Settings", onBack = { vm.back() }) {
        Text("Appearance", fontWeight = FontWeight.Bold)
        listOf("dark" to "Dark", "light" to "Light", "system" to "Follow the phone").forEach { (k, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { vm.chooseTheme(k) }) {
                RadioButton(selected = vm.theme == k, onClick = { vm.chooseTheme(k) })
                Text(label)
            }
        }
        HorizontalDivider()
        Text("Server", fontWeight = FontWeight.Bold)
        Text(if (com.eazytagg.agent.Brand.isPd) (if (vm.serverUrl.isBlank()) "Not set" else "Connected") else vm.serverUrl.ifBlank { "Not set" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Signed in as", fontWeight = FontWeight.Bold)
        Text("${vm.agentName} (${vm.email})", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        OutlinedButton(onClick = { vm.changeEvent() }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Change event") }
        Button(
            onClick = { vm.signOut() }, modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
        ) { Text("Sign out / change server") }
        Text(if (com.eazytagg.agent.Brand.isPd) "Popular Dynamics door staff 0.1.0" else "EazyTagg Agent 0.1.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
