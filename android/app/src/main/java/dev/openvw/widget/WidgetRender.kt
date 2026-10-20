package dev.openvw.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.PathParser
import dev.openvw.R
import dev.openvw.data.WidgetSnapshot

/** Draws the OpenVW widgets as bitmaps so they match the design exactly. */
object WidgetRender {
  private const val INK = 0xFF0E0E10.toInt()
  private const val MUTED = 0xFF6B6B70.toInt()
  private const val WHITE = 0xFFFFFFFF.toInt()
  private const val TRACK = 0xFFEFEDE8.toInt()
  private const val GREEN = 0xFF12A06B.toInt()
  private const val RED = 0xFFE5484D.toInt()
  private const val AMBER = 0xFFD98A00.toInt()
  private const val ACCENT = 0xFFE8562A.toInt()
  private const val FIELD = 0xFFF7F6F2.toInt()
  private const val LINE = 0xFFECEAE5.toInt()

  private val bold = Typeface.create("sans-serif", Typeface.BOLD)
  private val black = Typeface.create("sans-serif-black", Typeface.NORMAL)
  private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)

  private fun fuelColor(p: Int) = if (p <= 15) RED else if (p <= 25) AMBER else GREEN

  private fun text(size: Float, tf: Typeface, color: Int, align: Paint.Align = Paint.Align.LEFT) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = tf; this.color = color; textAlign = align }

  private fun icon(canvas: Canvas, d: String, x: Float, y: Float, size: Float, color: Int, stroke: Float) {
    val p = PathParser.createPathFromPathData(d) ?: return
    val m = Matrix().apply { setScale(size / 24f, size / 24f); postTranslate(x, y) }
    p.transform(m)
    canvas.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE; this.color = color; strokeWidth = stroke
      strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    })
  }

  private fun drawCard(canvas: Canvas, r: RectF, radius: Float, density: Float) {
    canvas.drawRoundRect(r, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = WHITE; setShadowLayer(14f * density, 0f, 8f * density, 0x33000000)
    })
  }

  // ---------- FUEL (small) ----------
  fun fuel(ctx: Context, w: Int, h: Int, s: WidgetSnapshot): Bitmap {
    val d = ctx.resources.displayMetrics.density
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val pad = 6f * d
    val card = RectF(pad, pad, w - pad, h - pad)
    drawCard(c, card, 24f * d, d)

    val fc = fuelColor(s.fuelPct)
    val left = card.left + 16f * d
    icon(c, "M5 20V5a1.5 1.5 0 0 1 1.5-1.5h6A1.5 1.5 0 0 1 14 5v15M3.5 20h12 M5 10h9 M14 8.5l3 2.5v6a1.5 1.5 0 0 0 3 0V9l-2.5-2.5",
      left, card.top + 14f * d, 15f * d, fc, 2f * d)
    c.drawText("Fuel", left + 20f * d, card.top + 26f * d, text(13f * d, bold, INK))
    c.drawText(s.status, card.right - 16f * d, card.top + 25f * d, text(11f * d, medium, MUTED, Paint.Align.RIGHT))

    // ring (270°, start 135)
    val cx = card.centerX()
    val cy = card.top + (card.height() * 0.6f)
    val rr = minOf(card.width(), card.height()) * 0.30f
    val ring = RectF(cx - rr, cy - rr, cx + rr, cy + rr)
    val stroke = 10f * d
    val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND; color = TRACK }
    c.drawArc(ring, 135f, 270f, false, base)
    base.color = fc
    c.drawArc(ring, 135f, 270f * (s.fuelPct / 100f).coerceIn(0f, 1f), false, base)
    c.drawText(s.fuelPct.toString(), cx, cy + 6f * d, text(30f * d, black, INK, Paint.Align.CENTER))
    c.drawText("% of tank", cx, cy + 22f * d, text(10f * d, medium, MUTED, Paint.Align.CENTER))
    return bmp
  }

  // ---------- OVERVIEW (medium) ----------
  // Adapts to the size the launcher gives it: content scales with the widget, the stats column is
  // centred vertically and the car fills the space under the name (no empty gaps when resized taller).
  fun overview(ctx: Context, w: Int, h: Int, s: WidgetSnapshot): Bitmap {
    val d = ctx.resources.displayMetrics.density
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val pad = 6f * d
    val card = RectF(pad, pad, w - pad, h - pad)
    drawCard(c, card, 24f * d, d)
    val u = scaleFor(card, d, 320f, 158f)

    val fc = fuelColor(s.fuelPct)
    val lx = card.left + 16f * u
    val colRight = card.left + card.width() * 0.47f
    val topY = card.top + 24f * u
    c.drawText(fit(s.variant, text(15f * u, bold, INK), colRight - lx), lx, topY, text(15f * u, bold, INK))

    // status pill
    val pillY = topY + 10f * u
    val pillW = 16f * u + text(10f * u, bold, WHITE).measureText(s.status.uppercase())
    val pill = RectF(lx, pillY, lx + pillW, pillY + 20f * u)
    c.drawRoundRect(pill, 10f * u, 10f * u, fill(INK))
    c.drawCircle(pill.left + 10f * u, pill.centerY(), 2.6f * u, fill(ACCENT))
    c.drawText(s.status.uppercase(), pill.left + 16f * u, pill.centerY() + 3.5f * u, text(9f * u, bold, WHITE))

    // car: fit into the left column below the pill, bottom-aligned
    carImage(ctx, s)?.let { car ->
      val area = RectF(lx - 4f * u, pill.bottom + 6f * u, colRight, card.bottom - 10f * u)
      if (area.height() > 20f * u) {
        val sc = minOf(area.width() / car.width, area.height() / car.height)
        val cw = car.width * sc; val ch = car.height * sc
        val x0 = area.centerX() - cw / 2
        c.drawBitmap(car, null, RectF(x0, area.bottom - ch, x0 + cw, area.bottom), Paint(Paint.FILTER_BITMAP_FLAG))
      }
    }

    // right column: bars + last trip, centred vertically
    val rx = card.left + card.width() * 0.5f
    val rRight = card.right - 16f * u
    val blockH = 84f * u // fuel row → last-trip baseline, cap to descent
    var y = maxOf(card.top + 30f * u, card.centerY() - blockH / 2 + 11f * u)
    y = bar(c, u, rx, rRight, y, "Fuel", "${s.fuelPct}%", s.fuelPct / 100f, fc)
    y += 14f * u
    val battFrac = ((s.batteryV - 10.5) / (14.8 - 10.5)).toFloat().coerceIn(0f, 1f)
    y = bar(c, u, rx, rRight, y, "Battery", "${trim(s.batteryV)} V", battFrac, GREEN)
    y += 16f * u
    c.drawLine(rx, y, rRight, y, Paint().apply { color = LINE; strokeWidth = 1f * u })
    y += 16f * u
    c.drawText("Last trip", rx, y, text(11f * u, medium, MUTED))
    c.drawText("${s.tripMin} min · ${s.tripDate}", rRight, y, text(11f * u, bold, INK, Paint.Align.RIGHT))
    return bmp
  }

  private fun bar(c: Canvas, d: Float, x: Float, right: Float, y: Float, label: String, value: String, frac: Float, color: Int): Float {
    c.drawText(label, x, y, text(11f * d, medium, MUTED))
    c.drawText(value, right, y, text(11f * d, bold, INK, Paint.Align.RIGHT))
    val by = y + 7f * d
    val h = 5f * d
    val track = RectF(x, by, right, by + h)
    c.drawRoundRect(track, h / 2, h / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = TRACK })
    val vw = x + (right - x) * frac.coerceIn(0f, 1f)
    if (vw > x) c.drawRoundRect(RectF(x, by, vw, by + h), h / 2, h / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    return by + h
  }

  private fun trim(v: Double) = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

  // ---------- shared bits for the larger widgets (design px == dp, scaled by k) ----------
  private const val STRIP = 0xFFF7F6F2.toInt()
  private const val PILL_BG = 0xFFF5F4F0.toInt()
  private const val PILL_LINE = 0xFFE6E4DF.toInt()
  private const val TIMELINE = 0xFFD6D3CB.toInt()
  private const val ROUTE = "M8 19h7a3.5 3.5 0 0 0 0-7H9a3.5 3.5 0 0 1 0-7h7 M4 19a2 2 0 1 0 4 0 2 2 0 1 0-4 0z M16 5a2 2 0 1 0 4 0 2 2 0 1 0-4 0z"
  private const val CAR = "M5 16v2.5M19 16v2.5 M3.5 16V12.5l2-5A2 2 0 0 1 7.4 6h9.2a2 2 0 0 1 1.9 1.5l2 5V16z M3.5 12.5h17"
  private const val ARROW = "M20 4L4 11l7 2 2 7z"
  private const val BATTERY = "M5 7h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z M7 7V5h3v2M14 7V5h3v2"
  private const val FUEL = "M5 20V5a1.5 1.5 0 0 1 1.5-1.5h6A1.5 1.5 0 0 1 14 5v15M3.5 20h12 M5 10h9 M14 8.5l3 2.5v6a1.5 1.5 0 0 0 3 0V9l-2.5-2.5"
  private val heavy = Typeface.create("sans-serif", Typeface.BOLD)

  private fun fit(t: String, p: Paint, maxW: Float): String =
    android.text.TextUtils.ellipsize(t, android.text.TextPaint(p), maxW, android.text.TextUtils.TruncateAt.END).toString()

  private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

  private fun scaleFor(card: RectF, d: Float, dw: Float, dh: Float) =
    (minOf(card.width() / (dw * d), card.height() / (dh * d))).coerceIn(0.7f, 1.3f) * d

  private fun statusPill(c: Canvas, u: Float, right: Float, cy: Float, status: String) {
    val tp = text(11f * u, bold, WHITE)
    val w = 10f * u + 6f * u + 6f * u + tp.measureText(status.uppercase()) + 10f * u
    val r = RectF(right - w, cy - 12f * u, right, cy + 12f * u)
    c.drawRoundRect(r, 12f * u, 12f * u, fill(INK))
    c.drawCircle(r.left + 13f * u, cy, 3f * u, fill(ACCENT))
    c.drawText(status.uppercase(), r.left + 22f * u, cy + 4f * u, tp)
  }

  private fun darkButton(c: Canvas, u: Float, r: RectF, label: String, badge: Boolean) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = android.graphics.LinearGradient(0f, r.top, 0f, r.bottom, 0xFF2C2C31.toInt(), INK, android.graphics.Shader.TileMode.CLAMP)
    }
    c.drawRoundRect(r, r.height() / 2, r.height() / 2, p)
    val tp = text(12f * u, medium, WHITE)
    val tw = tp.measureText(label)
    if (badge) {
      // 24dp accent circle with the arrow, then label (Parked widget)
      val bx = r.left + 6f * u + 12f * u
      c.drawCircle(bx, r.centerY(), 12f * u, fill(ACCENT))
      icon(c, ARROW, bx - 6f * u, r.centerY() - 6f * u, 12f * u, WHITE, 2.4f * u / 2f)
      c.drawText(label, bx + 12f * u + 6f * u, r.centerY() + 4f * u, tp)
    } else {
      val total = 13f * u + 6f * u + tw
      val x0 = r.centerX() - total / 2
      icon(c, ARROW, x0, r.centerY() - 6.5f * u, 13f * u, ACCENT, 2.2f * 13f / 24f * u)
      c.drawText(label, x0 + 19f * u, r.centerY() + 4f * u, tp)
    }
  }

  /** Two-row trip timeline: times | dots | places. */
  private fun timeline(c: Canvas, u: Float, x: Float, right: Float, row1: Float, row2: Float, s: WidgetSnapshot) {
    val tp = text(11f * u, bold, INK)
    val pp = text(12f * u, medium, INK)
    val dx = x + 62f * u + 6f * u
    val tx = x + 82f * u
    c.drawText(s.tripStart, x, row1 + 11f * u, tp)
    c.drawText(s.tripEnd, x, row2 + 11f * u, tp)
    c.drawRect(dx - 0.75f * u, row1 + 14f * u, dx + 0.75f * u, row2 + 0f * u, fill(TIMELINE))
    c.drawCircle(dx, row1 + 7f * u, 3f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * u; color = INK })
    c.drawCircle(dx, row2 + 7f * u, 4f * u, fill(INK))
    c.drawText(fit(s.tripFrom, pp, right - tx), tx, row1 + 11.5f * u, pp)
    c.drawText(fit(s.tripTo, pp, right - tx), tx, row2 + 11.5f * u, pp)
  }

  private fun carImage(ctx: Context, s: WidgetSnapshot) = BitmapFactory.decodeResource(ctx.resources, dev.openvw.data.CarLook.image(s.colour))

  // ---------- LARGE (4x4) ----------
  fun large(ctx: Context, w: Int, h: Int, s: WidgetSnapshot): Bitmap {
    val d = ctx.resources.displayMetrics.density
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val card = RectF(6f * d, 6f * d, w - 6f * d, h - 6f * d)
    drawCard(c, card, 26f * d, d)
    val u = scaleFor(card, d, 364f, 382f)
    val L = card.left + 18f * u; val R = card.right - 18f * u; val T = card.top

    // header
    c.drawText(fit(s.variant, text(17f * u, bold, INK), R - L - 110f * u), L, T + 31f * u, text(17f * u, bold, INK))
    c.drawText("1.5L TSI · DSG", L, T + 47f * u, text(11f * u, medium, MUTED))
    statusPill(c, u, R, T + 33f * u, s.status)

    // vertical budget: header 55 | car (flexible) | strip 56 | last trip 84 | buttons 52.
    // The car absorbs height changes; anything beyond its max is shared out as spacing.
    val fixed = (55f + 56f + 84f + 52f) * u
    val avail = card.height() - fixed
    val carH = avail.coerceIn(0f, 190f * u)
    val gap = ((avail - carH) / 3f).coerceAtLeast(0f)
    val carTop = T + 55f * u + gap
    val stripTop = carTop + carH
    carImage(ctx, s)?.takeIf { carH > 40f * u }?.let { car ->
      val sc = minOf(236f * u / car.width, (carH - 10f * u) / car.height)
      val cw = car.width * sc; val ch = car.height * sc
      val cx = card.centerX()
      val sw = cw * 0.48f
      c.drawOval(RectF(cx - sw, stripTop - 22f * u, cx + sw, stripTop - 6f * u), Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.RadialGradient(cx, stripTop - 14f * u, sw, 0x330E0E10, 0x000E0E10, android.graphics.Shader.TileMode.CLAMP)
      })
      c.drawBitmap(car, null, RectF(cx - cw / 2, stripTop - 8f * u - ch, cx + cw / 2, stripTop - 8f * u), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    // stats strip
    val strip = RectF(L, stripTop, R, stripTop + 56f * u)
    c.drawRoundRect(strip, 16f * u, 16f * u, fill(STRIP))
    val colW = strip.width() / 3
    val cols = listOf(
      Triple("Fuel", s.fuelPct.toString(), "%") to fuelColor(s.fuelPct),
      Triple("Battery", trim(s.batteryV), " V") to INK,
      Triple("Range", if (s.rangeKm > 0) s.rangeKm.toString() else "--", " km") to INK,
    )
    cols.forEachIndexed { i, (t, col) ->
      val cx = strip.left + colW * i + colW / 2
      if (i > 0) c.drawRect(strip.left + colW * i - 0.5f * u, strip.top + 10f * u, strip.left + colW * i + 0.5f * u, strip.bottom - 10f * u, fill(LINE))
      c.drawText(t.first, cx, strip.top + 20f * u, text(10f * u, medium, MUTED, Paint.Align.CENTER))
      val vp = text(18f * u, black, col); val up = text(11f * u, medium, MUTED)
      val vw = vp.measureText(t.second); val uw = up.measureText(t.third)
      val x0 = cx - (vw + uw) / 2
      c.drawText(t.second, x0, strip.top + 42f * u, vp)
      c.drawText(t.third, x0 + vw, strip.top + 42f * u, up)
    }

    // last trip
    val ltY = strip.bottom + gap + 24f * u
    c.drawText("Last trip", L, ltY, text(12f * u, bold, INK))
    val meta = listOf(s.tripDate, "${s.tripMin} min", if (s.tripKm > 0) "%.1f km".format(s.tripKm) else "").filter { it.isNotEmpty() }.joinToString(" · ")
    c.drawText(meta, R, ltY, text(11f * u, medium, MUTED, Paint.Align.RIGHT))
    timeline(c, u, L, R, ltY + 7f * u, ltY + 29f * u, s)

    // buttons (bottom)
    val by = card.bottom - 16f * u
    val half = (R - L - 8f * u) / 2
    darkButton(c, u, RectF(L, by - 36f * u, L + half, by), "Locate car", false)
    val r2 = RectF(R - half, by - 36f * u, R, by)
    c.drawRoundRect(r2, 18f * u, 18f * u, fill(PILL_BG))
    c.drawRoundRect(RectF(r2.left + 0.5f * u, r2.top + 0.5f * u, r2.right - 0.5f * u, r2.bottom - 0.5f * u), 18f * u, 18f * u,
      Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * u; color = PILL_LINE })
    c.drawText("All trips", r2.centerX(), r2.centerY() + 4f * u, text(12f * u, medium, INK, Paint.Align.CENTER))
    return bmp
  }

  // ---------- LAST TRIP (4x2) ----------
  fun trip(ctx: Context, w: Int, h: Int, s: WidgetSnapshot): Bitmap {
    val d = ctx.resources.displayMetrics.density
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val card = RectF(6f * d, 6f * d, w - 6f * d, h - 6f * d)
    drawCard(c, card, 26f * d, d)
    val u = scaleFor(card, d, 364f, 170f)
    val L = card.left + 18f * u; val R = card.right - 18f * u; val T = card.top + 14f * u

    icon(c, ROUTE, L, T + 3.5f * u, 15f * u, INK, 2f * 15f / 24f * u)
    c.drawText("Last trip", L + 21f * u, T + 15f * u, text(13f * u, bold, INK))
    val pp = text(10f * u, bold, INK)
    val pw = pp.measureText(s.tripType) + 18f * u
    val pill = RectF(R - pw, T, R, T + 22f * u)
    c.drawRoundRect(pill, 11f * u, 11f * u, fill(PILL_BG))
    c.drawRoundRect(pill, 11f * u, 11f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * u; color = PILL_LINE })
    c.drawText(s.tripType, pill.centerX() - pp.measureText(s.tripType) / 2, pill.centerY() + 3.5f * u, pp)

    val big = text(30f * u, black, INK).apply { letterSpacing = -0.03f }
    val by = T + 58f * u
    c.drawText(s.tripMin.toString(), L, by, big)
    val meta = listOf("min", s.tripDate, if (s.tripKm > 0) "%.1f km".format(s.tripKm) else "").filter { it.isNotEmpty() }.joinToString(" · ")
    c.drawText(meta, L + big.measureText(s.tripMin.toString()) + 6f * u, by, text(12f * u, medium, MUTED))

    val B = card.bottom
    timeline(c, u, L, R, B - 52f * u, B - 30f * u, s)
    return bmp
  }

  // ---------- PARKED LOCATION (4x2) ----------
  fun parked(ctx: Context, w: Int, h: Int, s: WidgetSnapshot): Bitmap {
    val d = ctx.resources.displayMetrics.density
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val card = RectF(6f * d, 6f * d, w - 6f * d, h - 6f * d)
    drawCard(c, card, 26f * d, d)
    val u = scaleFor(card, d, 364f, 170f)

    c.save()
    c.clipPath(Path().apply { addRoundRect(card, 26f * d, 26f * d, Path.Direction.CW) })
    c.drawColor(0xFFECEAE4.toInt())
    val map = dev.openvw.data.MapSnapshot.file(ctx).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
    if (map != null) {
      // centre-crop: the car is the centre of the snapshot
      val sc = maxOf(card.width() / map.width, card.height() / map.height)
      val mw = map.width * sc; val mh = map.height * sc
      c.drawBitmap(map, null, RectF(card.centerX() - mw / 2, card.centerY() - mh / 2, card.centerX() + mw / 2, card.centerY() + mh / 2), Paint(Paint.FILTER_BITMAP_FLAG))
    } else {
      // stylised street grid from the design (offline fallback)
      c.translate(card.left, card.top); c.scale(card.width() / 364f, card.height() / 170f)
      c.drawRoundRect(RectF(30f, 18f, 100f, 58f), 6f, 6f, fill(0xFFE3E0D9.toInt()))
      c.drawRoundRect(RectF(240f, 104f, 300f, 150f), 6f, 6f, fill(0xFFDCE5D6.toInt()))
      fun road(path: String, col: Int, wd: Float) = c.drawPath(PathParser.createPathFromPathData(path), Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = col; strokeWidth = wd })
      road("M-10 140 L380 10", WHITE, 16f)
      road("M0 86 H364 M150 0 V170 M260 0 V170 M60 0 V170", 0xFFF8F7F3.toInt(), 7f)
      road("M0 36 H140 M100 86 V170 M200 86 V150 M300 50 H364", 0xFFF3F1EC.toInt(), 3f)
    }
    c.restore()

    // car pin with accent halo at the centre
    val px = card.centerX(); val py = card.centerY()
    c.drawCircle(px, py, 28f * u, fill(0x29E8562A))
    c.drawCircle(px, py, 19f * u, fill(WHITE))
    c.drawCircle(px, py, 16f * u, fill(INK))
    icon(c, CAR, px - 8.5f * u, py - 8.5f * u, 17f * u, WHITE, 2f * 17f / 24f * u)

    // chip
    val t1 = text(12f * u, bold, INK); val t2 = text(10f * u, medium, MUTED)
    val since = if (s.parkEpoch > 0) {
      val dd = java.util.Date(s.parkEpoch * 1000)
      val f = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ENGLISH)
      val today = f.format(dd) == f.format(java.util.Date())
      java.text.SimpleDateFormat(if (today) "h:mm a" else "d MMM, h:mm a", java.util.Locale.ENGLISH).format(dd)
    } else s.tripEnd
    val line2 = "${s.status} · since $since"
    val maxW = card.width() / 2 - 40f * u
    val l3 = fit(s.parkTitle, t2, maxW)
    val cw = maxOf(t1.measureText("Last parked here"), t2.measureText(line2), t2.measureText(l3)).coerceAtMost(maxW) + 24f * u
    val chip = RectF(card.left + 14f * u, card.top + 14f * u, card.left + 14f * u + cw, card.top + 14f * u + 56f * u)
    c.drawRoundRect(chip, 14f * u, 14f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE; setShadowLayer(7f * u, 0f, 6f * u, 0x300E0E10) })
    c.drawText(if (s.parkTitle.isEmpty()) "No location yet" else "Last parked here", chip.left + 12f * u, chip.top + 20f * u, t1)
    c.drawText(fit(line2, t2, maxW), chip.left + 12f * u, chip.top + 34f * u, t2)
    c.drawText(l3, chip.left + 12f * u, chip.top + 47f * u, t2)

    // locate button
    val b = RectF(card.right - 14f * u - 92f * u, card.bottom - 14f * u - 36f * u, card.right - 14f * u, card.bottom - 14f * u)
    c.drawRoundRect(RectF(b.left, b.top + 4f * u, b.right, b.bottom + 4f * u), 18f * u, 18f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x000E0E10; setShadowLayer(8f * u, 0f, 6f * u, 0x660E0E10) })
    darkButton(c, u, b, "Locate", true)
    return bmp
  }

  // ---------- LOCK SCREEN (dark glance) ----------
  fun lock(ctx: Context, w: Int, h: Int, s: WidgetSnapshot): Bitmap {
    val d = ctx.resources.displayMetrics.density
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val card = RectF(4f * d, 4f * d, w - 4f * d, h - 4f * d)
    c.drawRoundRect(card, 26f * d, 26f * d, fill(0xFF1B1B1E.toInt()))
    val u = scaleFor(card, d, 320f, 150f)
    val L = card.left + 22f * u; val T = card.top + 18f * u

    // inline line
    icon(c, CAR, L, T, 15f * u, WHITE, 2f * 15f / 24f * u)
    c.drawText(fit("Taigun · ${s.status} · ${s.fuelPct}% fuel", text(14f * u, medium, 0xD9FFFFFF.toInt()), card.right - L - 40f * u),
      L + 21f * u, T + 12.5f * u, text(14f * u, medium, 0xD9FFFFFF.toInt()))

    // three circular complications
    val top = T + 34f * u
    val r = 38f * u
    val circleBg = fill(0x1FFFFFFF)
    fun gauge(cx: Float, frac: Float) {
      c.drawCircle(cx, top + r, r, circleBg)
      val ring = RectF(cx - 30f * u, top + r - 30f * u, cx + 30f * u, top + r + 30f * u)
      val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f * u; strokeCap = Paint.Cap.ROUND; color = 0x38FFFFFF }
      c.drawArc(ring, 135f, 270f, false, p)
      p.color = WHITE
      if (frac > 0f) c.drawArc(ring, 135f, 270f * frac.coerceIn(0f, 1f), false, p)
    }
    val x1 = L + r; val x2 = x1 + 2 * r + 14f * u; val x3 = x2 + 2 * r + 14f * u
    gauge(x1, s.fuelPct / 100f)
    icon(c, FUEL, x1 - 7f * u, top + 18f * u, 14f * u, WHITE, 2f * 14f / 24f * u)
    c.drawText(s.fuelPct.toString(), x1, top + 50f * u, text(17f * u, bold, WHITE, Paint.Align.CENTER))
    gauge(x2, ((s.batteryV - 10.5) / (14.8 - 10.5)).toFloat())
    icon(c, BATTERY, x2 - 7f * u, top + 18f * u, 14f * u, WHITE, 2f * 14f / 24f * u)
    c.drawText(trim(s.batteryV), x2, top + 50f * u, text(15f * u, bold, WHITE, Paint.Align.CENTER))
    c.drawCircle(x3, top + r, r, circleBg)
    val driving = s.status.equals("Driving", true)
    c.drawText(if (driving) "D" else "P", x3, top + r + 7f * u, text(28f * u, heavy, WHITE, Paint.Align.CENTER))
    c.drawText(s.status.uppercase(), x3, top + r + 22f * u, text(9f * u, bold, 0xBFFFFFFF.toInt(), Paint.Align.CENTER).apply { letterSpacing = 0.06f })

    // rectangular complication — beside the circles when wide, below when tall
    val rw = 172f * u; val rh = 76f * u
    val rect = when {
      card.right - 22f * u - (x3 + r + 14f * u) >= rw * 0.8f -> RectF(x3 + r + 14f * u, top, card.right - 22f * u, top + rh)
      card.bottom - 18f * u - (top + 2 * r + 18f * u) >= rh -> RectF(L, top + 2 * r + 18f * u, L + rw, top + 2 * r + 18f * u + rh)
      else -> null
    }
    rect?.let { rr ->
      c.drawRoundRect(rr, 18f * u, 18f * u, circleBg)
      val x = rr.left + 12f * u; val mw = rr.width() - 24f * u
      val sub = text(12f * u, medium, 0xD9FFFFFF.toInt())
      c.drawText(fit(s.variant, text(13f * u, bold, WHITE), mw), x, rr.top + 24f * u, text(13f * u, bold, WHITE))
      c.drawText(fit("Fuel ${s.fuelPct}% · ${trim(s.batteryV)} V", sub, mw), x, rr.top + 43f * u, sub)
      c.drawText(fit("Last trip ${s.tripMin} min", sub, mw), x, rr.top + 61f * u, sub)
    }
    return bmp
  }
}
