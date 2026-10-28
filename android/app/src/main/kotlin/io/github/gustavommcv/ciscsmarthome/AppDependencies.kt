package io.github.gustavommcv.ciscsmarthome

import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertSettings
import io.github.gustavommcv.ciscsmarthome.domain.LinkSettings
import io.github.gustavommcv.ciscsmarthome.domain.NameBook
import io.github.gustavommcv.ciscsmarthome.domain.ThemeSettings

/**
 * What the screen and the service ask of the application. The screen may not import the service, nor
 * the service the screen, so they meet here, and a test can give the service a fake application.
 */
interface AppDependencies {
  val homeRepository: HomeRepository

  /** The theme the user chose, which the screen reads to draw itself and the settings tab changes. */
  val themeSettings: ThemeSettings

  /** How the user chose to reach the board, which the settings tab shows and changes, and the link follows. */
  val linkSettings: LinkSettings

  /** How the user chose to be warned of a gas leak, room by room, which the room screen changes and the service reads when a leak comes. */
  val leakAlertSettings: LeakAlertSettings

  /** The names the user gave to rooms and devices, which the screen shows and the warning of a leak says. */
  val nameBook: NameBook

  /** Starts the service that keeps the app in touch with the board, also when the screen is gone. */
  fun startMonitoring()

  /** Closes every screen of the app and takes it out of the recent apps: what the service does when the user stops the monitoring. */
  fun closeApp()
}
