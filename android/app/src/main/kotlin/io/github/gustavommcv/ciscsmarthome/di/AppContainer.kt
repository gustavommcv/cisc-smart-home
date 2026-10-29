package io.github.gustavommcv.ciscsmarthome.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room
import io.github.gustavommcv.ciscsmarthome.data.local.HISTORY_MIGRATIONS
import io.github.gustavommcv.ciscsmarthome.data.local.HistoryDatabase
import io.github.gustavommcv.ciscsmarthome.data.link.AndroidNetworkJoiner
import io.github.gustavommcv.ciscsmarthome.data.link.BoardLink
import io.github.gustavommcv.ciscsmarthome.data.link.ChosenLink
import io.github.gustavommcv.ciscsmarthome.data.link.DirectLink
import io.github.gustavommcv.ciscsmarthome.data.link.RoutedBoardApi
import io.github.gustavommcv.ciscsmarthome.data.link.RoutedBoardEvents
import io.github.gustavommcv.ciscsmarthome.data.link.WifiAccessPointLink
import io.github.gustavommcv.ciscsmarthome.data.local.PreferencesAlertMemory
import io.github.gustavommcv.ciscsmarthome.data.local.PreferencesLeakAlertSettings
import io.github.gustavommcv.ciscsmarthome.data.local.PreferencesLinkSettings
import io.github.gustavommcv.ciscsmarthome.data.local.PreferencesNameBook
import io.github.gustavommcv.ciscsmarthome.data.local.PreferencesThemeSettings
import io.github.gustavommcv.ciscsmarthome.data.local.RoomHistoryStore
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardClient
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardEventStream
import io.github.gustavommcv.ciscsmarthome.data.repository.HomeRepositoryImpl
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertSettings
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import io.github.gustavommcv.ciscsmarthome.domain.LinkSettings
import io.github.gustavommcv.ciscsmarthome.domain.NameBook
import io.github.gustavommcv.ciscsmarthome.domain.ThemeSettings
import java.time.Clock
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

/**
 * Builds the pieces of the app and joins them, by hand: the app is small enough that a framework for
 * it would be more to read than the wiring itself. The screens and the service get what they need
 * from here, and only here do the layers meet. It builds; who runs the sync is the service.
 */
class AppContainer(context: Context) {
  private val boardUrl = BOARD_URL.toHttpUrl()

  // The board is on the phone's own network, so it answers in milliseconds or not at all.
  private val http = OkHttpClient.Builder().connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS).readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS).build()

  private val database = Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, DATABASE_NAME).addMigrations(*HISTORY_MIGRATIONS).build()

  private val alertMemory = PreferencesAlertMemory(context.applicationContext.getSharedPreferences(ALERT_MEMORY_FILE, Context.MODE_PRIVATE))

  val themeSettings: ThemeSettings = PreferencesThemeSettings(context.applicationContext.getSharedPreferences(THEME_FILE, Context.MODE_PRIVATE))

  val nameBook: NameBook = PreferencesNameBook(context.applicationContext.getSharedPreferences(NAMES_FILE, Context.MODE_PRIVATE))

  val leakAlertSettings: LeakAlertSettings = PreferencesLeakAlertSettings(context.applicationContext.getSharedPreferences(LEAK_ALERT_FILE, Context.MODE_PRIVATE))

  val linkSettings: LinkSettings = PreferencesLinkSettings(context.applicationContext.getSharedPreferences(LINK_FILE, Context.MODE_PRIVATE))

  // How the app gets to the board is decided here and nowhere else: a new way (the cloud, a VPN) is a new LinkKind and a new line in this `when`.
  private val boardLink: BoardLink =
    ChosenLink(linkSettings) { kind ->
      when (kind) {
        LinkKind.BoardWifi ->
          WifiAccessPointLink(
            AndroidNetworkJoiner(context.applicationContext.getSystemService(ConnectivityManager::class.java)),
            BOARD_SSID,
            boardUrl,
            http,
            linkSettings.boardPassword,
          )
        LinkKind.CurrentNetwork -> DirectLink(boardUrl, http)
      }
    }

  val homeRepository: HomeRepository =
    HomeRepositoryImpl(
      RoutedBoardApi(boardLink) { route -> BoardClient(route.baseUrl, route.http) },
      RoutedBoardEvents(boardLink) { route -> BoardEventStream(route.baseUrl, route.http) },
      RoomHistoryStore(database),
      Clock.systemUTC(),
      alertMemory,
    )

  companion object {
    /** The board's own network gives it this address (docs/arquitetura.md); network_security_config.xml allows plain HTTP to it. */
    const val BOARD_URL = "http://192.168.4.1/"

    /** The name of the Wi-Fi the board makes: firmware/src/WifiConfig.h says it, and a test keeps the two equal. */
    const val BOARD_SSID = "cisc-smart-home"

    private const val NETWORK_TIMEOUT_SECONDS = 5L
    private const val DATABASE_NAME = "history.db"
    private const val ALERT_MEMORY_FILE = "alert-memory"
    private const val THEME_FILE = "theme-settings"
    private const val NAMES_FILE = "user-names"
    private const val LINK_FILE = "link-settings"
    private const val LEAK_ALERT_FILE = "leak-alert-settings"
  }
}
