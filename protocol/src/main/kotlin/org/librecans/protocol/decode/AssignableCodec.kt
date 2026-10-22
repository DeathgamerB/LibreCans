package org.librecans.protocol.decode

import org.librecans.protocol.model.GestureBinding
import org.librecans.protocol.model.PresetOption
import org.librecans.protocol.model.TouchKey
import org.librecans.protocol.wire.PayloadReader

/**
 * Assignable settings structures. A preset capability stores both of its counts up front:
 * preset, n fixed actions, n customizable actions, fixed (action, function) pairs,
 * then customizable (action, default function, [functions]) entries.
 */
internal object AssignableCodec {
    fun readKeys(r: PayloadReader): List<TouchKey> = r.list {
        val key = u8()
        val type = u8()
        val defaultPreset = u8()
        TouchKey(key, type, defaultPreset, list { readPresetCapability(this) })
    }

    private fun readPresetCapability(r: PayloadReader): PresetOption {
        val preset = r.u8()
        val fixedCount = r.u8()
        val customizableCount = r.u8()
        val fixed = List(fixedCount) { GestureBinding(r.u8(), r.u8()) }
        val customizable = List(customizableCount) {
            val action = r.u8()
            val defaultFunction = r.u8()
            r.list { u8() }
            GestureBinding(action, defaultFunction)
        }
        return PresetOption(preset, fixed + customizable)
    }

    fun readPreset(r: PayloadReader): PresetOption {
        val preset = r.u8()
        return PresetOption(preset, r.list { GestureBinding(u8(), u8()) })
    }

    fun writePreset(option: PresetOption): List<Int> =
        listOf(option.preset, option.gestures.size) + option.gestures.flatMap { listOf(it.action, it.function) }
}
