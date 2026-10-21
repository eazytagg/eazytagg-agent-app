package com.eazytagg.agent.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eazytagg.agent.R
import com.eazytagg.agent.AppViewModel
import com.eazytagg.agent.Screen
import com.eazytagg.agent.data.Field
import com.eazytagg.agent.data.Mode
import com.eazytagg.agent.data.ResultKind

@Composable
fun Root(vm: AppViewModel) {
    when (vm.screen) {
        Screen.SignIn -> SignInScreen(vm)
        Screen.EventCode -> EventCodeScreen(vm)
        Screen.Dashboard -> DashboardScreen(vm)
        Screen.Sessions -> SessionsScreen(vm)
        Screen.Scan -> ScanScreen(vm)
        Screen.Search -> SearchScreen(vm)
        Screen.Guest -> GuestScreen(vm)
        Screen.Edit -> EditScreen(vm)
        Screen.Sync -> SyncScreen(vm)
        Screen.Activity -> ActivityScreen(vm)
        Screen.Settings -> SettingsScreen(vm)
    }
}

private fun pendingCount(vm: AppViewModel) = vm.queue.count { !it.failed }
private fun offlineBanner(vm: AppViewModel): String? {
    val n = pendingCount(vm)
    return when {
        !vm.online && n > 0 -> "Offline - $n waiting to sync"
        !vm.online -> "Offline - actions are saved and sent later"
        else -> null
    }
}

// ---------------------------------------------------------------- sign in
@Composable
fun SignInScreen(vm: AppViewModel) {
    AccountLayout(title = "PDynamics Agent", subtitle = "Sign in with your system account.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.chooseTheme("light") }) { Text("Light") }
            OutlinedButton(onClick = { vm.chooseTheme("dark") }) { Text("Dark") }
            TextButton(onClick = { vm.chooseTheme("system") }) { Text("Device") }
        }
        TextField(
            value = vm.serverUrl, onValueChange = { vm.serverUrl = it; vm.clearMsg() },
            label = { Text("Server address") },
            placeholder = { Text("https://portal.populardynamics.org") },
            supportingText = { Text("Enter a server IP or a full website address.") },
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        TextField(
            value = vm.email, onValueChange = { vm.email = it; vm.clearMsg() },
            label = { Text("Email") }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        TextField(
            value = vm.password, onValueChange = { vm.password = it; vm.clearMsg() },
            label = { Text("Password") }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
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
    AccountLayout(title = "Open your event", subtitle = "Signed in as ${vm.agentName}") {
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
        TextButton(onClick = { vm.signOut() }) { Text("Sign out") }
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
                    Text("Attendance · server figures", fontWeight = FontWeight.Bold)
                    Text(if (vm.online) "Server reachable" else "Offline · saved figures", fontSize = 12.sp, color = if (vm.online) Color(0xFF238E43) else MaterialTheme.colorScheme.error)
                    val needsReview = vm.queue.count { it.failed }
                    if (needsReview > 0) Text("$needsReview saved actions need attention", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Total attendees", fontSize = 12.sp); Text(vm.counts.total?.toString() ?: "—", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold) }
                        Column { Text("Event days", fontSize = 12.sp); Text(vm.counts.eventDays?.toString() ?: "—", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold) }
                    }
                    if (vm.counts.currentDay > 0) Text("Day ${vm.counts.currentDay} of ${vm.counts.eventDays ?: "—"}", color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("Checked in today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text("${vm.counts.today}", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    vm.counts.days.forEach { (k, v) -> SpaceBetween { Text(k); Text("$v", fontWeight = FontWeight.Bold) } }
                    if (vm.counts.sessions.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        vm.counts.sessions.forEach { (k, v) -> SpaceBetween { Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("$v", fontWeight = FontWeight.Bold) } }
                    }
                    if (pending > 0) Text("$pending actions saved on this device · waiting to sync", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text(if (vm.counts.updatedAt > 0) "Last refreshed ${timeText(vm.counts.updatedAt)}" else "Counts not loaded yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { vm.refreshCounts() }) { Text("Refresh counts") }
                }
            }
        }
        val actions: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ActionTile("Check in", R.drawable.ic_checkin, Color(0xFF238E43), Modifier.weight(1f)) { vm.startScan(Mode.CHECKIN) }
                    ActionTile("Check out", R.drawable.ic_checkout, Color(0xFF587681), Modifier.weight(1f)) { vm.startScan(Mode.CHECKOUT) }
                    ActionTile("Sessions", R.drawable.ic_sessions, Color(0xFF8E24AA), Modifier.weight(1f), vm.sessions.isNotEmpty()) { vm.go(Screen.Sessions) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ActionTile("Search & edit", R.drawable.ic_search, Color(0xFF1976D2), Modifier.weight(1f)) { vm.searchTerm = ""; vm.hits = emptyList(); vm.clearMsg(); vm.go(Screen.Search) }
                    ActionTile(if (pending > 0) "Sync ($pending)" else "Sync", R.drawable.ic_sync, Color(0xFF00897B), Modifier.weight(1f)) { vm.go(Screen.Sync) }
                }
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
                CameraScanner(Modifier.fillMaxSize()) { vm.onScanned(it) }
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
    val bg = when (r.kind) {
        ResultKind.OK -> Color(0xFF137A3F)
        ResultKind.ALREADY, ResultKind.QUEUED -> Color(0xFFA65C00)
        ResultKind.ERROR -> Color(0xFFB50D31)
    }
    val symbol = when (r.kind) { ResultKind.OK -> R.drawable.ic_check; ResultKind.ERROR -> R.drawable.ic_close; else -> R.drawable.ic_warning }
    Column(Modifier.fillMaxSize().background(bg).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(painterResource(symbol), r.kind.name, Modifier.size(72.dp).background(Color.White, RoundedCornerShape(50)).padding(16.dp), tint = bg)
        Spacer(Modifier.height(20.dp))
        Text(r.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        if (r.name.isNotBlank()) Text(r.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (r.detail.isNotBlank()) Text(r.detail, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        OutlinedButton(onClick = { vm.dismissResult() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color.White)) { Text("Scan next") }
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
        if (!vm.searching && vm.hits.isNotEmpty()) Text("${vm.hits.size} guests found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (vm.searching) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        MessageText(vm.message, vm.messageIsError)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(vm.hits, key = { it.uuid }) { h ->
                Card(onClick = { vm.openGuest(h) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(h.display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("View guest  ›", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GuestScreen(vm: AppViewModel) {
    val guest = vm.selectedGuest ?: return
    Page(title = "PDynamics Agent", subtitle = vm.event?.title, onBack = { vm.back() }, banner = offlineBanner(vm)) {
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(guest.display, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("Choose an action", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ActionTile("Check in", R.drawable.ic_checkin, Color(0xFF238E43), Modifier.weight(1f), !vm.busy) { vm.guestAction(Mode.CHECKIN) }
            ActionTile("Check out", R.drawable.ic_checkout, Color(0xFF587681), Modifier.weight(1f), !vm.busy) { vm.guestAction(Mode.CHECKOUT) }
            ActionTile("Print tag", R.drawable.ic_print, Color(0xFFD81C3C), Modifier.weight(1f), !vm.busy) { vm.printTag(guest.uuid) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ActionTile("Sessions", R.drawable.ic_sessions, Color(0xFF8E24AA), Modifier.weight(1f), vm.sessions.isNotEmpty() && !vm.busy) { vm.guestAction(Mode.SESSION) }
            ActionTile("Edit details", R.drawable.ic_edit, Color(0xFF1976D2), Modifier.weight(1f), !vm.busy) { vm.startEdit(guest) }

        }
        MessageText(vm.printMessage, !vm.online)
        MessageText(vm.message, vm.messageIsError)
        Box(Modifier.height(280.dp).fillMaxWidth()) { ResultOverlay(vm) }
    }
}

// ---------------------------------------------------------------- edit
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(vm: AppViewModel) {
    val ev = vm.event
    if (vm.editSaved) ModalBottomSheet(onDismissRequest = { vm.editSaved = false }) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(painterResource(R.drawable.ic_check), "Saved", Modifier.size(56.dp), tint = Color(0xFF238E43))
            Text(if (vm.online) "Details saved" else "Saved on this device", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(if (vm.online) "The tag has not been printed with the new details yet." else "Sync the saved details before printing the updated tag.", textAlign = TextAlign.Center)
            MessageText(vm.printMessage, !vm.online)
            Button(onClick = { vm.printTag(vm.editUuid) }, enabled = vm.online && !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD81C3C))) { Text("Print tag now") }
            TextButton(onClick = { vm.editSaved = false; vm.back() }) { Text("Not now") }
        }
    }
    Page(title = "Edit details", subtitle = vm.editName, onBack = { vm.back() }, banner = offlineBanner(vm)) {
        if (ev == null) { Text("No event open."); return@Page }
        val fields = ev.fields.filter { it.editable }
        if (vm.busy && vm.editValues.isEmpty() && vm.editMulti.isEmpty()) CircularProgressIndicator()
        fields.forEach { f -> FieldEditor(vm, f) }
        MessageText(vm.message, vm.messageIsError)
        Button(onClick = { vm.saveEdit() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Save changes", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = { vm.back() }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Done") }
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
        listOf("dark" to "Dark", "light" to "Light", "system" to "Follow device").forEach { (k, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { vm.chooseTheme(k) }) {
                RadioButton(selected = vm.theme == k, onClick = { vm.chooseTheme(k) })
                Text(label)
            }
        }
        HorizontalDivider()
        Text("Server", fontWeight = FontWeight.Bold)
        Text(vm.serverUrl.ifBlank { "Not set" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Signed in as", fontWeight = FontWeight.Bold)
        Text("${vm.agentName} (${vm.email})", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        OutlinedButton(onClick = { vm.changeEvent() }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Change event") }
        Button(
            onClick = { vm.signOut() }, modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
        ) { Text("Sign out / change server") }
        Text("PDynamics Agent 0.1.1", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
