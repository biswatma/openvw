package dev.openvw.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.theme.Chip
import dev.openvw.theme.Ink
import dev.openvw.theme.LineSoft
import dev.openvw.theme.Muted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Ownership {
  private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
  private val out = SimpleDateFormat("d MMM yyyy", Locale.US)
  private fun parse(s: String): Date? = runCatching { iso.parse(s) }.getOrNull()
  fun daysSince(s: String): Long {
    val d = parse(s) ?: return 0
    return ((System.currentTimeMillis() - d.time) / 86_400_000L).coerceAtLeast(0)
  }
  fun formatDate(s: String): String = parse(s)?.let { out.format(it) } ?: "—"
}

@Composable
fun OwnershipCard(odometerKm: Int?, connectedSinceIso: String, modifier: Modifier = Modifier) {
  val odo = odometerKm ?: 0
  val days = Ownership.daysSince(connectedSinceIso)
  val avg = if (days > 0) odo.toDouble() / days else 0.0

  Column(modifier.card(24.dp).padding(18.dp)) {
    Text("OWNERSHIP", style = label())
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Total distance", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
          Text(if (odo > 0) "%,d".format(odo) else "--", style = TextStyle(color = Ink, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp).merge(Tnum))
          Text(" km", style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
        }
      }
      Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Chip), contentAlignment = Alignment.Center) {
        VIcon(Ic.route, size = 22.dp, color = Ink, stroke = 1.8f)
      }
    }
    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth()) {
      Stat("Registered", Ownership.formatDate(connectedSinceIso), Modifier.weight(1.3f))
      Stat("Days owned", if (days > 0) days.toString() else "--", Modifier.weight(1f))
      Stat("Avg / day", if (avg > 0) "%.1f km".format(avg) else "--", Modifier.weight(1f))
    }
  }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
  Column(modifier) {
    Text(label.uppercase(), style = dev.openvw.ui.label())
    Spacer(Modifier.height(4.dp))
    Text(value, style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold).merge(Tnum))
  }
}
