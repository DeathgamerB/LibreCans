package org.librecans.protocol.frame

import org.librecans.protocol.wire.toHex

class Frame(val type: DataType, val sequence: Int, val payload: ByteArray) {
    init {
        require(sequence in 0..0xFF) { "Sequence out of range: $sequence" }
    }

    override fun equals(other: Any?): Boolean =
        other is Frame && type == other.type && sequence == other.sequence && payload.contentEquals(other.payload)

    override fun hashCode(): Int = (type.hashCode() * 31 + sequence) * 31 + payload.contentHashCode()

    override fun toString(): String = "Frame($type, seq=$sequence, ${payload.toHex()})"

    companion object {
        fun ack(sequence: Int): Frame = Frame(DataType.ACK, sequence, ByteArray(0))
    }
}
