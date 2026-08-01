package org.librecans.protocol

import org.junit.Assert.*
import org.junit.Test

class ProtocolTest {

    @Test
    fun testByteOrderSwap() {
        val original = 0x12345678
        val swapped = ByteMagic.byteOrderSwap(original)
        assertEquals(0x78563412, swapped)
    }

    @Test
    fun testIntToBytesBE() {
        val num = 0x12345678
        val bytes = ByteMagic.intToBytesBE(num)
        assertArrayEquals(
            byteArrayOf(0x12, 0x34, 0x56, 0x78),
            bytes
        )
    }

    @Test
    fun testMacConversions() {
        val macStr = "00:11:22:33:44:55"
        val macLong = ByteMagic.macStringToLong(macStr)
        assertEquals(0x001122334455L, macLong)

        // macBytesToString converts in reverse order, matching C++ layout
        // C++: addr[MAC_ADDR_BYTES_LEN - 1 - i]
        // Input bytes: index 5 is most significant (first printed), index 0 is least significant
        val bytes = byteArrayOf(0x55.toByte(), 0x44.toByte(), 0x33.toByte(), 0x22.toByte(), 0x11.toByte(), 0x00.toByte())
        val converted = ByteMagic.macBytesToString(bytes)
        assertEquals("00:11:22:33:44:55", converted)
    }

    @Test
    fun testChecksum() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val checksum = CommandSerializer.sumChecksum(data)
        // 1 + 2 + 3 + 4 = 10
        assertEquals(10.toByte(), checksum)

        val largeSum = byteArrayOf(100.toByte(), 200.toByte()) // 200 in signed byte is -56 (0xC8)
        val checksum2 = CommandSerializer.sumChecksum(largeSum)
        // 100 + 200 = 300 -> 300 % 256 = 44 (0x2C)
        assertEquals(44.toByte(), checksum2)
    }

    @Test
    fun testEscapingAndUnescaping() {
        val original = byteArrayOf(59, 60, 61, 62, 63)
        val escaped = CommandSerializer.escapeSpecials(original)

        // 60 -> 61, 44
        // 61 -> 61, 45
        // 62 -> 61, 46
        val expectedEscaped = byteArrayOf(
            59,
            61, 44,
            61, 45,
            61, 46,
            63
        )
        assertArrayEquals(expectedEscaped, escaped)

        val unescaped = CommandSerializer.unescapeSpecials(escaped)
        assertArrayEquals(original, unescaped)
    }

    @Test
    fun testPackageAndUnpack() {
        val payload = byteArrayOf(0x10, 0x20, 0x30)
        val seqNumber: Byte = 5
        val dataType = DataType.DATA_MDR

        // Package the data
        val packaged = CommandSerializer.packageDataForBt(payload, dataType, seqNumber)

        // Verify start and end markers are present
        assertEquals(Constants.START_MARKER, packaged.first())
        assertEquals(Constants.END_MARKER, packaged.last())

        // Extract bytes between start and end markers for unpacking
        val innerMessage = ByteArray(packaged.size - 2)
        System.arraycopy(packaged, 1, innerMessage, 0, innerMessage.size)

        // Unpack and verify
        val unpacked = CommandSerializer.unpackBtMessage(innerMessage)
        assertEquals(dataType, unpacked.dataType)
        assertEquals(seqNumber, unpacked.seqNumber)
        assertArrayEquals(payload, unpacked.payload)
    }

    @Test
    fun testSerializeNcAndAsmSetting() {
        val result = CommandSerializer.serializeNcAndAsmSetting(
            NcAsmEffect.ADJUSTMENT_COMPLETION,
            NcAsmSettingType.LEVEL_ADJUSTMENT,
            AsmSettingType.LEVEL_ADJUSTMENT,
            AsmId.VOICE,
            15.toByte()
        )

        // Expected output format:
        // [0x68, 0x02, ncAsmEffect, ncAsmSettingType, dualSingle, asmSettingType, asmId, asmLevel]
        // 0x68 = 104 (CommandType.NCASM_SET_PARAM)
        // 0x02 = NcAsmInquiredType.NOISE_CANCELLING_AND_AMBIENT_SOUND_MODE
        // dualSingle = getDualSingleForAsmLevel(15) -> 15 > 1 -> OFF (0)
        assertArrayEquals(
            byteArrayOf(
                104.toByte(),
                2.toByte(),
                17.toByte(), // ADJUSTMENT_COMPLETION
                1.toByte(),  // LEVEL_ADJUSTMENT
                0.toByte(),  // OFF
                1.toByte(),  // LEVEL_ADJUSTMENT
                1.toByte(),  // VOICE
                15.toByte()
            ),
            result
        )
    }

    @Test
    fun testSerializeNcAndAsmSettingV2() {
        val result = CommandSerializer.serializeNcAndAsmSettingV2(
            NcAsmEffect.ON,
            NcAsmSettingTypeV2.AMBIENT_SOUND,
            AsmId.NORMAL,
            12.toByte()
        )

        // Expected output format:
        // [0x68, 0x17, 0x01, ncAsmEffect, ncAsmSettingType, voicePassthrough, asmLevel]
        assertArrayEquals(
            byteArrayOf(
                104.toByte(),
                0x17.toByte(),
                0x01.toByte(),
                1.toByte(), // ON
                1.toByte(), // AMBIENT_SOUND
                0.toByte(), // NORMAL
                12.toByte()
            ),
            result
        )
    }
}
