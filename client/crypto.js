// OpenVW crypto — mirrors com.vwindia...utils.encryption.a
// method a(): DES/ECB/PKCS5Padding, key=ENCRYPTION_DATA, output = UPPERCASE HEX (alphabet 0123456789ABCDEF)
// Keys VERIFIED from the real libnative-lib.so (arm64 split APK) via ghidra:
// SecureNative.getSecureKey() returns plain .rodata literals from an inline
// std::unordered_map (no obfuscation).
const CryptoJS = require("crypto-js");

const KEYS = {
  ENCRYPTION_DATA: "12345678",      // DES key — login/OTP/auth req+resp channel
  ENCRYPTION_REQUEST: "97b1ec16",   // DES key — generic o() wrapper channel
};

function desEncryptHex(plaintext, key = KEYS.ENCRYPTION_DATA) {
  const k = CryptoJS.enc.Utf8.parse(key);
  const enc = CryptoJS.DES.encrypt(CryptoJS.enc.Utf8.parse(plaintext), k, {
    mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7,
  });
  return enc.ciphertext.toString(CryptoJS.enc.Hex).toUpperCase();
}
function desDecryptHex(hex, key = KEYS.ENCRYPTION_DATA) {
  const k = CryptoJS.enc.Utf8.parse(key);
  const cipherParams = CryptoJS.lib.CipherParams.create({
    ciphertext: CryptoJS.enc.Hex.parse(hex),
  });
  const dec = CryptoJS.DES.decrypt(cipherParams, k, {
    mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7,
  });
  return dec.toString(CryptoJS.enc.Utf8);
}
module.exports = { KEYS, desEncryptHex, desDecryptHex };

if (require.main === module) {
  const samples = ['{"mobileNumber":"9999999999"}', "9999999999", "hello"];
  for (const s of samples) {
    const e = desEncryptHex(s), back = desDecryptHex(e);
    console.log(back === s ? "OK " : "FAIL", JSON.stringify(s), "->", e.slice(0,32)+(e.length>32?"…":""), "->", JSON.stringify(back));
  }
}
