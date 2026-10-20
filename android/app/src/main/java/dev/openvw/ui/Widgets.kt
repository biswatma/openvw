package dev.openvw.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.theme.Accent
import dev.openvw.theme.Chip
import dev.openvw.theme.Border
import dev.openvw.theme.BorderStrong
import dev.openvw.theme.Card
import dev.openvw.theme.Ink
import dev.openvw.theme.Ink2
import dev.openvw.theme.Line
import dev.openvw.theme.Muted
import dev.openvw.theme.Muted2

fun Modifier.card(radius: Dp = 24.dp, border: Color = Line, bg: Color = Card): Modifier =
  this.clip(RoundedCornerShape(radius)).background(bg).border(1.dp, border, RoundedCornerShape(radius))

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
  Row(
    modifier.fillMaxWidth().padding(horizontal = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(title, style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp))
    trailing?.invoke()
  }
}

@Composable
fun Seg(text: String, selected: Boolean, onClick: () -> Unit) {
  Box(
    Modifier
      .height(40.dp)
      .clip(RoundedCornerShape(99.dp))
      .background(if (selected) Ink else Color.Transparent)
      .border(1.dp, if (selected) Ink else BorderStrong, RoundedCornerShape(99.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 18.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text, style = TextStyle(color = if (selected) Card else Color(0xFF3A3A40), fontSize = 13.sp, fontWeight = FontWeight.Bold))
  }
}

@Composable
fun DarkButton(text: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null, onClick: () -> Unit = {}) {
  Row(
    modifier
      .height(48.dp)
      .clip(RoundedCornerShape(99.dp))
      .background(Brush.verticalGradient(listOf(Ink2, Ink)))
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    if (leading != null) { leading(); Spacer(Modifier.width(8.dp)) }
    Text(text, style = TextStyle(color = Card, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
  }
}

@Composable
fun AccentBadge(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Box(modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(Accent), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun BottomNav(current: Screen, onGo: (Screen) -> Unit) {
  val items = listOf(
    Triple(Screen.HOME, Ic.home, "Home"),
    Triple(Screen.TRIPS, Ic.route, "Trips"),
    Triple(Screen.LOCATE, Ic.pin, "Locate"),
    Triple(Screen.ALERTS, Ic.bell, "Updates"),
    Triple(Screen.VEHICLE, Ic.car, "My car"),
  )
  Row(
    Modifier.fillMaxWidth().background(Card).border(0.dp, Color.Transparent)
      .padding(top = 6.dp, bottom = 10.dp, start = 8.dp, end = 8.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    items.forEach { (scr, path, lbl) ->
      val active = current == scr
      Column(
        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onGo(scr) }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        VIcon(path, size = 22.dp, color = if (active) Ink else Muted2, stroke = 1.8f)
        Spacer(Modifier.height(4.dp))
        Text(lbl, style = TextStyle(color = if (active) Ink else Muted2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(if (active) Accent else Color.Transparent))
      }
    }
  }
}

@Composable
fun BackBar(onBack: () -> Unit, showBack: Boolean = true, trailing: (@Composable () -> Unit)? = null) {
  Row(
    Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // keep a 44dp slot even without a back button, so the title and trailing stay put
    if (showBack) Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
      VIcon(Ic.chevronBack, size = 22.dp, color = Ink, stroke = 2f)
    } else Box(Modifier.size(44.dp))
    trailing?.invoke()
  }
}

@Composable
fun CircleButton(path: String, modifier: Modifier = Modifier, bg: Color = Card, fg: Color = Ink, border: Color = Border, onClick: () -> Unit = {}) {
  Box(
    modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(bg).border(1.dp, border, RoundedCornerShape(22.dp)).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) { VIcon(path, size = 20.dp, color = fg, stroke = 1.7f) }
}

val H1 get() = TextStyle(color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.0).sp)
val Mono get() = TextStyle(color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)

/** Vertical navigation for wide screens (foldable inner display, tablets). */
@Composable
fun NavRail(current: Screen, onGo: (Screen) -> Unit) {
  val items = listOf(
    Triple(Screen.HOME, Ic.home, "Home"),
    Triple(Screen.TRIPS, Ic.route, "Trips"),
    Triple(Screen.LOCATE, Ic.pin, "Locate"),
    Triple(Screen.ALERTS, Ic.bell, "Updates"),
    Triple(Screen.VEHICLE, Ic.car, "My car"),
  )
  Column(
    Modifier.fillMaxHeight().width(88.dp).padding(vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    items.forEach { (scr, path, lbl) ->
      val active = current == scr
      Column(
        Modifier.width(72.dp).clip(RoundedCornerShape(18.dp)).background(if (active) Chip else Color.Transparent)
          .clickable { onGo(scr) }.padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        VIcon(path, size = 22.dp, color = if (active) Ink else Muted2, stroke = 1.8f)
        Spacer(Modifier.height(4.dp))
        Text(lbl, style = TextStyle(color = if (active) Ink else Muted2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(if (active) Accent else Color.Transparent))
      }
    }
  }
}
