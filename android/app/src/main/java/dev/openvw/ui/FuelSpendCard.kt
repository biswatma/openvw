package dev.openvw.ui

import androidx.compose.foundation.background
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.openvw.theme.Accent
import dev.openvw.theme.Ink
import dev.openvw.theme.LineSoft
import dev.openvw.theme.Muted
import dev.openvw.theme.Track
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Rough fuel-cost estimate from odometer, Taigun mileage and petrol price (owner's, or Bengaluru by default). */
object FuelCalc {
  const val MILEAGE = 13.0      // km/l — Taigun 1.5 TSI DSG, real-world city/mixed
  const val DEFAULT_PRICE = 105.0 // ₹/L — avg Bengaluru petrol over the ownership period
  const val TANK_L = 50.0       // Taigun fuel tank

  val custom: Boolean get() = dev.openvw.data.Store.fuelPrice > 0
  val AVG_PRICE: Double get() = if (custom) dev.openvw.data.Store.fuelPrice else DEFAULT_PRICE

  fun mileage(real: Double) = if (real in 6.0..28.0) real else MILEAGE
  fun litres(km: Int, m: Double) = km / m
  fun total(km: Int, m: Double) = litres(km, m) * AVG_PRICE
  fun perKm(m: Double) = AVG_PRICE / m
  fun perMonth(km: Int, days: Long, m: Double): Double {
    val mo = (days / 30.44).coerceAtLeast(1.0)
    return total(km, m) / mo
  }

  // Spend for the last 6 months (even distance × approx monthly price, scaled to the owner's price).
  fun monthly(km: Int, days: Long, m: Double): List<Pair<String, Double>> {
    val mo = (days / 30.44).coerceAtLeast(1.0)
    val kmPerMonth = km / mo
    val prices = listOf(104.0, 105.0, 107.0, 109.0, 110.0, 110.82).map { it * AVG_PRICE / DEFAULT_PRICE } // oldest → newest
    val fmt = SimpleDateFormat("MMM", Locale.US)
    return (5 downTo 0).toList().mapIndexed { i, back ->
      val c = Calendar.getInstance().apply { add(Calendar.MONTH, -back) }
      fmt.format(c.time) to (kmPerMonth / m) * prices[i]
    }
  }
}

private fun money(v: Double) = "₹%,d".format(v.toInt())

@Composable
fun FuelSpendCard(
  odometerKm: Int?,
  connectedSinceIso: String,
  measuredMileage: Double = 0.0,
  modifier: Modifier = Modifier,
  onPrice: (Double) -> Unit = {},
) {
  var editing by remember { mutableStateOf(false) }
  if (editing) PriceDialog(onDismiss = { editing = false }) { onPrice(it); editing = false }
  val km = odometerKm ?: 0
  val days = Ownership.daysSince(connectedSinceIso)
  val isReal = measuredMileage in 6.0..28.0
  val m = FuelCalc.mileage(measuredMileage)
  val total = FuelCalc.total(km, m)
  val litres = FuelCalc.litres(km, m)
  val perMonth = FuelCalc.perMonth(km, days, m)
  val months = FuelCalc.monthly(km, days, m)
  val maxSpend = (months.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)

  Column(modifier.card(24.dp).padding(18.dp)) {
    Text(if (isReal) "FUEL SPEND · BASED ON REAL MILEAGE" else "FUEL SPEND · ESTIMATED", style = label())
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(if (km > 0) money(total) else "--", style = TextStyle(color = Ink, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.0).sp).merge(Tnum))
      Text("  since ${Ownership.formatDate(connectedSinceIso)}", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }

    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth()) {
      Stat2("Fuel used", "${litres.toInt()} L", Modifier.weight(1f))
      Stat2("Avg price", "₹${FuelCalc.AVG_PRICE.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}/L", Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { editing = true }, editable = true)
      Stat2("Per km", "₹%.1f".format(FuelCalc.perKm(m)), Modifier.weight(1f))
      Stat2("Per month", money(perMonth), Modifier.weight(1.1f))
    }

    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
    Spacer(Modifier.height(14.dp))

    // mini 6-month bar chart
    Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
      months.forEach { (lbl, spend) ->
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
          Text("₹${(spend / 1000).let { "%.1f".format(it) }}k", style = TextStyle(color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold))
          Spacer(Modifier.height(3.dp))
          Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(5.dp)).background(Track), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().fillMaxHeight((spend / maxSpend).toFloat().coerceIn(0.08f, 1f)).clip(RoundedCornerShape(5.dp)).background(Accent))
          }
          Spacer(Modifier.height(4.dp))
          Text(lbl, style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
        }
      }
    }

    Spacer(Modifier.height(12.dp))
    val priceNote = if (FuelCalc.custom) "your petrol price ₹%.2f/L".format(FuelCalc.AVG_PRICE) else "avg Bengaluru petrol ₹${FuelCalc.DEFAULT_PRICE.toInt()}/L (tap Avg price to set yours)"
    Text(
      if (isReal) "Based on measured %.1f km/l from your trips × %s.".format(m, priceNote)
      else "Rough estimate — ${FuelCalc.MILEAGE.toInt()} km/l (Taigun 1.5 TSI, city/mixed) × $priceNote.",
      style = TextStyle(color = Muted, fontSize = 11.sp, lineHeight = 15.sp),
    )
  }
}

@Composable
private fun Stat2(label: String, value: String, modifier: Modifier = Modifier, editable: Boolean = false) {
  Column(modifier) {
    Text(label.uppercase(), style = dev.openvw.ui.label(size = 9))
    Spacer(Modifier.height(3.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(value, style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start).merge(Tnum))
      if (editable) { Spacer(Modifier.width(4.dp)); VIcon(Ic.edit, size = 12.dp, color = Accent, stroke = 2f) }
    }
  }
}

/** Enter your city's average petrol price (₹/L); "Use default" goes back to Bengaluru's. */
@Composable
private fun PriceDialog(onDismiss: () -> Unit, onSave: (Double) -> Unit) {
  var text by remember { mutableStateOf(if (FuelCalc.custom) "%.2f".format(FuelCalc.AVG_PRICE) else "") }
  val value = text.replace(',', '.').toDoubleOrNull()
  val valid = value != null && value in 50.0..300.0
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = dev.openvw.theme.Card,
    title = { Text("Average petrol price", style = TextStyle(color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)) },
    text = {
      Column {
        Text("Used for fuel spend estimates. Enter the average price in your city.", style = TextStyle(color = Muted, fontSize = 13.sp, lineHeight = 18.sp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
          value = text, onValueChange = { t -> text = t.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
          singleLine = true, prefix = { Text("₹ ") }, suffix = { Text(" / L") },
          placeholder = { Text("%.2f".format(FuelCalc.DEFAULT_PRICE)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          isError = text.isNotEmpty() && !valid,
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, cursorColor = Ink),
        )
      }
    },
    confirmButton = {
      TextButton(enabled = valid, onClick = { onSave(value!!) }) { Text("Save", color = if (valid) Ink else Muted, fontWeight = FontWeight.Bold) }
    },
    dismissButton = {
      TextButton(onClick = { onSave(0.0) }) { Text("Use default", color = Muted) }
    },
  )
}
