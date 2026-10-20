// OpenVW bridge — serves the UI and proxies the live My Volkswagen backend.
// The browser can't call savwconnect/Cerence directly (CORS) or do the DES
// request-wrapping, so this local Node server does it and returns UI-shaped JSON.
const http = require("http");
const fs = require("fs");
const path = require("path");
const api = require("./api");
const { desDecryptHex } = require("./crypto");

const PORT = process.env.PORT || 8787;
const SF = path.join(__dirname, ".session.json");
const HTML = path.join(__dirname, "..", "openvw.html");

const load = () => { try { return JSON.parse(fs.readFileSync(SF, "utf8")); } catch { return {}; } };
const store = s => fs.writeFileSync(SF, JSON.stringify(s, null, 2));
let S = load();

const dec = h => { try { return h && /^[0-9A-Fa-f]+$/.test(h) ? desDecryptHex(h, "12345678") : h; } catch { return h; } };
const TOKEN_TTL = 25 * 60 * 1000; // re-bootstrap before the 30-min expiry

// Ensure we hold a fresh bootstrap token (used as the personalassist bearer).
async function ensureAuth() {
  if (!S.installationID) throw new Error("not_logged_in");
  if (S.token && S.tokenAt && Date.now() - S.tokenAt < TOKEN_TTL) return;
  const a = await api.authenticate(S.installationID, S.signStamp || 0);
  if (!a.data || !a.data.accessToken) throw new Error("auth_failed");
  S.token = a.data.accessToken; S.refreshToken = a.data.refreshToken; S.tokenAt = Date.now();
  store(S);
}

function mapVehicle(v, odo) {
  if (!v) return null;
  const make = v.telematics_Make || "Volkswagen";
  const model = v.telematics_Model || (v.model || "").split(" ")[0] || "";
  const trans = /dsg/i.test(v.model || "") ? "7-speed DSG" : (/mt|manual/i.test(v.model || "") ? "Manual" : "");
  return {
    name: (make + " " + model).trim(),
    variant: v.variant || v.telematics_Variant || v.model || "",
    year: String(v.telematics_Year || v.modelyear || ""),
    plate: v.telematics_RegistrationNumber || v.registrationNo || "",
    vin: v.vinNo || v.vin || "",
    color: v.color || "",
    fuel: v.telematics_FuelType || v.fuelUsed || "Petrol",
    transmission: trans,
    odometer: odo || v.odometer || 0,
  };
}

function mapTelemetry(t) {
  if (!t) return null;
  const g = (k, d = 0) => (t[k] && typeof t[k].value !== "undefined" ? t[k].value : d);
  return {
    fuelPct: g("FuelLevel"), speed: g("Speed"), engineSpeed: g("EngineSpeed"),
    coolant: g("CoolantTemperature"), voltage: g("ControlModuleVoltage"),
    intakeTemp: g("IntakeAirTemperature"), engineLoad: g("EngineLoad"),
    odometer: g("Odometer"), milOn: g("IsMILOn", false), engine: t.engine || "OFF",
    distanceSinceDTCClear: g("DistanceSinceDTCClear"), timestamp: t.timestamp || null,
  };
}

// Full post-login telematics token chain → latest telemetry.
async function fetchTelematics() {
  if (!S.vin) return null;
  if (!S.teleToken) {
    const ct = await api.getCerenceToken(S.userId);
    const cerence = ct.data && (ct.data.cerenceToken || ct.data.CerenceToken);
    if (!cerence) return null;
    S.cerenceToken = cerence;
    const phone = "+91" + String(S.mobile).replace(/^\+?91/, "");
    const tt = await api.getTelematicsToken(cerence, phone, [S.vin]);
    const d = tt.data || {};
    const tok = (d.loginInfo && d.loginInfo.tokenInfo && d.loginInfo.tokenInfo.accessToken) || d.accessToken;
    if (!tok) return null;
    S.teleToken = tok; store(S);
  }
  if (!S.carId) {
    const rc = await api.resolveCar(S.vin, S.teleToken);
    const cars = rc.data && (rc.data.cars || rc.data.data);
    if (Array.isArray(cars) && cars[0]) { S.carId = cars[0]._id || cars[0].id; S.deviceId = cars[0].deviceId; store(S); }
  }
  if (!S.carId) return null;
  const t = await api.getTelematics(S.carId, S.teleToken);
  if (t.status === 401) { S.teleToken = null; store(S); return null; } // token expired → re-mint next call
  return (t.data && t.data.telemetry) ? mapTelemetry(t.data.telemetry) : null;
}

// Profile + vehicle (+ telemetry if available) in UI shape.
async function buildData() {
  await ensureAuth();
  await api.getCmsSfdcToken(S.userId, S.token).then(r => {
    if (r.data && r.data.sfdcToken) { S.crmToken = r.data.sfdcToken; S.token = r.data.sfdcToken; store(S); }
  }).catch(() => {});
  const ui = await api.getUserInfo(S);
  const d = (ui.data && !ui.data._decryptFailed) ? ui.data : {};
  const vehicles = d.vehicle || d.vehicleList || [];
  const v = Array.isArray(vehicles) ? vehicles[0] : null;
  if (v) { S.vin = v.vinNo || v.vin; S.vehicle = v; store(S); }
  const tel = await fetchTelematics().catch(() => null);
  const prof = d.userInfo || {};
  return {
    vehicle: mapVehicle(v, tel && tel.odometer),
    telemetry: tel,
    profile: {
      name: dec(prof.name) || S.telematicsName || "",
      email: dec(prof.primaryMailID) || "",
      phone: "+91" + String(S.mobile || "").replace(/^\+?91/, ""),
      ownerMobile: dec(v && v.ownerMobileNo) || "",
    },
    docs: v ? {
      insuranceExpiry: v.insuranceExpireDate, pucExpiry: v.puc_ExpireDate,
      warrantyEnd: v.standardWarrantyEndDate, serviceEnd: v.serviceEndDate,
    } : null,
  };
}

// ---- tiny HTTP layer ----
const send = (res, code, obj) => { res.writeHead(code, { "Content-Type": "application/json" }); res.end(JSON.stringify(obj)); };
const body = req => new Promise(r => { let b = ""; req.on("data", c => b += c); req.on("end", () => { try { r(JSON.parse(b || "{}")); } catch { r({}); } }); });

const server = http.createServer(async (req, res) => {
  const url = req.url.split("?")[0];
  try {
    if (req.method === "GET" && (url === "/" || url === "/index.html")) {
      res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" }); return res.end(fs.readFileSync(HTML));
    }
    if (req.method === "POST" && url === "/api/login") {
      const { phone } = await body(req);
      if (!phone) return send(res, 400, { error: "phone required" });
      S = load(); S.mobile = String(phone).replace(/\D/g, "");
      if (!S.deviceID) S.deviceID = require("crypto").randomUUID();
      const inst = await api.appInstallation({ deviceID: S.deviceID });
      if (inst.data && inst.data.installationID) S.installationID = inst.data.installationID;
      const a = await api.authenticate(S.installationID, 0);
      if (a.data && a.data.accessToken) { S.token = a.data.accessToken; S.refreshToken = a.data.refreshToken; S.tokenAt = Date.now(); }
      const reg = await api.registerUser(S.mobile, S.installationID, S.token);
      if (reg.data && reg.data.loginId != null) { S.loginId = reg.data.loginId; S.userId = reg.data.userId; }
      store(S);
      const ok = !!(reg.data && reg.data.isSuccessful);
      return send(res, 200, { ok, existingUser: reg.data && reg.data.existingUser, message: reg.data && reg.data.message });
    }
    if (req.method === "POST" && url === "/api/verify") {
      const { otp } = await body(req);
      if (!otp) return send(res, 400, { error: "otp required" });
      S = load();
      const r = await api.verifyLoginOTP(String(otp).replace(/\D/g, ""), S);
      const good = r.data && r.data.isSuccessful;
      if (!good) return send(res, 200, { ok: false, message: (r.data && r.data.message) || "verify failed" });
      if (r.data.signStamp) { S.signStamp = r.data.signStamp; store(S); }
      const data = await buildData();
      return send(res, 200, { ok: true, ...data });
    }
    if (req.method === "GET" && url === "/api/data") return send(res, 200, await buildData());
    if (req.method === "GET" && url === "/api/telematics") return send(res, 200, { telemetry: await fetchTelematics() });
    if (req.method === "GET" && url === "/api/session") return send(res, 200, { loggedIn: !!S.installationID && !!S.loginId, phone: S.mobile });
    send(res, 404, { error: "not found" });
  } catch (e) { send(res, 500, { error: e.message }); }
});

server.listen(PORT, () => console.log(`OpenVW bridge → http://localhost:${PORT}`));
