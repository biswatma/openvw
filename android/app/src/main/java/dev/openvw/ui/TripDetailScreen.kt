package dev.openvw.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.data.GoogleMaps
import dev.openvw.data.Trip
import dev.openvw.theme.AmberDark
import dev.openvw.theme.AmberSoftBg
import dev.openvw.theme.Border
import dev.openvw.theme.Card
import dev.openvw.theme.Chip
import dev.openvw.theme.Field
import dev.openvw.theme.Ink
import dev.openvw.theme.LineSoft
import dev.openvw.theme.Muted

@Composable
fun TripDetailScreen(trip: Trip?, onBack: () -> Unit) {
  val ctx = LocalContext.current
  Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
    BackBar(onBack)
    Text("Trip details", style = H1, modifier = Modifier.padding(start = 20.dp, top = 4.dp))

    if (trip == null) {
      Text("Trip not available.", style = TextStyle(color = Muted, fontSize = 14.sp), modifier = Modifier.padding(20.dp))
      return@Column
    }

    // headline: duration · distance · date
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).card(24.dp).padding(18.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(trip.durMin.toString(), style = TextStyle(color = Ink, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.0).sp).merge(Tnum))
          Text(
            if (trip.km > 0) "  min · %.1f km · %s".format(trip.km, trip.date) else "  min · ${trip.date}",
            style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
          )
        }
        if (trip.pending)
          Box(Modifier.clip(RoundedCornerShape(99.dp)).background(AmberSoftBg).padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text("Finalizing…", style = TextStyle(color = AmberDark, fontSize = 12.sp, fontWeight = FontWeight.Bold))
          }
        else
          Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Chip).border(1.dp, Border, RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text(trip.type, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold))
          }
      }
      Spacer(Modifier.height(16.dp))
      TripRoute(trip.start, trip.end, trip.fromTitle, trip.fromSub, trip.toTitle, trip.toSub)
    }

    if (trip.pending) {
      Text(
        "Full trip stats (distance, mileage, driver score) arrive a little after you park.",
        style = TextStyle(color = Muted, fontSize = 12.sp, lineHeight = 17.sp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
      )
    } else {
      // mileage highlight
      Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).card(24.dp).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text("MILEAGE (THIS TRIP)", style = label())
          Spacer(Modifier.height(4.dp))
          Text(
            if (trip.mileage > 0) "%.1f km/l".format(trip.mileage) else "—",
            style = TextStyle(color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold).merge(Tnum),
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("FUEL USED", style = label())
          Spacer(Modifier.height(4.dp))
          val delta = trip.startFuelPct - trip.endFuelPct
          Text(if (delta > 0) "$delta%" else "Refuelled", style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold).merge(Tnum))
          Text("${trip.startFuelPct}% → ${trip.endFuelPct}%", style = TextStyle(color = Muted, fontSize = 11.sp).merge(Tnum))
        }
      }

      // full stat grid
      Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).card(24.dp).padding(horizontal = 18.dp, vertical = 4.dp)) {
        val rows = listOf(
          listOf("Distance" to km(trip.km), "Avg speed" to "${trip.avgSpeed} km/h"),
          listOf("Driving time" to mins(trip.drivingMin), "Max speed" to "${trip.maxSpeed} km/h"),
          listOf("Idle time" to mins(trip.idleMin), "Stopped" to mins(trip.haltMin)),
          listOf("Driver score" to "${trip.score}/100", "Hard brakes" to trip.brakes.toString()),
          listOf("Odometer start" to km(trip.startOdo), "Odometer end" to km(trip.endOdo)),
        )
        rows.forEachIndexed { i, pair ->
          if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
          Row(Modifier.fillMaxWidth()) {
            pair.forEach { (lbl, v) ->
              Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
                Text(lbl.uppercase(), style = label())
                Spacer(Modifier.height(4.dp))
                Text(v, style = TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold).merge(Tnum))
              }
            }
          }
        }
      }

      Text(
        "Mileage is estimated from the fuel-level drop (VW reports percent, not litres), so single short trips are rough; the Statistics average is more reliable.",
        style = TextStyle(color = Muted, fontSize = 11.sp, lineHeight = 15.sp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
      )

      if (trip.endLat != 0.0) {
        Box(
          Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().height(52.dp)
            .clip(RoundedCornerShape(99.dp)).background(Field).border(1.dp, dev.openvw.theme.Hairline, RoundedCornerShape(99.dp))
            .clickable {
              if (trip.startLat != 0.0) GoogleMaps.route(ctx, trip.startLat, trip.startLon, trip.endLat, trip.endLon)
              else GoogleMaps.pin(ctx, trip.endLat, trip.endLon)
            },
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            VIcon(Ic.navArrow, size = 16.dp, color = Ink, stroke = 2f)
            Spacer(Modifier.width(8.dp))
            Text("View route in Google Maps", style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
          }
        }
      }
    }
  }
}

private fun km(v: Double) = if (v > 0) "%.1f km".format(v) else "—"
private fun mins(v: Int) = if (v >= 60) "${v / 60}h ${v % 60}m" else "$v min"
