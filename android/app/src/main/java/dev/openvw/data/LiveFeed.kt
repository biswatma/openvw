package dev.openvw.data

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import io.reactivex.rxjava3.core.Single
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Push feed from the Cerence car hub (SignalR over websocket) — what the official app's
 * SignalRService does. The hub sends "carStatus" (same shape as GET /api/v1/car/{id}, plus a live
 * `location`) whenever the dongle reports, and "events" (engine on/off, alarms). Nothing arrives
 * while the car is parked, so there is no polling.
 */
class LiveFeed(
  private val carId: String,
  private val token: () -> String,
  private val onStatus: (JSONObject) -> Unit,
  private val onEvent: (JSONObject) -> Unit,
  private val onClosed: (Throwable?) -> Unit,
) {
  private var hub: HubConnection? = null

  val connected: Boolean get() = hub?.connectionState == HubConnectionState.CONNECTED

  /** Blocking connect (call off the main thread). Throws on failure (e.g. 401 → token expired). */
  fun start() {
    val h = HubConnectionBuilder.create("$CERENCE/api/v1/car/$carId")
      .withAccessTokenProvider(Single.defer { Single.just(token()) })
      .build()
    h.on("carStatus", { m: Map<*, *> -> onStatus(JSONObject(m)) }, Map::class.java)
    h.on("events", { m: Map<*, *> -> onEvent(JSONObject(m)) }, Map::class.java)
    h.onClosed { e -> onClosed(e) }
    hub = h
    h.start().blockingAwait(20, TimeUnit.SECONDS)
  }

  fun stop() {
    val h = hub ?: return
    hub = null
    runCatching { h.stop().blockingAwait(5, TimeUnit.SECONDS) }
    runCatching { h.close() }
  }

  companion object {
    private const val CERENCE = "https://savwipl-connect15.in.onecloud.cerenceapi.com"
  }
}
