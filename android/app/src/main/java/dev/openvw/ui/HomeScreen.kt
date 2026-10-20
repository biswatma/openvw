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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
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
import dev.openvw.theme.MapBg
import dev.openvw.theme.Field
import dev.openvw.theme.DrumWell
import dev.openvw.theme.DrumBg
import dev.openvw.theme.DrumBorder
import dev.openvw.theme.Timeline
import kotlin.math.roundToInt

@Composable
fun HomeScreen(onGo: (Screen) -> Unit, onOwner: () -> Unit, playKey: Int, onShown: () -> Unit = {}, wide: Boolean = false, onOpenTrip: (dev.openvw.data.Trip) -> Unit = {}) {
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
  val odometer: @Composable () -> Unit = {
    val odo = Store.telemetry.odometerKm
    // roll the drums up from 0 to the real reading, using the same self-test timing as the cluster
    val odoK = easeOutPublic((tv - 0.15f) / 0.85f)
    Row(
      Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 14.dp)
        .fillMaxWidth()
        .card(24.dp)
        .padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("ODOMETER", style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp))
        Text("Total distance", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
      }
      if (odo == null) {
        Text("-- km", style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold).merge(Tnum))
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(DrumWell)
              .border(1.dp, DrumBorder, RoundedCornerShape(10.dp))
              .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            for (i in 5 downTo 0) {
              val p = Math.pow(10.0, i.toDouble()).toLong()
              val targetDigit = ((odo / p) % 10).toInt()
              val extraSpins = if (i == 0) 10f else 0f
              val animatedPos = (targetDigit + extraSpins) * odoK
              val pos = if (tv >= 1f) targetDigit.toFloat() else animatedPos
              val units = i == 0
              DigitDrum(
                pos = pos,
                bg = if (units) Ink else DrumBg,
                fg = if (units) Card else Ink,
              )
            }
          }
          Text(
            "km",
            style = TextStyle(
              color = Muted,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
            ),
          )
        }
      }
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
    listOfNotNull(Store.trips.firstOrNull(), Store.pendingDrive).maxByOrNull { it.epoch }?.let { tr -> Column(Modifier.padding(horizontal = 16.dp).card(24.dp).clickable { onOpenTrip(tr) }.padding(18.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(tr.durMin.toString(), style = TextStyle(color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold).merge(Tnum))
          Spacer(Modifier.width(8.dp))
          Text(if (tr.km > 0) "min · %.1f km · %s".format(tr.km, tr.date) else "min · ${tr.date}", style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        }
        if (tr.pending)
          Box(Modifier.clip(RoundedCornerShape(99.dp)).background(dev.openvw.theme.AmberSoftBg).padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text("Finalizing…", style = TextStyle(color = dev.openvw.theme.AmberDark, fontSize = 12.sp, fontWeight = FontWeight.Bold))
          }
        else
          Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Chip).border(1.dp, Border, RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text(tr.type, style = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold))
          }
      }
      Spacer(Modifier.height(14.dp))
      TripRoute(tr.start, tr.end, tr.fromTitle, tr.fromSub, tr.toTitle, tr.toSub)
      if (tr.pending) {
        Spacer(Modifier.height(10.dp))
        Text("Full trip stats arrive a little after you park.", style = TextStyle(color = Muted, fontSize = 11.sp))
      }
    } }
  }
  val parked: @Composable () -> Unit = {
    // map
    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(MapBg).border(1.dp, dev.openvw.theme.Hairline, RoundedCornerShape(24.dp))) {
      val park = Store.parked
      if (Store.parkedKnown) {
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
      } else {
        // no real location yet — don't show a placeholder city
        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          VIcon(Ic.pin, size = 26.dp, color = Muted, stroke = 1.8f)
          Spacer(Modifier.height(8.dp))
          Text("Location appears after your next drive", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        }
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
      header(); hero(); odometer(); cluster(); recent(); parked()
    }
  } else {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
      header()
      Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) { hero(); odometer(); parked() }
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
private fun DigitDrum(
  pos: Float,
  bg: Color,
  fg: Color,
  modifier: Modifier = Modifier,
) {
  val itemH = 38.dp
  val itemW = 26.dp
  val norm = ((pos % 10f) + 10f) % 10f
  val curD = norm.toInt() % 10
  val nextD = (curD + 1) % 10
  val frac = norm - norm.toInt()

  Box(
    modifier
      .width(itemW)
      .height(itemH)
      .clip(RoundedCornerShape(6.dp))
      .background(bg),
    contentAlignment = Alignment.Center,
  ) {
    // Current digit (slides up as frac increases)
    Box(
      Modifier
        .fillMaxSize()
        .offset(y = -(frac * itemH.value).dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        curD.toString(),
        style = TextStyle(
          color = fg,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        ).merge(Tnum),
      )
    }

    // Next digit (slides in from bottom when rolling)
    if (frac > 0.001f) {
      Box(
        Modifier
          .fillMaxSize()
          .offset(y = ((1f - frac) * itemH.value).dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          nextD.toString(),
          style = TextStyle(
            color = fg,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          ).merge(Tnum),
        )
      }
    }

    // Cylindrical curvature gloss:
    // Convex mechanical roller lighting with top and bottom shadows
    Box(
      Modifier
        .matchParentSize()
        .background(
          Brush.verticalGradient(
            0.0f to Color(0x35000000),
            0.18f to Color(0x0A000000),
            0.35f to Color.Transparent,
            0.65f to Color.Transparent,
            0.82f to Color(0x0A000000),
            1.0f to Color(0x38000000),
          )
        )
    )

    // Subtle edge highlight to give 3D tumbler wheel separation
    Box(
      Modifier
        .matchParentSize()
        .border(0.5.dp, Color(0x18000000), RoundedCornerShape(6.dp))
    )
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
