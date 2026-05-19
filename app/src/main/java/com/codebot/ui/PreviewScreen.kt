package com.codebot.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.codebot.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onBack: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val selectedFile by viewModel.selectedFile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color(0xFF38BDF8),
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                }
            },
            update = { webView ->
                selectedFile?.let { file ->
                    val content = when (file.extension.lowercase()) {
                        "html" -> file.content
                        "js", "javascript" -> "<html><body><script>${file.content}</script></body></html>"
                        "css" -> "<html><head><style>${file.content}</style></head><body><h1>CSS Preview</h1></body></html>"
                        else -> "<html><body><pre>${file.content}</pre></body></html>"
                    }
                    webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
