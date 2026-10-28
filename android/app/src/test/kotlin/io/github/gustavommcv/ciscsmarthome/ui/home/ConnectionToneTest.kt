package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import org.junit.Assert.assertEquals
import org.junit.Test

/** How the connection is shown: a tone (calm when the board is heard, quiet while it is being looked for, an alert when it is lost) and a few words. */
class ConnectionToneTest {
  @Test
  fun `a board that is heard is the primary tone`() {
    assertEquals(Tone.Primary, ConnectionState.Connected.tone())
  }

  @Test
  fun `a board still being looked for is the quiet tone`() {
    assertEquals(Tone.Neutral, ConnectionState.Connecting.tone())
  }

  @Test
  fun `a board that is lost is an alert, whatever the reason`() {
    ConnectionProblem.entries.forEach { problem ->
      assertEquals(problem.toString(), Tone.Alert, ConnectionState.Disconnected(problem).tone())
    }
  }

  @Test
  fun `the pill says in a few words how the connection is`() {
    assertEquals(R.string.connection_connecting, ConnectionState.Connecting.pillRes())
    assertEquals(R.string.connection_connected, ConnectionState.Connected.pillRes())
  }

  @Test
  fun `a lost board is a short word in the pill, whatever the reason, and the reason follows`() {
    ConnectionProblem.entries.forEach { problem ->
      assertEquals(problem.toString(), R.string.no_connection_title, ConnectionState.Disconnected(problem).pillRes())
    }
  }
}
