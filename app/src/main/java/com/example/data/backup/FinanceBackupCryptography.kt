package com.example.data.backup

import android.os.Build
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.Charset
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based authenticated encryption for exported financial backups.
 *
 * The password and derived key are never persisted. Every file uses a fresh random salt and IV.
 * The caller must also clear its password char array after any work done before invoking this
 * helper. API 24-25 use the platform's PBKDF2-HMAC-SHA1 provider; API 26+ use SHA-256.
 */
object FinanceBackupCryptography {
    const val ENCRYPTED_FORMAT = "budget-tracker-encrypted-backup"
    const val MIN_PASSWORD_LENGTH = 12
    const val MAX_PLAINTEXT_BYTES = 25 * 1024 * 1024
    const val MAX_ENCRYPTED_BACKUP_BYTES = 36 * 1024 * 1024

    private const val CRYPTO_VERSION = 1
    private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    private const val KDF_SHA256 = "PBKDF2WithHmacSHA256"
    private const val KDF_SHA1 = "PBKDF2WithHmacSHA1"
    private const val SHA256_ITERATIONS = 310_000
    private const val SHA1_ITERATIONS = 600_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private val UTF8: Charset = Charsets.UTF_8
    private val secureRandom = SecureRandom()

    fun isEncryptedBackup(contents: String): Boolean =
        runCatching { JSONObject(contents).optString("format") == ENCRYPTED_FORMAT }
            .getOrDefault(false)

    /** Encrypts a plaintext, versioned finance backup into a self-describing JSON envelope. */
    fun encrypt(plaintext: String, password: CharArray): String {
        try {
            require(password.size >= MIN_PASSWORD_LENGTH) {
                "Use a backup password with at least $MIN_PASSWORD_LENGTH characters."
            }
            val clearBytes = plaintext.toByteArray(UTF8)
            require(clearBytes.size <= MAX_PLAINTEXT_BYTES) {
                "Backup is larger than the supported 25 MB limit."
            }

            val kdf = preferredKdf()
            val iterations = iterationsFor(kdf)
            val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
            val iv = ByteArray(IV_BYTES).also(secureRandom::nextBytes)
            var keyBytes: ByteArray? = null
            try {
                val derivedKey = deriveKey(password, salt, iterations, kdf)
                keyBytes = derivedKey
                val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derivedKey, "AES"), GCMParameterSpec(TAG_BITS, iv))
                cipher.updateAAD(authenticatedHeader(kdf, iterations))
                val ciphertext = cipher.doFinal(clearBytes)
                val envelope = JSONObject()
                    .put("format", ENCRYPTED_FORMAT)
                    .put("cryptoVersion", CRYPTO_VERSION)
                    .put("kdf", kdf)
                    .put("iterations", iterations)
                    .put("cipher", CIPHER_ALGORITHM)
                    .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                    .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                    .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                    .toString(2)
                require(envelope.toByteArray(UTF8).size <= MAX_ENCRYPTED_BACKUP_BYTES) {
                    "Encrypted backup is larger than the supported file limit."
                }
                return envelope
            } finally {
                clearBytes.fill(0)
                salt.fill(0)
                iv.fill(0)
                keyBytes?.fill(0)
            }
        } finally {
            password.fill('\u0000')
        }
    }

    /** Decrypts and authenticates a backup envelope before JSON validation or data writes. */
    fun decrypt(envelopeText: String, password: CharArray): String {
        try {
            require(envelopeText.toByteArray(UTF8).size <= MAX_ENCRYPTED_BACKUP_BYTES) {
                "Encrypted backup is larger than the supported file limit."
            }
            val envelope = try {
                JSONObject(envelopeText)
            } catch (error: Exception) {
                throw IllegalArgumentException("The encrypted backup file is malformed.", error)
            }
            require(envelope.getString("format") == ENCRYPTED_FORMAT) {
                "This is not an encrypted Budget Tracker backup."
            }
            require(envelope.getInt("cryptoVersion") == CRYPTO_VERSION) {
                "This encrypted backup uses an unsupported encryption version."
            }
            require(envelope.getString("cipher") == CIPHER_ALGORITHM) {
                "This encrypted backup uses an unsupported cipher."
            }
            val kdf = envelope.getString("kdf")
            require(kdf == KDF_SHA256 || kdf == KDF_SHA1) {
                "This encrypted backup uses an unsupported key derivation method."
            }
            val iterations = envelope.getInt("iterations")
            require(iterations == iterationsFor(kdf)) {
                "The encrypted backup has invalid key derivation parameters."
            }
            require(password.size >= MIN_PASSWORD_LENGTH) {
                "The backup password must contain at least $MIN_PASSWORD_LENGTH characters."
            }

            val salt = decodeBase64(envelope.getString("salt"), "salt")
            val iv = decodeBase64(envelope.getString("iv"), "initialization vector")
            val ciphertext = decodeBase64(envelope.getString("ciphertext"), "ciphertext")
            require(salt.size == SALT_BYTES && iv.size == IV_BYTES) {
                "The encrypted backup has invalid encryption parameters."
            }
            require(ciphertext.size in 16..(MAX_PLAINTEXT_BYTES + 16)) {
                "The encrypted backup payload has an invalid size."
            }

            var keyBytes: ByteArray? = null
            var clearBytes: ByteArray? = null
            try {
                val derivedKey = deriveKey(password, salt, iterations, kdf)
                keyBytes = derivedKey
                val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derivedKey, "AES"), GCMParameterSpec(TAG_BITS, iv))
                cipher.updateAAD(authenticatedHeader(kdf, iterations))
                val decryptedBytes = try {
                    cipher.doFinal(ciphertext)
                } catch (error: AEADBadTagException) {
                    throw IllegalArgumentException(
                        "Incorrect password or the backup file was modified.",
                        error
                    )
                }
                clearBytes = decryptedBytes
                require(decryptedBytes.size <= MAX_PLAINTEXT_BYTES) {
                    "Decrypted backup is larger than the supported 25 MB limit."
                }
                return String(decryptedBytes, UTF8)
            } catch (error: GeneralSecurityException) {
                throw IllegalArgumentException(
                    "Incorrect password or the backup file could not be authenticated.",
                    error
                )
            } finally {
                keyBytes?.fill(0)
                clearBytes?.fill(0)
                salt.fill(0)
                iv.fill(0)
                ciphertext.fill(0)
            }
        } finally {
            password.fill('\u0000')
        }
    }

    private fun preferredKdf(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) KDF_SHA256 else KDF_SHA1

    private fun iterationsFor(kdf: String): Int = when (kdf) {
        KDF_SHA256 -> SHA256_ITERATIONS
        KDF_SHA1 -> SHA1_ITERATIONS
        else -> throw IllegalArgumentException("Unsupported key derivation method.")
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int, kdf: String): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance(kdf).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun authenticatedHeader(kdf: String, iterations: Int): ByteArray =
        "$ENCRYPTED_FORMAT|$CRYPTO_VERSION|$kdf|$iterations|$CIPHER_ALGORITHM".toByteArray(UTF8)

    private fun decodeBase64(value: String, field: String): ByteArray =
        try {
            Base64.decode(value, Base64.DEFAULT)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("The encrypted backup $field is malformed.", error)
        }
}
