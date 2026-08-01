package org.librecans.protocol

import java.io.ByteArrayOutputStream

open class RecoverableException(message: String, val shouldDisconnect: Boolean = false) : Exception(message)

object CommandSerializer {
    const val ESCAPED_BYTE_SENTRY: Byte = 61
    const val ESCAPED_60: Byte = 44
    const val ESCAPED_61: Byte = 45
    const val ESCAPED_62: Byte = 46
    const val MAX_STEPS_WH_1000_XM3 = 19
    const val MINIMUM_VOICE_FOCUS_STEP = 2
    const val ASM_LEVEL_DISABLED: Byte = -1

    class Message(
        val dataType: DataType,
        val seqNumber: Byte,
        val payload: ByteArray
    )

    fun escapeSpecials(src: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        for (b in src) {
            when (b.toInt()) {
                60 -> {
                    out.write(ESCAPED_BYTE_SENTRY.toInt())
                    out.write(ESCAPED_60.toInt())
                }
                61 -> {
                    out.write(ESCAPED_BYTE_SENTRY.toInt())
                    out.write(ESCAPED_61.toInt())
                }
                62 -> {
                    out.write(ESCAPED_BYTE_SENTRY.toInt())
                    out.write(ESCAPED_62.toInt())
                }
                else -> {
                    out.write(b.toInt())
                }
            }
        }
        return out.toByteArray()
    }

    fun unescapeSpecials(src: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < src.size) {
            val currByte = src[i]
            if (currByte == ESCAPED_BYTE_SENTRY) {
                if (i == src.size - 1) {
                    throw RuntimeException("No data left for escaped byte data")
                }
                i++
                when (src[i]) {
                    ESCAPED_60 -> out.write(60)
                    ESCAPED_61 -> out.write(61)
                    ESCAPED_62 -> out.write(62)
                    else -> throw RuntimeException("Unexpected escaped byte")
                }
            } else {
                out.write(currByte.toInt())
            }
            i++
        }
        return out.toByteArray()
    }

    fun sumChecksum(src: ByteArray, offset: Int = 0, size: Int = src.size): Byte {
        var accumulator = 0
        for (i in offset until (offset + size)) {
            accumulator += src[i].toInt() and 0xFF
        }
        return (accumulator and 0xFF).toByte()
    }

    fun packageDataForBt(src: ByteArray, dataType: DataType, seqNumber: Byte): ByteArray {
        val toEscape = ByteArrayOutputStream()
        toEscape.write(dataType.value.toInt())
        toEscape.write(seqNumber.toInt())
        val sizeBytes = ByteMagic.intToBytesBE(src.size)
        toEscape.write(sizeBytes)
        toEscape.write(src)

        val tempBytes = toEscape.toByteArray()
        val checksum = sumChecksum(tempBytes)

        val fullUnescaped = ByteArray(tempBytes.size + 1)
        System.arraycopy(tempBytes, 0, fullUnescaped, 0, tempBytes.size)
        fullUnescaped[tempBytes.size] = checksum

        val escaped = escapeSpecials(fullUnescaped)

        val out = ByteArrayOutputStream()
        out.write(Constants.START_MARKER.toInt())
        out.write(escaped)
        out.write(Constants.END_MARKER.toInt())

        val result = out.toByteArray()
        if (result.size > Constants.MAX_BLUETOOTH_MESSAGE_SIZE) {
            throw RuntimeException("Exceeded the max bluetooth message size, and I can't handle chunked messages")
        }
        return result
    }

    fun unpackBtMessage(src: ByteArray): Message {
        val unescaped = unescapeSpecials(src)

        if (unescaped.size < 7) {
            throw RuntimeException("Invalid message: Smaller than the minimum message size")
        }

        val dataSize = ((unescaped[2].toInt() and 0xFF) shl 24) or
                ((unescaped[3].toInt() and 0xFF) shl 16) or
                ((unescaped[4].toInt() and 0xFF) shl 8) or
                (unescaped[5].toInt() and 0xFF)

        if (unescaped.size < 6 + dataSize + 1) {
            throw RecoverableException("Invalid message: declared size exceeds received data", true)
        }

        val receivedChecksum = unescaped[6 + dataSize]
        val calculatedChecksum = sumChecksum(unescaped, 0, 6 + dataSize)
        if (receivedChecksum != calculatedChecksum) {
            throw RecoverableException("Invalid checksum!", true)
        }

        val payload = ByteArray(dataSize)
        System.arraycopy(unescaped, 6, payload, 0, dataSize)

        return Message(
            dataType = DataType.fromByte(unescaped[0]),
            seqNumber = unescaped[1],
            payload = payload
        )
    }

    fun getDualSingleForAsmLevel(asmLevel: Byte): NcDualSingleValue {
        if (asmLevel > MAX_STEPS_WH_1000_XM3) {
            throw RuntimeException("Exceeded max steps")
        }
        return when (asmLevel.toInt()) {
            1 -> NcDualSingleValue.SINGLE
            0 -> NcDualSingleValue.DUAL
            else -> NcDualSingleValue.OFF
        }
    }

    fun serializeNcAndAsmSetting(
        ncAsmEffect: NcAsmEffect,
        ncAsmSettingType: NcAsmSettingType,
        asmSettingType: AsmSettingType,
        asmId: AsmId,
        asmLevel: Byte
    ): ByteArray {
        return byteArrayOf(
            CommandType.NCASM_SET_PARAM.value,
            NcAsmInquiredType.NOISE_CANCELLING_AND_AMBIENT_SOUND_MODE.value,
            ncAsmEffect.value,
            ncAsmSettingType.value,
            getDualSingleForAsmLevel(asmLevel).value,
            asmSettingType.value,
            asmId.value,
            asmLevel
        )
    }

    fun serializeNcAndAsmSettingV2(
        ncAsmEffect: NcAsmEffect,
        ncAsmSettingType: NcAsmSettingTypeV2,
        voicePassthrough: AsmId,
        asmLevel: Byte
    ): ByteArray {
        return byteArrayOf(
            CommandType.NCASM_SET_PARAM.value,
            0x17.toByte(),
            0x01.toByte(),
            ncAsmEffect.value,
            ncAsmSettingType.value,
            voicePassthrough.value,
            asmLevel
        )
    }

    fun serializeVPTSetting(type: VptInquiredType, preset: Byte): ByteArray {
        return byteArrayOf(
            CommandType.VPT_SET_PARAM.value,
            type.value,
            preset
        )
    }
}
