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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import dev.openvw.data.GoogleMaps
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.data.Store
import dev.openvw.theme.Accent
import dev.openvw.theme.Border
import dev.openvw.theme.Card
import dev.openvw.theme.Field
import dev.openvw.theme.Ink
import dev.openvw.theme.Muted

@Composable
fun LocateScreen(onBack: () -> Unit) {
  var tab by remember { mutableStateOf(0) }
  var mode by remember { mutableStateOf(MapMode.STREET) }
  var carCard by remember { mutableStateOf(false) }
  val ctx = LocalContext.current
  Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
    BackBar(onBack, showBack = false, trailing = { CircleButton(Ic.heart) })
    Text("Locate us", style = H1, modifier = Modifier.padding(start = 20.dp, top = 4.dp))

    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
      VIcon(Ic.search + " M11 4.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13z", size = 18.dp, color = Muted, stroke = 2f)
      Spacer(Modifier.width(10.dp))
      Text("Search workshop or showroom", style = TextStyle(color = Muted, fontSize = 14.sp))
    }

    Row(Modifier.padding(start = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Seg("Workshop", tab == 0) { tab = 0 }
      Seg("Showroom", tab == 1) { tab = 1 }
    }

    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)).border(1.dp, dev.openvw.theme.Hairline, RoundedCornerShape(24.dp))) {
      OsmMap(
        Store.parked.lat, Store.parked.lon, Modifier.matchParentSize(), zoom = if (Store.parkedKnown) 15.0 else 10.0, interactive = true, showMarker = Store.parkedKnown, mode = mode,
        onMarkerClick = { carCard = !carCard },
      )
      MapModeToggle(mode, Modifier.align(Alignment.TopEnd).padding(10.dp)) { mode = it }
      // tapping the car pin offers a hand-off to Google Maps
      if (carCard && Store.parkedKnown) Row(
        Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp, end = 60.dp)
          .shadow(8.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Card).padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f, fill = false)) {
          Text("Your ${dev.openvw.data.CarLook.name()}", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
          Text(Store.parked.title, maxLines = 1, style = TextStyle(color = Muted, fontSize = 11.sp))
        }
        Spacer(Modifier.width(10.dp))
        SmallDark("Google Maps", Ic.navArrow) { GoogleMaps.pin(ctx, Store.parked.lat, Store.parked.lon) }
      }
    }

    // where the car is
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).card(24.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(Ink), contentAlignment = Alignment.Center) {
        VIcon(Ic.car, size = 20.dp, color = Card, stroke = 1.9f)
      }
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Text("YOUR CAR · ${Store.vehicle.status.uppercase()}", style = label())
        Spacer(Modifier.height(2.dp))
        Text(if (Store.parkedKnown) Store.parked.title else "Location not available yet", maxLines = 1, style = TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
        Text(
          if (Store.parkedKnown) listOf(Store.parked.sub, sinceLabel(Store.parked.epoch)).filter { it.isNotEmpty() }.joinToString(" · ")
          else "Shows after your next drive, once the engine is switched off",
          maxLines = 2, style = TextStyle(color = Muted, fontSize = 12.sp, lineHeight = 16.sp),
        )
      }
      if (Store.parkedKnown) {
        Spacer(Modifier.width(10.dp))
        CircleButton(Ic.navArrow, bg = Field, border = Field) { GoogleMaps.pin(ctx, Store.parked.lat, Store.parked.lon) }
      }
    }

    if (tab == 0) {
      Row(Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text("${Store.workshops.size} workshops", style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp))
        Text(" near you", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
      }
      Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Store.workshops.forEach { w ->
          Column(Modifier.card(24.dp).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
              Column(Modifier.weight(1f)) {
                Text(w.name, style = TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(4.dp))
                Text(w.address, style = TextStyle(color = Muted, fontSize = 12.sp))
              }
              CircleButton(Ic.heart, bg = Field, border = Field)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              val dest = "${w.name}, ${w.address}"
              SmallDark("Directions", Ic.navArrow, Modifier.weight(1f)) { GoogleMaps.directions(ctx, dest) }
              Row(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(99.dp)).background(Card)
                  .border(1.dp, dev.openvw.theme.Hairline, RoundedCornerShape(99.dp)).clickable { GoogleMaps.place(ctx, dest) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
              ) {
                VIcon(Ic.service, size = 14.dp, color = Ink, stroke = 1.9f)
                Spacer(Modifier.width(7.dp))
                Text("Book service", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
              }
            }
          }
        }
      }
    } else {
      Column(Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Card).border(1.dp, dev.openvw.theme.BorderStrong, RoundedCornerShape(24.dp)).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Showrooms near you", style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp))
        Text("Nearest Volkswagen showrooms", style = TextStyle(color = Muted, fontSize = 13.sp))
      }
    }
  }
}

/** Compact dark pill with a small accent arrow badge. */
@Composable
private fun SmallDark(text: String, icon: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Row(
    modifier.height(40.dp).clip(RoundedCornerShape(99.dp))
      .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(dev.openvw.theme.Ink2, Ink)))
      .clickable(onClick = onClick).padding(start = 6.dp, end = 14.dp),
    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
  ) {
    Box(Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(dev.openvw.theme.Accent), contentAlignment = Alignment.Center) {
      VIcon(icon, size = 12.dp, color = Card, stroke = 2.4f)
    }
    Spacer(Modifier.width(7.dp))
    Text(text, maxLines = 1, style = TextStyle(color = Card, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
  }
}
