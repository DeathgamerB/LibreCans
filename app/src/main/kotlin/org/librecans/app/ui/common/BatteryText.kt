package org.librecans.app.ui.common

import android.content.Context
import org.librecans.app.R
import org.librecans.protocol.model.Batteries

/** "L 80% · R 76% · Case 53%" style summary for notifications. */
fun batterySummary(context: Context, batteries: Batteries): String? {
    val parts = buildList {
        batteries.single?.let { add("${it.percent}%") }
        batteries.left?.let { add("${context.getString(R.string.dashboard_battery_left)} ${it.percent}%") }
        batteries.right?.let { add("${context.getString(R.string.dashboard_battery_right)} ${it.percent}%") }
        batteries.case?.let { add("${context.getString(R.string.dashboard_battery_case)} ${it.percent}%") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
