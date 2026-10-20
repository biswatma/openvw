package dev.openvw.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.data.CarEvent
import dev.openvw.data.Places
import dev.openvw.data.Store
import dev.openvw.theme.Border
import dev.openvw.theme.Field
import dev.openvw.theme.LineSoft
import dev.openvw.theme.Red
import dev.openvw.theme.RedSoftBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.openvw.theme.Card
import dev.openvw.theme.Ink
import dev.openvw.theme.Muted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertsScreen(onBack: () -> Unit) {
  var tab by remember { mutableStateOf(0) }
  Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
    BackBar(onBack, showBack = false)
    Text("Updates", style = H1, modifier = Modifier.padding(start = 20.dp, top = 4.dp))
    Row(Modifier.padding(start = 16.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Seg("Notifications", tab == 0) { tab = 0 }
      Seg("Alert setup", tab == 1) { tab = 1 }
    }

    if (tab == 0) {
      if (Store.events.isEmpty()) {
        Empty(Ic.bell, "No activity yet", "Engine and driving alerts from your car show up here.")
      }
      Store.events.groupBy { dayKey(it.epoch) }.forEach { (_, evs) ->
        Text(dayLabel(evs.first().epoch).uppercase(), style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp), modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp))
        Column(Modifier.padding(horizontal = 16.dp).card(24.dp).padding(horizontal = 16.dp)) {
          evs.forEachIndexed { i, e ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
            EventRow(e)
          }
        }
      }
    } else {
      val setup = Store.alertSetup
      Text("GEOFENCES", style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp), modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp))
      Column(Modifier.padding(horizontal = 16.dp).card(24.dp).padding(18.dp)) {
        if (setup.fences.isEmpty()) {
          Text("No geofences set", style = TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold))
          Spacer(Modifier.height(2.dp))
          Text("Fences are created in the My Volkswagen app; they'll appear here once set.", style = TextStyle(color = Muted, fontSize = 12.sp, lineHeight = 17.sp))
        } else setup.fences.forEach {
          Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            VIcon(Ic.pin, size = 16.dp, color = Ink, stroke = 1.9f)
            Spacer(Modifier.width(10.dp))
            Text(it, style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold))
          }
        }
      }
      Text("ALERTS ON (${setup.triggers.size})", style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp), modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp))
      if (setup.triggers.isEmpty()) {
        Empty(Ic.clock, "All clear", "Alert triggers will show up here.")
      } else {
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          setup.triggers.forEach { t ->
            Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 7.dp)) {
              Text(triggerLabel(t), style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EventRow(e: CarEvent) {
  val alarm = e.kind == "alarm"
  val on = e.title == "Engine on"
  Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier.size(38.dp).clip(RoundedCornerShape(19.dp)).background(if (alarm) RedSoftBg else if (on) Ink else Field),
      contentAlignment = Alignment.Center,
    ) {
      VIcon(
        if (alarm) "M12 3.5l9 16H3z M12 9.5v4 M12 16.5h0.01" else Ic.car,
        size = 18.dp, color = if (alarm) Red else if (on) Card else Ink, stroke = 1.9f,
      )
    }
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(e.title, style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
      val place = Places.cached(LocalContext.current, e.lat, e.lon)?.first
      if (place != null) Text(place, maxLines = 1, style = TextStyle(color = Muted, fontSize = 12.sp))
    }
    Text(SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(e.epoch * 1000)), style = TextStyle(color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold).merge(Tnum))
  }
}

@Composable
private fun Empty(icon: String, title: String, sub: String) {
  Column(Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Box(Modifier.size(88.dp).clip(RoundedCornerShape(44.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(44.dp)), contentAlignment = Alignment.Center) {
      VIcon(icon, size = 34.dp, color = Ink, stroke = 1.4f)
    }
    Spacer(Modifier.height(12.dp))
    Text(title, style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp))
    Text(sub, style = TextStyle(color = Muted, fontSize = 14.sp))
  }
}

private fun dayKey(epoch: Long) = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date(epoch * 1000))

private fun dayLabel(epoch: Long): String = when (dayKey(epoch)) {
  dayKey(System.currentTimeMillis() / 1000) -> "Today"
  dayKey(System.currentTimeMillis() / 1000 - 86400) -> "Yesterday"
  else -> SimpleDateFormat("EEEE, d MMM", Locale.ENGLISH).format(Date(epoch * 1000))
}

private fun triggerLabel(t: String) = when (t) {
  "on" -> "Engine on"
  "off" -> "Engine off"
  "speedLimit" -> "Speed limit"
  "PUC" -> "PUC"
  else -> t.replaceFirstChar { it.uppercase() }
}
