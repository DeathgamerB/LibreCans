package org.librecans.protocol.request

import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.T2
import org.librecans.protocol.mdr.onOff
import org.librecans.protocol.mdr.t1
import org.librecans.protocol.mdr.t2
import org.librecans.protocol.model.PlaybackCommand

object GeneralSettingRequests {
    private const val BOOLEAN_TYPE = 0x00
    private const val ENGLISH = 0x01

    fun capability(id: Int): MdrRequest = t1("generalSettingCapability", T1.GENERAL_SETTING_GET_CAPABILITY, id, ENGLISH)

    fun get(id: Int): MdrRequest = t1("generalSetting", T1.GENERAL_SETTING_GET_PARAM, id)

    fun set(id: Int, enabled: Boolean): MdrRequest =
        t1("setGeneralSetting", T1.GENERAL_SETTING_SET_PARAM, id, BOOLEAN_TYPE, onOff(enabled))
}

object PlayRequests {
    private const val ENABLE = 0x00

    fun volume(): MdrRequest = t1("volume", T1.PLAY_GET_PARAM, Inquired.PLAY_MUSIC_VOLUME)

    fun setVolume(volume: Int): MdrRequest = t1("setVolume", T1.PLAY_SET_PARAM, Inquired.PLAY_MUSIC_VOLUME, volume)

    fun status(): MdrRequest = t1("playbackStatus", T1.PLAY_GET_STATUS, Inquired.PLAY_CONTROL)

    fun control(command: PlaybackCommand): MdrRequest =
        t1("playbackControl", T1.PLAY_SET_STATUS, Inquired.PLAY_CONTROL, ENABLE, command.code)
}

object VoiceGuidanceRequests {
    fun get(): MdrRequest = t2("voiceGuidance", T2.VOICE_GUIDANCE_GET_PARAM, Inquired.VOICE_GUIDANCE_SETTING)

    fun set(enabled: Boolean): MdrRequest =
        t2("setVoiceGuidance", T2.VOICE_GUIDANCE_SET_PARAM, Inquired.VOICE_GUIDANCE_SETTING, onOff(enabled))

    fun volume(): MdrRequest = t2("voiceGuidanceVolume", T2.VOICE_GUIDANCE_GET_PARAM, Inquired.VOICE_GUIDANCE_VOLUME)

    /** Plays a short sample at the new volume. */
    fun setVolume(volume: Int): MdrRequest =
        t2("setVoiceGuidanceVolume", T2.VOICE_GUIDANCE_SET_PARAM, Inquired.VOICE_GUIDANCE_VOLUME, volume, onOff(true))
}
