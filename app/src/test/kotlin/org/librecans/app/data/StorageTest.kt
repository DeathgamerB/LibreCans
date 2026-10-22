package org.librecans.app.data

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.librecans.app.data.diagnostics.DiagnosticsLog
import org.librecans.app.data.diagnostics.LogEntry
import org.librecans.app.data.settings.AppSettings
import org.librecans.app.data.settings.EncryptedJsonSerializer
import org.librecans.app.data.settings.SavedDevice
import org.librecans.app.data.settings.SettingsCipher
import org.librecans.app.data.settings.ThemeMode
import org.librecans.protocol.frame.DataType
import org.librecans.protocol.frame.Frame
import org.librecans.protocol.session.Direction

class StorageTest {
    /** Reversible stand-in for the Keystore cipher with a magic prefix to detect tampering. */
    private class FakeCipher : SettingsCipher {
        override fun encrypt(plain: ByteArray) = MAGIC + plain.map { (it.toInt() xor 0x5A).toByte() }

        override fun decrypt(sealed: ByteArray): ByteArray {
            require(sealed.take(MAGIC.size) == MAGIC.toList()) { "tampered" }
            return sealed.drop(MAGIC.size).map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        }

        companion object {
            val MAGIC = byteArrayOf(1, 2, 3)
        }
    }

    private val serializer = EncryptedJsonSerializer(AppSettings.serializer(), AppSettings(), FakeCipher())

    @Test
    fun `settings round trip through the cipher`() = runTest {
        val settings = AppSettings(
            lastDevice = SavedDevice("AA:BB:CC:DD:EE:FF", "WF-1000XM5"),
            themeMode = ThemeMode.DARK,
            diagnosticsEnabled = true,
        )
        val out = ByteArrayOutputStream()
        serializer.writeTo(settings, out)

        assertThat(out.toByteArray().decodeToString()).doesNotContain("AA:BB:CC:DD:EE:FF")
        assertThat(serializer.readFrom(ByteArrayInputStream(out.toByteArray()))).isEqualTo(settings)
    }

    @Test
    fun `empty or undecryptable files fall back to defaults`() = runTest {
        assertThat(serializer.readFrom(ByteArrayInputStream(ByteArray(0)))).isEqualTo(AppSettings())
        assertThat(serializer.readFrom(ByteArrayInputStream(byteArrayOf(9, 9, 9, 9)))).isEqualTo(AppSettings())
    }

    @Test
    fun `fields from newer versions are ignored`() = runTest {
        val json = """{"autoConnect":false,"someFutureField":42}""".encodeToByteArray()
        val sealed = FakeCipher().encrypt(json)

        assertThat(serializer.readFrom(ByteArrayInputStream(sealed)).autoConnect).isFalse()
    }

    @Test
    fun `diagnostics only record while enabled and stay bounded`() {
        val log = DiagnosticsLog(capacity = 3, clock = { 0L })
        log.onFrame(Direction.OUT, Frame(DataType.DATA_MDR, 0, byteArrayOf(0x12, 0x02)), "codec")
        assertThat(log.entries.value).isEmpty()

        log.enabled = true
        log.onFrame(Direction.OUT, Frame(DataType.DATA_MDR, 0, byteArrayOf(0x12, 0x02)), "codec")
        log.onFrame(Direction.IN, Frame.ack(1), null)
        log.onError("boom")
        log.event("hello")

        assertThat(log.entries.value.map { it.kind }).containsExactly(LogEntry.Kind.ACK, LogEntry.Kind.ERROR, LogEntry.Kind.EVENT).inOrder()
        assertThat(log.export()).contains("hello")

        log.enabled = false
        assertThat(log.entries.value).isEmpty()
    }

    @Test
    fun `sent frames are described with their payload`() {
        val log = DiagnosticsLog(clock = { 0L }).apply { enabled = true }
        log.onFrame(Direction.OUT, Frame(DataType.DATA_MDR, 1, byteArrayOf(0x12, 0x02)), "codec")

        assertThat(log.entries.value.single().text).isEqualTo("DATA_MDR #1 12 02  (codec)")
    }
}
