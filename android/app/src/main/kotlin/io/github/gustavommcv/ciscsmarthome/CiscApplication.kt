package io.github.gustavommcv.ciscsmarthome

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.gustavommcv.ciscsmarthome.di.AppContainer
import io.github.gustavommcv.ciscsmarthome.service.HomeService

/** Builds the container when something first needs it. The sync with the board is the service's, and it starts when the screen asks. */
class CiscApplication : Application(), AppDependencies {
  private val container: AppContainer by lazy { AppContainer(this) }

  override val homeRepository get() = container.homeRepository

  override val themeSettings get() = container.themeSettings

  override val linkSettings get() = container.linkSettings

  override val nameBook get() = container.nameBook

  override val leakAlertSettings get() = container.leakAlertSettings

  override fun startMonitoring() {
    ContextCompat.startForegroundService(this, Intent(this, HomeService::class.java))
  }

  override fun closeApp() {
    getSystemService(ActivityManager::class.java).appTasks.forEach { it.finishAndRemoveTask() }
  }
}
