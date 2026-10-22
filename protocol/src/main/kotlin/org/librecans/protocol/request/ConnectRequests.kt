package org.librecans.protocol.request

import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.T2
import org.librecans.protocol.mdr.t1
import org.librecans.protocol.mdr.t2

object ConnectRequests {
    fun protocolInfo(): MdrRequest = t1("protocolInfo", T1.CONNECT_GET_PROTOCOL_INFO, Inquired.FIXED)

    /** The reply carries the headphones' unique device ID. */
    fun capabilityInfo(): MdrRequest = t1("capabilityInfo", T1.CONNECT_GET_CAPABILITY_INFO, Inquired.FIXED)

    fun modelName(): MdrRequest = t1("modelName", T1.CONNECT_GET_DEVICE_INFO, Inquired.DEVICE_INFO_MODEL_NAME)

    fun firmware(): MdrRequest = t1("firmware", T1.CONNECT_GET_DEVICE_INFO, Inquired.DEVICE_INFO_FW_VERSION)

    fun seriesAndColor(): MdrRequest =
        t1("seriesAndColor", T1.CONNECT_GET_DEVICE_INFO, Inquired.DEVICE_INFO_SERIES_AND_COLOR)

    fun supportedFeatures(): MdrRequest =
        t1("supportedFeatures", T1.CONNECT_GET_SUPPORT_FUNCTION, Inquired.FIXED)

    fun supportedFeaturesTable2(): MdrRequest =
        t2("supportedFeatures2", T2.CONNECT_GET_SUPPORT_FUNCTION, Inquired.FIXED)
}
