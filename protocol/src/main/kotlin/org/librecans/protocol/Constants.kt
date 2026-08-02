package org.librecans.protocol

object Constants {
    const val MAX_BLUETOOTH_MESSAGE_SIZE = 2048
    const val START_MARKER: Byte = 0x3E // 62
    const val END_MARKER: Byte = 0x3C // 60

    const val SERVICE_UUID = "96CC203E-5068-46ad-B32D-E316F5E069BA"
    val SERVICE_UUID_IN_BYTES = byteArrayOf(
        0x96.toByte(), 0xcc.toByte(), 0x20.toByte(), 0x3e.toByte(),
        0x50.toByte(), 0x68.toByte(), 0x46.toByte(), 0xad.toByte(),
        0xb3.toByte(), 0x2d.toByte(), 0xe3.toByte(), 0x16.toByte(),
        0xf5.toByte(), 0xe0.toByte(), 0x69.toByte(), 0xba.toByte()
    )

    const val SERVICE_UUID_V2 = "956C7B26-D49A-4BA8-B03F-B17D393CB6E2"
    val SERVICE_UUID_V2_IN_BYTES = byteArrayOf(
        0x95.toByte(), 0x6c.toByte(), 0x7b.toByte(), 0x26.toByte(),
        0xd4.toByte(), 0x9a.toByte(), 0x4b.toByte(), 0xa8.toByte(),
        0xb0.toByte(), 0x3f.toByte(), 0xb1.toByte(), 0x7d.toByte(),
        0x39.toByte(), 0x3c.toByte(), 0xb6.toByte(), 0xe2.toByte()
    )
}

enum class SonyProtocolVersion(val value: Byte) {
    V1(0),
    V2(1)
}

object V2Command {
    const val INIT_REQUEST: Byte = 0x00.toByte()
    const val INIT_REPLY: Byte = 0x01.toByte()
    const val BATTERY_GET: Byte = 0x22.toByte()
    const val BATTERY_RET: Byte = 0x23.toByte()
    const val BATTERY_NTFY: Byte = 0x25.toByte()
    const val EQ_GET: Byte = 0x56.toByte()
    const val EQ_RET: Byte = 0x57.toByte()
    const val EQ_SET: Byte = 0x58.toByte()
    const val DSEE_GET: Byte = 0xe6.toByte()
    const val DSEE_RET: Byte = 0xe7.toByte()
    const val DSEE_SET: Byte = 0xe8.toByte()
    const val FW_GET: Byte = 0x04.toByte()
    const val FW_RET: Byte = 0x05.toByte()
    const val CODEC_GET: Byte = 0x12.toByte()
    const val CODEC_RET: Byte = 0x13.toByte()
    const val APO_GET: Byte = 0x26.toByte()
    const val APO_RET: Byte = 0x27.toByte()
    const val APO_SET: Byte = 0x28.toByte()
    const val BTNMODE_GET: Byte = 0xf6.toByte()
    const val BTNMODE_RET: Byte = 0xf7.toByte()
    const val BTNMODE_SET: Byte = 0xf8.toByte()

    const val SUB_ADAPTIVE_VOLUME: Byte = 0x0a.toByte()
    const val SUB_SPEAK_TO_CHAT: Byte = 0x0c.toByte()
}

enum class EqPreset(val value: Byte) {
    OFF(0x00.toByte()),
    MANUAL_V2(0x01.toByte()),
    CUSTOM_1(0x02.toByte()),
    CUSTOM_2(0x03.toByte()),
    BRIGHT(0x10.toByte()),
    EXCITED(0x11.toByte()),
    MELLOW(0x12.toByte()),
    RELAXED(0x13.toByte()),
    VOCAL(0x14.toByte()),
    TREBLE_BOOST(0x15.toByte()),
    BASS_BOOST(0x16.toByte()),
    SPEECH(0x17.toByte()),
    MANUAL(0xa0.toByte());

    companion object {
        fun fromByte(b: Byte): EqPreset {
            return values().firstOrNull { it.value == b } ?: OFF
        }
    }
}

enum class DataType(val value: Byte) {
    DATA(0),
    ACK(1),
    DATA_MC_NO1(2),
    DATA_ICD(9),
    DATA_EV(10),
    DATA_MDR(12),
    DATA_COMMON(13),
    DATA_MDR_NO2(14),
    SHOT(16),
    SHOT_MC_NO1(18),
    SHOT_ICD(25),
    SHOT_EV(26),
    SHOT_MDR(28),
    SHOT_COMMON(29),
    SHOT_MDR_NO2(30),
    LARGE_DATA_COMMON(45),
    UNKNOWN(-1);

    companion object {
        fun fromByte(b: Byte): DataType {
            return values().firstOrNull { it.value == b } ?: UNKNOWN
        }
    }
}

enum class NcAsmInquiredType(val value: Byte) {
    NO_USE(0),
    NOISE_CANCELLING(1),
    NOISE_CANCELLING_AND_AMBIENT_SOUND_MODE(2),
    AMBIENT_SOUND_MODE(3)
}

enum class NcAsmEffect(val value: Byte) {
    OFF(0),
    ON(1),
    ADJUSTMENT_IN_PROGRESS(16),
    ADJUSTMENT_COMPLETION(17)
}

enum class NcAsmSettingType(val value: Byte) {
    ON_OFF(0),
    LEVEL_ADJUSTMENT(1),
    DUAL_SINGLE_OFF(2)
}

enum class NcAsmSettingTypeV2(val value: Byte) {
    NOISE_CANCELLING(0),
    AMBIENT_SOUND(1)
}

enum class AsmSettingType(val value: Byte) {
    ON_OFF(0),
    LEVEL_ADJUSTMENT(1)
}

enum class AsmId(val value: Byte) {
    NORMAL(0),
    VOICE(1)
}

enum class NcDualSingleValue(val value: Byte) {
    OFF(0),
    SINGLE(1),
    DUAL(2)
}

enum class CommandType(val value: Byte) {
    VPT_SET_PARAM(72),
    NCASM_SET_PARAM(104)
}

enum class VptPresetId(val value: Byte) {
    OFF(0),
    OUTDOOR_FESTIVAL(1),
    ARENA(2),
    CONCERT_HALL(3),
    CLUB(4)
}

enum class SoundPositionPreset(val value: Byte) {
    OFF(0),
    FRONT_LEFT(1),
    FRONT_RIGHT(2),
    FRONT(3),
    REAR_LEFT(17),
    REAR_RIGHT(18),
    OUT_OF_RANGE(-1);

    companion object {
        val ARRAY = arrayOf(
            OFF, FRONT_LEFT, FRONT_RIGHT, FRONT, REAR_LEFT, REAR_RIGHT, OUT_OF_RANGE
        )
    }
}

enum class VptInquiredType(val value: Byte) {
    NO_USE(0),
    VPT(1),
    SOUND_POSITION(2),
    OUT_OF_RANGE(-1)
}
