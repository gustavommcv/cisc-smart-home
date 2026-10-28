package io.github.gustavommcv.ciscsmarthome.service

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode

/**
 * What the service tells the user. [ConnectionWatch] and [LeakWarnings] decide when; this says how,
 * which on the phone is a notification ([AndroidNotifier]). It keeps the rules free of Android, so they
 * can be tested without a phone.
 */
internal interface Notifier {
  /** The notice that stays while the service works: it says whether the board is being heard. */
  fun showConnection(connection: ConnectionState)

  /** The board has been out of reach for a while, so nothing is watching the house. */
  fun showBoardLost(connection: ConnectionState.Disconnected)

  /** The board is no longer lost, or never was: takes that warning down, if it is up. */
  fun clearBoardLost()

  /** The warning of a leak, in the way the user chose for that room ([mode] is never [LeakAlertMode.Off]: a room with the warning off is not warned of). [home] is the house as the app last heard it, if it has heard it, to name the area. */
  fun showGasLeak(alert: GasAlert, home: Home?, mode: LeakAlertMode)

  /** Takes the warning of this leak down. */
  fun clearGasLeak(alert: GasAlert)
}
