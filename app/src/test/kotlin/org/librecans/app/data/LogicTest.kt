package org.librecans.app.data

import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Test
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.bluetooth.PairedDevice
import org.librecans.app.data.bluetooth.SonyDevices
import org.librecans.app.data.bluetooth.SonyUuids
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.DemoHeadphones
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.data.connection.FailureReason
import org.librecans.app.service.ServiceController
import org.librecans.app.ui.common.humanize
import org.librecans.app.ui.common.next
import org.librecans.app.ui.devices.AutoConnectPolicy
import org.librecans.protocol.model.NoiseMode

class LogicTest {
    private val headset = UUID.fromString("00001108-0000-1000-8000-00805F9B34FB")

    @Test
    fun `sony headphones are recognised by service record or name`() {
        assertThat(SonyDevices.isSony("My buds", listOf(SonyUuids.V2))).isTrue()
        assertThat(SonyDevices.isSony(null, listOf(SonyUuids.V1))).isTrue()
        assertThat(SonyDevices.isSony("WF-1000XM5", emptyList())).isTrue()
        assertThat(SonyDevices.isSony("LinkBuds S", emptyList())).isTrue()
        assertThat(SonyDevices.isSony("Pixel Buds Pro", listOf(headset))).isFalse()
        assertThat(SonyDevices.isSony(null, emptyList())).isFalse()
    }

    @Test
    fun `earbuds and headphones are told apart`() {
        assertThat(SonyDevices.kindOf("WF-1000XM5")).isEqualTo(DeviceKind.EARBUDS)
        assertThat(SonyDevices.kindOf("LinkBuds S")).isEqualTo(DeviceKind.EARBUDS)
        assertThat(SonyDevices.kindOf("WH-1000XM5")).isEqualTo(DeviceKind.HEADPHONES)
        assertThat(SonyDevices.kindOf(null)).isEqualTo(DeviceKind.HEADPHONES)
    }

    @Test
    fun `the advertised protocol is tried first`() {
        assertThat(SonyDevices.serviceOrder(listOf(SonyUuids.V1))).containsExactly(SonyUuids.V1, SonyUuids.V2).inOrder()
        assertThat(SonyDevices.serviceOrder(listOf(SonyUuids.V2, SonyUuids.V1))).containsExactly(SonyUuids.V2, SonyUuids.V1).inOrder()
        assertThat(SonyDevices.serviceOrder(emptyList())).containsExactly(SonyUuids.V2, SonyUuids.V1).inOrder()
    }

    @Test
    fun `auto connect prefers the last device then a single playing one`() {
        val a = PairedDevice("A", "WF-1000XM5", DeviceKind.EARBUDS, audioConnected = true)
        val b = PairedDevice("B", "WH-1000XM5", DeviceKind.HEADPHONES, audioConnected = true)
        val idle = PairedDevice("C", "WH-CH720N", DeviceKind.HEADPHONES, audioConnected = false)

        assertThat(AutoConnectPolicy.choose(listOf(a, b), lastAddress = "B")).isEqualTo(b)
        assertThat(AutoConnectPolicy.choose(listOf(a, idle), lastAddress = null)).isEqualTo(a)
        assertThat(AutoConnectPolicy.choose(listOf(a, b), lastAddress = null)).isNull()
        assertThat(AutoConnectPolicy.choose(listOf(idle), lastAddress = "C")).isNull()
    }

    @Test
    fun `the service runs for real connections and lost links only`() {
        val target = DeviceTarget("A", "WF-1000XM5", DeviceKind.EARBUDS)
        assertThat(ServiceController.shouldRun(ConnectionState.Connected(target))).isTrue()
        assertThat(ServiceController.shouldRun(ConnectionState.Connecting(target))).isTrue()
        assertThat(ServiceController.shouldRun(ConnectionState.Failed(target, FailureReason.CONNECTION_LOST))).isTrue()
        assertThat(ServiceController.shouldRun(ConnectionState.Failed(target, FailureReason.NO_RESPONSE))).isFalse()
        assertThat(ServiceController.shouldRun(ConnectionState.Idle)).isFalse()
        assertThat(ServiceController.shouldRun(ConnectionState.Connected(DemoHeadphones.target))).isFalse()
    }

    @Test
    fun `noise modes cycle like the earbuds`() {
        assertThat(NoiseMode.NOISE_CANCELLING.next()).isEqualTo(NoiseMode.AMBIENT)
        assertThat(NoiseMode.AMBIENT.next()).isEqualTo(NoiseMode.OFF)
        assertThat(NoiseMode.OFF.next()).isEqualTo(NoiseMode.NOISE_CANCELLING)
    }

    @Test
    fun `unknown setting identifiers become readable`() {
        assertThat(humanize("WIDE_AREA_TAP_SETTING")).isEqualTo("Wide area tap")
        assertThat(humanize("AUTO_PLAY")).isEqualTo("Auto play")
    }
}
