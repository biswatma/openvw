package dev.openvw.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Observable app data. Screens read these; the ViewModel writes live values in. */
object Store {
  var vehicle by mutableStateOf(Repo.vehicle)
  var telemetry by mutableStateOf(Repo.telemetry)
  var owner by mutableStateOf(Repo.owner)
  var trips by mutableStateOf(emptyList<Trip>())  // real trips only; empty until live data loads
  var live by mutableStateOf(false)           // true once real telemetry arrived
  var liveConnected by mutableStateOf(false)  // SignalR push feed is connected
  var privacy by mutableStateOf(false)        // mask VIN / reg / personal info with mimic data
  var connectedSince by mutableStateOf("2024-11-22T12:58:04Z") // vehicle onboarded to My VW Connect
  var dongle by mutableStateOf(Dongle())
  var drive by mutableStateOf(DriveStats())
  var events by mutableStateOf(emptyList<CarEvent>())
  var parked by mutableStateOf(Parked())
  val parkedKnown get() = parked.epoch > 0L   // false until a real telematics event sets the location
  var pendingDrive by mutableStateOf<Trip?>(null)  // last drive from engine on/off, until the full trip finalizes
  var alertSetup by mutableStateOf(AlertSetup())
  var paintPick by mutableStateOf("")         // "" = from account colour, else a CarLook.Paint name
  var fuelPrice by mutableStateOf(0.0)        // ₹/L entered by the owner; 0 = default (Bengaluru)
  var dark by mutableStateOf(false)           // dark mode (mirrors theme.Palette.dark)

  val workshops get() = Repo.workshops

  val specs: List<Spec>
    get() = listOf(
      Spec("Model", vehicle.model),
      Spec("Variant", vehicle.variant),
      Spec("Colour", vehicle.colour),
      Spec("Fuel", "Petrol"),
      Spec("Engine type", vehicle.engineType, mono = true),
      Spec("Dongle firmware", vehicle.dongleFirmware, mono = true),
      Spec("Dongle IMEI", Mask.imei(vehicle.dongleImei), mono = true),
    )
}

/** Privacy mode: swap identifying fields for plausible mimic values (for sharing/screens). */
object Mask {
  val on: Boolean get() = Store.privacy
  fun name(s: String) = if (on) "Rohan Mehta" else s
  fun first(s: String) = if (on) "Rohan" else s
  fun initials(s: String) = if (on) "RM" else s
  fun mobile(s: String) = if (on) "+91 98765 43210" else s
  fun email(s: String) = if (on) "rohan.mehta@email.com" else s
  fun vehicleName(s: String) = if (on) "Factory:MEXA11AA0AA000000" else s
  fun imei(s: String) = if (on) "860000000000000" else s
  fun reg(s: String) = if (on) "KA01AA0000" else s
}
