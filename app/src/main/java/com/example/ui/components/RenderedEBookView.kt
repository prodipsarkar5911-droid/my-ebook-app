package com.example.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.EBookUiState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RenderedEBookView(uiState: EBookUiState) {
    val htmlContent = uiState.sanitizedHtml32

    var selectedStyleMode by remember { mutableStateOf(1) } // 0: Pure Unstyled, 1: Classic eBook CSS, 2: Dark eBook

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (htmlContent.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "No eBook Content to Render",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Process a document to see the rendered HTML 3.2 preview here with dynamic eBook styling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Style Selector Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStyleMode == 1,
                    onClick = { selectedStyleMode = 1 },
                    label = { Text("eBook Sepia/Book") },
                    leadingIcon = { Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("style_ebook_chip")
                )
                FilterChip(
                    selected = selectedStyleMode == 2,
                    onClick = { selectedStyleMode = 2 },
                    label = { Text("Night Reader") },
                    leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("style_dark_chip")
                )
                FilterChip(
                    selected = selectedStyleMode == 0,
                    onClick = { selectedStyleMode = 0 },
                    label = { Text("Pure HTML 3.2") },
                    leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("style_raw_chip")
                )
            }

            // Build Full HTML for WebView with injected external style sheet
            val fullHtmlDocument = buildCompleteHtmlDocument(htmlContent, selectedStyleMode)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = false
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, fullHtmlDocument, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ebook_webview")
                )
            }
        }
    }
}

private fun buildCompleteHtmlDocument(bodyHtml: String, styleMode: Int): String {
    val css = when (styleMode) {
        1 -> """
            body {
                background-color: #FBF8F1;
                color: #2D241E;
                font-family: Georgia, 'Times New Roman', serif;
                padding: 24px 20px;
                line-height: 1.7;
                font-size: 16px;
                margin: 0;
            }
            h1, h2, h3, h4, h5, h6 {
                color: #1A1412;
                font-family: 'Palatino', Georgia, serif;
                margin-top: 24px;
                margin-bottom: 12px;
                font-weight: bold;
            }
            h1 { font-size: 24px; border-bottom: 2px solid #E2D9C8; padding-bottom: 8px; }
            h2 { font-size: 20px; }
            h3 { font-size: 18px; color: #4A3B32; }
            p.indent {
                text-indent: 2em;
                margin-top: 0;
                margin-bottom: 8px;
                text-align: justify;
            }
            .color1 { color: #B45309; font-weight: 600; background: #FEF3C7; padding: 2px 6px; border-radius: 4px; }
            .color2 { color: #047857; font-weight: 600; background: #D1FAE5; padding: 2px 6px; border-radius: 4px; }
            .font1 { font-family: 'Baskerville', 'Garamond', serif; font-style: italic; }
            .font2 { font-family: 'Courier New', monospace; font-size: 0.9em; }
        """.trimIndent()

        2 -> """
            body {
                background-color: #121824;
                color: #E2E8F0;
                font-family: 'Georgia', serif;
                padding: 24px 20px;
                line-height: 1.7;
                font-size: 16px;
                margin: 0;
            }
            h1, h2, h3, h4, h5, h6 {
                color: #F8FAFC;
                margin-top: 24px;
                margin-bottom: 12px;
            }
            h1 { font-size: 24px; border-bottom: 1px solid #334155; padding-bottom: 8px; }
            h2 { font-size: 20px; color: #93C5FD; }
            h3 { font-size: 18px; color: #CBD5E1; }
            p.indent {
                text-indent: 2em;
                margin-top: 0;
                margin-bottom: 8px;
                text-align: justify;
            }
            .color1 { color: #FCD34D; font-weight: 600; }
            .color2 { color: #6EE7B7; font-weight: 600; }
            .font1 { font-style: italic; color: #E0E7FF; }
            .font2 { font-family: monospace; color: #FBCFE8; }
        """.trimIndent()

        else -> """
            body {
                background-color: #FFFFFF;
                color: #000000;
                padding: 16px;
                font-family: serif;
            }
            p.indent {
                text-indent: 2em;
            }
        """.trimIndent()
    }

    return """
        <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Rendered eBook Page</title>
            <style>
                $css
            </style>
        </head>
        <body>
            $bodyHtml
        </body>
        </html>
    """.trimIndent()
}
