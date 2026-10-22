package org.librecans.app.ui.about

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.librecans.app.BuildConfig
import org.librecans.app.R
import org.librecans.app.ui.components.CardSurface
import org.librecans.app.ui.components.InfoItem
import org.librecans.app.ui.components.NavigationItem
import org.librecans.app.ui.components.segmentedGroup

const val SOURCE_URL = "https://github.com/DeathgamerB/LibreCans"
private const val AUTHOR = "DeathgamerB"

/** Who made LibreCans, where its code lives and its license, followed by credits and the disclaimer. */
fun LazyListScope.appInfoSection(title: String, onOpenSource: () -> Unit, showVersion: Boolean = true) {
    segmentedGroup("app_info", title) {
        if (showVersion) {
            row("version") { segment, modifier ->
                InfoItem(stringResource(R.string.about_app_version), BuildConfig.VERSION_NAME, segment, modifier, R.drawable.ic_info)
            }
        }
        row("author") { segment, modifier ->
            InfoItem(stringResource(R.string.about_made_by), AUTHOR, segment, modifier, R.drawable.ic_favorite)
        }
        row("license") { segment, modifier ->
            InfoItem(stringResource(R.string.about_license), stringResource(R.string.about_license_body), segment, modifier, R.drawable.ic_gavel)
        }
        row("source") { segment, modifier ->
            NavigationItem(
                title = stringResource(R.string.about_source),
                summary = SOURCE_URL.removePrefix("https://"),
                icon = R.drawable.ic_code,
                onClick = onOpenSource,
                trailing = { Icon(painterResource(R.drawable.ic_open_in_new), null) },
                segment = segment,
                modifier = modifier.testTag("open_source"),
            )
        }
    }
    item(key = "credits") { Notice(R.string.about_credits, R.string.about_credits_body, Modifier.padding(top = 16.dp).animateItem()) }
    item(key = "disclaimer") { Notice(R.string.about_disclaimer_title, R.string.about_disclaimer, Modifier.padding(top = 8.dp).animateItem()) }
}

@Composable
private fun Notice(title: Int, body: Int, modifier: Modifier = Modifier) {
    CardSurface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmallEmphasized)
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
