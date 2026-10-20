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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.data.Store
import dev.openvw.data.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.openvw.theme.Border
import dev.openvw.theme.Card
import dev.openvw.theme.Chip
import dev.openvw.theme.Ink
import dev.openvw.theme.Muted
import dev.openvw.theme.OnDarkMuted

@Composable
fun TripsScreen(onBack: () -> Unit) {
  var tab by remember { mutableStateOf(0) }
  Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
    BackBar(onBack, showBack = false)
    Text("Your trips", style = H1, modifier = Modifier.padding(start = 20.dp, top = 4.dp))
    Row(Modifier.padding(start = 16.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Seg("Overview", tab == 0) { tab = 0 }
      Seg("Statistics", tab == 1) { tab = 1 }
    }

    val d = Store.drive
    if (tab == 0 && Store.trips.isEmpty()) {
      Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp).clip(RoundedCornerShape(46.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(46.dp)), contentAlignment = Alignment.Center) {
          VIcon(Ic.route + " M4 19a2 2 0 1 0 4 0 2 2 0 1 0-4 0z M16 5a2 2 0 1 0 4 0 2 2 0 1 0-4 0z", size = 40.dp, color = Ink, stroke = 1.4f)
        }
        Spacer(Modifier.height(12.dp))
        Text("No trips yet", style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp))
        Text("Your trips show up here after you drive.", style = TextStyle(color = Muted, fontSize = 14.sp))
      }
    } else if (tab == 0) {
      val live = d.tripCount > 0
      Row(
        Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ink).padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        DarkStat("This week", (if (live) d.weekTrips else Store.trips.size).toString(), null, Modifier.weight(1f))
        DarkStat("Time on road", (if (live) d.weekMin else Store.trips.sumOf { it.durMin }).toString(), "min", Modifier.weight(1f))
        if (live) DarkStat("Distance", "%.0f".format(d.weekKm), "km", Modifier.weight(1f))
      }
      Store.trips.groupBy { it.date }.forEach { (date, trips) ->
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
          Text(dayTitle(trips.first()), style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp))
          Text("${trips.size} trip${if (trips.size == 1) "" else "s"}", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          trips.forEach { tr ->
            Column(Modifier.card(24.dp).padding(18.dp)) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.Bottom) {
                  Text(tr.durMin.toString(), style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold).merge(Tnum))
                  Text(" min", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                  if (tr.km > 0) Text("  ·  %.1f km".format(tr.km), style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold).merge(Tnum))
                }
                Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Chip).border(1.dp, Border, RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 7.dp)) {
                  Text(tr.type, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
              }
              Spacer(Modifier.height(14.dp))
              TripRoute(tr.start, tr.end, tr.fromTitle, tr.fromSub, tr.toTitle, tr.toSub)
            }
          }
        }
      }
    } else if (d.tripCount > 0) {
      Row(
        Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ink).padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        DarkStat("Trips", d.tripCount.toString(), null, Modifier.weight(1f))
        DarkStat("Distance", "%.0f".format(d.totalKm), "km", Modifier.weight(1f))
        DarkStat("Driving", "%.1f".format(d.totalMin / 60.0), "h", Modifier.weight(1f))
      }
      DrivingCard(d, Modifier.padding(horizontal = 16.dp))
      Text(
        "Across your last ${d.tripCount} trips. Real mileage is measured from fuel level drop per trip.",
        style = TextStyle(color = Muted, fontSize = 12.sp, lineHeight = 17.sp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
      )
    } else {
      Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp).clip(RoundedCornerShape(46.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(46.dp)), contentAlignment = Alignment.Center) {
          VIcon(Ic.fuel, size = 40.dp, color = Ink, stroke = 1.4f)
        }
        Spacer(Modifier.height(12.dp))
        Text("No trip data", style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp))
        Text("Time to take your ride for a spin.", style = TextStyle(color = Muted, fontSize = 14.sp))
      }
    }
  }
}

/** "Today", "Yesterday" or "Friday, 16 Oct". */
private fun dayTitle(t: Trip): String {
  if (t.epoch <= 0) return "Friday, ${t.date}"
  val f = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
  val day = f.format(Date(t.epoch * 1000))
  return when (day) {
    f.format(Date()) -> "Today"
    f.format(Date(System.currentTimeMillis() - 86_400_000L)) -> "Yesterday"
    else -> SimpleDateFormat("EEEE, d MMM", Locale.ENGLISH).format(Date(t.epoch * 1000))
  }
}

@Composable
private fun DarkStat(label: String, value: String, unit: String?, modifier: Modifier = Modifier) {
  Column(modifier) {
    Text(label.uppercase(), style = TextStyle(color = OnDarkMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp))
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(value, style = TextStyle(color = Card, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp).merge(Tnum))
      if (unit != null) Text(" $unit", style = TextStyle(color = OnDarkMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
  }
}
