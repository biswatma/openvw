package dev.openvw.data

import android.content.Context

/** Snapshot the home-screen widgets read (written by the app after each data load). */
data class WidgetSnapshot(
  val fuelPct: Int = 34,
  val batteryV: Double = 12.4,
  val status: String = "Parked",
  val variant: String = "Taigun GT Plus",
  val tripMin: Int = 14,
  val tripDate: String = "16 Oct",
  val tripStart: String = "10:10 AM",
  val tripEnd: String = "10:24 AM",
  val tripFrom: String = "MG Road",
  val tripTo: String = "Indiranagar",
  val tripType: String = "Personal",
  val tripKm: Double = 0.0,
  val tripScore: Int = 0,
  val parkTitle: String = "MG Road",
  val parkSub: String = "Bengaluru, Karnataka",
  val parkEpoch: Long = 0,
  val parkLat: Double = 12.9756,
  val parkLon: Double = 77.6050,
  val odometerKm: Int = 10229,
  val rangeKm: Int = 0,
  val coolantC: Int = 0,
  val milOn: Boolean = false,
  val updatedAt: Long = 0,
  val colour: String = "Candy White",
)

object WidgetData {
  private fun prefs(ctx: Context) = ctx.getSharedPreferences("openvw_widget", Context.MODE_PRIVATE)

  fun save(ctx: Context, s: WidgetSnapshot) {
    prefs(ctx).edit()
      .putInt("fuelPct", s.fuelPct)
      .putFloat("batteryV", s.batteryV.toFloat())
      .putString("status", s.status)
      .putString("variant", s.variant)
      .putInt("tripMin", s.tripMin)
      .putString("tripDate", s.tripDate)
      .putString("tripStart", s.tripStart)
      .putString("tripEnd", s.tripEnd)
      .putString("tripFrom", s.tripFrom)
      .putString("tripTo", s.tripTo)
      .putString("tripType", s.tripType)
      .putFloat("tripKm", s.tripKm.toFloat())
      .putInt("tripScore", s.tripScore)
      .putString("parkTitle", s.parkTitle)
      .putString("parkSub", s.parkSub)
      .putLong("parkEpoch", s.parkEpoch)
      .putFloat("parkLat", s.parkLat.toFloat())
      .putFloat("parkLon", s.parkLon.toFloat())
      .putInt("odometerKm", s.odometerKm)
      .putInt("rangeKm", s.rangeKm)
      .putInt("coolantC", s.coolantC)
      .putBoolean("milOn", s.milOn)
      .putLong("updatedAt", s.updatedAt)
      .putString("colour", s.colour)
      .apply()
  }

  fun load(ctx: Context): WidgetSnapshot {
    val p = prefs(ctx)
    val d = WidgetSnapshot()
    return WidgetSnapshot(
      fuelPct = p.getInt("fuelPct", d.fuelPct),
      batteryV = p.getFloat("batteryV", d.batteryV.toFloat()).toDouble(),
      status = p.getString("status", d.status) ?: d.status,
      variant = p.getString("variant", d.variant) ?: d.variant,
      tripMin = p.getInt("tripMin", d.tripMin),
      tripDate = p.getString("tripDate", d.tripDate) ?: d.tripDate,
      tripStart = p.getString("tripStart", d.tripStart) ?: d.tripStart,
      tripEnd = p.getString("tripEnd", d.tripEnd) ?: d.tripEnd,
      tripFrom = p.getString("tripFrom", d.tripFrom) ?: d.tripFrom,
      tripTo = p.getString("tripTo", d.tripTo) ?: d.tripTo,
      tripType = p.getString("tripType", d.tripType) ?: d.tripType,
      tripKm = p.getFloat("tripKm", d.tripKm.toFloat()).toDouble(),
      tripScore = p.getInt("tripScore", d.tripScore),
      parkTitle = p.getString("parkTitle", d.parkTitle) ?: d.parkTitle,
      parkSub = p.getString("parkSub", d.parkSub) ?: d.parkSub,
      parkEpoch = p.getLong("parkEpoch", d.parkEpoch),
      parkLat = p.getFloat("parkLat", d.parkLat.toFloat()).toDouble(),
      parkLon = p.getFloat("parkLon", d.parkLon.toFloat()).toDouble(),
      odometerKm = p.getInt("odometerKm", d.odometerKm),
      rangeKm = p.getInt("rangeKm", d.rangeKm),
      coolantC = p.getInt("coolantC", d.coolantC),
      milOn = p.getBoolean("milOn", d.milOn),
      updatedAt = p.getLong("updatedAt", d.updatedAt),
      colour = p.getString("colour", d.colour) ?: d.colour,
    )
  }
}
