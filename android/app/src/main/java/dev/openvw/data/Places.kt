package dev.openvw.data

import android.content.Context
import android.location.Geocoder
import java.util.Locale

/** Reverse geocoding (Android Geocoder) with a persistent cache, so trips/parking get place names. */
object Places {
  private val plusCode = Regex("^[A-Z0-9]{4,8}\\+[A-Z0-9]{2,3}$")
  private val houseNo = Regex("^[0-9][0-9A-Za-z/-]{0,7}$")

  private fun key(lat: Double, lon: Double) = "%.4f,%.4f".format(Locale.US, lat, lon)

  fun coords(lat: Double, lon: Double) = "%.4f, %.4f".format(Locale.US, lat, lon)

  /** Cached name only (never blocks). */
  fun cached(ctx: Context, lat: Double, lon: Double): Pair<String, String>? =
    ctx.getSharedPreferences("openvw_places", Context.MODE_PRIVATE).getString(key(lat, lon), null)
      ?.split("|", limit = 2)?.let { it[0] to it.getOrElse(1) { "" } }

  /** (title, subtitle) for a coordinate. Blocking — call off the main thread. */
  @Suppress("DEPRECATION")
  fun lookup(ctx: Context, lat: Double, lon: Double): Pair<String, String> {
    cached(ctx, lat, lon)?.let { return it }
    val fallback = coords(lat, lon) to ""
    if (!Geocoder.isPresent()) return fallback
    val line = runCatching { Geocoder(ctx, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()?.getAddressLine(0) }
      .getOrNull() ?: return fallback
    val parts = line.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    // drop leading plus codes and bare house numbers ("2794", "8/8/A")
    while (parts.size > 1 && (plusCode.matches(parts[0]) || houseNo.matches(parts[0]))) parts.removeAt(0)
    if (parts.isEmpty()) return fallback
    // "VMRF+WHG Some Place" -> "Some Place"
    val head = parts[0].substringBefore(" ")
    val title = if (plusCode.matches(head) && parts[0].length > head.length) parts[0].substring(head.length).trim() else parts[0]
    val sub = parts.drop(1).joinToString(", ")
    ctx.getSharedPreferences("openvw_places", Context.MODE_PRIVATE).edit().putString(key(lat, lon), "$title|$sub").apply()
    return title to sub
  }
}
