package dev.openvw.data

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// DES/ECB/PKCS5, UPPERCASE hex — verified from the app's libnative-lib.so.
object Crypto {
  const val DATA = "12345678"      // ENCRYPTION_DATA  (login/OTP/owner fields)
  const val GEN = "97b1ec16"       // ENCRYPTION_REQUEST (generic rqt wrapper)

  private fun cipher(mode: Int, key: String): Cipher =
    Cipher.getInstance("DES/ECB/PKCS5Padding").apply {
      init(mode, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "DES"))
    }

  fun encHex(plain: String, key: String): String =
    cipher(Cipher.ENCRYPT_MODE, key).doFinal(plain.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02X".format(it) }

  fun decHex(hex: String, key: String): String {
    if (hex.length % 2 != 0) return ""
    val bytes = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    return String(cipher(Cipher.DECRYPT_MODE, key).doFinal(bytes), Charsets.UTF_8)
  }

  fun decOrSelf(hex: String?): String {
    if (hex.isNullOrEmpty()) return ""
    if (!hex.matches(Regex("[0-9A-Fa-f]+")) || hex.length % 2 != 0) return hex
    return runCatching { decHex(hex, DATA) }.getOrDefault(hex)
  }
}
