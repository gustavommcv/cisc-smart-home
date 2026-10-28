package io.github.gustavommcv.ciscsmarthome.service

import android.app.Application
import io.github.gustavommcv.ciscsmarthome.AppDependencies
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertSettings
import io.github.gustavommcv.ciscsmarthome.domain.LinkSettings
import io.github.gustavommcv.ciscsmarthome.domain.NameBook
import io.github.gustavommcv.ciscsmarthome.domain.ThemeSettings
import io.github.gustavommcv.ciscsmarthome.testing.FakeHomeRepository
import io.github.gustavommcv.ciscsmarthome.testing.FakeLeakAlertSettings
import io.github.gustavommcv.ciscsmarthome.testing.FakeLinkSettings
import io.github.gustavommcv.ciscsmarthome.testing.FakeNameBook
import io.github.gustavommcv.ciscsmarthome.testing.FakeThemeSettings

/** The application of the service tests: what the service asks of it is a fake repository the test moves by hand. */
class TestApplication : Application(), AppDependencies {
  val repository = FakeHomeRepository()

  override val homeRepository: HomeRepository get() = repository

  override val themeSettings: ThemeSettings = FakeThemeSettings()

  override val linkSettings: LinkSettings = FakeLinkSettings()

  val leakModes = FakeLeakAlertSettings()

  override val leakAlertSettings: LeakAlertSettings get() = leakModes

  val names = FakeNameBook()

  override val nameBook: NameBook get() = names

  override fun startMonitoring() = Unit

  /** How many times the service closed the screens of the app. */
  var appsClosed = 0
    private set

  override fun closeApp() {
    appsClosed++
  }
}
