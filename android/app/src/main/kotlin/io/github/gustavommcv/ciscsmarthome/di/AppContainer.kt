package io.github.gustavommcv.ciscsmarthome.di

import android.content.Context
import androidx.room.Room
import io.github.gustavommcv.ciscsmarthome.data.local.HistoryDatabase
import io.github.gustavommcv.ciscsmarthome.data.local.RoomHistoryStore
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardClient
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardEventStream
import io.github.gustavommcv.ciscsmarthome.data.repository.HomeRepositoryImpl
import io.github.gustavommcv.ciscsmarthome.domain.AlertTracker
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

/**
 * Builds the pieces of the app and joins them, by hand: the app is small enough that a framework for
 * it would be more to read than the wiring itself. The screens and the service get what they need
 * from here, and only here do the layers meet.
 */
class AppContainer(context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val boardUrl = BOARD_URL.toHttpUrl()

  // The board is on the phone's own network, so it answers in milliseconds or not at all.
  private val http = OkHttpClient.Builder().connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS).readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS).build()

  private val database = Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, DATABASE_NAME).build()

  val homeRepository: HomeRepository =
    HomeRepositoryImpl(BoardClient(boardUrl, http), BoardEventStream(boardUrl, http), RoomHistoryStore(database), Clock.systemUTC(), AlertTracker())

  /** Starts keeping the repository in step with the board, for as long as the app's process lives. */
  fun start() {
    scope.launch { homeRepository.keepSynced() }
  }

  companion object {
    /** The board's own network gives it this address (docs/arquitetura.md); network_security_config.xml allows plain HTTP to it. */
    const val BOARD_URL = "http://192.168.4.1/"

    private const val NETWORK_TIMEOUT_SECONDS = 5L
    private const val DATABASE_NAME = "history.db"
  }
}
