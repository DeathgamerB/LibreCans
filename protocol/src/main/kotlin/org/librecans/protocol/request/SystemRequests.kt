package org.librecans.protocol.request

import org.librecans.protocol.decode.AssignableCodec
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.mdr.onOff
import org.librecans.protocol.mdr.t1
import org.librecans.protocol.model.PresetOption
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout

object SystemRequests {
    fun touchCapability(): MdrRequest = t1("touchCapability", T1.SYSTEM_GET_CAPABILITY, Inquired.SYSTEM_ASSIGNABLE)

    fun touchPresets(): MdrRequest = t1("touchPresets", T1.SYSTEM_GET_PARAM, Inquired.SYSTEM_ASSIGNABLE)

    fun setTouchPresets(presets: List<Int>): MdrRequest =
        bytes("setTouchPresets", listOf(T1.SYSTEM_SET_PARAM, Inquired.SYSTEM_ASSIGNABLE, presets.size) + presets)

    fun touchCustomizations(): MdrRequest =
        t1("touchCustomizations", T1.SYSTEM_GET_EXT_PARAM, Inquired.SYSTEM_ASSIGNABLE)

    fun setTouchCustomizations(options: List<PresetOption>): MdrRequest = bytes(
        "setTouchCustomizations",
        listOf(T1.SYSTEM_SET_EXT_PARAM, Inquired.SYSTEM_ASSIGNABLE, options.size) +
            options.flatMap(AssignableCodec::writePreset),
    )

    fun pauseWhenRemoved(): MdrRequest = get("pauseWhenRemoved", Inquired.SYSTEM_PLAYBACK_BY_WEARING)

    fun setPauseWhenRemoved(enabled: Boolean): MdrRequest =
        set("setPauseWhenRemoved", Inquired.SYSTEM_PLAYBACK_BY_WEARING, enabled)

    fun speakToChat(): MdrRequest = get("speakToChat", Inquired.SYSTEM_SMART_TALKING)

    /** The trailing byte disables the official app's preview mode. */
    fun setSpeakToChat(enabled: Boolean): MdrRequest =
        t1("setSpeakToChat", T1.SYSTEM_SET_PARAM, Inquired.SYSTEM_SMART_TALKING, onOff(enabled), onOff(false))

    fun speakToChatConfig(): MdrRequest =
        t1("speakToChatConfig", T1.SYSTEM_GET_EXT_PARAM, Inquired.SYSTEM_SMART_TALKING)

    fun setSpeakToChatConfig(sensitivity: SpeakToChatSensitivity, timeout: SpeakToChatTimeout): MdrRequest =
        t1("setSpeakToChatConfig", T1.SYSTEM_SET_EXT_PARAM, Inquired.SYSTEM_SMART_TALKING, sensitivity.code, timeout.code)

    fun headGestures(): MdrRequest = get("headGestures", Inquired.SYSTEM_HEAD_GESTURE)

    fun setHeadGestures(enabled: Boolean): MdrRequest = set("setHeadGestures", Inquired.SYSTEM_HEAD_GESTURE, enabled)

    fun adaptiveVolume(): MdrRequest = get("adaptiveVolume", Inquired.SYSTEM_AUTO_VOLUME)

    fun setAdaptiveVolume(enabled: Boolean): MdrRequest =
        set("setAdaptiveVolume", Inquired.SYSTEM_AUTO_VOLUME, enabled)

    private fun get(label: String, type: Int) = t1(label, T1.SYSTEM_GET_PARAM, type)

    private fun set(label: String, type: Int, enabled: Boolean) = t1(label, T1.SYSTEM_SET_PARAM, type, onOff(enabled))

    private fun bytes(label: String, values: List<Int>) =
        MdrRequest(Table.ONE, ByteArray(values.size) { values[it].toByte() }, label)
}
