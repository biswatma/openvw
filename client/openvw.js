#!/usr/bin/env node
// OpenVW CLI — personal client for My Volkswagen Connect Plus
// Usage:
//   node openvw.js login <mobile>     # bootstrap + send OTP
//   node openvw.js otp <code>         # verify the OTP
//   node openvw.js me                 # profile
//   node openvw.js vehicle [vin]      # vehicle info
//   node openvw.js telematics [vin]   # latest telematics
//   node openvw.js raw <endpoint> '<json>'   # any endpoint, encrypted
const fs = require("fs");
const path = require("path");
const api = require("./api");

const SESSION = path.join(__dirname, ".session.json");
const load = () => { try { return JSON.parse(fs.readFileSync(SESSION, "utf8")); } catch { return {}; } };
const store = s => fs.writeFileSync(SESSION, JSON.stringify(s, null, 2));
const pretty = o => console.log(JSON.stringify(o, null, 2));

async function main() {
  const [cmd, a1, a2] = process.argv.slice(2);
  const s = load();

  if (cmd === "login") {
    if (!a1) return console.error("usage: login <mobile>");
    s.mobile = a1;
    if (!s.deviceID) s.deviceID = require("crypto").randomUUID();

    const inst = await api.appInstallation({ deviceID: s.deviceID });
    console.log("appInstallation:", inst.status, inst.data && inst.data.message);
    if (inst.data && inst.data.installationID) s.installationID = inst.data.installationID;

    const auth = await api.authenticate(s.installationID, s.signStamp || 0);
    console.log("authenticate:", auth.status, auth.data && auth.data.message);
    if (auth.data && auth.data.accessToken) {
      s.token = auth.data.accessToken;
      s.refreshToken = auth.data.refreshToken;
    }
    const reg = await api.registerUser(s.mobile, s.installationID, s.token);
    console.log("registerUser:", reg.status); pretty(reg.data);
    if (reg.data) {
      if (reg.data.loginId != null) s.loginId = reg.data.loginId;
      if (reg.data.userId != null) s.userId = reg.data.userId;
    }
    store(s);
    console.log("\n→ check your phone, then: node openvw.js otp <code>");
    return;
  }
  if (cmd === "otp") {
    if (!a1) return console.error("usage: otp <code>");
    const r = await api.verifyLoginOTP(a1, s);
    console.log("verify:", r.status); pretty(r.data);
    if (r.data && r.data.signStamp) s.signStamp = r.data.signStamp;
    store(s);
    return;
  }
  if (cmd === "me")        return pretty((await api.getUserInfo(s.installationID, s.token)).data);
  if (cmd === "vehicle")   return pretty((await api.getVehicleInfo(a1 || s.vin, s.installationID, s.token)).data);
  if (cmd === "telematics")return pretty((await api.getTelematics(a1 || s.vin, s.installationID, s.token)).data);
  if (cmd === "raw") {
    const r = await api.call(a1, JSON.parse(a2 || "{}"), s.token);
    console.log(r.status); return pretty(r.data);
  }
  console.log("commands: login <mobile> | otp <code> | me | vehicle [vin] | telematics [vin] | raw <url> '<json>'");
}
main().catch(e => console.error("ERR", e.message));
