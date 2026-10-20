package dev.openvw.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/** Hand-offs to Google Maps (app if installed, browser otherwise). */
object GoogleMaps {
  private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

  /** Drop a pin on a coordinate (e.g. where the car is parked). */
  fun pin(ctx: Context, lat: Double, lon: Double) =
    open(ctx, "https://www.google.com/maps/search/?api=1&query=$lat,$lon")

  /** Show a place by name/address. */
  fun place(ctx: Context, query: String) =
    open(ctx, "https://www.google.com/maps/search/?api=1&query=${enc(query)}")

  /** Turn-by-turn driving directions to a place. */
  fun directions(ctx: Context, destination: String) =
    open(ctx, "https://www.google.com/maps/dir/?api=1&destination=${enc(destination)}&travelmode=driving")

  private fun open(ctx: Context, url: String) {
    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
      ctx.startActivity(Intent(i).setPackage("com.google.android.apps.maps"))
    } catch (_: ActivityNotFoundException) {
      runCatching { ctx.startActivity(i) }
    }
  }
}
