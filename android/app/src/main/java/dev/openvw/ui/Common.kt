package dev.openvw.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.theme.Ink
import dev.openvw.theme.Muted

/** Renders an SVG 24x24 path string (stroke or fill) as a crisp icon. */
@Composable
fun VIcon(
  path: String,
  modifier: Modifier = Modifier,
  size: Dp = 20.dp,
  color: Color = Ink,
  stroke: Float = 1.8f,
  filled: Boolean = false,
) {
  val p = remember(path) { PathParser().parsePathString(path).toPath() }
  Canvas(modifier.size(size)) {
    val sc = this.size.minDimension / 24f
    scale(sc, sc, pivot = Offset.Zero) {
      if (filled) drawPath(p, color, style = Fill)
      else drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
  }
}

// Common icon path data (lifted from the design's SVGs)
object Ic {
  const val bell = "M4 10v4h3l7 4V6L7 10z M17.5 9a4 4 0 0 1 0 6"
  const val edit = "M4 20h4L19 9l-4-4L4 16z M13.5 6.5l4 4"
  const val chevronR = "M9 6l6 6-6 6"
  const val chevronBack = "M15 6l-6 6 6 6"
  const val chevronDown = "M6 9l6 6 6-6"
  const val replay = "M4 12a8 8 0 1 0 2.4-5.7 M4 4v4h4"
  const val navArrow = "M20 4L4 11l7 2 2 7z"
  const val fuel = "M5 20V5a1.5 1.5 0 0 1 1.5-1.5h6A1.5 1.5 0 0 1 14 5v15M3.5 20h12 M5 10h9 M14 8.5l3 2.5v6a1.5 1.5 0 0 0 3 0V9l-2.5-2.5"
  const val home = "M4 15a8 8 0 1 1 16 0 M12 15l4-5 M4 19h16"
  const val route = "M8 19h7a3.5 3.5 0 0 0 0-7H9a3.5 3.5 0 0 1 0-7h7"
  const val pin = "M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z"
  const val car = "M5 16v2.5M19 16v2.5 M3.5 16V12.5l2-5A2 2 0 0 1 7.4 6h9.2a2 2 0 0 1 1.9 1.5l2 5V16z M3.5 12.5h17"
  const val calendar = "M4 10h16M8.5 3.5v4M15.5 3.5v4 M6 5.5h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-10a2 2 0 0 1 2-2z"
  const val heart = "M12 20s-7.5-4.6-7.5-10A4.3 4.3 0 0 1 12 7.4 4.3 4.3 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z"
  const val search = "M20 20l-4.2-4.2"
  // crossed wrench + screwdriver
  const val service = "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z M3 5l2-2 2.8 2.8-2 2z M6.8 6.8l2.7 2.7 M14.2 16.2l1.9-1.9 4.5 4.5a1.34 1.34 0 0 1-1.9 1.9z"
  const val moon = "M20 14.5A8 8 0 0 1 9.5 4 7 7 0 1 0 20 14.5z"
  const val sun = "M12 4.5v-2 M12 21.5v-2 M4.5 12h-2 M21.5 12h-2 M6 6l-1.4-1.4 M19.4 19.4L18 18 M18 6l1.4-1.4 M4.6 19.4L6 18 M12 8.2a3.8 3.8 0 1 0 0 7.6 3.8 3.8 0 0 0 0-7.6z"
  const val clock = "M12 8v4l2.5 2 M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16z"
}

fun label(size: Int = 10, color: Color = Muted) = TextStyle(
  color = color, fontSize = size.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
)

val Tnum = TextStyle(fontFeatureSettings = "tnum")

/** "since 6:28 PM" today, "since 16 Oct, 6:28 PM" otherwise. */
fun sinceLabel(epoch: Long): String {
  if (epoch <= 0) return ""
  val d = java.util.Date(epoch * 1000)
  val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ENGLISH).let { it.format(d) == it.format(java.util.Date()) }
  val f = if (today) "h:mm a" else "d MMM, h:mm a"
  return "since " + java.text.SimpleDateFormat(f, java.util.Locale.ENGLISH).format(d)
}
