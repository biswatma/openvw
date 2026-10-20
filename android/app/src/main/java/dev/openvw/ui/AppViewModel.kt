package dev.openvw.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.openvw.data.AlertSetup
import dev.openvw.data.Api
import dev.openvw.data.CarEvent
import dev.openvw.data.CarLook
import dev.openvw.data.LiveFeed
import dev.openvw.data.MapSnapshot
import dev.openvw.data.Parked
import dev.openvw.data.Places
import dev.openvw.data.Trip
import dev.openvw.data.Crypto
import dev.openvw.data.DriveStats
import dev.openvw.data.Dongle
import dev.openvw.data.Owner
import dev.openvw.data.Repo
import dev.openvw.data.Session
import dev.openvw.data.Store
import dev.openvw.data.Telemetry
import dev.openvw.data.VehicleInfo
import dev.openvw.data.WidgetData
import dev.openvw.data.WidgetSnapshot
import dev.openvw.widget.OpenVWWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import org.json.JSONObject

enum class Phase { BOOTING, LOGIN, OTP, LOADING, READY }

class AppViewModel(app: Application) : AndroidViewModel(app) {
  val session = Session(app)

  var phase by mutableStateOf(Phase.BOOTING); private set
  var message by mutableStateOf<String?>(null); private set
  var busy by mutableStateOf(false); private set
  var phone by mutableStateOf("")

  private val loadLock = Mutex()
  private var lastFull = 0L
  private var widgetsDue = true // widgets refresh once per app open, not on every poll

  // live push feed (SignalR) — runs only while the app is in the foreground
  private var feed: LiveFeed? = null
  private var liveWanted = false
  private var liveJob: Job? = null
  private var lastGeo: Pair<Double, Double>? = null
  private var pollQueued = false

  init {
    phone = session.mobile
    Store.privacy = session.privacy
    Store.paintPick = session.paintPick
    Store.fuelPrice = session.fuelPrice.toDouble()
    Store.dark = session.dark; dev.openvw.theme.Palette.dark = session.dark
    Store.connectedSince = session.connectedSince
    if (session.loggedIn) { phase = Phase.LOADING; refresh() } else phase = Phase.LOGIN
  }

  fun setPrivacy(b: Boolean) { session.privacy = b; Store.privacy = b }

  fun setDark(b: Boolean) { session.dark = b; Store.dark = b; dev.openvw.theme.Palette.dark = b }

  /** Manual car colour ("" = follow the account colour); widgets redraw with it right away. */
  fun setPaint(name: String) {
    session.paintPick = name; Store.paintPick = name
    runCatching { saveWidgets() }
  }

  /** Owner's average petrol price in ₹/L (0 = back to the default). */
  fun setFuelPrice(p: Double) { session.fuelPrice = p.toFloat(); Store.fuelPrice = p }

  fun startLogin(p: String) {
    val mob = p.filter { it.isDigit() }
    if (mob.length != 10) { message = "Enter a 10-digit mobile number"; return }
    busy = true; message = null
    viewModelScope.launch {
      try {
        session.mobile = mob
        val inst = Api.appInstallation(session.deviceID)
        session.installationID = inst.optInt("installationID", session.installationID)
        if (session.installationID == 0) { fail("Could not register device"); return@launch }
        val auth = Api.authenticate(session.installationID, 0)
        val tok = auth.optString("accessToken")
        if (tok.isEmpty()) { fail("Could not reach Volkswagen (try again)"); return@launch }
        session.token = tok; session.refreshToken = auth.optString("refreshToken")
        val reg = Api.registerUser(mob, session.installationID, tok)
        if (!reg.optBoolean("isSuccessful")) { fail(reg.optString("message", "Could not send OTP")); return@launch }
        session.loginId = reg.optInt("loginId"); session.userId = reg.optInt("userId")
        busy = false; phase = Phase.OTP
      } catch (e: Exception) { fail(e.message ?: "Network error") }
    }
  }

  /** Back from the OTP step to the phone-number step. */
  fun backToPhone() { busy = false; message = null; phase = Phase.LOGIN }

  fun resend() {
    viewModelScope.launch {
      runCatching { Api.registerUser(session.mobile, session.installationID, session.token) }
      message = "Code re-sent"
    }
  }

  fun verify(otp: String) {
    val code = otp.filter { it.isDigit() }
    if (code.length != 6) { message = "Enter the 6-digit code"; return }
    busy = true; message = null
    viewModelScope.launch {
      try {
        val r = Api.verifyLoginOTP(code, session)
        if (!r.optBoolean("isSuccessful")) { fail(r.optString("message", "Incorrect code")); return@launch }
        session.verified = true
        if (r.has("signStamp")) session.signStamp = r.optLong("signStamp")
        busy = false; phase = Phase.LOADING
        loadData(); phase = Phase.READY; poll()
      } catch (e: Exception) { fail(e.message ?: "Network error") }
    }
  }

  fun refresh() {
    viewModelScope.launch {
      try { loadData() } catch (_: Exception) {}
      phase = Phase.READY
      poll()
    }
  }

  /** App opened / brought to the foreground: full refresh, widgets once, then the live push feed. */
  fun onOpened() {
    widgetsDue = true; lastFull = 0L
    liveWanted = true
    poll()
    startLive()
  }

  /** App left the foreground: drop the live connection (nothing runs in the background). */
  fun onBackground() {
    liveWanted = false
    liveJob?.cancel(); liveJob = null
    feed?.let { f -> feed = null; viewModelScope.launch(Dispatchers.IO) { f.stop() } }
    Store.liveConnected = false
  }

  /** One REST refresh: live telemetry, plus trips/events if the last full load is >5 min old. */
  fun poll() {
    if (phase != Phase.READY || session.vin.isEmpty() || pollQueued) return
    pollQueued = true // waits for any running load instead of being dropped
    viewModelScope.launch {
      try {
        loadLock.withLock {
          pollQueued = false
          runCatching { loadTelematics(full = System.currentTimeMillis() - lastFull > 5 * 60_000L) }
        }
      } finally { pollQueued = false }
    }
  }

  fun refreshTelematics() { if (feed?.connected != true) poll() }

  /** Connect to the car hub; on failure retry with backoff (re-minting the token on 401). */
  private fun startLive() {
    if (!liveWanted || liveJob?.isActive == true || feed?.connected == true) return
    liveJob = viewModelScope.launch {
      var backoff = 5_000L
      while (liveWanted && isActive) {
        if (phase == Phase.READY && session.vin.isNotEmpty()) {
          // (re)mint the telematics token / resolve the car if needed
          if (session.carId.isEmpty() || session.teleToken.isEmpty()) runCatching { loadLock.withLock { loadTelematics(full = false) } }
          if (session.carId.isEmpty() || session.teleToken.isEmpty()) { delay(backoff); backoff = (backoff * 2).coerceAtMost(60_000L); continue }
          val f = LiveFeed(
            session.carId, { session.teleToken },
            onStatus = { m -> viewModelScope.launch { applyCar(m, live = true) } },
            onEvent = { m -> viewModelScope.launch { applyEvent(m) } },
            onClosed = { viewModelScope.launch { if (feed != null) { feed = null; Store.liveConnected = false; startLive() } } },
          )
          val err = withContext(Dispatchers.IO) { runCatching { f.start() }.exceptionOrNull() }
          if (err == null && liveWanted) { feed = f; Store.liveConnected = true; return@launch }
          withContext(Dispatchers.IO) { f.stop() }
          if (err?.message?.contains("401") == true) session.teleToken = "" // expired: re-mint below
          // keep the screen fresh while the hub is unreachable
          runCatching { loadLock.withLock { loadTelematics(full = false) } }
          delay(backoff); backoff = (backoff * 2).coerceAtMost(60_000L)
        } else delay(2_000) // still signing in / first load
      }
    }
  }

  fun logout() { onBackground(); session.clearAuth(); Store.live = false; phase = Phase.LOGIN }

  private fun fail(m: String) { busy = false; message = m; if (phase == Phase.LOADING) phase = if (session.verified) Phase.READY else Phase.LOGIN }

  private suspend fun loadData() {
    // fresh bootstrap token (30-min TTL)
    val auth = Api.authenticate(session.installationID, 0)
    if (auth.optString("accessToken").isNotEmpty()) {
      session.token = auth.optString("accessToken"); session.refreshToken = auth.optString("refreshToken")
    }
    // CRM token (may override bearer; often null — bootstrap token still works)
    runCatching { Api.crmToken(session.userId, session.token) }.getOrNull()?.let { crm ->
      val sfdc = crm.optString("sfdcToken")
      if (sfdc.isNotEmpty() && sfdc != "null") session.token = sfdc
    }
    // profile + vehicle
    val ui = Api.userInfo(session)
    if (!ui.optBoolean("_decryptFailed")) mapUserInfo(ui)
    // live telemetry now; trips/events/places follow in the background via poll()
    runCatching { loadLock.withLock { loadTelematics(full = false) } }
  }

  private fun saveWidgets() {
    val tr = Store.trips.firstOrNull()
    val p = Store.parked
    val t = Store.telemetry
    val kmpl = Store.drive.realKmpl.takeIf { it > 0 } ?: FuelCalc.MILEAGE
    WidgetData.save(getApplication(), WidgetSnapshot(
      fuelPct = t.fuelPct,
      batteryV = t.batteryV,
      status = Store.vehicle.status,
      variant = Store.vehicle.variant,
      tripMin = tr?.durMin ?: 0,
      tripDate = tr?.date ?: "",
      tripStart = tr?.start ?: "",
      tripEnd = tr?.end ?: "",
      tripFrom = tr?.fromTitle ?: "",
      tripTo = tr?.toTitle ?: "",
      tripType = tr?.type ?: "Personal",
      tripKm = tr?.km ?: 0.0,
      tripScore = tr?.score ?: 0,
      parkTitle = if (Store.parkedKnown) p.title else "",
      parkSub = if (Store.parkedKnown) p.sub else "",
      parkEpoch = p.epoch,
      parkLat = p.lat,
      parkLon = p.lon,
      odometerKm = t.odometerKm ?: 0,
      rangeKm = (t.fuelPct / 100.0 * FuelCalc.TANK_L * kmpl).toInt(),
      coolantC = t.coolantC ?: 0,
      milOn = t.milOn,
      updatedAt = System.currentTimeMillis(),
      colour = CarLook.effective(Store.vehicle.colour).name, // widgets run without the app's Store
    ))
    OpenVWWidgets.updateAll(getApplication())
  }

  private fun mapUserInfo(ui: JSONObject) {
    val prof = ui.optJSONObject("userInfo")
    val arr = ui.optJSONArray("vehicle")
    val v = if (arr != null && arr.length() > 0) arr.optJSONObject(0) else null
    if (v != null) {
      val vin = v.optString("vinNo").ifEmpty { v.optString("vin") }
      session.vin = vin
      session.telePhone = Crypto.decOrSelf(v.optString("telematicsMobileNo")).ifEmpty { "+91" + session.mobile }
      Store.vehicle = VehicleInfo(
        variant = v.optString("variant").ifEmpty { v.optString("telematics_Variant").ifEmpty { "Taigun GT Plus" } },
        model = v.optString("model").ifEmpty { Repo.vehicle.model },
        vehicleName = if (vin.isNotEmpty()) "Factory:$vin" else Repo.vehicle.vehicleName,
        colour = v.optString("color").ifEmpty { Repo.vehicle.colour },
        engineType = v.optString("engineNo").ifEmpty { Repo.vehicle.engineType },
        dongleImei = Repo.vehicle.dongleImei,
        dongleFirmware = Repo.vehicle.dongleFirmware,
        status = Store.vehicle.status,
      )
    }
    if (prof != null) {
      val nm = Crypto.decOrSelf(prof.optString("name")).ifEmpty { Repo.owner.name }
      Store.owner = Owner(
        name = nm,
        email = Crypto.decOrSelf(prof.optString("primaryMailID")),
        mobile = session.telePhone.ifEmpty { "+91" + session.mobile },
        address = Crypto.decOrSelf(prof.optString("communicationAddress")),
      )
    }
  }

  private suspend fun loadTelematics(full: Boolean) {
    if (session.vin.isEmpty()) return
    if (session.cerenceToken.isEmpty()) {
      val ct = Api.cerenceToken(session.userId)
      session.cerenceToken = ct.optString("cerenceToken").ifEmpty { ct.optString("CerenceToken") }
    }
    if (session.cerenceToken.isEmpty()) return
    if (session.teleToken.isEmpty()) {
      val phone = session.telePhone.ifEmpty { "+91" + session.mobile }
      val tt = Api.telematicsToken(session.cerenceToken, phone, session.vin)
      val tok = tt.optJSONObject("loginInfo")?.optJSONObject("tokenInfo")?.optString("accessToken")
        ?: tt.optString("accessToken")
      if (!tok.isNullOrEmpty()) session.teleToken = tok
      else session.cerenceToken = "" // stale CMS token: re-fetch it on the next poll
      // "connected since" = the telematics account creation date
      tt.optJSONObject("userInfo")?.optString("createDate")?.takeIf { it.isNotEmpty() }?.let {
        session.connectedSince = it; Store.connectedSince = it
      }
    }
    if (session.teleToken.isEmpty()) return
    if (session.carId.isEmpty()) {
      val rc = Api.resolveCar(session.vin, session.teleToken)
      rc.optJSONArray("cars")?.optJSONObject(0)?.let {
        session.carId = it.optString("_id").ifEmpty { it.optString("id") }
      }
    }
    if (session.carId.isEmpty()) return
    val t = Api.telematics(session.carId, session.teleToken)
    if (t.optInt("_status") == 401) { session.teleToken = ""; return }
    applyCar(t, live = false)
    if (full) { runCatching { loadActivity() }; lastFull = System.currentTimeMillis() }
    if (full && widgetsDue) { widgetsDue = false; runCatching { saveWidgets() } }
  }

  /** Car JSON (REST GET /car/{id} or a pushed "carStatus") -> Store. Missing fields keep their last value. */
  private suspend fun applyCar(t: JSONObject, live: Boolean) {
    val tj = t.optJSONObject("telemetry") ?: return
    val old = Store.telemetry
    fun gi(k: String): Int? = tj.optJSONObject(k)?.let { if (it.has("value")) it.optDouble("value").toInt() else null }
    fun gd(k: String): Double? = tj.optJSONObject(k)?.let { if (it.has("value")) it.optDouble("value") else null }
    fun gb(k: String): Boolean? = tj.optJSONObject(k)?.let { if (it.has("value")) it.optBoolean("value") else null }
    Store.telemetry = Telemetry(
      speedKmh = gi("Speed") ?: old.speedKmh,
      rpm = gi("EngineSpeed") ?: old.rpm,
      fuelPct = gi("FuelLevel") ?: old.fuelPct,
      batteryV = gd("ControlModuleVoltage") ?: old.batteryV,
      coolantC = gi("CoolantTemperature") ?: old.coolantC,
      intakeC = gi("IntakeAirTemperature") ?: old.intakeC,
      engineLoadPct = gi("EngineLoad") ?: old.engineLoadPct,
      odometerKm = gi("Odometer") ?: old.odometerKm,
      milOn = gb("IsMILOn") ?: old.milOn,
      distanceWithMil = gi("DistanceWithMIL") ?: old.distanceWithMil,
      distanceSinceDtc = gi("DistanceSinceDTCClear") ?: old.distanceSinceDtc,
      engineOn = tj.optString("engine").takeIf { it.isNotEmpty() }?.let { it == "ON" } ?: old.engineOn,
    )
    Store.vehicle = Store.vehicle.copy(status = if ((Store.telemetry.speedKmh ?: 0) > 0 || Store.telemetry.engineOn) "Driving" else "Parked")
    // dongle health
    t.optJSONArray("devices")?.optJSONObject(0)?.let { d ->
      Store.dongle = Dongle(d.optInt("netSignal"), d.optString("status"), d.optJSONObject("firmware")?.optString("version") ?: "")
    }
    // pushes carry the live position: follow the car on the maps
    if (live) t.optJSONObject("location")?.let { l ->
      val lat = l.optDouble("latitude", 0.0); val lon = l.optDouble("longitude", 0.0)
      if (lat != 0.0 && lon != 0.0) {
        val ts = l.optLong("timestamp").takeIf { it > 0 } ?: t.optLong("timestamp").takeIf { it > 0 } ?: (System.currentTimeMillis() / 1000)
        Store.parked = Store.parked.copy(lat = lat, lon = lon, epoch = ts)
        renamePlace(lat, lon)
      }
    }
    Store.live = true
  }

  /** Geocode the car's spot only after it moved ~150 m, so a drive doesn't spam the geocoder. */
  private suspend fun renamePlace(lat: Double, lon: Double) {
    val prev = lastGeo
    if (prev != null && abs(prev.first - lat) < 0.0014 && abs(prev.second - lon) < 0.0014) return
    lastGeo = lat to lon
    val (title, sub) = withContext(Dispatchers.IO) { Places.lookup(getApplication(), lat, lon) }
    Store.parked = Store.parked.copy(title = title, sub = sub)
  }

  /** Pushed "events" message (engine on/off, alarms) -> top of Updates. */
  private fun applyEvent(e: JSONObject) {
    val kind = e.optString("type")
    val title = when (kind) {
      "engine" -> if (e.optString("event") == "on") "Engine on" else "Engine off"
      else -> e.optString("name").ifEmpty { e.optString("event") }.lowercase().replaceFirstChar { it.uppercase() }
    }
    if (title.isEmpty()) return
    val p = Store.parked
    val ev = CarEvent(e.optString("id").ifEmpty { "live-${e.optLong("timestamp")}" }, kind, title,
      e.optLong("timestamp").takeIf { it > 0 } ?: (System.currentTimeMillis() / 1000), p.lat, p.lon)
    Store.events = listOf(ev) + Store.events.filter { it.id != ev.id }
    if (kind == "engine") {
      val on = title == "Engine on"
      Store.telemetry = Store.telemetry.copy(engineOn = on)
      Store.vehicle = Store.vehicle.copy(status = if (on) "Driving" else "Parked")
      // a finished drive becomes a trip on the server shortly after engine off: reload trips then
      if (!on) viewModelScope.launch { delay(90_000); lastFull = 0L; poll() }
    }
  }

  /** Trips, events, alert policies and the parked spot (Cerence). */
  private suspend fun loadActivity() {
    val ctx = getApplication<Application>()
    val now = System.currentTimeMillis() / 1000
    val weekAgo = now - 7 * 86400

    // ---- trips ----
    Api.trips(session.carId, session.teleToken).optJSONArray("trips")?.takeIf { it.length() > 0 }?.let { arr ->
      var count = 0; var scoreSum = 0; var maxSp = 0; var idle = 0; var brakes = 0; var sumKm = 0.0; var sumFuelL = 0.0
      var totKm = 0.0; var totMin = 0; var wTrips = 0; var wKm = 0.0; var wMin = 0
      val list = ArrayList<Trip>()
      for (i in 0 until arr.length()) {
        val tr = arr.optJSONObject(i) ?: continue
        count++
        scoreSum += tr.optInt("driverScore")
        maxSp = maxOf(maxSp, tr.optInt("maxSpeed"))
        idle += tr.optInt("idleTime")
        brakes += tr.optJSONObject("eventCount")?.optInt("sudden brake") ?: 0
        val odoKm = (tr.optJSONObject("endOdometer")?.optDouble("value") ?: 0.0) - (tr.optJSONObject("startOdometer")?.optDouble("value") ?: 0.0)
        val sf = tr.optJSONObject("startFuelLevel")?.optDouble("value") ?: 0.0
        val ef = tr.optJSONObject("endFuelLevel")?.optDouble("value") ?: 0.0
        val fuelL = (sf - ef) / 100.0 * FuelCalc.TANK_L
        if (odoKm > 0 && fuelL > 0) { sumKm += odoKm; sumFuelL += fuelL }

        val st = tr.optLong("startTime"); val et = tr.optLong("endTime")
        val km = tr.optDouble("distance", 0.0).let { if (it > 0) it / 1000.0 else odoKm }
        val min = (((et - st) + 30) / 60).toInt().coerceAtLeast(0)
        totKm += km; totMin += min
        if (st >= weekAgo) { wTrips++; wKm += km; wMin += min }

        val sl = tr.optJSONObject("startLocation"); val el = tr.optJSONObject("endLocation")
        suspend fun place(l: org.json.JSONObject?): Pair<String, String> {
          if (l == null) return "Unknown" to ""
          val lat = l.optDouble("latitude"); val lon = l.optDouble("longitude")
          // geocode the most recent trips; older ones use the cache or raw coordinates
          return if (list.size < 10) withContext(Dispatchers.IO) { Places.lookup(ctx, lat, lon) }
          else Places.cached(ctx, lat, lon) ?: (Places.coords(lat, lon) to "")
        }
        val (fT, fS) = place(sl); val (tT, tS) = place(el)
        list += Trip(
          id = tr.optString("id"), start = clock(st), end = clock(et),
          fromTitle = fT, fromSub = fS, toTitle = tT, toSub = tS,
          durMin = min, type = tr.optJSONArray("tags")?.optString(0)?.ifEmpty { null } ?: "Personal",
          date = day(st), km = km, score = tr.optInt("driverScore"), epoch = st,
        )
      }
      list.sortByDescending { it.epoch }
      Store.trips = list
      val real = if (sumFuelL > 0) sumKm / sumFuelL else 0.0
      Store.drive = DriveStats(count, if (count > 0) scoreSum / count else 0, maxSp, idle, brakes, real, totKm, totMin, wTrips, wKm, wMin)
    }

    // ---- events (engine on/off, alarms) + parked spot ----
    Api.events(session.carId, session.teleToken).optJSONArray("events")?.let { arr ->
      val list = ArrayList<CarEvent>()
      for (i in 0 until arr.length()) {
        val e = arr.optJSONObject(i) ?: continue
        val loc = e.optJSONObject("location")
        val kind = e.optString("type")
        val title = when (kind) {
          "engine" -> if (e.optString("event") == "on") "Engine on" else "Engine off"
          else -> e.optString("name").ifEmpty { e.optString("code") }.lowercase().replaceFirstChar { it.uppercase() }
        }
        list += CarEvent(e.optString("id"), kind, title, e.optLong("timestamp"), loc?.optDouble("latitude") ?: 0.0, loc?.optDouble("longitude") ?: 0.0)
      }
      list.sortByDescending { it.epoch }
      Store.events = list
      // name the places of the latest events (most hit the cache from trip endpoints)
      withContext(Dispatchers.IO) { list.take(15).forEach { Places.lookup(ctx, it.lat, it.lon) } }
      Store.events = list.toList()
    }
    // the telemetry endpoint has no position; the newest event (usually "engine off") marks where the car is
    Store.events.firstOrNull { it.lat != 0.0 }?.let { ev ->
      val (title, sub) = withContext(Dispatchers.IO) { Places.lookup(ctx, ev.lat, ev.lon) }
      Store.parked = Parked(ev.lat, ev.lon, ev.epoch, title, sub)
      withContext(Dispatchers.IO) { runCatching { MapSnapshot.update(ctx, ev.lat, ev.lon) } }
    }

    // ---- alert policies (geofences + enabled triggers) ----
    runCatching {
      val p = Api.policies(session.carId, session.teleToken)
      val fences = p.optJSONArray("fences")?.let { a -> (0 until a.length()).map { a.optJSONObject(it)?.optString("name")?.ifEmpty { null } ?: "Geofence ${it + 1}" } } ?: emptyList()
      val trig = p.optJSONObject("firebaseNotificationTrigger")?.let { o ->
        o.keys().asSequence().filter { o.optString(it) == "true" }.toList()
      } ?: emptyList()
      Store.alertSetup = AlertSetup(fences, trig)
    }

    runCatching { recomputePending() }
  }

  /** A drive that has ended (latest engine-off) but isn't in trip history yet — built from events. */
  private suspend fun recomputePending() {
    val ctx = getApplication<Application>()
    val off = Store.events.filter { it.kind == "engine" && it.title == "Engine off" && it.lat != 0.0 }.maxByOrNull { it.epoch }
    val newestTrip = Store.trips.firstOrNull()?.epoch ?: 0L
    if (off == null || off.epoch <= newestTrip) { Store.pendingDrive = null; return }
    val on = Store.events.filter { it.kind == "engine" && it.title == "Engine on" && it.epoch <= off.epoch && it.lat != 0.0 }.maxByOrNull { it.epoch }
    val (tT, tS) = withContext(Dispatchers.IO) { Places.lookup(ctx, off.lat, off.lon) }
    val (fT, fS) = on?.let { withContext(Dispatchers.IO) { Places.lookup(ctx, it.lat, it.lon) } } ?: ("" to "")
    val startE = on?.epoch ?: off.epoch
    Store.pendingDrive = Trip(
      id = "pending-${off.epoch}", start = clock(startE), end = clock(off.epoch),
      fromTitle = fT, fromSub = fS, toTitle = tT, toSub = tS,
      durMin = (((off.epoch - startE) + 30) / 60).toInt().coerceAtLeast(0),
      type = "Personal", date = day(startE), epoch = startE, pending = true,
    )
  }

  private fun clock(epoch: Long) = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(epoch * 1000))
  private fun day(epoch: Long) = SimpleDateFormat("d MMM", Locale.ENGLISH).format(Date(epoch * 1000))
}
