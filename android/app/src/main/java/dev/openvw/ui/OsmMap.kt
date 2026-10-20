package dev.openvw.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.openvw.theme.Border
import dev.openvw.theme.Card
import dev.openvw.theme.Ink
import dev.openvw.theme.Muted
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

enum class MapMode { STREET, SATELLITE, HYBRID }

// Esri basemaps — free, no API key ({z}/{y}/{x} order).
// (OSM's own volunteer servers block unregistered apps; CARTO now needs a key.)
private fun esri(name: String, service: String, maxZoom: Int) = object : OnlineTileSourceBase(
  name, 0, maxZoom, 256, "",
  arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/$service/MapServer/tile/"),
) {
  override fun getTileURLString(pMapTileIndex: Long): String =
    baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
      MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
}

private val EsriLight = esri("EsriLightGray", "Canvas/World_Light_Gray_Base", 16) // clean street map
private val EsriSat = esri("EsriWorldImagery", "World_Imagery", 19)               // satellite / aerial
private val EsriRef = esri("EsriReference", "Reference/World_Boundaries_and_Places", 19) // transparent labels

/** Open map (osmdroid) with street / satellite / hybrid (satellite + labels) modes. */
@Composable
fun OsmMap(
  lat: Double,
  lon: Double,
  modifier: Modifier = Modifier,
  zoom: Double = 15.0,
  interactive: Boolean = true,
  showMarker: Boolean = true,
  mode: MapMode = MapMode.STREET,
  onMarkerClick: (() -> Unit)? = null,
) {
  val ctx = LocalContext.current
  val onTap by rememberUpdatedState(onMarkerClick)
  val labels = remember {
    TilesOverlay(MapTileProviderBasic(ctx, EsriRef), ctx).apply {
      loadingBackgroundColor = android.graphics.Color.TRANSPARENT
    }
  }
  val map = remember {
    MapView(ctx).apply {
      setTileSource(EsriLight)
      isTilesScaledToDpi = true
      setMultiTouchControls(interactive)
      zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER) // custom +/− below
      controller.setZoom(zoom)
      controller.setCenter(GeoPoint(lat, lon))
      if (showMarker) overlays.add(Marker(this).apply {
        position = GeoPoint(lat, lon)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "Last parked here"
        icon = carPin(ctx)
        // our own card instead of osmdroid's default info bubble
        setOnMarkerClickListener { _, _ -> onTap?.invoke(); true }
      })
      if (!interactive) setOnTouchListener { v, _ -> v.performClick(); true }
    }
  }
  DisposableEffect(Unit) {
    map.onResume()
    onDispose { map.onPause() }
  }
  Box(modifier) {
  AndroidView(factory = { map }, modifier = Modifier.matchParentSize()) { mv ->
    val base = if (mode == MapMode.STREET) EsriLight else EsriSat
    if (mv.tileProvider.tileSource.name() != base.name()) mv.setTileSource(base)
    val hasLabels = mv.overlays.contains(labels)
    if (mode == MapMode.HYBRID && !hasLabels) mv.overlays.add(0, labels)
    else if (mode != MapMode.HYBRID && hasLabels) mv.overlays.remove(labels)
    val pt = GeoPoint(lat, lon)
    mv.controller.setCenter(pt)
    mv.overlays.filterIsInstance<Marker>().forEach { it.position = pt }
    mv.invalidate()
  }
  if (interactive) ZoomButtons(Modifier.align(Alignment.BottomEnd).padding(10.dp), { map.controller.zoomIn() }, { map.controller.zoomOut() })
  }
}

/** Compact white +/− pill. */
@Composable
private fun ZoomButtons(modifier: Modifier, onIn: () -> Unit, onOut: () -> Unit) {
  androidx.compose.foundation.layout.Column(
    modifier.shadow(6.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(14.dp)),
  ) {
    Box(Modifier.size(38.dp).clickable(onClick = onIn), contentAlignment = Alignment.Center) {
      VIcon("M12 6v12M6 12h12", size = 16.dp, color = Ink, stroke = 2.2f)
    }
    Box(Modifier.padding(horizontal = 9.dp).size(20.dp, 1.dp).background(Border))
    Box(Modifier.size(38.dp).clickable(onClick = onOut), contentAlignment = Alignment.Center) {
      VIcon("M6 12h12", size = 16.dp, color = Ink, stroke = 2.2f)
    }
  }
}

/** Floating Map / Satellite / Hybrid selector. */
@Composable
fun MapModeToggle(mode: MapMode, modifier: Modifier = Modifier, onMode: (MapMode) -> Unit) {
  Row(
    modifier.clip(RoundedCornerShape(99.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(99.dp)).padding(3.dp),
  ) {
    MapChip("Map", mode == MapMode.STREET) { onMode(MapMode.STREET) }
    MapChip("Satellite", mode == MapMode.SATELLITE) { onMode(MapMode.SATELLITE) }
    MapChip("Hybrid", mode == MapMode.HYBRID) { onMode(MapMode.HYBRID) }
  }
}

@Composable
private fun MapChip(text: String, active: Boolean, onClick: () -> Unit) {
  Box(
    Modifier.clip(RoundedCornerShape(99.dp)).background(if (active) Ink else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text, style = TextStyle(color = if (active) Card else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
  }
}

/** Ink car pin with a white ring (matches the Home map pin). */
private fun carPin(ctx: android.content.Context): android.graphics.drawable.Drawable {
  val d = ctx.resources.displayMetrics.density
  val sz = (40 * d).toInt()
  val bmp = android.graphics.Bitmap.createBitmap(sz, sz, android.graphics.Bitmap.Config.ARGB_8888)
  val c = android.graphics.Canvas(bmp)
  val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
  p.color = android.graphics.Color.WHITE; c.drawCircle(sz / 2f, sz / 2f, sz / 2f, p)
  p.color = 0xFF0E0E10.toInt(); c.drawCircle(sz / 2f, sz / 2f, sz / 2f - 4 * d, p)
  androidx.core.graphics.PathParser.createPathFromPathData(Ic.car)?.let { path ->
    val s = 20 * d / 24f
    path.transform(android.graphics.Matrix().apply { setScale(s, s); postTranslate(sz / 2f - 10 * d, sz / 2f - 10 * d) })
    p.style = android.graphics.Paint.Style.STROKE; p.strokeWidth = 1.9f * s; p.color = android.graphics.Color.WHITE
    p.strokeCap = android.graphics.Paint.Cap.ROUND; p.strokeJoin = android.graphics.Paint.Join.ROUND
    c.drawPath(path, p)
  }
  return android.graphics.drawable.BitmapDrawable(ctx.resources, bmp)
}
