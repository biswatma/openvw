package dev.openvw.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.data.Telemetry
import dev.openvw.theme.Ink
import dev.openvw.theme.Ink2
import dev.openvw.theme.Track
import dev.openvw.theme.TickMinor
import dev.openvw.theme.Muted2
import dev.openvw.theme.CardAlt
import dev.openvw.theme.Card
import dev.openvw.theme.Line
import dev.openvw.theme.Muted
import dev.openvw.theme.Red
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Design board coordinates: the card is 358×292, gauges share CY=125, radius 112.
private const val DW = 358f
private const val DH = 292f
private const val LX = 150f
private const val RX = 208f
private const val CY = 125f
private const val RR = 112f

private val Teal = Color(0xFF00C2A8)
private val Blue = Color(0xFF2F80ED)
private val Amb = Color(0xFFFFB547)
private val Orange = Color(0xFFE8562A)
// gauge track / ticks / labels use theme tokens so they flip in dark mode

private fun lA(f: Float) = 125f + 110f * f // left arc: bottom-left → top, clockwise
private fun rA(f: Float) = 55f - 110f * f  // right arc: bottom-right → top, counter-clockwise

/**
 * Twin-arc instrument cluster (speed left, RPM right) with gear badge, gradient digital readouts
 * and segmented fuel / battery bars. [tv] is the 0..1 self-test sweep progress.
 */
@Composable
fun ClusterCard(tv: Float, tele: Telemetry, status: String, modifier: Modifier = Modifier) {
  val done = tv >= 1f
  val engineOn = tele.engineOn || (tele.speedKmh ?: 0) > 0
  // the dongle keeps reporting the last RPM after the engine stops, so only trust it while running
  val rpm = if (engineOn) tele.rpm else null
  val spdFrac = sweep(tv, ((tele.speedKmh ?: 0) / 240f).coerceIn(0f, 1f))
  val rpmFrac = sweep(tv, ((rpm ?: 0) / 8000f).coerceIn(0f, 1f))
  val speedText = if (done) (tele.speedKmh?.toString() ?: if (status == "Parked") "0" else "--") else (spdFrac * 240).roundToInt().toString()
  val rpmText = if (done) (rpm?.let { "%.1f".format(it / 1000f) } ?: "--") else "%.1f".format(rpmFrac * 8)
  val fuelK = easeOutPublic((tv - 0.2f) / 0.65f)
  val battK = battReveal(tv)
  val battFrac = (((tele.batteryV - 10.5) / (14.8 - 10.5)).toFloat()).coerceIn(0f, 1f)
  val measurer = rememberTextMeasurer()

  BoxWithConstraints(
    modifier.widthIn(max = 560.dp).fillMaxWidth().aspectRatio(DW / DH)
      .shadow(18.dp, RoundedCornerShape(30.dp), ambientColor = Color(0x47000000), spotColor = Color(0x47000000))
      .clip(RoundedCornerShape(30.dp))
      .background(Brush.verticalGradient(listOf(Card, CardAlt)))
      .border(1.dp, Line, RoundedCornerShape(30.dp)),
  ) {
    val k = maxWidth.value / DW // dp per design px
    fun d(v: Float) = (v * k).dp

    Canvas(Modifier.fillMaxSize()) {
      val px = size.width / DW // px per design px
      // soft mint / peach glows behind the arcs
      drawRect(Brush.radialGradient(listOf(Teal.copy(alpha = 0.13f), Color.Transparent), center = Offset(0f, size.height * 0.45f), radius = size.width * 0.62f))
      drawRect(Brush.radialGradient(listOf(Color(0x24FF8C3C), Color.Transparent), center = Offset(size.width, size.height * 0.45f), radius = size.width * 0.62f))

      val spdBrush = Brush.verticalGradient(listOf(Blue, Teal), startY = 32f * px, endY = 218f * px)
      val rpmBrush = Brush.verticalGradient(listOf(Orange, Amb), startY = 32f * px, endY = 218f * px)
      val labelStyle = TextStyle(color = Muted2, fontSize = (10 * k).sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")

      // ---- speed (left) ----
      band(LX, px, 125f, 110f, null, Track)
      if (spdFrac > 0.004f) {
        band(LX, px, 125f, 110f * spdFrac, null, Color(0x5900A0C8), shadow = true)
        band(LX, px, 125f, 110f * spdFrac, spdBrush, Color.Unspecified)
      }
      for (v in 0..240 step 20) tick(LX, px, lA(v / 240f), TickMinor)
      for (v in 0..240 step 40) label(measurer, v.toString(), LX, px, lA(v / 240f), labelStyle)
      needle(LX, px, lA(spdFrac), Ink)

      // ---- rpm (right) ----
      band(RX, px, 55f, -110f, null, Track)
      drawArc(Red.copy(alpha = 0.5f), -27.5f, -27.5f, false, Offset((RX - 123f) * px, (CY - 123f) * px), Size(246f * px, 246f * px), style = Stroke(3f * px, cap = StrokeCap.Round))
      if (rpmFrac > 0.004f) {
        band(RX, px, 55f, -110f * rpmFrac, null, Color(0x59E8562A), shadow = true)
        band(RX, px, 55f, -110f * rpmFrac, rpmBrush, Color.Unspecified)
      }
      for (v in 0..8) tick(RX, px, rA(v / 8f), TickMinor)
      for (v in 0..8 step 2) label(measurer, v.toString(), RX, px, rA(v / 8f), labelStyle)
      needle(RX, px, rA(rpmFrac), Ink)
    }

    // gear badge
    Column(Modifier.align(Alignment.TopCenter).padding(top = d(20f)), horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        Modifier.size(d(40f)).shadow(8.dp, RoundedCornerShape(50), spotColor = Color(0x990E0E10))
          .clip(RoundedCornerShape(50)).background(Brush.verticalGradient(listOf(Ink2, Ink))),
        contentAlignment = Alignment.Center,
      ) { Text(if (status == "Parked") "P" else "D", style = TextStyle(color = Card, fontSize = (18 * k).sp, fontWeight = FontWeight.Bold)) }
      Spacer(Modifier.height(d(4f)))
      Text(status, style = TextStyle(color = Muted, fontSize = (11 * k).sp, fontWeight = FontWeight.SemiBold))
    }

    // digital readouts
    Column(Modifier.offset(x = d(84f), y = d(94f))) {
      Text(speedText, style = digits(k, Brush.verticalGradient(listOf(Blue, Teal))))
      Text("km/h", style = TextStyle(color = Muted, fontSize = (9 * k).sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = d(4f)))
    }
    Column(Modifier.align(Alignment.TopEnd).offset(x = -d(84f), y = d(94f)), horizontalAlignment = Alignment.End) {
      Text(rpmText, style = digits(k, Brush.verticalGradient(listOf(Orange, Amb))))
      Text("rpm ×1000", style = TextStyle(color = Muted, fontSize = (9 * k).sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = d(4f)))
    }

    // segmented fuel / battery bars
    Row(
      Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = d(20f), end = d(20f), bottom = d(18f)),
      horizontalArrangement = Arrangement.spacedBy(d(22f)),
    ) {
      val fuelPct = (tele.fuelPct * fuelK).roundToInt()
      SegBar(
        k, Ic.fuel, "Fuel", "$fuelPct%", tele.fuelPct / 100f * fuelK,
        if (tele.fuelPct <= 15) Red to Red else Teal to Blue, "E" to "F", Modifier.weight(1f),
      )
      SegBar(
        k, BATTERY_ICON, "Battery", "%.1f V".format(10.5 + (tele.batteryV - 10.5) * battK), battFrac * battK,
        Amb to Orange, "10.5" to "14.8", Modifier.weight(1f),
      )
    }
  }
}

private const val BATTERY_ICON = "M5 7h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z M7 7V5h3v2M14 7V5h3v2"

private fun digits(k: Float, brush: Brush) = TextStyle(
  brush = brush, fontSize = (52 * k).sp, fontWeight = FontWeight.Bold, lineHeight = (52 * k).sp,
  letterSpacing = (-0.04 * 52 * k).sp, fontFeatureSettings = "tnum",
  // trim font padding so the box is exactly one line tall, as in the design (line-height: 1)
  lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

private fun DrawScope.band(cx: Float, px: Float, start: Float, sweepDeg: Float, brush: Brush?, color: Color, shadow: Boolean = false) {
  val tl = Offset((cx - RR) * px, (CY - RR + if (shadow) 4f else 0f) * px)
  val sz = Size(2 * RR * px, 2 * RR * px)
  val st = Stroke((if (shadow) 14f else 12f) * px, cap = StrokeCap.Round)
  if (brush != null) drawArc(brush, start, sweepDeg, false, tl, sz, style = st)
  else drawArc(color, start, sweepDeg, false, tl, sz, alpha = if (shadow) 0.5f else 1f, style = st)
}

private fun pt(cx: Float, px: Float, r: Float, a: Float): Offset {
  val rad = Math.toRadians(a.toDouble())
  return Offset((cx + r * cos(rad).toFloat()) * px, (CY + r * sin(rad).toFloat()) * px)
}

private fun DrawScope.tick(cx: Float, px: Float, a: Float, color: Color) =
  drawLine(color, pt(cx, px, 93f, a), pt(cx, px, 100f, a), strokeWidth = 1.5f * px, cap = StrokeCap.Round)

private fun DrawScope.needle(cx: Float, px: Float, a: Float, color: Color) =
  drawLine(color, pt(cx, px, 96f, a), pt(cx, px, 121f, a), strokeWidth = 3f * px, cap = StrokeCap.Round)

private fun DrawScope.label(m: androidx.compose.ui.text.TextMeasurer, t: String, cx: Float, px: Float, a: Float, style: TextStyle) {
  val p = pt(cx, px, 82f, a)
  val l = m.measure(t, style)
  drawText(l, topLeft = Offset(p.x - l.size.width / 2f, p.y - l.size.height / 2f))
}

@Composable
private fun SegBar(
  k: Float, icon: String, label: String, value: String, frac: Float,
  colors: Pair<Color, Color>, ends: Pair<String, String>, modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy((7 * k).dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        VIcon(icon, size = (13 * k).dp, color = Muted, stroke = 2f)
        Spacer(Modifier.width((5 * k).dp))
        Text(label, style = TextStyle(color = Muted, fontSize = (12 * k).sp, fontWeight = FontWeight.SemiBold))
      }
      Text(value, style = TextStyle(color = Ink, fontSize = (14 * k).sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((3 * k).dp)) {
      for (i in 0 until 10) {
        val lit = (i + 0.5f) / 10f <= frac + 0.0001f
        Box(
          Modifier.weight(1f).height((8 * k).dp).clip(RoundedCornerShape((2 * k).dp))
            .background(if (lit) lerp(colors.first, colors.second, i / 9f) else Line),
        )
      }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      val st = TextStyle(color = Muted2, fontSize = (9 * k).sp, fontWeight = FontWeight.Bold)
      Text(ends.first, style = if (ends.first == "E") st.copy(color = Red) else st)
      Text(ends.second, style = st)
    }
  }
}
