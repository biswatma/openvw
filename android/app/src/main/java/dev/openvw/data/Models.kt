package dev.openvw.data

// --- Data shapes (mirror the live My Volkswagen / OpenVW API fields) ---

data class Telemetry(
  val speedKmh: Int? = 0,
  val rpm: Int? = 950,
  val fuelPct: Int = 34,
  val batteryV: Double = 12.4,
  val coolantC: Int? = 98,
  val intakeC: Int? = 50,
  val engineLoadPct: Int? = 27,
  val odometerKm: Int? = 10229,
  val milOn: Boolean = false,          // check-engine light
  val distanceWithMil: Int? = 0,       // km driven with MIL on
  val distanceSinceDtc: Int? = 6145,   // km since last diagnostic reset
  val engineOn: Boolean = false,
)

/** OBD dongle health. */
data class Dongle(val signal: Int = 0, val status: String = "", val firmware: String = "")

/** Driving-behaviour summary computed from per-trip telematics. */
data class DriveStats(
  val tripCount: Int = 0,
  val avgScore: Int = 0,
  val maxSpeedKmh: Int = 0,
  val idleMin: Int = 0,
  val harshBrakes: Int = 0,
  val realKmpl: Double = 0.0,   // measured mileage from per-trip fuel use (0 = unknown)
  val totalKm: Double = 0.0,
  val totalMin: Int = 0,
  val weekTrips: Int = 0,       // trips in the last 7 days
  val weekKm: Double = 0.0,
  val weekMin: Int = 0,
)

/** Something the car reported: engine on/off or a driving alarm (sudden brake, sharp turn…). */
data class CarEvent(
  val id: String,
  val kind: String,      // "engine" | "alarm"
  val title: String,     // "Engine off", "Sudden brake"
  val epoch: Long,       // seconds
  val lat: Double,
  val lon: Double,
)

/** Where the car was last seen (latest event / trip end). */
data class Parked(
  val lat: Double = 12.9756,
  val lon: Double = 77.6050,
  val epoch: Long = 0,
  val title: String = "MG Road",
  val sub: String = "Bengaluru, Karnataka",
)

/** Geofences + which alert triggers are switched on (Cerence /policies). */
data class AlertSetup(val fences: List<String> = emptyList(), val triggers: List<String> = emptyList())

/** Which Taigun render matches the paint name from the account (or the owner's manual pick). */
object CarLook {
  enum class Paint(val label: String, val swatch: Long) {
    WHITE("White", 0xFFF4F4F2), BLACK("Black", 0xFF16161A), GREY("Grey", 0xFF5B6B78), RED("Red", 0xFFC8102E), BLUE("Blue", 0xFF1F7FD8), YELLOW("Yellow", 0xFFF2C200),
  }

  private val words = listOf(
    Regex("white|silver") to Paint.WHITE,
    Regex("grey|gray|carbon|steel|graphite") to Paint.GREY,
    Regex("black") to Paint.BLACK,
    Regex("red|cherry") to Paint.RED,
    Regex("blue") to Paint.BLUE,
    Regex("yellow|curcuma") to Paint.YELLOW,
  )

  /** The first colour word wins: "Candy White Deep/Black" is a white car with a black roof. */
  fun paint(colour: String): Paint {
    val c = colour.lowercase()
    return words.mapNotNull { (re, p) -> re.find(c)?.let { it.range.first to p } }.minByOrNull { it.first }?.second ?: Paint.WHITE
  }

  /** Paint to show: the owner's manual pick (Store.paintPick, a Paint name) wins over the account colour. */
  fun effective(colour: String): Paint =
    Paint.entries.firstOrNull { it.name == Store.paintPick } ?: paint(colour)

  fun image(colour: String) = when (effective(colour)) {
    Paint.WHITE -> dev.openvw.R.drawable.taigun_hero
    Paint.BLACK -> dev.openvw.R.drawable.taigun_hero_black
    Paint.GREY -> dev.openvw.R.drawable.taigun_hero_grey
    Paint.RED -> dev.openvw.R.drawable.taigun_hero_red
    Paint.BLUE -> dev.openvw.R.drawable.taigun_hero_blue
    Paint.YELLOW -> dev.openvw.R.drawable.taigun_hero_yellow
  }
}

data class VehicleInfo(
  val variant: String = "Taigun GT Plus",
  val model: String = "TAIGUN GT PLUS 1.5L TSI 110kW DSG",
  val vehicleName: String = "Factory:MEXA11AA0AA000000",
  val colour: String = "Candy White Deep/Black",
  val engineType: String = "DUM A00000",
  val dongleImei: String = "860000000000000",
  val dongleFirmware: String = "1.0.0.27",
  val status: String = "Parked",
)

data class Owner(
  val name: String = "VW OWNER",
  val email: String = "",
  val mobile: String = "+91 00000 00000",
  val address: String = "",
) {
  val initials: String
    get() = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
      .take(2).joinToString("") { it.first().uppercase() }
  val first: String
    get() = name.trim().split(Regex("\\s+")).firstOrNull()
      ?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
  val proper: String
    get() = name.lowercase().split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
}

data class Trip(
  val id: String,
  val start: String,
  val end: String,
  val fromTitle: String,
  val fromSub: String,
  val toTitle: String,
  val toSub: String,
  val durMin: Int,
  val type: String,
  val date: String = "16 Oct",
  val km: Double = 0.0,
  val score: Int = 0,
  val epoch: Long = 0,   // trip start, seconds
)

data class Workshop(val id: String, val name: String, val address: String)

data class Spec(val label: String, val value: String, val mono: Boolean = false)

// --- Sample payload shown until live data loads (identifiers are placeholders) ---
object Repo {
  val vehicle = VehicleInfo()
  val telemetry = Telemetry()
  val owner = Owner()

  val trips = listOf(
    Trip(
      "a", "10:10 AM", "10:24 AM",
      "MG Road", "Mahatma Gandhi Rd, Bengaluru, Karnataka 560001",
      "Indiranagar", "100 Feet Rd, Indiranagar, Bengaluru, Karnataka 560038", 14, "Personal",
    ),
    Trip(
      "b", "09:13 AM", "09:40 AM",
      "Indiranagar", "100 Feet Rd, Indiranagar, Bengaluru, Karnataka 560038",
      "MG Road", "Mahatma Gandhi Rd, Bengaluru, Karnataka 560001", 27, "Personal",
    ),
  )

  val workshops = listOf(
    Workshop("w1", "Volkswagen Sarjapur Road", "Survey No.154/1, Doddakanahalli Village, Varthur Hobli, Bengaluru East"),
    Workshop("w2", "Volkswagen Bangalore", "Elite Motors P Ltd, 49/8-9-10, Singasandra Village, Hosur Main Road"),
    Workshop("w3", "Volkswagen Old Airport Road", "#227, 16th D Main Road, HAL 2nd Stage, Bengaluru"),
  )

  val specs: List<Spec>
    get() = listOf(
      Spec("Model", vehicle.model),
      Spec("Variant", vehicle.variant),
      Spec("Colour", vehicle.colour),
      Spec("Fuel", "Petrol"),
      Spec("Engine type", vehicle.engineType, mono = true),
      Spec("Dongle firmware", vehicle.dongleFirmware, mono = true),
      Spec("Dongle IMEI", vehicle.dongleImei, mono = true),
    )
}
