package dev.openvw.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import dev.openvw.data.Mask
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.R
import dev.openvw.data.CarLook
import dev.openvw.data.Store
import dev.openvw.theme.Border
import dev.openvw.theme.Card
import dev.openvw.theme.Ink
import dev.openvw.theme.Line
import dev.openvw.theme.LineSoft
import dev.openvw.theme.Muted
import dev.openvw.theme.OnDarkMuted

@Composable
fun VehicleScreen(
  initialTab: VehicleTab,
  onBack: () -> Unit,
  privacy: Boolean = false,
  onPrivacy: (Boolean) -> Unit = {},
  onLogout: () -> Unit = {},
  onPaint: (String) -> Unit = {},
  onPrice: (Double) -> Unit = {},
  onDark: (Boolean) -> Unit = {},
) {
  var tab by remember { mutableStateOf(initialTab) }
  Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
    BackBar(onBack, showBack = false, trailing = {
      // quick light/dark toggle (same setting as Owner info → Dark mode)
      CircleButton(if (Store.dark) Ic.sun else Ic.moon) { onDark(!Store.dark) }
    })
    Text(if (tab == VehicleTab.OWNER) "Owner" else "Your car", style = H1, modifier = Modifier.padding(start = 20.dp, top = 4.dp))
    Row(Modifier.padding(start = 16.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Seg("Vehicle info", tab == VehicleTab.VEHICLE) { tab = VehicleTab.VEHICLE }
      Seg("Owner info", tab == VehicleTab.OWNER) { tab = VehicleTab.OWNER }
    }

    if (tab == VehicleTab.VEHICLE) {
      Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().height(200.dp).card(28.dp)) {
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).fillMaxWidth(0.72f).height(16.dp).background(Color(0x22101010), RoundedCornerShape(50)))
        Image(painterResource(CarLook.image(Store.vehicle.colour)), "Your ${Store.vehicle.variant}", Modifier.align(Alignment.Center).fillMaxWidth(0.86f).fillMaxHeight(0.9f), contentScale = ContentScale.Fit)
      }
      // car colour: follow the account, or pick manually
      ColourPicker(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), onPaint)

      // name + reg rows
      Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).card(24.dp).padding(horizontal = 18.dp)) {
        InfoRow("Vehicle name", Mask.vehicleName(Store.vehicle.vehicleName), mono = true)
        Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
        InfoRow("Vehicle reg. no.", if (privacy) Mask.reg("") else "Not added", muted = !privacy)
      }
      // specs grid
      Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).card(24.dp).padding(horizontal = 18.dp, vertical = 4.dp)) {
        Store.specs.chunked(2).forEach { pair ->
          Row(Modifier.fillMaxWidth()) {
            pair.forEach { s ->
              Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
                Text(s.label.uppercase(), style = label())
                Spacer(Modifier.height(4.dp))
                Text(s.value, style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp, fontFamily = if (s.mono) FontFamily.Monospace else FontFamily.Default))
              }
              Spacer(Modifier.width(14.dp))
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
          }
        }
      }
      HealthCard(Store.telemetry, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))

      OwnershipCard(
        Store.telemetry.odometerKm,
        Store.connectedSince,
        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
      )

      FuelSpendCard(
        Store.telemetry.odometerKm,
        Store.connectedSince,
        Store.drive.realKmpl,
        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        onPrice = onPrice,
      )

      DarkButton("We Connect", Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().height(52.dp))
    } else {
      // owner
      Row(Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Ink).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(30.dp)).background(Card), contentAlignment = Alignment.Center) {
          Text(Mask.initials(Store.owner.initials), style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
        }
        Spacer(Modifier.width(16.dp))
        Column {
          Text(Mask.name(Store.owner.proper), style = TextStyle(color = Card, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp))
          Text("PRIMARY OWNER · ${Store.vehicle.variant.uppercase()}", style = TextStyle(color = OnDarkMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp))
        }
      }
      Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).card(24.dp).padding(horizontal = 18.dp)) {
        InfoRow("Name", Mask.name(Store.owner.proper))
        Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
        InfoRow("Email ID", Store.owner.email.ifEmpty { "Add your email" }.let { Mask.email(it) }, muted = !privacy && Store.owner.email.isEmpty())
        Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
        InfoRow("Mobile number", Mask.mobile(Store.owner.mobile))
        Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
        InfoRow("Address", "Add your address", muted = true)
      }

      // Dark mode toggle
      SettingToggle("Dark mode", "Easier on the eyes at night", Store.dark, onDark)

      // Privacy mode toggle
      SettingToggle("Privacy mode", "Mask VIN, reg & personal info with mimic data", privacy, onPrivacy)

      Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(99.dp)).background(Card).border(1.dp, dev.openvw.theme.Hairline, RoundedCornerShape(99.dp)).clickable(onClick = onLogout),
          contentAlignment = Alignment.Center,
        ) {
          Text("Sign out", style = TextStyle(color = dev.openvw.theme.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
        DarkButton("Save changes", Modifier.weight(1f).height(54.dp))
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String, mono: Boolean = false, muted: Boolean = false) {
  Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
    Text(label.uppercase(), style = label())
    Spacer(Modifier.height(4.dp))
    Text(
      value,
      style = TextStyle(
        color = if (muted) Muted else Ink, fontSize = if (mono) 13.sp else 15.sp,
        fontWeight = if (muted) FontWeight.Normal else FontWeight.Bold,
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
      ),
    )
  }
}

@Composable
private fun ColourPicker(modifier: Modifier, onPaint: (String) -> Unit) {
  val pick = Store.paintPick
  val auto = CarLook.paint(Store.vehicle.colour)
  Column(modifier.card(24.dp).padding(horizontal = 18.dp, vertical = 14.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("CAR COLOUR", style = label())
      Text(
        if (pick.isEmpty()) "Auto · ${auto.label}" else CarLook.effective(Store.vehicle.colour).label,
        style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
      )
    }
    Spacer(Modifier.height(12.dp))
    val few = CarLook.paints().size < 4
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (few) Arrangement.spacedBy(12.dp) else Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      // "Auto" follows the colour on the account
      Box(
        Modifier.height(34.dp).clip(RoundedCornerShape(99.dp)).background(if (pick.isEmpty()) Ink else Card)
          .border(1.dp, if (pick.isEmpty()) Ink else Border, RoundedCornerShape(99.dp)).clickable { onPaint("") }.padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
      ) { Text("Auto", style = TextStyle(color = if (pick.isEmpty()) Card else Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)) }
      CarLook.paints().forEach { p ->
        val sel = pick == p.name
        Box(
          Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).border(2.dp, if (sel) Ink else Color.Transparent, RoundedCornerShape(17.dp))
            .clickable { onPaint(p.name) }.padding(4.dp).clip(RoundedCornerShape(13.dp)).background(Color(p.swatch))
            .border(1.dp, Color(0x22000000), RoundedCornerShape(13.dp)),
        )
      }
    }
  }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(
    Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).card(24.dp).padding(horizontal = 18.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text(title, style = TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold))
      Spacer(Modifier.height(2.dp))
      Text(subtitle, style = TextStyle(color = Muted, fontSize = 12.sp, lineHeight = 16.sp))
    }
    Spacer(Modifier.width(12.dp))
    Switch(
      checked = checked, onCheckedChange = onChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Card, checkedTrackColor = Ink,
        uncheckedThumbColor = Card, uncheckedTrackColor = dev.openvw.theme.Border, uncheckedBorderColor = dev.openvw.theme.Border,
      ),
    )
  }
}
