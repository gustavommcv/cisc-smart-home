package io.github.gustavommcv.ciscsmarthome

import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.ThemeSettings

/**
 * What the screen and the service ask of the application. The screen may not import the service, nor
 * the service the screen, so they meet here, and a test can give the service a fake application.
 */
interface AppDependencies {
  val homeRepository: HomeRepository

  /** The theme the user chose, which the screen reads to draw itself and the settings tab changes. */
  val themeSettings: ThemeSettings

  /** Starts the service that keeps the app in touch with the board, also when the screen is gone. */
  fun startMonitoring()
}
