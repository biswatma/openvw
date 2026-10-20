package dev.openvw.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.theme.Ink
import dev.openvw.theme.Muted2
import dev.openvw.theme.Red
import dev.openvw.theme.Track
import dev.openvw.theme.Track2
import dev.openvw.theme.TickMinor
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// ---- Animation: the instrument-cluster "self-test" sweep ----
// Needle rushes to full (ease-out) then settles back to the real value (ease-in-out),
// exactly like a real car cockpit powering on. t runs 0..1 over 2800ms.
private fun clamp01(x: Float) = x.coerceIn(0f, 1f)
private fun easeOut(x: Float) = 1f - (1f - x).pow(3)
private fun easeInOut(x: Float) =
  if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f

fun sweep(t: Float, target: Float): Float {
  val up = easeOut(clamp01(t / 0.36f))
  val down = easeInOut(clamp01((t - 0.42f) / 0.4f))
  return if (t < 0.42f) up else 1f + (target - 1f) * down
}

fun fuelReveal(t: Float, pct: Int): Float = clamp01(pct / 100f) * easeOut(clamp01((t - 0.2f) / 0.65f))
fun battReveal(t: Float): Float = easeOut(clamp01((t - 0.2f) / 0.65f))

private fun DrawScope.deg(cx: Float, cy: Float, r: Float, d: Float): Offset {
  val a = d * (Math.PI.toFloat() / 180f)
  return Offset(cx + r * cos(a), cy + r * sin(a))
}

private fun DrawScope.arc(
  cx: Float, cy: Float, r: Float, start: Float, sweepDeg: Float, color: Color, w: Float,
  cap: StrokeCap = StrokeCap.Round, alpha: Float = 1f,
) {
  if (sweepDeg <= 0f) return
  drawArc(
    color = color, startAngle = start, sweepAngle = sweepDeg, useCenter = false,
    topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2),
    style = Stroke(width = w, cap = cap), alpha = alpha,
  )
}

private fun DrawScope.needle(cx: Float, cy: Float, len: Float, half: Float, angle: Float, color: Color) {
  rotate(degrees = angle, pivot = Offset(cx, cy)) {
    val p = Path().apply {
      moveTo(cx, cy - half)
      lineTo(cx + len, cy)
      lineTo(cx, cy + half)
      close()
    }
    drawPath(p, color)
  }
}

/** Big 240° speedometer (0..220 km/h) with ticks, number labels and needle. */
@Composable
fun SpeedGauge(frac: Float, speedText: String, accent: Color, modifier: Modifier = Modifier) {
  val measurer = rememberTextMeasurer()
  val labelStyle = TextStyle(color = Muted2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
  Box(modifier) {
    Canvas(Modifier.fillMaxSize()) {
      val s = min(size.width, size.height)
      val cx = size.width / 2f
      val cy = size.height / 2f
      val half = s / 2f
      val rArc = half * 112f / 130f
      val w = s * 10f / 260f
      val f = frac.coerceIn(0f, 1f)

      arc(cx, cy, rArc, 150f, 240f, Track, w)
      // ticks
      for (v in 0..220 step 10) {
        val d = 150f + 240f * v / 220f
        val major = v % 20 == 0
        val inner = half * (if (major) 88f else 94f) / 130f
        val outer = half * 100f / 130f
        drawLine(
          if (major) Ink else TickMinor,
          deg(cx, cy, inner, d), deg(cx, cy, outer, d),
          strokeWidth = if (major) s * 2.5f / 260f else s * 1.5f / 260f, cap = StrokeCap.Round,
        )
        if (major) {
          val p = deg(cx, cy, half * 73f / 130f, d)
          val tl = measurer.measure(v.toString(), labelStyle)
          drawText(measurer, v.toString(), topLeft = Offset(p.x - tl.size.width / 2f, p.y - tl.size.height / 2f), style = labelStyle)
        }
      }
      arc(cx, cy, rArc, 150f, 240f * f, Ink, w, alpha = if (f > 0.002f) 1f else 0f)
      needle(cx, cy, half * 94f / 130f, s * 3f / 260f, 150f + 240f * f, accent)
      drawCircle(Ink, radius = half * 12f / 130f, center = Offset(cx, cy))
      drawCircle(accent, radius = half * 4f / 130f, center = Offset(cx, cy))
    }
    Column(
      Modifier.align(Alignment.TopCenter).padding(top = 128.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      androidx.compose.material3.Text(
        speedText,
        style = TextStyle(color = Ink, fontSize = 50.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp, fontFeatureSettings = "tnum"),
      )
      androidx.compose.material3.Text(
        "KM/H",
        style = TextStyle(color = dev.openvw.theme.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
      )
    }
  }
}

/** Small 240° gauge (used for RPM — with a red redline zone). */
@Composable
fun MiniGauge(
  frac: Float, accent: Color, valueColor: Color, modifier: Modifier = Modifier,
  redline: Boolean = false, lowMark: Boolean = false, center: @Composable () -> Unit,
) {
  Box(modifier.size(120.dp), contentAlignment = Alignment.Center) {
    Canvas(Modifier.size(120.dp)) {
      val s = size.width
      val cx = s / 2f; val cy = s / 2f; val half = s / 2f
      val r = half * 46f / 60f
      val w = s * 7f / 120f
      val f = frac.coerceIn(0f, 1f)
      arc(cx, cy, r, 150f, 240f, Track2, w)
      if (redline) arc(cx, cy, r, 150f + 240f * 0.75f, 240f * 0.25f, Red, w, alpha = 0.35f)
      if (lowMark) arc(cx, cy, r, 150f, 240f * 0.15f, Red, s * 2f / 120f, alpha = 0.6f)
      arc(cx, cy, r, 150f, 240f * f, valueColor, w, alpha = if (f > 0.002f) 1f else 0f)
      needle(cx, cy, half * 40f / 60f, s * 1.5f / 120f, 150f + 240f * f, accent)
      drawCircle(Ink, radius = half * 6f / 60f, center = Offset(cx, cy))
    }
    center()
  }
}

// Expose easings for other callers (e.g. header count-ups) if needed.
fun easeOutPublic(x: Float) = easeOut(clamp01(x))
