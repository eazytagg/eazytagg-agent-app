package com.eazytagg.agent.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.scale as drawScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eazytagg.agent.AppViewModel
import com.eazytagg.agent.R
import com.eazytagg.agent.Screen
import com.eazytagg.agent.data.Mode

/** The Popular Dynamics logo. In dark mode a soft blue glow keeps the navy lettering readable. */
@Composable
fun PdLogo(modifier: Modifier = Modifier) {
    val dark = LocalIsDark.current
    val glow = if (dark) Modifier.drawBehind {
        // An ellipse that fades to nothing before it reaches the edges, so no box shows.
        drawScale(scaleX = size.width / size.height, scaleY = 1f, pivot = center) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0x663AB0FF), Color(0x223AB0FF), Color.Transparent),
                    center = center, radius = size.height / 2f
                ),
                radius = size.height / 2f, center = center
            )
        }
    } else Modifier
    Box(modifier.then(glow).padding(vertical = 26.dp), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.pd_logo),
            contentDescription = "Popular Dynamics",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ------------------------------------------------------------------ splash
@Composable
fun PdSplash(vm: AppViewModel) {
    val appear = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        bar.animateTo(1f, tween(1800))
        vm.finishSplash()
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp).widthIn(max = 420.dp)) {
            PdLogo(Modifier.alpha(appear.value).scale(0.85f + 0.15f * appear.value))
            Spacer(Modifier.height(24.dp))
            Text("Door staff app", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.alpha(appear.value))
            Spacer(Modifier.height(40.dp))
            LinearProgressIndicator(
                progress = { bar.value },
                modifier = Modifier.fillMaxWidth(0.6f).height(6.dp),
                color = PdBlue,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(12.dp))
            Text("Getting your desk ready", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------------ sign in
@Composable
fun PdSignIn(vm: AppViewModel) {
    var show by remember { mutableStateOf(false) }
    androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Box(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .padding(top = 64.dp, bottom = 36.dp, start = 40.dp, end = 40.dp),
            contentAlignment = Alignment.Center
        ) { PdLogo(Modifier.widthIn(max = 320.dp)) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Welcome back", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("Sign in to your Popular Dynamics account.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = vm.serverUrl, onValueChange = { vm.serverUrl = it.filter { c -> c.isDigit() || c == '.' }; vm.clearMsg() },
                    label = { Text("Server IP address") },
                    isError = vm.serverUrl.isNotBlank() && !vm.isValidIp(vm.serverUrl),
                    supportingText = { Text(if (vm.serverUrl.isNotBlank() && !vm.isValidIp(vm.serverUrl)) "Use the format 192.168.0.120" else "IP address only. Numbers and dots.") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = vm.email, onValueChange = { vm.email = it; vm.clearMsg() },
                    label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None)
                )
                OutlinedTextField(
                    value = vm.password, onValueChange = { vm.password = it; vm.clearMsg() },
                    label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { show = !show }) {
                            Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (show) "Hide password" else "Show password")
                        }
                    }
                )
                MessageText(vm.message, vm.messageIsError)
                Button(onClick = { vm.signIn() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Sign in", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { vm.testConnection() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Test connection")
                }
            }
        }
    }
    }
}

// ------------------------------------------------------------------ find event
@Composable
fun PdFindEvent(vm: AppViewModel) {
    Page(
        title = "Find your event", subtitle = "Signed in as ${vm.agentName}",
        onBack = if (vm.stack.size > 1) ({ vm.back() }) else null,
        banner = offlineBanner(vm),
        actions = { TextButton(onClick = { vm.signOut() }) { Text("Sign out") } }
    ) {
        Text("Enter the code, or search by event name.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = vm.eventCodeInput, onValueChange = { vm.eventCodeInput = it.uppercase(); vm.clearMsg() },
            label = { Text("Event code") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(Modifier.weight(1f)); Text("or", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp); HorizontalDivider(Modifier.weight(1f))
        }
        OutlinedTextField(
            value = vm.eventQuery, onValueChange = { vm.onEventQueryChange(it) },
            label = { Text("Search event name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = { if (vm.eventSearching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
        )
        if (vm.eventHits.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("${vm.eventHits.size} events found. Tap one to use its code.", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                    vm.eventHits.forEach { h ->
                        val picked = vm.eventCodeInput == h.code
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Row(
                            Modifier.fillMaxWidth().background(if (picked) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { vm.pickEvent(h) }.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(h.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(h.sub, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (picked) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Test mode", fontWeight = FontWeight.SemiBold)
                    Text("Practice at the desk. Nothing is saved to the event or sent to the server.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = vm.testMode, onCheckedChange = { vm.testMode = it })
            }
        }
        MessageText(vm.message, vm.messageIsError)
        Button(onClick = { vm.openEvent() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Continue", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ------------------------------------------------------------------ menu
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PdMenu(vm: AppViewModel) {
    val ev = vm.event
    val pending = vm.queue.count { !it.failed }
    Page(
        title = if (ev == null) "Event" else ev.title.ifBlank { "Event ${ev.code}" },
        subtitle = vm.agentName.ifBlank { null },
        banner = offlineBanner(vm),
        actions = { TextButton(onClick = { vm.go(Screen.Settings) }) { Text("Settings") } }
    ) {
        PdViewToggle(vm.menuView) { vm.chooseMenuView(it) }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Checked in today", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${vm.counts.today}", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    vm.counts.sessions.take(3).forEach { (k, v) ->
                        Text("$k  $v", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { vm.refreshCounts() }) { Text("Refresh") }
                }
            }
        }
        if (vm.recent.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Recent at this desk", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp))
                    vm.recent.forEachIndexed { i, r ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.openRecent(r) }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(r.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(r.action, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(timeText(r.at), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (vm.menuView == "icons") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PdTile("Check in", Icons.Filled.QrCodeScanner, true, Modifier.weight(1f)) { vm.startScan(Mode.CHECKIN) }
                PdTile("Check out", Icons.Filled.Logout, false, Modifier.weight(1f)) { vm.startScan(Mode.CHECKOUT) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PdTile("Walk-in", Icons.Filled.PersonAdd, false, Modifier.weight(1f)) { vm.startWalkIn() }
                PdTile("Search", Icons.Filled.Search, false, Modifier.weight(1f)) { openSearch(vm) }
            }
        } else {
            PdRow("Check in", "Scan a QR code or enter a PIN") { vm.startScan(Mode.CHECKIN) }
            PdRow("Check out", "Record someone leaving") { vm.startScan(Mode.CHECKOUT) }
            PdRow("Register walk-in", "New guest, details and badge") { vm.startWalkIn() }
            PdRow("Search & edit", "Find by name, fix details") { openSearch(vm) }
        }
        MessageText(vm.message, vm.messageIsError)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { vm.go(Screen.Sync) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Text(if (pending > 0) "Sync ($pending)" else "Sync")
            }
            OutlinedButton(onClick = { vm.go(Screen.Activity) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("Activity") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun openSearch(vm: AppViewModel) { vm.searchTerm = ""; vm.hits = emptyList(); vm.clearMsg(); vm.go(Screen.Search) }

@Composable
private fun PdViewToggle(current: String, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("View", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50)).padding(3.dp)
        ) {
            listOf("icons" to ("Icons" to Icons.Filled.GridView), "list" to ("List" to Icons.Filled.ViewList)).forEach { (key, p) ->
                val on = current == key
                Row(
                    Modifier.background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(50))
                        .clickable { onPick(key) }.height(36.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val fg = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Icon(p.second, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                    Text(p.first, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PdTile(title: String, icon: ImageVector, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val ic = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
    Card(
        onClick = onClick, shape = RoundedCornerShape(22.dp), modifier = modifier.aspectRatio(1.2f),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = if (primary) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = ic, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, color = fg, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PdRow(title: String, sub: String, onClick: () -> Unit) {
    Card(
        onClick = onClick, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 13.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------------ search result (tap to open)
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PdHitCard(vm: AppViewModel, h: com.eazytagg.agent.data.SearchHit) {
    Card(
        onClick = { vm.openPerson(h) }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(h.display, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                val extra = h.data.values.map { it.trim() }.filter { it.isNotEmpty() && it != h.display && !it.startsWith("(") }.take(2).joinToString(" · ")
                if (extra.isNotBlank()) Text(extra, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text("Open", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ------------------------------------------------------------------ person
@Composable
fun PdPerson(vm: AppViewModel) {
    val ev = vm.event
    val pending = vm.personUuid in vm.needPrint
    Page(title = vm.personName.ifBlank { "Attendee" }, subtitle = ev?.title?.ifBlank { null }, onBack = { vm.back() }, banner = offlineBanner(vm)) {
        if (pending) {
            val st = LocalStatus.current
            Box(Modifier.fillMaxWidth().background(st.warnBg, RoundedCornerShape(14.dp)).padding(14.dp)) {
                Text("Details changed. Print a new tag so it matches.", color = st.warnFg, fontWeight = FontWeight.SemiBold)
            }
        }
        MessageText(vm.message, vm.messageIsError)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { vm.checkInPerson() }, enabled = !vm.busy, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("Check in", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = { vm.editPerson() }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("Edit") }
        }
        if (pending) {
            Button(onClick = { vm.printPersonTag() }, enabled = !vm.printing, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                if (vm.printing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text("Print new tag", fontWeight = FontWeight.Bold)
            }
        } else {
            OutlinedButton(onClick = { vm.printPersonTag() }, enabled = !vm.printing, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                if (vm.printing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Print tag")
            }
        }
        if (vm.personLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                val shown = (ev?.fields ?: emptyList()).filter { it.editable }
                shown.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(f.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(vm.personValues[f.id].orEmpty().ifBlank { "-" }, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ------------------------------------------------------------------ walk-in
@Composable
fun PdWalkIn(vm: AppViewModel) {
    val ev = vm.event
    Page(title = "Register walk-in", subtitle = ev?.title?.ifBlank { null }, onBack = { vm.back() }, banner = offlineBanner(vm)) {
        if (ev == null) { Text("No event open."); return@Page }
        if (vm.walkDone.isNotBlank()) {
            MessageText(vm.message, vm.messageIsError)
            Text(vm.walkDone, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Button(onClick = { vm.resetWalkIn() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Register another", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = { vm.back() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) { Text("Done") }
        } else {
            ev.fields.filter { it.editable }.forEach { f -> FieldEditor(vm, f, walk = true) }
            MessageText(vm.message, vm.messageIsError)
            Button(onClick = { vm.submitWalkIn() }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text("Register and print tag", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
