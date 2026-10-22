package org.librecans.protocol.mdr

import org.librecans.protocol.wire.toHex

class MdrRequest(val table: Table, val payload: ByteArray, val label: String) {
    override fun equals(other: Any?): Boolean =
        other is MdrRequest && table == other.table && payload.contentEquals(other.payload)

    override fun hashCode(): Int = table.hashCode() * 31 + payload.contentHashCode()

    override fun toString(): String = "$label[$table: ${payload.toHex()}]"
}

internal fun t1(label: String, vararg bytes: Int) = MdrRequest(Table.ONE, ByteArray(bytes.size) { bytes[it].toByte() }, label)

internal fun t2(label: String, vararg bytes: Int) = MdrRequest(Table.TWO, ByteArray(bytes.size) { bytes[it].toByte() }, label)
