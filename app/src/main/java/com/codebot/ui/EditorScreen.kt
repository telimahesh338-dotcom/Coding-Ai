package com.codebot.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codebot.data.CodeFile
import com.codebot.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onNavigateToPreview: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val files by viewModel.files.collectAsState(initial = emptyList())
    val selectedFile by viewModel.selectedFile.collectAsState()
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            viewModel.exportToDirectory(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedFile?.name ?: "Files", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { if (selectedFile == null) onBack() else viewModel.selectFile(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedFile != null) {
                        IconButton(onClick = onNavigateToPreview) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                        }
                    } else {
                        IconButton(onClick = { launcher.launch(null) }) {
                            Icon(Icons.Default.FolderZip, contentDescription = "Export to Phone")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color(0xFF38BDF8),
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        if (selectedFile == null) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(files) { file ->
                    ListItem(
                        headlineContent = { Text(file.name, color = Color.White) },
                        supportingContent = { Text(file.extension, color = Color(0xFF94A3B8)) },
                        leadingContent = { Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF38BDF8)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        trailingContent = {
                            IconButton(onClick = { viewModel.selectFile(file) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8))
                            }
                        }
                    )
                }
            }
        } else {
            TextField(
                value = selectedFile!!.content,
                onValueChange = { viewModel.updateFileContent(it) },
                modifier = Modifier.padding(padding).fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF000000),
                    unfocusedContainerColor = Color(0xFF000000)
                )
            )
        }
    }
}
