package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.model.PlaybackStatus
import org.librecans.protocol.wire.PayloadReader

internal object PlayDecoder {
    fun decode(command: Int, r: PayloadReader): DeviceEvent? {
        val type = r.u8()
        return when (command) {
            T1.PLAY_RET_PARAM, T1.PLAY_NTFY_PARAM ->
                if (type == Inquired.PLAY_MUSIC_VOLUME) DeviceEvent.VolumeChanged(r.u8()) else null
            T1.PLAY_RET_STATUS, T1.PLAY_NTFY_STATUS ->
                if (type == Inquired.PLAY_CONTROL) {
                    r.u8()
                    DeviceEvent.PlaybackStatusChanged(PlaybackStatus.fromCode(r.u8()))
                } else null
            else -> null
        }
    }
}
