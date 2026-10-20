// OpenVW bootstrap test: appInstallation -> authenticate (no OTP sent).
// Two live requests, no loops. Verifies the full pre-login chain.
const fs = require("fs"), crypto = require("crypto"), api = require("./api");
const SESSION = ".session.json";
const load = () => { try { return JSON.parse(fs.readFileSync(SESSION, "utf8")); } catch { return {}; } };
const save = s => fs.writeFileSync(SESSION, JSON.stringify(s, null, 2));

(async () => {
  const s = load();
  if (!s.deviceID) s.deviceID = crypto.randomUUID();

  const inst = await api.appInstallation({ deviceID: s.deviceID, deviceName: "google", deviceOsVersion: "14" });
  console.log("appInstallation:", inst.status, JSON.stringify(inst.data));
  const id = inst.data && inst.data.installationID;
  if (!id) { console.log("-> no installationID returned; stopping."); save(s); return; }
  s.installationID = id;

  const auth = await api.authenticate(s.installationID, s.signStamp || 0);
  console.log("authenticate:", auth.status, JSON.stringify(auth.data));
  if (auth.data) {
    if (auth.data.accessToken) s.token = auth.data.accessToken;
    if (auth.data.refreshToken) s.refreshToken = auth.data.refreshToken;
    if (auth.data.signStamp) s.signStamp = auth.data.signStamp;
  }
  save(s);
  console.log(s.token ? "\n*** BOOTSTRAP OK — token acquired ***" : "\n(no token yet)");
})().catch(e => console.error("ERR", e.message));
