package org.librecans.protocol.mdr

import org.librecans.protocol.frame.DataType

/** MDR v2 splits its commands into two tables carried by different frame types. */
enum class Table(val dataType: DataType) {
    ONE(DataType.DATA_MDR),
    TWO(DataType.DATA_MDR_NO2);

    companion object {
        fun of(dataType: DataType): Table? = entries.firstOrNull { it.dataType == dataType }
    }
}
