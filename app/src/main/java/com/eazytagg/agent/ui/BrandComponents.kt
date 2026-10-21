package com.eazytagg.agent.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eazytagg.agent.R

@Composable
internal fun CompanyLogo(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.pdynamics_logo), "Popular Dynamics", modifier
        .width(158.dp).height(56.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(8.dp))
}

/** Uses the supplied split-panel template; portrait stacks the brand above the form. */
@Composable
internal fun AccountLayout(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            val viewportHeight = maxHeight
            val wide = maxWidth >= 840.dp && maxWidth > viewportHeight
            val brand: @Composable (Modifier) -> Unit = { m ->
                Column(m.background(Color(0xFF17155E)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CompanyLogo()
                    if (wide) {
                        Spacer(Modifier.height(24.dp))
                        Text("Connecting people.\nPowering possibilities.", color = Color.White, textAlign = TextAlign.Center, fontSize = 22.sp)
                    }
                }
            }
            val form: @Composable (Modifier) -> Unit = { m ->
                Column(m.padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(12.dp))
                    Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    content()
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("Powered by", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CompanyLogo(Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (wide) Row(Modifier.fillMaxSize()) {
                brand(Modifier.weight(0.36f).fillMaxHeight())
                form(Modifier.weight(0.64f).fillMaxHeight())
            } else Column(Modifier.fillMaxSize()) {
                brand(Modifier.fillMaxWidth().height(if (viewportHeight < 500.dp) 84.dp else 130.dp))
                form(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun ActionTile(label: String, icon: Int, color: Color, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).shadow(if (enabled) 5.dp else 0.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(color.copy(alpha = if (enabled) 0.85f else 0.35f), color.copy(alpha = if (enabled) 1f else 0.35f))))
            .clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), label, Modifier.size(32.dp), tint = Color.White)
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}
