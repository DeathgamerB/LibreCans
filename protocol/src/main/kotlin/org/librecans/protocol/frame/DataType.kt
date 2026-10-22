package org.librecans.protocol.frame

enum class DataType(val code: Int) {
    DATA(0x00),
    ACK(0x01),
    DATA_MC_NO1(0x02),
    DATA_ICD(0x09),
    DATA_EV(0x0a),
    DATA_MDR(0x0c),
    DATA_COMMON(0x0d),
    DATA_MDR_NO2(0x0e),
    SHOT(0x10),
    SHOT_MDR(0x1c),
    SHOT_MDR_NO2(0x1e),
    LARGE_DATA_COMMON(0x2d);

    val needsAck: Boolean get() = this != ACK

    companion object {
        fun fromCode(code: Int): DataType? = entries.firstOrNull { it.code == code }
    }
}
