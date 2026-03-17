package com.example.encryptaction.ui.screen.keys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.encryptaction.domain.model.UserKeyBundle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysScreen(
    navController: NavController,
    onProfileClick: () -> Unit,
    viewModel: KeysViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val bundles by viewModel.keyBundles.collectAsState()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Key Directory") },
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
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { viewModel.exportMyKeys() }, modifier = Modifier.weight(1f)) {
                        Text("Export My Keys")
                    }
                    OutlinedButton(onClick = { viewModel.showImportDialog() }, modifier = Modifier.weight(1f)) {
                        Text("Import Keys")
                    }
                }

                if (bundles.isEmpty()) {
                    Text(
                        "No keys in directory yet.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn {
                        items(bundles, key = { it.userId }) { bundle ->
                            KeyBundleRow(bundle = bundle)
                        }
                    }
                }
            }

            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    // Export dialog — shows PEM text with copy button
    state.exportedPem?.let { pem ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissExport() },
            title = { Text("Your Public Keys") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Share this with teammates so they can encrypt files for you.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        pem,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(pem))
                    viewModel.dismissExport()
                }) { Text("Copy & Close") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExport() }) { Text("Close") }
            }
        )
    }

    // Import dialog — paste PEM + enter user ID
    if (state.showImportDialog) {
        ImportDialog(
            onImport = { userId, pem -> viewModel.importKeys(userId, pem) },
            onDismiss = { viewModel.dismissImportDialog() }
        )
    }

    // Result message
    state.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("Result") },
            text = { Text(msg, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("OK") } }
        )
    }
}

@Composable
private fun KeyBundleRow(bundle: UserKeyBundle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(bundle.userId, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Fingerprint: ${bundle.fingerprint}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImportDialog(onImport: (userId: String, pem: String) -> Unit, onDismiss: () -> Unit) {
    var userId by remember { mutableStateOf("") }
    var pemText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Public Keys") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = pemText,
                    onValueChange = { pemText = it },
                    label = { Text("Paste PEM key bundle here") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (userId.isNotBlank() && pemText.isNotBlank()) onImport(userId, pemText) },
                enabled = userId.isNotBlank() && pemText.isNotBlank()
            ) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
