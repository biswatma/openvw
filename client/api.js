// OpenVW — live client for My Volkswagen Connect Plus (savwconnect.com)
// Verified from the real libnative-lib.so (arm64 split) + smali:
//   DES/ECB/PKCS5, UPPERCASE-hex bodies. Request field "encryptedRequest",
//   response field "data" (model EncryptionResponse(data=...)).
// Encryption class utils/encryption/a: g()=encrypt w/ ENCRYPTION_DATA(12345678),
//   e()=decrypt w/ ENCRYPTION_DATA(12345678), h()=encrypt & f()=decrypt w/
//   ENCRYPTION_REQUEST(97b1ec16). The auth/common/dashboard/login repositories
//   all use the ENCRYPTION_DATA(12345678) channel BOTH ways — so req and resp
//   share the same key here. (The 97b1ec16 channel is the generic o() wrapper,
//   used by a different subset we don't call.)
const { desEncryptHex, desDecryptHex, KEYS } = require("./crypto");

// Two DES channels. Which one an endpoint uses is per-endpoint (seen in smali):
//   GEN  = ENCRYPTION_REQUEST(97b1ec16): generic o()/h()/f() wrapper.
//          Bootstrap authenticationEncryption uses this BOTH ways (verified live:
//          response decrypts with 97b1ec16).
//   DATA = ENCRYPTION_DATA(12345678): login/OTP repos call g() to encrypt and
//          e() to decrypt — this channel both ways.
const GEN  = KEYS.ENCRYPTION_REQUEST;   // 97b1ec16
const DATA = KEYS.ENCRYPTION_DATA;      // 12345678

const BASE = {
  auth: "https://savwconnect.com/authentication/api/",
  reg:  "https://savwconnect.com/registration/api/",
  pa:   "https://savwconnect.com/personalassist/api/",
  cerence: "https://savwipl-connect15.in.onecloud.cerenceapi.com",
};
const UA = "okhttp/4.12.0";

function headers(token) {
  const h = { "Content-Type": "application/json", "Accept": "application/json",
    "User-Agent": UA, "brand": "VW" };
  if (token) h["Authorization"] = "Bearer " + token;
  return h;
}

// Core encrypted POST: plaintext object in, decrypted object out.
// opts.reqKey / opts.resKey select the DES channel (default GEN both ways).
async function call(url, payload, token, opts = {}) {
  const reqKey = opts.reqKey || GEN;
  const resKey = opts.resKey || GEN;
  // Wire field is "rqt" (@SerializedName on EncryptionRequest.encryptedRequest).
  const body = JSON.stringify({ rqt: desEncryptHex(JSON.stringify(payload), reqKey) });
  const res = await fetch(url, { method: "POST", headers: headers(token), body });
  const text = await res.text();
  let j; try { j = JSON.parse(text); } catch { return { status: res.status, raw: text }; }
  const enc = j.data || j.encryptedResponse || j.response;
  let data = j;
  if (enc && typeof enc === "string" && /^[0-9A-Fa-f]+$/.test(enc)) {
    // Try the declared channel, then fall back to the other (endpoint channels vary).
    for (const k of [resKey, resKey === GEN ? DATA : GEN]) {
      try { const p = desDecryptHex(enc, k); if (p) { data = JSON.parse(p); break; } } catch {}
    }
    if (data === j) data = { _decryptFailed: true, _raw: j };
  }
  return { status: res.status, data };
}

const GENCH  = { reqKey: GEN,  resKey: GEN  };   // generic 97b1ec16 channel
const DATACH = { reqKey: DATA, resKey: DATA };   // login/OTP 12345678 channel

// 0) register this app installation → server-assigned integer installationID.
//    PLAINTEXT body (not encrypted). Endpoint: Installation/1.0/appInstallation.
//    Fields + casing taken from AppInstallationRequest smali; versionCode=44.
async function appInstallation(info = {}) {
  const payload = {
    appDisplayVersion: "2.0.29",
    appVersionWhileInstall: 44,
    brand: "VW",
    deviceID: info.deviceID || "",
    deviceName: info.deviceName || "google",
    deviceOS: "Android",
    deviceOsVersion: info.deviceOsVersion || "14",
    deviceType: "Mobile",
    pushNotificationID: info.pushNotificationID || "",
  };
  const res = await fetch(BASE.auth + "Installation/1.0/appInstallation",
    { method: "POST", headers: headers(), body: JSON.stringify(payload) });
  const text = await res.text();
  let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
  return { status: res.status, data };
}

// 1) bootstrap → accessToken / refreshToken  (generic channel, verified)
async function authenticate(installationID = 0, signStamp = 0) {
  return call(BASE.auth + "Authentication/1.0/authenticationEncryption",
    { installationID, brand: "VW", signStamp }, null, GENCH);
}

// 2a) registerUser: the first login step — looks up the mobile, returns loginId
//     + existingUser, and SENDS the first OTP SMS. Endpoint Registration/1.0/
//     registerUser. Plaintext body; mobileNo + emailID DES-encrypted (DATA).
async function registerUser(mobile, installationID, token) {
  const payload = {
    brand: "VW",
    emailID: desEncryptHex("", DATA),
    installationID: Number(installationID),
    mobileNo: desEncryptHex(String(mobile), DATA),
    sixDigitOTP: true,
    type: "Mobile",
  };
  const res = await fetch(BASE.reg + "Registration/1.0/registerUser",
    { method: "POST", headers: headers(token), body: JSON.stringify(payload) });
  const text = await res.text();
  let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
  return { status: res.status, data };
}

// 2b) RE-send login OTP (after registerUser established loginId).
//    NOT the generic encrypted wrapper: resendLoginOTP (no "Encryption" suffix)
//    sends a PLAINTEXT ResendLoginOTPRequest JSON body where only mobileNo and
//    emailID are individually DES-encrypted (DATA/12345678 via g()). Auth header
//    carries the bootstrap token. Field names/types from the DTO smali.
async function sendLoginOTP(mobile, installationID, token) {
  const payload = {
    loginId: 0,
    installationID: Number(installationID),
    userId: 0,
    emailID: desEncryptHex("", DATA),
    mobileNo: desEncryptHex(String(mobile), DATA),
    type: "Mobile",
    sixDigitOTP: true,
    brand: "VW",
  };
  const res = await fetch(BASE.reg + "UserLogin/1.0/resendLoginOTP",
    { method: "POST", headers: headers(token), body: JSON.stringify(payload) });
  const text = await res.text();
  let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
  return { status: res.status, data };
}

// 3) verify OTP → user session. Generic ENCRYPTED wrapper (rqt, GEN/97b1ec16).
//    Inner JSON: type_Detail is itself DES-encrypted (DATA) with the mobile no;
//    then the whole JSON is GEN-encrypted and rqt-wrapped. Needs loginID+userID
//    from registerUser. signStamp = currentTimeMillis().
async function verifyLoginOTP(otp, s) {
  const payload = {
    otp: String(otp),
    installationID: Number(s.installationID),
    type: "Mobile",
    brand: "VW",
    type_Detail: desEncryptHex(String(s.mobile), DATA),
    loginID: Number(s.loginId),
    sixDigitOTP: true,
    userID: Number(s.userId),
    signStamp: Date.now(),
  };
  return call(BASE.reg + "UserLogin/1.0/verifyLoginOTPEncryption", payload, s.token, GENCH);
}

// 4) CRM/SFDC token. Generic GEN wrapper on the authentication base, Bearer
//    bootstrap token. Response sfdcToken becomes the bearer for personalassist.
async function getCmsSfdcToken(userID, token) {
  return call(BASE.auth + "CRMToken/1.0/CRMToken", { userID: Number(userID), brand: "VW" }, token, GENCH);
}

// 5) profile + vehicle list. personalassist base, GEN wrapper, Bearer CRM token.
async function getUserInfo(s) {
  const payload = { installationID: Number(s.installationID), brand: "VW",
    userID: Number(s.userId), signStamp: Number(s.signStamp) || 0 };
  return call(BASE.pa + "UserMaster/1.0/getUserInfoEncryption", payload, s.token, GENCH);
}

// 6) vehicle master/catalog data. personalassist base, PLAINTEXT body, Bearer.
async function getVehicleInfo(s) {
  const payload = { lastUpdatedOn: s.vehLastUpdated || "", installationID: Number(s.installationID), brand: "VW" };
  const res = await fetch(BASE.pa + "VehicleMaster/1.0/getVehicleInfo",
    { method: "POST", headers: headers(s.token), body: JSON.stringify(payload) });
  const text = await res.text(); let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
  return { status: res.status, data };
}

// ---- Telematics (Cerence) ----
// B) CMS Cerence token: PLAINTEXT, no auth header, on the authentication base.
async function getCerenceToken(userID) {
  const res = await fetch(BASE.auth + "CerenceToken/1.0/CerenceToken",
    { method: "POST", headers: headers(), body: JSON.stringify({ userID: Number(userID), brand: "VW" }) });
  const text = await res.text(); let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
  return { status: res.status, data };
}

// E-prep) exchange the CMS cerenceToken for a Cerence end-user access token.
async function getTelematicsToken(cerenceToken, phone, vins) {
  const res = await fetch(BASE.cerence + "/api/identity/v1/users/end-user/token", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json",
      "User-Agent": UA, "Authorization": "Bearer " + cerenceToken },
    body: JSON.stringify({ phone: String(phone), vins: vins || [] }),
  });
  const text = await res.text(); let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
  return { status: res.status, data };
}

// resolve the Cerence Car_Id for a VIN.
async function resolveCar(vin, teleToken) {
  const res = await fetch(BASE.cerence + "/api/v1/car?vin=" + encodeURIComponent(vin),
    { headers: { "Accept": "application/json", "User-Agent": UA, "Authorization": "Bearer " + teleToken } });
  const text = await res.text(); let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
  return { status: res.status, data };
}

// E) latest/live telemetry for a Car_Id. Authorization already "Bearer <token>".
async function getTelematics(carId, teleToken) {
  const res = await fetch(BASE.cerence + "/api/v1/car/" + encodeURIComponent(carId),
    { headers: { "Accept": "application/json", "User-Agent": UA, "Authorization": "Bearer " + teleToken } });
  const text = await res.text(); let data; try { data = JSON.parse(text); } catch { data = { raw: text }; }
  return { status: res.status, data };
}

module.exports = { call, appInstallation, authenticate, registerUser, sendLoginOTP, verifyLoginOTP,
  getCmsSfdcToken, getUserInfo, getVehicleInfo,
  getCerenceToken, getTelematicsToken, resolveCar, getTelematics, BASE };
