package com.example

import com.example.data.backup.FinanceBackupCryptography
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FinanceBackupCryptographyTest {
    @Test
    fun encryptedBackupDoesNotExposeFinancialTextAndRoundTrips() {
        val plaintext = """{"format":"budget-tracker-finance-backup","description":"salary R 45 000.00"}"""
        val encrypted = FinanceBackupCryptography.encrypt(plaintext, "correct horse battery".toCharArray())

        assertTrue(FinanceBackupCryptography.isEncryptedBackup(encrypted))
        assertFalse(encrypted.contains("salary"))
        assertFalse(encrypted.contains("45 000"))
        assertEquals(plaintext, FinanceBackupCryptography.decrypt(encrypted, "correct horse battery".toCharArray()))
    }

    @Test
    fun wrongPasswordIsRejectedBeforePlaintextCanBeUsed() {
        val encrypted = FinanceBackupCryptography.encrypt(
            """{"private":"transaction details"}""",
            "correct horse battery".toCharArray()
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            FinanceBackupCryptography.decrypt(encrypted, "wrong password value".toCharArray())
        }
        assertTrue(error.message.orEmpty().contains("Incorrect password"))
    }

    @Test
    fun modifiedCiphertextFailsAuthentication() {
        val encrypted = FinanceBackupCryptography.encrypt(
            """{"private":"financial details"}""",
            "correct horse battery".toCharArray()
        )
        val envelope = JSONObject(encrypted)
        val payload = envelope.getString("ciphertext")
        val replacement = if (payload.first() == 'A') 'B' else 'A'
        envelope.put("ciphertext", replacement + payload.drop(1))

        assertThrows(IllegalArgumentException::class.java) {
            FinanceBackupCryptography.decrypt(envelope.toString(), "correct horse battery".toCharArray())
        }
    }

    @Test
    fun rejectsWeakPasswords() {
        assertThrows(IllegalArgumentException::class.java) {
            FinanceBackupCryptography.encrypt("""{"data":1}""", "short-pass".toCharArray())
        }
    }
}
