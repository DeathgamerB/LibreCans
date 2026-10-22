package org.librecans.app.ui.about

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.librecans.app.R
import org.librecans.app.data.diagnostics.LogEntry
import org.librecans.app.ui.common.appViewModel
import org.librecans.app.ui.components.AppScaffold

@Composable
fun DiagnosticsEntry(onBack: () -> Unit) {
    val viewModel = appViewModel { DiagnosticsViewModel(it.diagnostics) }
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DiagnosticsScreen(
        entries = entries,
        recording = viewModel.enabled,
        onBack = onBack,
        onShare = { shareLog(context, viewModel.export()) },
        onClear = viewModel::clear,
    )
}

@Composable
fun DiagnosticsScreen(entries: List<LogEntry>, recording: Boolean, onBack: () -> Unit, onShare: () -> Unit, onClear: () -> Unit) {
    val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    AppScaffold(
        title = stringResource(R.string.diagnostics_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = onShare, enabled = entries.isNotEmpty()) {
                Icon(painterResource(R.drawable.ic_share), stringResource(R.string.diagnostics_share))
            }
            IconButton(onClick = onClear, enabled = entries.isNotEmpty()) {
                Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.diagnostics_clear))
            }
        },
    ) {
        if (entries.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(if (recording) R.string.diagnostics_empty else R.string.diagnostics_disabled),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp).testTag("diagnostics_empty"),
                )
            }
        }
        val newestFirst = entries.asReversed()
        items(newestFirst.size) { index ->
            val entry = newestFirst[index]
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${format.format(Date(entry.timeMillis))}  ${entry.kind.symbol}  ${entry.text}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = entry.kind.color(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private val LogEntry.Kind.symbol: String
    get() = when (this) {
        LogEntry.Kind.SENT -> "→"
        LogEntry.Kind.RECEIVED -> "←"
        LogEntry.Kind.ACK -> "✓"
        LogEntry.Kind.EVENT -> "•"
        LogEntry.Kind.ERROR -> "!"
    }

@Composable
private fun LogEntry.Kind.color(): Color = when (this) {
    LogEntry.Kind.SENT -> MaterialTheme.colorScheme.primary
    LogEntry.Kind.RECEIVED -> MaterialTheme.colorScheme.tertiary
    LogEntry.Kind.ACK -> MaterialTheme.colorScheme.onSurfaceVariant
    LogEntry.Kind.EVENT -> MaterialTheme.colorScheme.onSurface
    LogEntry.Kind.ERROR -> MaterialTheme.colorScheme.error
}

/** Writes the log to the private cache and hands it to the share sheet through a content URI. */
private fun shareLog(context: Context, text: String) {
    val dir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
    val file = File(dir, "librecans-protocol.log").apply { writeText(text) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.diagnostics_share_subject))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.diagnostics_share)))
}
