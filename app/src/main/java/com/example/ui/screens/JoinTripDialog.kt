package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R

@Composable
fun JoinTripDialog(
    onDismissRequest: () -> Unit,
    onJoin: (String, (Boolean, String?) -> Unit) -> Unit,
    isSyncing: Boolean
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = { if (!isSubmitting && !isSyncing) onDismissRequest() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .testTag("join_trip_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Title & Subtitle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Join Trip with Code",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter the 6-character code shared by your tour leader to view live expenses and fund balances.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Code Input Field
                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isLetterOrDigit() }.take(6).uppercase()
                        code = filtered
                        errorMessage = null
                    },
                    label = { Text("6-Character Trip Code") },
                    placeholder = { Text("e.g. BNDR26") },
                    singleLine = true,
                    enabled = !isSubmitting && !isSyncing,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (code.length == 6 && !isSubmitting && !isSyncing) {
                                isSubmitting = true
                                errorMessage = null
                                onJoin(code) { success, error ->
                                    isSubmitting = false
                                    if (!success) {
                                        errorMessage = error ?: "Failed to find trip"
                                    }
                                }
                            }
                        }
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Pin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "${code.length}/6 characters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_code_input")
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        enabled = !isSubmitting && !isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_join_button")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (code.length != 6) {
                                errorMessage = "Please enter all 6 characters"
                                return@Button
                            }
                            isSubmitting = true
                            errorMessage = null
                            onJoin(code) { success, error ->
                                isSubmitting = false
                                if (!success) {
                                    errorMessage = error ?: "Trip not found"
                                }
                            }
                        },
                        enabled = code.length == 6 && !isSubmitting && !isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_join_button")
                    ) {
                        if (isSubmitting || isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Joining...")
                        } else {
                            Text("Join Trip")
                        }
                    }
                }
            }
        }
    }
}
