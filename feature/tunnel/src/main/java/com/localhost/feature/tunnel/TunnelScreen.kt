package com.localhost.feature.tunnel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkBorder
import com.localhost.core.designsystem.theme.DarkBorderSubtle
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.DarkSurfaceElevated
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.StatusGreen
import com.localhost.core.designsystem.theme.StatusRed
import com.localhost.core.designsystem.theme.StatusYellow
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextOnAccent
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.model.TunnelStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelScreen(
    viewModel: TunnelViewModel
) {
    val status by viewModel.tunnelStatus.collectAsState()
    val activeUrl by viewModel.activeUrl.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val config by viewModel.tunnelConfig.collectAsState()
    val savedTokens by viewModel.savedTokens.collectAsState()
    val nginxConfig by viewModel.nginxProxyConfig.collectAsState()
    val context = LocalContext.current

    var tokenText by remember(config.token) { mutableStateOf(config.token) }
    var tokenNameText by remember { mutableStateOf("") }
    var tokensDropdownExpanded by remember { mutableStateOf(false) }

    var targetPortText by remember { mutableStateOf("8080") }
    var httpsPortText by remember { mutableStateOf("8443") }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("HTTPS & Tunnels", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                modifier = Modifier.height(48.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Local Nginx HTTPS Proxy", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Tunnel local HTTP port to local SSL/TLS", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        val statusColor = if (nginxConfig.isRunning) StatusGreen else TextMuted
                        val statusText = if (nginxConfig.isRunning) "ACTIVE" else "STOPPED"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(statusText, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = statusColor)
                        }
                    }

                    if (nginxConfig.isRunning) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, PrimaryAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Active HTTPS Endpoints:", fontSize = 11.sp, color = TextMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(nginxConfig.localHttpsUrl, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = PrimaryAccent)
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(nginxConfig.localHttpsUrl))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (nginxConfig.lanHttpsUrl.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(nginxConfig.lanHttpsUrl, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextSecondary)
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("LAN URL", nginxConfig.lanHttpsUrl))
                                            Toast.makeText(context, "Copied LAN HTTPS URL", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.stopNginx() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Nginx HTTPS Proxy", color = StatusRed, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = targetPortText,
                                onValueChange = { targetPortText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Target Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PrimaryAccent,
                                    unfocusedBorderColor = DarkBorderSubtle,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated
                                )
                            )
                            OutlinedTextField(
                                value = httpsPortText,
                                onValueChange = { httpsPortText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("HTTPS Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PrimaryAccent,
                                    unfocusedBorderColor = DarkBorderSubtle,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val tPort = targetPortText.toIntOrNull() ?: 8080
                                val hPort = httpsPortText.toIntOrNull() ?: 8443
                                viewModel.startNginx(tPort, hPort)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Nginx HTTPS Proxy", color = TextOnAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Cloudflare Tunnel", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Public HTTPS via Named Tunnel Token", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        val (dotColor, statusLabel) = when (status) {
                            TunnelStatus.CONNECTED -> Pair(StatusGreen, "LIVE")
                            TunnelStatus.CONNECTING -> Pair(StatusYellow, "CONNECTING")
                            TunnelStatus.ERROR -> Pair(StatusRed, "ERROR")
                            TunnelStatus.DISCONNECTED -> Pair(TextMuted, "OFFLINE")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(statusLabel, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = dotColor)
                        }
                    }

                    if (activeUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, PrimaryAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeUrl,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = PrimaryAccent,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", tint = PrimaryAccent)
                            }
                        }
                    }

                    errorMessage?.let {
                        Text(text = it, color = StatusRed, fontSize = 12.sp)
                    }

                    if (status == TunnelStatus.CONNECTED || status == TunnelStatus.CONNECTING) {
                        Button(
                            onClick = { viewModel.stopTunnel() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stop Tunnel", color = StatusRed, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        if (savedTokens.isNotEmpty()) {
                            Box {
                                OutlinedButton(
                                    onClick = { tokensDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, DarkBorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select from Saved Tokens (${savedTokens.size})", color = TextPrimary, fontSize = 12.sp)
                                }
                                DropdownMenu(
                                    expanded = tokensDropdownExpanded,
                                    onDismissRequest = { tokensDropdownExpanded = false },
                                    modifier = Modifier.background(DarkSurfaceElevated)
                                ) {
                                    savedTokens.forEach { saved ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(saved.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                                    Text(saved.token.take(16) + "...", color = TextMuted, fontSize = 11.sp)
                                                }
                                            },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = { viewModel.deleteToken(saved.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                tokenText = saved.token
                                                tokenNameText = saved.name
                                                tokensDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tokenNameText,
                            onValueChange = { tokenNameText = it },
                            label = { Text("Token Nickname / Label (Optional)") },
                            placeholder = { Text("Production Tunnel", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = DarkBorderSubtle,
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated
                            )
                        )

                        OutlinedTextField(
                            value = tokenText,
                            onValueChange = { tokenText = it },
                            label = { Text("Cloudflare Tunnel Token") },
                            placeholder = { Text("eyJhIjoi...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = DarkBorderSubtle,
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (tokenText.isNotBlank()) {
                                        viewModel.saveToken(tokenNameText, tokenText)
                                        Toast.makeText(context, "Saved token to history", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Token", color = TextPrimary)
                            }

                            Button(
                                onClick = {
                                    if (tokenText.isNotBlank()) {
                                        viewModel.saveToken(tokenNameText, tokenText)
                                        viewModel.startNamedTunnel(tokenText)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("Connect Named Tunnel", color = TextOnAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
