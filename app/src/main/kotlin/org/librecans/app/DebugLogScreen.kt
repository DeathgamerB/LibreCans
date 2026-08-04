package org.librecans.app

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(DebugLogger.getLogs(context)) }

    OverlayLayout(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bluetooth Diagnostics",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        content = { topPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .padding(top = topPadding, bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Share Logs Button (Expressive spring shape)
                val copyInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        val fullLogText = logs.joinToString("\n")
                        val tempFile = File(context.cacheDir, "bluetooth_diagnostics_shared.log")
                        tempFile.writeText(fullLogText)

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )

                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Logs")
                        context.startActivity(shareIntent)
                    },
                    interactionSource = copyInteraction,
                    modifier = Modifier.weight(1f).bounceClick(copyInteraction),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 24.dp, bottomEnd = 12.dp, bottomStart = 16.dp)
                ) {
                    Text("Share Logs", fontWeight = FontWeight.Bold)
                }

                // Clear Logs Button (Expressive shape)
                val clearInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        DebugLogger.clearLogs(context)
                        logs = emptyList()
                        Toast.makeText(context, "Log buffer cleared!", Toast.LENGTH_SHORT).show()
                    },
                    interactionSource = clearInteraction,
                    modifier = Modifier.weight(1f).bounceClick(clearInteraction),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 12.dp, bottomEnd = 16.dp, bottomStart = 12.dp)
                ) {
                    Text("Clear Logs", fontWeight = FontWeight.Bold)
                }
            }

            // Scrollable List of Logs (Most recent first/last, scroll to end, etc.)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No diagnostic logs recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs.reversed()) { logEntry ->
                            Text(
                                text = logEntry,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    })
}
