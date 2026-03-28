package com.example.encryptaction.ui.screen.encrypt

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.encryptaction.core.util.resolveDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptScreen(
    navController: NavController,
    onProfileClick: () -> Unit,
    viewModel: EncryptViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val users by viewModel.users.collectAsState()
    val context = LocalContext.current

    var recipientDropdownExpanded by remember { mutableStateOf(false) }

    // Using OpenDocument for better compatibility with Google Drive and local storage
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val name = uri.resolveDisplayName(context) ?: "unknown"
        viewModel.onFilePicked(uri, name)
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
                title = { Text("Encrypt File") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(state.selectedFileName ?: "Select File")
                }

                ExposedDropdownMenuBox(
                    expanded = recipientDropdownExpanded,
                    onExpandedChange = { recipientDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = state.selectedRecipient?.displayName ?: "Select Recipient",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recipient") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recipientDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = recipientDropdownExpanded,
                        onDismissRequest = { recipientDropdownExpanded = false }
                    ) {
                        users.forEach { user ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(user.displayName)
                                        Text(
                                            user.username,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.onRecipientSelected(user)
                                    recipientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.encrypt() },
                    enabled = state.selectedFileUri != null && state.selectedRecipient != null && !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Encrypt & Sign")
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
            text = { Text(msg, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("OK") } }
        )
    }
}
