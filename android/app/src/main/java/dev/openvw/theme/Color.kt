package dev.openvw.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// --- OpenVW design tokens (Taigun Companion design) ---
// Each token is a computed property backed by [Palette.dark], so flipping that one flag
// re-themes every screen. `Ink` means "high-contrast foreground / emphasis fill" and `Card`
// means "surface": they swap together, so dark buttons/pills become light-on-dark inverses.

object Palette {
  var dark by mutableStateOf(false)
}

private fun tok(light: Long, dark: Long) = if (Palette.dark) Color(dark) else Color(light)

val Bg get() = tok(0xFFF5F4F0, 0xFF121214)
val Card get() = tok(0xFFFFFFFF, 0xFF1D1D21)
val CardAlt get() = tok(0xFFFAF9F6, 0xFF202025)   // cluster gradient bottom
val Ink get() = tok(0xFF0E0E10, 0xFFF3F2EF)
val Ink2 get() = tok(0xFF2C2C31, 0xFFE4E3DF)       // dark-button gradient top
val Muted get() = tok(0xFF6B6B70, 0xFF9B9BA2)
val Muted2 get() = tok(0xFF8A8A90, 0xFF86868E)
val MutedDark get() = tok(0xFF3A3A40, 0xFFC9C8C4)
val OnDarkMuted get() = tok(0xFFA8A8AE, 0xFF57575C) // muted text on an Ink-filled surface

val Line get() = tok(0xFFECEAE5, 0xFF2C2C31)
val LineSoft get() = tok(0xFFF0EEE9, 0xFF27272B)
val Border get() = tok(0xFFE6E4DF, 0xFF34343A)
val BorderStrong get() = tok(0xFFDAD7CF, 0xFF3E3E45)
val Hairline get() = tok(0xFFE2DFD8, 0xFF303036)

// brand accents — constant across themes
val Accent = Color(0xFFE8562A)
val Green = Color(0xFF12A06B)
val GreenDark get() = tok(0xFF0E7A50, 0xFF2BD08C)
val GreenSoftBg get() = tok(0xFFE3F5EC, 0xFF11372A)
val Red = Color(0xFFE5484D)
val RedSoftBg get() = tok(0xFFFDEBEC, 0xFF3A1F22)
val Amber = Color(0xFFD98A00)

val Field get() = tok(0xFFF7F6F2, 0xFF26262B)
val Chip get() = tok(0xFFF5F4F0, 0xFF26262B)
val ChipBg get() = tok(0xFFEDEBE6, 0xFF2A2A30)

val Track get() = tok(0xFFEFEDE8, 0xFF34343A)
val Track2 get() = tok(0xFFE8E6E0, 0xFF3A3A40)
val TickMinor get() = tok(0xFFCFCCC4, 0xFF55555C)
val MapBg get() = tok(0xFFECEAE4, 0xFF26262B)
val Timeline get() = tok(0xFFD6D3CB, 0xFF3A3A40) // trip route connector line
