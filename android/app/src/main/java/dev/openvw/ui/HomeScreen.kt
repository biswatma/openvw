package dev.openvw.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.R
import dev.openvw.data.Mask
import dev.openvw.data.CarLook
import dev.openvw.data.Store
import dev.openvw.theme.Accent
import dev.openvw.theme.Border
import dev.openvw.theme.Card
import dev.openvw.theme.Chip
import dev.openvw.theme.Green
import dev.openvw.theme.GreenDark
import dev.openvw.theme.Ink
import dev.openvw.theme.Line
import dev.openvw.theme.LineSoft
import dev.openvw.theme.Muted
import dev.openvw.theme.Timeline
import dev.openvw.theme.MapBg
import dev.openvw.theme.Field
import kotlin.math.roundToInt

@Composable
fun HomeScreen(onGo: (Screen) -> Unit, onOwner: () -> Unit, playKey: Int, onShown: () -> Unit = {}, wide: Boolean = false) {
  val tele = Store.telemetry
  val v = Store.vehicle

  val t = remember { Animatable(0f) }
  var replay by remember { mutableIntStateOf(0) }
  var mapMode by remember { mutableStateOf(MapMode.STREET) }
  LaunchedEffect(playKey, replay) { onShown(); t.snapTo(0f); t.animateTo(1f, tween(2800, easing = LinearEasing)) }
  val tv by t.asState()

  val fuelK = easeOutPublic((tv - 0.2f) / 0.65f)
  val fuelFrac = fuelReveal(tv, tele.fuelPct)
  val battK = battReveal(tv)
  val battLo = 10.5; val battHi = 14.8
  val fuelText = (tele.fuelPct * fuelK).roundToInt().toString()
  val battText = "%.1f".format(battLo + (tele.batteryV - battLo) * battK)
  val battW = (((tele.batteryV - battLo) / (battHi - battLo)).coerceIn(0.0, 1.0) * battK).toFloat()

  // sections (arranged as one column on phones, two columns on wide screens)
  val header: @Composable () -> Unit = {
    // header
    Row(
      Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      Column {
        Text("Good morning", style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        Text(Mask.first(Store.owner.first), style = TextStyle(color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.0).sp))
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircleButton(Ic.bell, onClick = { onGo(Screen.ALERTS) })
        Box(
          Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(Ink).clickable { onOwner() },
          contentAlignment = Alignment.Center,
        ) { Text(Mask.initials(Store.owner.initials), style = TextStyle(color = Card, fontSize = 14.sp, fontWeight = FontWeight.Bold)) }
      }
    }
  }
  val hero: @Composable () -> Unit = {
    // hero: name + status, car on the left, fuel / battery / last trip on the right
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).card(28.dp)) {
      Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(Modifier.weight(1f)) {
          Text(v.variant, style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.66).sp))
          Spacer(Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Mask.vehicleName(v.vehicleName), maxLines = 1, style = TextStyle(color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace), modifier = Modifier.widthIn(max = 200.dp))
            Spacer(Modifier.width(6.dp))
            VIcon(Ic.edit, size = 14.dp, color = Muted, stroke = 1.8f)
          }
        }
        StatusPill(v.status)
      }
      Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        BoxWithConstraints(Modifier.weight(1f).height(104.dp)) {
          // soft elliptical ground shadow (radial gradient squashed vertically)
          androidx.compose.foundation.Canvas(Modifier.align(Alignment.BottomCenter).padding(start = 14.dp, end = 14.dp, bottom = 2.dp).fillMaxWidth().height(14.dp)) {
            scale(1f, size.height / size.width) {
              drawCircle(Brush.radialGradient(listOf(Color(0x380E0E10), Color.Transparent), radius = size.width / 2), radius = size.width / 2)
            }
          }
          Image(
            painterResource(CarLook.image(v.colour)), contentDescription = "Taigun GT Plus",
            // fixed box, image fitted inside: renders with different proportions (white/black/red) all stay below the VIN line
            Modifier.align(Alignment.BottomStart).offset(x = (-6).dp).padding(bottom = 6.dp).requiredSize(minOf(168.dp, maxWidth + 8.dp), 98.dp),
            contentScale = ContentScale.Fit, alignment = Alignment.BottomCenter,
          )
        }
        Column(Modifier.weight(1f).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
          val fuelColor = if (tele.fuelPct <= 15) dev.openvw.theme.Red else if (tele.fuelPct <= 25) dev.openvw.theme.Amber else Green
          HeroBar("Fuel", "$fuelText%", fuelFrac, fuelColor)
          HeroBar("Battery", "$battText V", battW, Green)
          Column {
            Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Last trip", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
              val tr = Store.trips.firstOrNull()
              Text(tr?.let { "${it.durMin} min · ${it.date}" } ?: "--", maxLines = 1, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold).merge(Tnum))
            }
          }
        }
      }
    }
  }
  val quick: @Composable () -> Unit = {
    // quick actions
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      QuickTile("Locate", Ic.navArrow, 15.dp, 2.2f, accent = true, Modifier.weight(1f)) { onGo(Screen.LOCATE) }
      QuickTile("Trips", Ic.route + " M4 19a2 2 0 1 0 4 0 2 2 0 1 0-4 0z M16 5a2 2 0 1 0 4 0 2 2 0 1 0-4 0z", 16.dp, 2f, accent = false, Modifier.weight(1f)) { onGo(Screen.TRIPS) }
      QuickTile("Service", Ic.service, 17.dp, 1.9f, accent = false, Modifier.weight(1f)) { onGo(Screen.LOCATE) }
      QuickTile("Documents", "M7 3.5h7l4 4V20.5H7z M14 3.5v4h4M9.5 12h6M9.5 15.5h6", 16.dp, 2f, accent = false, Modifier.weight(1f)) { onGo(Screen.VEHICLE) }
    }
  }
  val cluster: @Composable () -> Unit = {
    // ---- live cluster ----
    SectionHeader("Live cluster", Modifier.padding(top = 28.dp, bottom = 12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) {
      if (Store.liveConnected) {
        Row(Modifier.padding(end = 8.dp).height(36.dp).clip(RoundedCornerShape(99.dp)).background(Ink).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(Green))
          Spacer(Modifier.width(6.dp))
          Text("Live", style = TextStyle(color = Card, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        }
      }
      Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(99.dp)).background(Card).border(1.dp, dev.openvw.theme.Hairline, RoundedCornerShape(99.dp))
          .clickable { replay++ }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        VIcon(Ic.replay, size = 13.dp, color = Ink, stroke = 2.4f)
        Spacer(Modifier.width(6.dp))
        Text("Replay", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
      }
    } }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      ClusterCard(tv, tele, v.status, Modifier.padding(horizontal = 16.dp))
    }

    Box(
      Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth().height(48.dp)
        .clip(RoundedCornerShape(16.dp)).border(1.dp, dev.openvw.theme.BorderStrong, RoundedCornerShape(16.dp))
        .clickable { onGo(Screen.VEHICLE) },
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("All vehicle parameters", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.width(6.dp))
        VIcon(Ic.chevronR, size = 16.dp, color = Ink, stroke = 2f)
      }
    }
  }
  val recent: @Composable () -> Unit = {
    // recent trip
    SectionHeader("Recent trip", Modifier.padding(top = 28.dp, bottom = 10.dp)) {
      Row(Modifier.clickable { onGo(Screen.TRIPS) }, verticalAlignment = Alignment.CenterVertically) {
        Text("History", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        VIcon(Ic.chevronR, size = 16.dp, color = Ink, stroke = 2f)
      }
    }
    Store.trips.firstOrNull()?.let { tr -> Column(Modifier.padding(horizontal = 16.dp).card(24.dp).padding(18.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(tr.durMin.toString(), style = TextStyle(color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold).merge(Tnum))
          Spacer(Modifier.width(8.dp))
          Text(if (tr.km > 0) "min · %.1f km · %s".format(tr.km, tr.date) else "min · ${tr.date}", style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        }
        Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Chip).border(1.dp, Border, RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 7.dp)) {
          Text(tr.type, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold))
        }
      }
      Spacer(Modifier.height(14.dp))
      TripRoute(tr.start, tr.end, tr.fromTitle, tr.fromSub, tr.toTitle, tr.toSub)
    } }
  }
  val parked: @Composable () -> Unit = {
    // map
    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(MapBg).border(1.dp, dev.openvw.theme.Hairline, RoundedCornerShape(24.dp))) {
      val park = Store.parked
      OsmMap(park.lat, park.lon, Modifier.matchParentSize(), zoom = 15.0, interactive = false, showMarker = false, mode = mapMode)
      MapModeToggle(mapMode, Modifier.align(Alignment.TopEnd).padding(10.dp)) { mapMode = it }
      Box(Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(10.dp)).background(Card).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Column {
          Text("Last parked here", style = TextStyle(color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold))
          Text(park.title, maxLines = 1, style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.width(140.dp))
        }
      }
      Box(Modifier.align(Alignment.Center).size(36.dp).clip(RoundedCornerShape(18.dp)).background(Ink).border(4.dp, Card, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        VIcon(Ic.car, size = 20.dp, color = Card, stroke = 1.9f)
      }
      DarkButton(
        "Locate & track",
        Modifier.align(Alignment.BottomEnd).padding(14.dp).wrapContentHeight(),
        leading = { AccentBadge { VIcon(Ic.navArrow, size = 14.dp, color = Card, stroke = 2.2f) } },
        onClick = { onGo(Screen.LOCATE) },
      )
    }
  }

  if (!wide) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
      header(); hero(); quick(); cluster(); recent(); parked()
    }
  } else {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
      header()
      Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) { hero(); quick(); parked() }
        Column(Modifier.weight(1f)) { cluster(); recent() }
      }
    }
  }
}

@Composable
private fun HeroBar(label: String, value: String, frac: Float, color: Color) {
  Column {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
      Text(value, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold).merge(Tnum))
    }
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Line)) {
      Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(5.dp).clip(RoundedCornerShape(3.dp)).background(color))
    }
  }
}

@Composable
private fun QuickTile(label: String, icon: String, iconSize: androidx.compose.ui.unit.Dp, stroke: Float, accent: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Column(
    modifier.height(76.dp).card(22.dp).clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(if (accent) Accent else Chip), contentAlignment = Alignment.Center) {
      VIcon(icon, size = iconSize, color = if (accent) Card else Ink, stroke = stroke)
    }
    Spacer(Modifier.height(6.dp))
    Text(label, maxLines = 1, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
  }
}

@Composable
private fun StatusPill(status: String) {
  Row(
    Modifier.height(28.dp).clip(RoundedCornerShape(99.dp)).background(Ink).padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Accent))
    Spacer(Modifier.width(6.dp))
    Text(status.uppercase(), style = TextStyle(color = Card, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp))
  }
}

@Composable
private fun StatCell(label: String, value: String, unit: String, modifier: Modifier = Modifier, valueColor: Color = Ink, border: Boolean = false) {
  Column(modifier.then(if (border) Modifier.border(0.dp, Color.Transparent) else Modifier).padding(horizontal = 14.dp, vertical = 12.dp)) {
    Text(label.uppercase(), style = dev.openvw.ui.label())
    Spacer(Modifier.height(2.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(value, style = TextStyle(color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold).merge(Tnum))
      Text(" $unit", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
  }
}

@Composable
fun TripRoute(start: String, end: String, fromTitle: String, fromSub: String, toTitle: String, toSub: String) {
  Row(Modifier.fillMaxWidth()) {
    Column(Modifier.width(62.dp)) {
      Text(start, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold).merge(Tnum))
      Spacer(Modifier.height(30.dp))
      Text(end, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold).merge(Tnum))
    }
    Column(Modifier.width(16.dp).padding(top = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).border(2.dp, Ink, RoundedCornerShape(5.dp)))
      Box(Modifier.width(1.5.dp).height(28.dp).background(Timeline))
      Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Ink))
    }
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
      Text(fromTitle, maxLines = 1, style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold))
      Text(fromSub, maxLines = 1, style = TextStyle(color = Muted, fontSize = 12.sp))
      Spacer(Modifier.height(10.dp))
      Text(toTitle, maxLines = 1, style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold))
      Text(toSub, maxLines = 1, style = TextStyle(color = Muted, fontSize = 12.sp))
    }
  }
}
