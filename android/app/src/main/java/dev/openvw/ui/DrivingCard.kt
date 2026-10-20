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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.data.DriveStats
import dev.openvw.theme.Amber
import dev.openvw.theme.Green
import dev.openvw.theme.Ink
import dev.openvw.theme.LineSoft
import dev.openvw.theme.Muted
import dev.openvw.theme.Red

@Composable
fun DrivingCard(d: DriveStats, modifier: Modifier = Modifier) {
  val score = d.avgScore
  val (lbl, col) = when {
    score >= 90 -> "Excellent" to Green
    score >= 75 -> "Good" to Green
    score >= 60 -> "Fair" to Amber
    else -> "Needs work" to Red
  }
  Column(modifier.card(24.dp).padding(18.dp)) {
    Text("DRIVING", style = label())
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Driver score", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
          Text(if (d.tripCount > 0) score.toString() else "--", style = TextStyle(color = Ink, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.0).sp).merge(Tnum))
          Text(" /100", style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
        }
      }
      if (d.tripCount > 0) {
        Box(Modifier.clip(RoundedCornerShape(99.dp)).background(col).padding(horizontal = 14.dp, vertical = 8.dp)) {
          Text(lbl, style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        }
      }
    }
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth()) {
      Stat3("Top speed", if (d.maxSpeedKmh > 0) "${d.maxSpeedKmh}" else "--", "km/h", Modifier.weight(1f))
      Stat3("Idle time", if (d.idleMin > 0) "${d.idleMin}" else "0", "min", Modifier.weight(1f))
      Stat3("Hard stops", d.harshBrakes.toString(), "", Modifier.weight(1f))
      Stat3("Real mileage", if (d.realKmpl > 0) "%.1f".format(d.realKmpl) else "--", "km/l", Modifier.weight(1.1f))
    }
  }
}

@Composable
private fun Stat3(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
  Column(modifier) {
    Text(label.uppercase(), style = dev.openvw.ui.label(size = 9))
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(value, style = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold).merge(Tnum))
      if (unit.isNotEmpty()) Text(" $unit", style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
    }
  }
}
