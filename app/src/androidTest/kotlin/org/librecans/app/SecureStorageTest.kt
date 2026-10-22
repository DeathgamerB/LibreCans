package org.librecans.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.File
import javax.crypto.AEADBadTagException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.librecans.app.data.settings.DataStoreSettingsRepository
import org.librecans.app.data.settings.KeystoreSettingsCipher
import org.librecans.app.data.settings.SavedDevice

/** Runs against the real Android Keystore, which the JVM tests cannot reach. */
@RunWith(AndroidJUnit4::class)
class SecureStorageTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val cipher = KeystoreSettingsCipher(alias = "librecans_test_key")

    @Test
    fun keystoreCipherRoundTripsWithFreshIvs() {
        val plain = "AA:BB:CC:DD:EE:FF".encodeToByteArray()

        val first = cipher.encrypt(plain)
        val second = cipher.encrypt(plain)

        assertThat(first).isNotEqualTo(second)
        assertThat(cipher.decrypt(first)).isEqualTo(plain)
        assertThat(cipher.decrypt(second)).isEqualTo(plain)
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val sealed = cipher.encrypt("secret".encodeToByteArray())
        sealed[sealed.lastIndex] = (sealed.last() + 1).toByte()

        assertThrows(AEADBadTagException::class.java) { cipher.decrypt(sealed) }
    }

    @Test
    fun settingsArePersistedEncrypted() = runBlocking {
        val file = File(context.filesDir, "datastore/test-settings.enc")
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = DataStoreSettingsRepository.create(context, scope, cipher, fileName = "test-settings.enc")

        repository.update { it.copy(lastDevice = SavedDevice("AA:BB:CC:DD:EE:FF", "WF-1000XM5")) }
        assertThat(repository.load().lastDevice?.name).isEqualTo("WF-1000XM5")
        scope.cancel()

        val stored = file.readBytes()
        assertThat(stored.decodeToString()).doesNotContain("WF-1000XM5")
        assertThat(stored.decodeToString()).doesNotContain("AA:BB")
    }
}
