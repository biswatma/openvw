package dev.openvw.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import dev.openvw.theme.Accent
import dev.openvw.theme.Border
import dev.openvw.theme.Card
import dev.openvw.theme.Chip
import dev.openvw.theme.Ink
import dev.openvw.theme.Muted
import dev.openvw.theme.OnDarkMuted
import dev.openvw.theme.Red

@Composable
fun TripsScreen(
  onBack: () -> Unit,
  onOpenTrip: (Trip) -> Unit = {},
  onSearch: (Long, Long) -> Unit = { _, _ -> },
  onClearSearch: () -> Unit = {},
) {
  var tab by remember { mutableStateOf(0) }
  var picking by remember { mutableStateOf(false) }
  Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
    BackBar(onBack, showBack = false)
    Text("Your trips", style = H1, modifier = Modifier.padding(start = 20.dp, top = 4.dp))
    Row(Modifier.padding(start = 16.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Seg("Overview", tab == 0) { tab = 0 }
      Seg("Statistics", tab == 1) { tab = 1 }
    }

    if (tab == 0) DateFilterBar(onPreset = onSearch, onCustom = { picking = true }, onClear = onClearSearch)

    val d = Store.drive
    val pending = Store.pendingDrive?.takeIf { p -> Store.trips.none { it.epoch == p.epoch } }
    if (tab == 0 && Store.tripRange != null) {
      SearchResults(onOpenTrip)
    } else if (tab == 0 && Store.trips.isEmpty() && pending == null) {
      EmptyState(Ic.route + " M4 19a2 2 0 1 0 4 0 2 2 0 1 0-4 0z M16 5a2 2 0 1 0 4 0 2 2 0 1 0-4 0z", "No trips yet", "Your trips show up here after you drive.")
    } else if (tab == 0) {
      val live = d.tripCount > 0
      if (Store.trips.isNotEmpty()) Row(
        Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ink).padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        DarkStat("This week", (if (live) d.weekTrips else Store.trips.size).toString(), null, Modifier.weight(1f))
        DarkStat("Time on road", (if (live) d.weekMin else Store.trips.sumOf { it.durMin }).toString(), "min", Modifier.weight(1f))
        if (live) DarkStat("Distance", "%.0f".format(d.weekKm), "km", Modifier.weight(1f))
      }
      if (pending != null) {
        Text("LATEST DRIVE", style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp), modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 10.dp))
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
          Column(Modifier.card(24.dp).clickable { onOpenTrip(pending) }.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.Bottom) {
                Text(pending.durMin.toString(), style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold).merge(Tnum))
                Text(" min", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
              }
              Box(Modifier.clip(RoundedCornerShape(99.dp)).background(dev.openvw.theme.AmberSoftBg).padding(horizontal = 12.dp, vertical = 7.dp)) {
                Text("Finalizing…", style = TextStyle(color = dev.openvw.theme.AmberDark, fontSize = 12.sp, fontWeight = FontWeight.Bold))
              }
            }
            Spacer(Modifier.height(14.dp))
            TripRoute(pending.start, pending.end, pending.fromTitle, pending.fromSub, pending.toTitle, pending.toSub)
            Spacer(Modifier.height(10.dp))
            Text("Full trip stats arrive a little after you park.", style = TextStyle(color = Muted, fontSize = 11.sp))
          }
        }
      }
      TripDays(Store.trips, onOpenTrip)
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
      EmptyState(Ic.fuel, "No trip data", "Time to take your ride for a spin.")
    }
  }

  if (picking) TripRangePicker(
    initial = Store.tripRange,
    onDismiss = { picking = false },
    onPick = { a, b -> picking = false; onSearch(a, b) },
  )
}

// ---------- date filter ----------

/** Quick ranges as date-picker values (UTC midnight of each calendar day). */
private enum class Preset(val label: String) { WEEK("Last 7 days"), MONTH30("Last 30 days"), THIS_MONTH("This month"), LAST_MONTH("Last month") }

private fun presetRange(p: Preset): Pair<Long, Long> {
  val now = Calendar.getInstance()
  val start = Calendar.getInstance(); val end = Calendar.getInstance()
  when (p) {
    Preset.WEEK -> start.add(Calendar.DAY_OF_YEAR, -6)
    Preset.MONTH30 -> start.add(Calendar.DAY_OF_YEAR, -29)
    Preset.THIS_MONTH -> start.set(Calendar.DAY_OF_MONTH, 1)
    Preset.LAST_MONTH -> {
      start.add(Calendar.MONTH, -1); start.set(Calendar.DAY_OF_MONTH, 1)
      end.timeInMillis = now.timeInMillis; end.set(Calendar.DAY_OF_MONTH, 1); end.add(Calendar.DAY_OF_YEAR, -1)
    }
  }
  return utcDay(start) to utcDay(end)
}

/** Local calendar day -> the date picker's representation (that day at 00:00 UTC). */
private fun utcDay(c: Calendar): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
  clear(); set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
}.timeInMillis

private fun fmtDay(utcMs: Long, withYear: Boolean): String =
  SimpleDateFormat(if (withYear) "d MMM yyyy" else "d MMM", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(utcMs))

private fun rangeLabel(r: Pair<Long, Long>): String {
  val y = { ms: Long -> Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = ms }.get(Calendar.YEAR) }
  val thisYear = Calendar.getInstance().get(Calendar.YEAR)
  if (r.first == r.second) return fmtDay(r.first, y(r.first) != thisYear)
  val sameYear = y(r.first) == y(r.second)
  return fmtDay(r.first, !sameYear) + " – " + fmtDay(r.second, !sameYear || y(r.second) != thisYear)
}

@Composable
private fun DateFilterBar(onPreset: (Long, Long) -> Unit, onCustom: () -> Unit, onClear: () -> Unit) {
  val range = Store.tripRange
  val active = Preset.entries.firstOrNull { range != null && presetRange(it) == range }
  Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
    // main bar: shows the picked range, opens the calendar
    Row(
      Modifier.padding(horizontal = 16.dp).fillMaxWidth().card(20.dp).clickable(onClick = onCustom).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(if (range != null) Ink else Chip), contentAlignment = Alignment.Center) {
        VIcon(Ic.calendar, size = 18.dp, color = if (range != null) Card else Ink, stroke = 1.8f)
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text(if (range != null) "SHOWING TRIPS FROM" else "FIND TRIPS BY DATE", style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp))
        Spacer(Modifier.height(2.dp))
        Text(if (range != null) rangeLabel(range) else "Pick a date range", style = TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold).merge(Tnum))
      }
      if (range != null) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClear), contentAlignment = Alignment.Center) {
          VIcon("M6 6l12 12 M18 6L6 18", size = 18.dp, color = Muted, stroke = 2f)
        }
      } else {
        VIcon(Ic.chevronR, size = 18.dp, color = Muted, stroke = 2f)
        Spacer(Modifier.width(6.dp))
      }
    }
    // quick ranges
    Row(
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Preset.entries.forEach { p ->
        val on = p == active
        Box(
          Modifier.clip(RoundedCornerShape(99.dp)).background(if (on) Ink else Card).border(1.dp, if (on) Ink else Border, RoundedCornerShape(99.dp))
            .clickable { if (on) onClear() else presetRange(p).let { onPreset(it.first, it.second) } }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
          Text(p.label, style = TextStyle(color = if (on) Card else Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripRangePicker(initial: Pair<Long, Long>?, onDismiss: () -> Unit, onPick: (Long, Long) -> Unit) {
  val today = utcDay(Calendar.getInstance())
  val state = rememberDateRangePickerState(
    initialSelectedStartDateMillis = initial?.first,
    initialSelectedEndDateMillis = initial?.second,
    yearRange = 2020..Calendar.getInstance().get(Calendar.YEAR),
    selectableDates = object : SelectableDates {
      override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
      override fun isSelectableYear(year: Int) = year <= Calendar.getInstance().get(Calendar.YEAR)
    },
  )
  val colors = DatePickerDefaults.colors(
    containerColor = Card,
    titleContentColor = Muted,
    headlineContentColor = Ink,
    weekdayContentColor = Muted,
    subheadContentColor = Muted,
    navigationContentColor = Ink,
    yearContentColor = Ink,
    currentYearContentColor = Accent,
    selectedYearContainerColor = Ink,
    selectedYearContentColor = Card,
    dayContentColor = Ink,
    disabledDayContentColor = Muted.copy(alpha = 0.4f),
    selectedDayContainerColor = Ink,
    selectedDayContentColor = Card,
    todayContentColor = Accent,
    todayDateBorderColor = Accent,
    dayInSelectionRangeContainerColor = Chip,
    dayInSelectionRangeContentColor = Ink,
    dividerColor = Border,
  )
  val start = state.selectedStartDateMillis
  val end = state.selectedEndDateMillis
  DatePickerDialog(
    onDismissRequest = onDismiss,
    colors = colors,
    confirmButton = {
      TextButton(onClick = { if (start != null) onPick(start, end ?: start) }, enabled = start != null) {
        Text("Show trips", style = TextStyle(color = if (start != null) Ink else Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel", style = TextStyle(color = Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)) }
    },
  ) {
    DateRangePicker(
      state = state,
      colors = colors,
      modifier = Modifier.weight(1f),
      title = { Text("Find trips", style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
      headline = {
        Text(
          when {
            start == null -> "Pick start date"
            end == null -> fmtDay(start, true) + " – …"
            else -> rangeLabel(start to end)
          },
          style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
          modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
        )
      },
      showModeToggle = false,
    )
  }
}

@Composable
private fun SearchResults(onOpenTrip: (Trip) -> Unit) {
  val res = Store.tripResults
  val err = Store.tripSearchError
  when {
    Store.tripSearching && res.isEmpty() -> Column(Modifier.fillMaxWidth().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      CircularProgressIndicator(color = Ink, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
      Spacer(Modifier.height(14.dp))
      Text("Finding your trips…", style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
    err != null -> Column(Modifier.fillMaxWidth().padding(top = 48.dp, start = 32.dp, end = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Couldn't load trips", style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold))
      Spacer(Modifier.height(4.dp))
      Text(err, style = TextStyle(color = Red, fontSize = 13.sp))
    }
    res.isEmpty() -> EmptyState(Ic.calendar, "No trips in this range", "Try a different set of dates.")
    else -> {
      val km = res.sumOf { it.km }
      val min = res.sumOf { it.durMin }
      Row(
        Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Ink).padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        DarkStat("Trips", res.size.toString(), null, Modifier.weight(1f))
        DarkStat("Distance", "%.0f".format(km), "km", Modifier.weight(1f))
        DarkStat("Driving", if (min < 120) min.toString() else "%.1f".format(min / 60.0), if (min < 120) "min" else "h", Modifier.weight(1f))
      }
      TripDays(res, onOpenTrip)
    }
  }
}

// ---------- shared list ----------

@Composable
private fun TripDays(trips: List<Trip>, onOpenTrip: (Trip) -> Unit) {
  trips.groupBy { dayKey(it) }.forEach { (_, day) ->
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
      Text(dayTitle(day.first()), style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp))
      Text("${day.size} trip${if (day.size == 1) "" else "s"}", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      day.forEach { tr -> TripCard(tr) { onOpenTrip(tr) } }
    }
  }
}

@Composable
private fun TripCard(tr: Trip, onClick: () -> Unit) {
  Column(Modifier.card(24.dp).clickable(onClick = onClick).padding(18.dp)) {
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

@Composable
private fun EmptyState(icon: String, title: String, sub: String) {
  Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Box(Modifier.size(92.dp).clip(RoundedCornerShape(46.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(46.dp)), contentAlignment = Alignment.Center) {
      VIcon(icon, size = 40.dp, color = Ink, stroke = 1.4f)
    }
    Spacer(Modifier.height(12.dp))
    Text(title, style = TextStyle(color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp))
    Text(sub, style = TextStyle(color = Muted, fontSize = 14.sp))
  }
}

/** Calendar-day key (date + year) so the same date in different years doesn't merge. */
private fun dayKey(t: Trip): String =
  if (t.epoch <= 0) t.date else SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date(t.epoch * 1000))

/** "Today", "Yesterday" or "Friday, 16 Oct" (year added when it isn't this year). */
private fun dayTitle(t: Trip): String {
  if (t.epoch <= 0) return "Friday, ${t.date}"
  val f = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
  val day = f.format(Date(t.epoch * 1000))
  val sameYear = SimpleDateFormat("yyyy", Locale.ENGLISH).let { it.format(Date(t.epoch * 1000)) == it.format(Date()) }
  return when (day) {
    f.format(Date()) -> "Today"
    f.format(Date(System.currentTimeMillis() - 86_400_000L)) -> "Yesterday"
    else -> SimpleDateFormat(if (sameYear) "EEEE, d MMM" else "EEE, d MMM yyyy", Locale.ENGLISH).format(Date(t.epoch * 1000))
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
