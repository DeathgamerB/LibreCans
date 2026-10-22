package org.librecans.app.ui.devices

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.librecans.app.R
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.ui.common.message
import org.librecans.app.ui.components.CardSurface
import org.librecans.app.ui.components.ShapedIcon

/** A centered card explaining what is missing, with an optional call to action. */
@Composable
fun StatusCard(
    @DrawableRes icon: Int,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    action: Pair<String, () -> Unit>? = null,
) {
    CardSurface(modifier = modifier.padding(top = 8.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShapedIcon(icon, size = 72.dp, shape = MaterialShapes.SoftBurst)
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized, textAlign = TextAlign.Center)
            body?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            action?.let { (label, onClick) ->
                Button(onClick = onClick, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(top = 4.dp)) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
fun FailureCard(failed: ConnectionState.Failed, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CardSurface(
        modifier = modifier.padding(bottom = 8.dp).testTag("failure_card"),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_error), null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(failed.reason.message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
        }
        if (failed.target != null) {
            Button(
                onClick = onRetry,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.align(Alignment.End).padding(top = 12.dp),
            ) { Text(stringResource(R.string.action_retry)) }
        }
    }
}
