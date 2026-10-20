package dev.openvw.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import dev.openvw.MainActivity
import dev.openvw.R
import dev.openvw.data.WidgetData
import dev.openvw.data.WidgetSnapshot

internal enum class Kind(val defW: Int, val defH: Int) {
  FUEL(150, 150), OVERVIEW(320, 158), LARGE(320, 336), TRIP(320, 150), PARKED(320, 150), LOCK(320, 150)
}

private fun dpToPx(ctx: Context, dp: Int) = (dp * ctx.resources.displayMetrics.density).toInt()

private fun openAppIntent(ctx: Context): PendingIntent {
  val i = Intent(ctx, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }
  return PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}

private fun draw(ctx: Context, kind: Kind, wDp: Float, hDp: Float, data: WidgetSnapshot, cap: Int): Bitmap {
  var w = dpToPx(ctx, wDp.toInt().coerceIn(80, 700))
  var h = dpToPx(ctx, hDp.toInt().coerceIn(80, 700))
  // cap pixel size (keep aspect) so the bitmaps stay within RemoteViews memory limits
  val longest = maxOf(w, h)
  if (longest > cap) { val f = cap.toFloat() / longest; w = (w * f).toInt(); h = (h * f).toInt() }
  return when (kind) {
    Kind.FUEL -> WidgetRender.fuel(ctx, w, h, data)
    Kind.OVERVIEW -> WidgetRender.overview(ctx, w, h, data)
    Kind.LARGE -> WidgetRender.large(ctx, w, h, data)
    Kind.TRIP -> WidgetRender.trip(ctx, w, h, data)
    Kind.PARKED -> WidgetRender.parked(ctx, w, h, data)
    Kind.LOCK -> WidgetRender.lock(ctx, w, h, data)
  }
}

private fun views(ctx: Context, bmp: Bitmap) = RemoteViews(ctx.packageName, R.layout.widget_image).apply {
  setImageViewBitmap(R.id.widget_image, bmp)
  setOnClickPendingIntent(R.id.widget_root, openAppIntent(ctx))
}

private fun render(ctx: Context, mgr: AppWidgetManager, id: Int, kind: Kind) {
  val opts = mgr.getAppWidgetOptions(id)
  val data = WidgetData.load(ctx)

  // Android 12+: the launcher reports every size it may show the widget at (e.g. a foldable's
  // cover vs inner screen, portrait vs landscape). Draw one bitmap per size and let it pick.
  if (Build.VERSION.SDK_INT >= 31) {
    @Suppress("DEPRECATION")
    val sizes = opts.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
      ?.filter { it.width > 0 && it.height > 0 }?.distinct()?.take(4).orEmpty()
    if (sizes.isNotEmpty()) {
      val cap = if (sizes.size > 2) 800 else 1000
      mgr.updateAppWidget(id, RemoteViews(sizes.associateWith { views(ctx, draw(ctx, kind, it.width, it.height, data, cap)) }))
      return
    }
  }

  // Older launchers: portrait shows MIN width + MAX height, landscape MAX width + MIN height.
  val portrait = ctx.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
  val wDp = (if (portrait) opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
             else opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)).let { if (it <= 0) kind.defW else it }
  val hDp = (if (portrait) opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
             else opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)).let { if (it <= 0) kind.defH else it }
  mgr.updateAppWidget(id, views(ctx, draw(ctx, kind, wDp.toFloat(), hDp.toFloat(), data, 1100)))
}

abstract class BaseWidget internal constructor(private val kind: Kind) : AppWidgetProvider() {
  override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
    ids.forEach { render(ctx, mgr, it, kind) }
  }

  override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
    render(ctx, mgr, id, kind)
  }
}

class FuelWidget : BaseWidget(Kind.FUEL)
class OverviewWidget : BaseWidget(Kind.OVERVIEW)
class LargeWidget : BaseWidget(Kind.LARGE)
class LastTripWidget : BaseWidget(Kind.TRIP)
class ParkedWidget : BaseWidget(Kind.PARKED)
class LockWidget : BaseWidget(Kind.LOCK)

/** Called by the app after fresh data loads, to refresh any placed widgets. */
object OpenVWWidgets {
  fun updateAll(ctx: Context) {
    val mgr = AppWidgetManager.getInstance(ctx)
    for (cls in listOf(
      FuelWidget::class.java, OverviewWidget::class.java, LargeWidget::class.java,
      LastTripWidget::class.java, ParkedWidget::class.java, LockWidget::class.java,
    )) {
      val ids = mgr.getAppWidgetIds(ComponentName(ctx, cls))
      if (ids.isNotEmpty()) {
        val i = Intent(ctx, cls).apply {
          action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
          putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        ctx.sendBroadcast(i)
      }
    }
  }
}
