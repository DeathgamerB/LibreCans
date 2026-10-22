package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.model.Feature
import org.librecans.protocol.wire.PayloadReader

internal object ConnectDecoder {
    private const val DISABLED = 0x01

    fun decode(command: Int, r: PayloadReader): DeviceEvent? = when (command) {
        T1.CONNECT_RET_PROTOCOL_INFO -> protocolInfo(r)
        T1.CONNECT_RET_CAPABILITY_INFO -> uniqueId(r)
        T1.CONNECT_RET_DEVICE_INFO -> deviceInfo(r)
        T1.CONNECT_RET_SUPPORT_FUNCTION -> supportedFeatures(Table.ONE, r)
        else -> null
    }

    /** MDR v1 replies with 4 bytes, v2 with 8. Only v2 carries the version and table flags. */
    private fun protocolInfo(r: PayloadReader): DeviceEvent {
        r.u8()
        if (r.remaining < 6) return DeviceEvent.ProtocolInfo(version = 0, table1 = false, table2 = false)
        val version = r.u32()
        return DeviceEvent.ProtocolInfo(version, table1 = r.u8() != DISABLED, table2 = r.u8() != DISABLED)
    }

    /** Inquired type, a capability counter, then the device's unique ID. */
    private fun uniqueId(r: PayloadReader): DeviceEvent? {
        r.u8()
        r.u8()
        return r.prefixedString().trim().takeIf { it.isNotEmpty() }?.let(DeviceEvent::UniqueId)
    }

    private fun deviceInfo(r: PayloadReader): DeviceEvent? = when (r.u8()) {
        Inquired.DEVICE_INFO_MODEL_NAME -> DeviceEvent.ModelName(r.prefixedString().trim())
        Inquired.DEVICE_INFO_FW_VERSION -> DeviceEvent.FirmwareVersion(r.prefixedString().trim())
        Inquired.DEVICE_INFO_SERIES_AND_COLOR -> DeviceEvent.SeriesAndColor(r.u8(), r.u8())
        else -> null
    }

    fun supportedFeatures(table: Table, r: PayloadReader): DeviceEvent {
        r.u8()
        val features = r.list { Feature(table, u8()).also { u8() } }.toSet()
        return DeviceEvent.SupportedFeatures(table, features)
    }
}
