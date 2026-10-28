package io.github.gustavommcv.ciscsmarthome

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals as assertSame
import org.junit.Test

class ConnectionMessagesTest {
  @Test
  fun `each state of the connection has its own message`() {
    assertEquals(R.string.connection_connecting, ConnectionState.Connecting.messageRes())
    assertEquals(R.string.connection_connected, ConnectionState.Connected.messageRes())
    assertEquals(R.string.connection_unreachable, ConnectionState.Disconnected(ConnectionProblem.Unreachable).messageRes())
    assertEquals(R.string.connection_too_many_clients, ConnectionState.Disconnected(ConnectionProblem.TooManyClients).messageRes())
    assertEquals(R.string.connection_incompatible, ConnectionState.Disconnected(ConnectionProblem.Incompatible).messageRes())
    assertEquals(R.string.connection_network_not_set, ConnectionState.Disconnected(ConnectionProblem.NetworkNotSet).messageRes())
    assertEquals(R.string.connection_network_not_joined, ConnectionState.Disconnected(ConnectionProblem.NetworkNotJoined).messageRes())
  }

  @Test
  fun `the messages are all different`() {
    val states = listOf(ConnectionState.Connecting, ConnectionState.Connected) + ConnectionProblem.entries.map { ConnectionState.Disconnected(it) }
    assertSame(states.size, states.map { it.messageRes() }.toSet().size)
  }
}
