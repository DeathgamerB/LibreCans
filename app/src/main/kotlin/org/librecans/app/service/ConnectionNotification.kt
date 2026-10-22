package org.librecans.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.librecans.app.MainActivity
import org.librecans.app.R
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.ui.common.batterySummary
import org.librecans.app.ui.common.label
import org.librecans.protocol.state.HeadphoneState

object ConnectionNotification {
    const val ID = 101
    private const val CHANNEL = "connection"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun build(context: Context, connection: ConnectionState, headphones: HeadphoneState): Notification {
        val name = connection.target?.name ?: context.getString(R.string.app_name)
        val text = when (connection) {
            is ConnectionState.Connected -> listOfNotNull(
                headphones.noiseControl?.mode?.let { context.getString(it.label) },
                batterySummary(context, headphones.batteries),
            ).joinToString(" · ").ifEmpty { context.getString(R.string.status_connected) }
            is ConnectionState.Connecting -> context.getString(R.string.status_connecting)
            is ConnectionState.Failed -> context.getString(R.string.status_waiting_for_headphones)
            ConnectionState.Idle -> context.getString(R.string.status_disconnected)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle(name)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(activityIntent(context))
        if (connection is ConnectionState.Connected && headphones.noiseControl != null) {
            builder.addAction(R.drawable.ic_noise_control_on, context.getString(R.string.action_switch_noise_mode),
                serviceIntent(context, ConnectionService.ACTION_CYCLE_NOISE_MODE))
        }
        builder.addAction(R.drawable.ic_link_off, context.getString(R.string.action_disconnect),
            serviceIntent(context, ConnectionService.ACTION_DISCONNECT))
        return builder.build()
    }

    private fun activityIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun serviceIntent(context: Context, action: String): PendingIntent = PendingIntent.getService(
        context, action.hashCode(),
        Intent(context, ConnectionService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
