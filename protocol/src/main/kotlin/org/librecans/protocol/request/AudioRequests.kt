package org.librecans.protocol.request

import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.t1
import org.librecans.protocol.model.ConnectionPriority

object AudioRequests {
    fun codec(): MdrRequest = t1("codec", T1.COMMON_GET_STATUS, Inquired.COMMON_AUDIO_CODEC)

    fun upscalingCapability(): MdrRequest = t1("upscalingCapability", T1.AUDIO_GET_CAPABILITY, Inquired.AUDIO_UPSCALING)

    fun upscaling(): MdrRequest = t1("upscaling", T1.AUDIO_GET_PARAM, Inquired.AUDIO_UPSCALING)

    fun setUpscaling(enabled: Boolean): MdrRequest =
        t1("setUpscaling", T1.AUDIO_SET_PARAM, Inquired.AUDIO_UPSCALING, if (enabled) 0x01 else 0x00)

    fun connectionPriority(): MdrRequest =
        t1("connectionPriority", T1.AUDIO_GET_PARAM, Inquired.AUDIO_CONNECTION_MODE)

    fun setConnectionPriority(priority: ConnectionPriority): MdrRequest =
        t1("setConnectionPriority", T1.AUDIO_SET_PARAM, Inquired.AUDIO_CONNECTION_MODE, priority.code)
}
