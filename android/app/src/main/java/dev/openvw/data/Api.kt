package dev.openvw.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Api {
  private const val AUTH = "https://savwconnect.com/authentication/api/"
  private const val REG = "https://savwconnect.com/registration/api/"
  private const val PA = "https://savwconnect.com/personalassist/api/"
  private const val CERENCE = "https://savwipl-connect15.in.onecloud.cerenceapi.com"
  private const val UA = "okhttp/4.12.0"

  // ---------- low level ----------
  private fun req(method: String, urlStr: String, body: String?, auth: String?): Pair<Int, String> {
    val c = (URL(urlStr).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15000
      readTimeout = 20000
      setRequestProperty("Accept", "application/json")
      setRequestProperty("User-Agent", UA)
      setRequestProperty("brand", "VW")
      if (auth != null) setRequestProperty("Authorization", if (auth.startsWith("Bearer")) auth else "Bearer $auth")
      if (body != null) {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
    }
    return try {
      if (body != null) c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
      val code = c.responseCode
      val stream = if (code in 200..299) c.inputStream else (c.errorStream ?: c.inputStream)
      val text = stream.bufferedReader().use { it.readText() }
      code to text
    } catch (e: Exception) {
      -1 to (e.message ?: "network error")
    } finally {
      c.disconnect()
    }
  }

  private fun json(text: String): JSONObject = runCatching { JSONObject(text) }.getOrDefault(JSONObject())

  /** Encrypted call: payload object -> {rqt: enc}; response {data: hex} -> decrypted object. */
  private fun encCall(url: String, payload: JSONObject, token: String?, reqKey: String, resKey: String): JSONObject {
    val body = JSONObject().put("rqt", Crypto.encHex(payload.toString(), reqKey)).toString()
    val (code, text) = req("POST", url, body, token)
    val j = json(text)
    val enc = j.optString("data").ifEmpty { j.optString("encryptedResponse").ifEmpty { j.optString("response") } }
    if (enc.isNotEmpty() && enc.matches(Regex("[0-9A-Fa-f]+"))) {
      for (k in listOf(resKey, if (resKey == Crypto.GEN) Crypto.DATA else Crypto.GEN)) {
        val p = runCatching { Crypto.decHex(enc, k) }.getOrNull()
        if (!p.isNullOrEmpty()) {
          val d = runCatching { JSONObject(p) }.getOrNull()
          if (d != null) return d.put("_status", code)
        }
      }
      return JSONObject().put("_decryptFailed", true).put("_status", code)
    }
    return j.put("_status", code)
  }

  // ---------- endpoints ----------
  suspend fun appInstallation(deviceId: String): JSONObject = withContext(Dispatchers.IO) {
    val p = JSONObject()
      .put("appDisplayVersion", "2.0.29").put("appVersionWhileInstall", 44).put("brand", "VW")
      .put("deviceID", deviceId).put("deviceName", "google").put("deviceOS", "Android")
      .put("deviceOsVersion", "14").put("deviceType", "Mobile").put("pushNotificationID", "")
    val (code, text) = req("POST", AUTH + "Installation/1.0/appInstallation", p.toString(), null)
    json(text).put("_status", code)
  }

  suspend fun authenticate(installationID: Int, signStamp: Long): JSONObject = withContext(Dispatchers.IO) {
    encCall(AUTH + "Authentication/1.0/authenticationEncryption",
      JSONObject().put("installationID", installationID).put("brand", "VW").put("signStamp", signStamp),
      null, Crypto.GEN, Crypto.GEN)
  }

  suspend fun registerUser(mobile: String, installationID: Int, token: String): JSONObject = withContext(Dispatchers.IO) {
    val p = JSONObject()
      .put("brand", "VW").put("emailID", Crypto.encHex("", Crypto.DATA)).put("installationID", installationID)
      .put("mobileNo", Crypto.encHex(mobile, Crypto.DATA)).put("sixDigitOTP", true).put("type", "Mobile")
    val (code, text) = req("POST", REG + "Registration/1.0/registerUser", p.toString(), token)
    json(text).put("_status", code)
  }

  suspend fun verifyLoginOTP(otp: String, s: Session): JSONObject = withContext(Dispatchers.IO) {
    val p = JSONObject()
      .put("otp", otp).put("installationID", s.installationID).put("type", "Mobile").put("brand", "VW")
      .put("type_Detail", Crypto.encHex(s.mobile, Crypto.DATA)).put("loginID", s.loginId)
      .put("sixDigitOTP", true).put("userID", s.userId).put("signStamp", System.currentTimeMillis())
    encCall(REG + "UserLogin/1.0/verifyLoginOTPEncryption", p, s.token, Crypto.GEN, Crypto.GEN)
  }

  suspend fun crmToken(userId: Int, token: String): JSONObject = withContext(Dispatchers.IO) {
    encCall(AUTH + "CRMToken/1.0/CRMToken", JSONObject().put("userID", userId).put("brand", "VW"), token, Crypto.GEN, Crypto.GEN)
  }

  suspend fun userInfo(s: Session): JSONObject = withContext(Dispatchers.IO) {
    val p = JSONObject().put("installationID", s.installationID).put("brand", "VW").put("userID", s.userId).put("signStamp", s.signStamp)
    encCall(PA + "UserMaster/1.0/getUserInfoEncryption", p, s.token, Crypto.GEN, Crypto.GEN)
  }

  suspend fun cerenceToken(userId: Int): JSONObject = withContext(Dispatchers.IO) {
    val (code, text) = req("POST", AUTH + "CerenceToken/1.0/CerenceToken", JSONObject().put("userID", userId).put("brand", "VW").toString(), null)
    json(text).put("_status", code)
  }

  suspend fun telematicsToken(cerenceToken: String, phone: String, vin: String): JSONObject = withContext(Dispatchers.IO) {
    val body = JSONObject().put("phone", phone).put("vins", JSONArray().put(vin)).toString()
    val (code, text) = req("POST", "$CERENCE/api/identity/v1/users/end-user/token", body, "Bearer $cerenceToken")
    json(text).put("_status", code)
  }

  suspend fun resolveCar(vin: String, teleToken: String): JSONObject = withContext(Dispatchers.IO) {
    val (code, text) = req("GET", "$CERENCE/api/v1/car?vin=$vin", null, "Bearer $teleToken")
    json(text).put("_status", code)
  }

  suspend fun telematics(carId: String, teleToken: String): JSONObject = withContext(Dispatchers.IO) {
    val (code, text) = req("GET", "$CERENCE/api/v1/car/$carId", null, "Bearer $teleToken")
    json(text).put("_status", code)
  }

  /** Trips; with [start]/[end] (epoch seconds) only that window — the server caps one query at 30 days. */
  suspend fun trips(carId: String, teleToken: String, start: Long? = null, end: Long? = null): JSONObject = withContext(Dispatchers.IO) {
    val q = if (start != null && end != null) "limit=200&start_date=$start&end_date=$end" else "limit=50"
    val (code, text) = req("GET", "$CERENCE/api/v1/car/$carId/trips?$q", null, "Bearer $teleToken")
    json(text).put("_status", code)
  }

  suspend fun events(carId: String, teleToken: String): JSONObject = withContext(Dispatchers.IO) {
    val (code, text) = req("GET", "$CERENCE/api/v1/car/$carId/events?limit=50", null, "Bearer $teleToken")
    json(text).put("_status", code)
  }

  suspend fun policies(carId: String, teleToken: String): JSONObject = withContext(Dispatchers.IO) {
    val (code, text) = req("GET", "$CERENCE/api/v1/car/$carId/policies", null, "Bearer $teleToken")
    json(text).put("_status", code)
  }
}
