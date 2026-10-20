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
import androidx.compose.foundation.layout.width
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
import dev.openvw.data.Telemetry
import dev.openvw.theme.Field
import dev.openvw.theme.Green
import dev.openvw.theme.GreenDark
import dev.openvw.theme.GreenSoftBg
import dev.openvw.theme.Ink
import dev.openvw.theme.Muted
import dev.openvw.theme.Red
import dev.openvw.theme.RedSoftBg

@Composable
fun HealthCard(t: Telemetry, modifier: Modifier = Modifier) {
  val ok = !t.milOn
  val battLabel = when {
    t.batteryV >= 12.4 -> "Good" to GreenDark
    t.batteryV >= 12.0 -> "Fair" to dev.openvw.theme.Amber
    else -> "Weak" to Red
  }
  Column(modifier.card(24.dp).padding(18.dp)) {
    Text("VEHICLE HEALTH", style = label())
    Spacer(Modifier.height(12.dp))

    // status banner
    Row(
      Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(if (ok) GreenSoftBg else RedSoftBg).padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(if (ok) Green else Red), contentAlignment = Alignment.Center) {
        VIcon(
          if (ok) "M12 3.5l7 3v5c0 4.5-3 7.8-7 9-4-1.2-7-4.5-7-9v-5z M9 12l2 2 4-4"
          else "M12 3.5l9 16H3z M12 9.5v4 M12 16.5h0.01",
          size = 18.dp, color = Color.White, stroke = 1.9f,
        )
      }
      Spacer(Modifier.width(12.dp))
      Column {
        Text(if (ok) "All systems OK" else "Check engine", style = TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold))
        Text(
          if (ok) "No active fault codes" else "Fault active for ${t.distanceWithMil ?: 0} km",
          style = TextStyle(color = Muted, fontSize = 12.sp),
        )
      }
    }

    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Tile("Battery", "${trimV(t.batteryV)} V", battLabel.first, battLabel.second, Modifier.weight(1f))
      Tile("Coolant", "${t.coolantC ?: "--"}°C", null, Ink, Modifier.weight(1f))
      Tile("Intake air", "${t.intakeC ?: "--"}°C", null, Ink, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Tile("Engine load", "${t.engineLoadPct ?: "--"}%", null, Ink, Modifier.weight(1f))
      Tile("Since reset", "${fmtKm(t.distanceSinceDtc)} km", null, Ink, Modifier.weight(1f))
      Tile("MIL distance", "${t.distanceWithMil ?: 0} km", null, Ink, Modifier.weight(1f))
    }
  }
}

@Composable
private fun Tile(label: String, value: String, badge: String?, badgeColor: Color, modifier: Modifier = Modifier) {
  Column(modifier.clip(RoundedCornerShape(16.dp)).background(Field).padding(vertical = 12.dp, horizontal = 12.dp)) {
    Text(label.uppercase(), style = dev.openvw.ui.label(size = 9))
    Spacer(Modifier.height(4.dp))
    Text(value, style = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold).merge(Tnum))
    if (badge != null) {
      Spacer(Modifier.height(2.dp))
      Text(badge, style = TextStyle(color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold))
    }
  }
}

private fun trimV(v: Double) = if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
private fun fmtKm(v: Int?) = if (v == null) "--" else "%,d".format(v)
