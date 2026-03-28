package com.example.encryptaction.ui.screen.sign

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignScreen(
    navController: NavController,
    onProfileClick: () -> Unit,
    viewModel: SignViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Using OpenDocument for better compatibility with Google Drive and local storage
    val filePickerForSign = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onSignFilePicked(it) }
    }
    val filePickerForVerify = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onVerifyFilePicked(it) }
    }
    val sigFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onVerifySigFilePicked(it) }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) viewModel.saveToUri(uri) else viewModel.onSavePickerDismissed()
    }

    val pendingSave = state.pendingSave
    LaunchedEffect(pendingSave) {
        if (pendingSave != null) saveDocumentLauncher.launch(pendingSave.suggestedName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign & Verify") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Sign") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Verify") })
                }

                when (selectedTab) {
                    0 -> SignTab(
                        fileName = state.signFileName,
                        onPickFile = { filePickerForSign.launch(arrayOf("*/*")) },
                        onSign = { viewModel.sign() },
                        isLoading = state.isLoading
                    )
                    1 -> VerifyTab(
                        fileName = state.verifyFileName,
                        sigFileName = state.verifySigFileName,
                        onPickFile = { filePickerForVerify.launch(arrayOf("*/*")) },
                        onPickSig = { sigFilePicker.launch(arrayOf("*/*")) },
                        onVerify = { viewModel.verify() },
                        isLoading = state.isLoading
                    )
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    state.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("Result") },
            text = { Text(msg, fontFamily = FontFamily.Monospace) },
            confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("OK") } }
        )
    }
}

@Composable
private fun SignTab(
    fileName: String?,
    onPickFile: () -> Unit,
    onSign: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create a detached signature for any file.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
            Text(fileName ?: "Select File to Sign")
        }
        Button(
            onClick = onSign,
            enabled = fileName != null && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign File")
        }
    }
}

@Composable
private fun VerifyTab(
    fileName: String?,
    sigFileName: String?,
    onPickFile: () -> Unit,
    onPickSig: () -> Unit,
    onVerify: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Verify a file against its .easig signature.", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
            Text(fileName ?: "Select Original File")
        }
        OutlinedButton(onClick = onPickSig, modifier = Modifier.fillMaxWidth()) {
            Text(sigFileName ?: "Select .easig Signature File")
        }
        Button(
            onClick = onVerify,
            enabled = fileName != null && sigFileName != null && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify Signature")
        }
    }
}
