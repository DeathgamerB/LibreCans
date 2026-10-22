package org.librecans.app.data.settings

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Stores a value as encrypted JSON. Anything that cannot be decrypted (for example after the
 * Keystore key was wiped) falls back to [defaultValue] instead of crashing.
 */
class EncryptedJsonSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
    private val cipher: SettingsCipher,
) : Serializer<T> {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readFrom(input: InputStream): T {
        val sealed = input.readBytes()
        if (sealed.isEmpty()) return defaultValue
        return runCatching { json.decodeFromString(serializer, cipher.decrypt(sealed).decodeToString()) }
            .getOrDefault(defaultValue)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(cipher.encrypt(json.encodeToString(serializer, t).encodeToByteArray()))
    }
}
