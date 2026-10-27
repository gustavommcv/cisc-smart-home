package io.github.gustavommcv.ciscsmarthome

import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository

/**
 * What the screen and the service ask of the application. The screen may not import the service, nor
 * the service the screen, so they meet here, and a test can give the service a fake application.
 */
interface AppDependencies {
  val homeRepository: HomeRepository

  /** Starts the service that keeps the app in touch with the board, also when the screen is gone. */
  fun startMonitoring()
}
