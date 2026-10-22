package org.librecans.app.data.bluetooth

import java.util.UUID

enum class DeviceKind { EARBUDS, HEADPHONES }

data class PairedDevice(
    val address: String,
    val name: String,
    val kind: DeviceKind,
    val audioConnected: Boolean,
)

object SonyUuids {
    val V1: UUID = UUID.fromString("96CC203E-5068-46ad-B32D-E316F5E069BA")
    val V2: UUID = UUID.fromString("956C7B26-D49A-4BA8-B03F-B17D393CB6E2")
}

object SonyDevices {
    private val NAME_PREFIXES = listOf("WH-", "WF-", "WI-", "MDR-", "LinkBuds", "ULT WEAR", "ULT WF", "INZONE")

    /** Prefers the advertised Sony service records and falls back to known model names. */
    fun isSony(name: String?, uuids: Collection<UUID>): Boolean =
        SonyUuids.V1 in uuids || SonyUuids.V2 in uuids ||
            (name != null && NAME_PREFIXES.any { name.startsWith(it, ignoreCase = true) })

    fun kindOf(name: String?): DeviceKind {
        val upper = name?.uppercase().orEmpty()
        return if (upper.startsWith("WF-") || "LINKBUDS" in upper || upper.startsWith("ULT WF")) {
            DeviceKind.EARBUDS
        } else {
            DeviceKind.HEADPHONES
        }
    }

    /** Service records to try, most likely first. Unknown devices try the newer protocol first. */
    fun serviceOrder(uuids: Collection<UUID>): List<UUID> = when {
        SonyUuids.V2 in uuids -> listOf(SonyUuids.V2, SonyUuids.V1)
        SonyUuids.V1 in uuids -> listOf(SonyUuids.V1, SonyUuids.V2)
        else -> listOf(SonyUuids.V2, SonyUuids.V1)
    }
}
