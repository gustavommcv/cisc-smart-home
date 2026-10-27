package io.github.gustavommcv.ciscsmarthome

import android.app.Application
import io.github.gustavommcv.ciscsmarthome.di.AppContainer

/** Builds the container when the process starts and sets the sync with the board going. */
class CiscApplication : Application() {
  val container: AppContainer by lazy { AppContainer(this) }

  override fun onCreate() {
    super.onCreate()
    container.start()
  }
}
