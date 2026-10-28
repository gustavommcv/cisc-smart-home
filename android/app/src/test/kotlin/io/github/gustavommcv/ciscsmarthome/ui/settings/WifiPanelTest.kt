package io.github.gustavommcv.ciscsmarthome.ui.settings

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the card of the board's Wi-Fi shows below the choice: the password only when it is needed, and the way to forget it when there is one. */
class WifiPanelTest {
  private fun lost(problem: ConnectionProblem) = ConnectionState.Disconnected(problem)

  @Test
  fun `without a password the field is asked for, and there is nothing to forget`() {
    listOf(ConnectionState.Connecting, lost(ConnectionProblem.NetworkNotSet)).forEach { connection ->
      assertEquals(connection.toString(), WifiPanel(showsPasswordField = true, canForgetPassword = false), wifiPanel(passwordSaved = false, connection))
    }
  }

  @Test
  fun `with a password and the board heard, the field is out of the way and the password can be forgotten`() {
    assertEquals(WifiPanel(showsPasswordField = false, canForgetPassword = true), wifiPanel(passwordSaved = true, ConnectionState.Connected))
  }

  @Test
  fun `with a password and the phone still joining, the field stays out of the way`() {
    assertFalse(wifiPanel(passwordSaved = true, ConnectionState.Connecting).showsPasswordField)
  }

  @Test
  fun `when the phone cannot join the network the field comes back, for the password may be wrong`() {
    val panel = wifiPanel(passwordSaved = true, lost(ConnectionProblem.NetworkNotJoined))
    assertTrue(panel.showsPasswordField)
    assertTrue(panel.canForgetPassword)
  }

  @Test
  fun `a board that does not answer is not a reason to ask for the password again`() {
    listOf(ConnectionProblem.Unreachable, ConnectionProblem.TooManyClients, ConnectionProblem.Incompatible).forEach { problem ->
      assertFalse(problem.toString(), wifiPanel(passwordSaved = true, lost(problem)).showsPasswordField)
    }
  }
}
