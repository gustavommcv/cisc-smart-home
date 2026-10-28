package io.github.gustavommcv.ciscsmarthome.ui.settings

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState

/** What the card of the board's Wi-Fi shows below the choice. */
internal data class WifiPanel(val showsPasswordField: Boolean, val canForgetPassword: Boolean)

/**
 * The password is asked for only when it is needed: when there is none, or when the phone cannot join the network (it may be the wrong one). While the board is heard,
 * or the phone is still joining, the card says how the connection is and offers to forget the password, which is what brings the field back.
 */
internal fun wifiPanel(passwordSaved: Boolean, connection: ConnectionState): WifiPanel {
  val joinFailed = connection == ConnectionState.Disconnected(ConnectionProblem.NetworkNotJoined)
  return WifiPanel(showsPasswordField = !passwordSaved || joinFailed, canForgetPassword = passwordSaved)
}
