package com.localhost.feature.projects

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.localhost.core.designsystem.theme.DarkBorder
import com.localhost.core.designsystem.theme.DarkSurfaceElevated
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary

@Composable
fun InAppBrowserDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onVisitRecorded: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = PrimaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = currentUrl,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("URL", currentUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = "Open in browser",
                            tint = PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = PrimaryAccent,
                        trackColor = DarkSurface
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.allowFileAccess = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                        url?.let { currentUrl = it }
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        url?.let {
                                            currentUrl = it
                                            onVisitRecorded?.invoke(it)
                                        }
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }
                                }

                                loadUrl(initialUrl)
                                webViewInstance = this
                            }
                        }
                    )
                }
            }
        }
    }
}
