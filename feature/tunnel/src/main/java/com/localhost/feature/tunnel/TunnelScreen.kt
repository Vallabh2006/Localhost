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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.localhost.core.designsystem.theme.StatusOrange
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
    viewModel: TunnelViewModel,
    onBackClick: () -> Unit = {}
) {
    BackHandler { onBackClick() }
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

    var targetPortText by remember { mutableStateOf("8000") }
    var httpsPortText by remember { mutableStateOf("8443") }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("HTTPS & Tunnels", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.5.dp, if (status == TunnelStatus.CONNECTED) StatusGreen.copy(alpha = 0.6f) else PrimaryAccent.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Public Internet Port Forwarding", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Worldwide public HTTPS URL (Zero setup)", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        val (tLabel, tColor) = when (status) {
                            TunnelStatus.DISCONNECTED -> Pair("OFFLINE", TextMuted)
                            TunnelStatus.CONNECTING -> Pair("CONNECTING", StatusOrange)
                            TunnelStatus.CONNECTED -> Pair("ONLINE", StatusGreen)
                            TunnelStatus.ERROR -> Pair("ERROR", StatusRed)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, tColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = tLabel,
                                color = tColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        "Forwards your local port to the public internet with a random HTTPS URL (like https://xxxx.trycloudflare.com). Anyone in the world can access it without being on the same Wi-Fi.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    if (status == TunnelStatus.CONNECTED && activeUrl.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Random Public URL (Worldwide)", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = activeUrl,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = StatusGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Public Tunnel URL", activeUrl))
                                            Toast.makeText(context, "Copied public URL", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = StatusGreen, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeUrl)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, activeUrl)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Public URL"))
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.stopTunnel() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Public Port Forwarding", color = StatusRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else if (status == TunnelStatus.CONNECTING) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = PrimaryAccent,
                                    strokeWidth = 2.dp
                                )
                                Column {
                                    Text("Generating random public URL...", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                    Text("Connecting your port to Cloudflare edge...", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.stopTunnel() },
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                    } else {
                        errorMessage?.let { err ->
                            Text(err, color = StatusRed, fontSize = 11.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = targetPortText,
                                onValueChange = { targetPortText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Local Port to Forward") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
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
                                viewModel.startQuickTunnel(tPort)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Forward Port to Internet (Free)", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
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
