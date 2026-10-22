package org.librecans.protocol.model

enum class ChargingState(val code: Int) {
    NOT_CHARGING(0x00),
    CHARGING(0x01),
    UNKNOWN(0x02),
    CHARGED(0x03);

    companion object {
        fun fromCode(code: Int): ChargingState = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

data class BatteryLevel(val percent: Int, val charging: ChargingState) {
    val isCharging: Boolean get() = charging == ChargingState.CHARGING
}

enum class BatterySlot { SINGLE, LEFT, RIGHT, CASE }

data class Batteries(
    val single: BatteryLevel? = null,
    val left: BatteryLevel? = null,
    val right: BatteryLevel? = null,
    val case: BatteryLevel? = null,
) {
    val isEmpty: Boolean get() = single == null && left == null && right == null && case == null

    /** Lowest level of the parts that are worn, used for compact summaries. */
    val worn: BatteryLevel? get() = single ?: listOfNotNull(left, right).minByOrNull { it.percent }

    fun with(slot: BatterySlot, level: BatteryLevel?): Batteries = when (slot) {
        BatterySlot.SINGLE -> copy(single = level)
        BatterySlot.LEFT -> copy(left = level)
        BatterySlot.RIGHT -> copy(right = level)
        BatterySlot.CASE -> copy(case = level)
    }
}
