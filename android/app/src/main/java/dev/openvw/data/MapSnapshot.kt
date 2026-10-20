package dev.openvw.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Static map image around the parked spot, stitched from Esri light-gray tiles, for the widgets
 * (widgets can't host a live map). Saved to filesDir/park_map.png, centred on the car.
 */
object MapSnapshot {
  private const val Z = 16
  private const val W = 1024
  private const val H = 512
  private const val URL_BASE = "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/"

  fun file(ctx: Context) = File(ctx.filesDir, "park_map.png")

  /** Re-render only when the spot moved. Blocking — call off the main thread. */
  fun update(ctx: Context, lat: Double, lon: Double) {
    val prefs = ctx.getSharedPreferences("openvw_widget", Context.MODE_PRIVATE)
    val key = "%.5f,%.5f".format(java.util.Locale.US, lat, lon)
    if (prefs.getString("mapKey", "") == key && file(ctx).exists()) return

    val n = 1 shl Z
    val px = (lon + 180.0) / 360.0 * n * 256
    val latR = lat * PI / 180
    val py = (1 - ln(tan(latR) + 1 / cos(latR)) / PI) / 2 * n * 256
    val left = px - W / 2; val top = py - H / 2

    val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawColor(0xFFECEAE4.toInt())
    var ok = 0; var total = 0
    for (tx in floor(left / 256).toInt()..floor((left + W) / 256).toInt()) {
      for (ty in floor(top / 256).toInt()..floor((top + H) / 256).toInt()) {
        total++
        val tile = fetch("$URL_BASE$Z/$ty/$tx") ?: continue
        c.drawBitmap(tile, (tx * 256 - left).toFloat(), (ty * 256 - top).toFloat(), null)
        ok++
      }
    }
    if (ok == 0) return
    file(ctx).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    if (ok == total) prefs.edit().putString("mapKey", key).apply() // else retry missing tiles next refresh
  }

  private fun fetch(u: String): Bitmap? = runCatching {
    val c = (URL(u).openConnection() as HttpURLConnection).apply {
      connectTimeout = 10000; readTimeout = 15000
      setRequestProperty("User-Agent", "OpenVW-Taigun/1.0")
    }
    try { if (c.responseCode == 200) c.inputStream.use { BitmapFactory.decodeStream(it) } else null } finally { c.disconnect() }
  }.getOrNull()
}
