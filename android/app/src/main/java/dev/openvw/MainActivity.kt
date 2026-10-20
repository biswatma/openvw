package dev.openvw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.openvw.theme.Bg
import dev.openvw.theme.OpenVWTheme
import dev.openvw.ui.OpenVWApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // apply the saved theme before first compose, so there is no light flash on a dark-mode launch
    dev.openvw.theme.Palette.dark = getSharedPreferences("openvw", MODE_PRIVATE).getBoolean("dark", false)
    val osm = org.osmdroid.config.Configuration.getInstance()
    osm.load(applicationContext, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE))
    osm.userAgentValue = "OpenVW-Taigun/1.0 ($packageName)"
    osm.osmdroidBasePath = java.io.File(cacheDir, "osmdroid").apply { mkdirs() }
    osm.osmdroidTileCache = java.io.File(osm.osmdroidBasePath, "tiles").apply { mkdirs() }
    enableEdgeToEdge()
    setContent {
      OpenVWTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) { OpenVWApp() }
      }
    }
  }
}
