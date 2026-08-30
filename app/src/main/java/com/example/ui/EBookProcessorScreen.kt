package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.ComplianceAuditView
import com.example.ui.components.DocumentInputView
import com.example.ui.components.RawHtmlView
import com.example.ui.components.RenderedEBookView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EBookProcessorScreen(viewModel: EBookProcessorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successToast) {
        uiState.successToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "eBook Processor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openApiKeyDialog() },
                        modifier = Modifier.testTag("open_api_key_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Configure API Key",
                            tint = if (uiState.apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Input & OCR") },
                    label = { Text("OCR & Input") },
                    modifier = Modifier.testTag("tab_input_ocr")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "HTML 3.2") },
                    label = { Text("HTML 3.2") },
                    modifier = Modifier.testTag("tab_raw_html")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.AutoStories, contentDescription = "Rendered View") },
                    label = { Text("Reader") },
                    modifier = Modifier.testTag("tab_rendered_view")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(Icons.Default.Rule, contentDescription = "Rules Audit") },
                    label = { Text("Rules Audit") },
                    modifier = Modifier.testTag("tab_rules_audit")
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> DocumentInputView(
                    uiState = uiState,
                    onLoadPdf = { viewModel.loadPdfFromUri(it) },
                    onLoadImage = { viewModel.loadImageFromUri(it) },
                    onLoadSample = { viewModel.loadSampleDocument(it) },
                    onSelectPage = { viewModel.setActivePageIndex(it) },
                    onProcessPage = { viewModel.processCurrentPage() },
                    onUpdateRawText = { viewModel.updateRawTextInput(it) },
                    onSanitizeRawText = { viewModel.sanitizeRawTextDirectly() }
                )
                1 -> RawHtmlView(
                    uiState = uiState,
                    onAutoFix = { viewModel.autoFixHtml32() }
                )
                2 -> RenderedEBookView(uiState = uiState)
                3 -> ComplianceAuditView(uiState = uiState)
            }
        }

        if (uiState.isApiKeyDialogOpen) {
            ApiKeyDialog(
                initialApiKey = uiState.apiKey,
                onDismiss = { viewModel.closeApiKeyDialog() },
                onSave = { viewModel.saveApiKey(it) }
            )
        }
    }
}
