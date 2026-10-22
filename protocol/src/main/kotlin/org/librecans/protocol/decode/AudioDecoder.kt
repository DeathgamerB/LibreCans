package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.model.ConnectionPriority
import org.librecans.protocol.model.UpscalingType
import org.librecans.protocol.wire.PayloadReader

internal object AudioDecoder {
    private const val UPSCALING_AUTO = 0x01

    fun decode(command: Int, r: PayloadReader): DeviceEvent? {
        val type = r.u8()
        return when (command) {
            T1.AUDIO_RET_CAPABILITY ->
                if (type == Inquired.AUDIO_UPSCALING) DeviceEvent.UpscalingCapabilityChanged(UpscalingType.fromCode(r.u8()))
                else null
            T1.AUDIO_RET_PARAM, T1.AUDIO_NTFY_PARAM -> when (type) {
                Inquired.AUDIO_UPSCALING -> DeviceEvent.UpscalingChanged(r.u8() == UPSCALING_AUTO)
                Inquired.AUDIO_CONNECTION_MODE ->
                    ConnectionPriority.fromCode(r.u8())?.let(DeviceEvent::ConnectionPriorityChanged)
                else -> null
            }
            else -> null
        }
    }
}
