package io.github.gustavommcv.ciscsmarthome.ui.settings

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import io.github.gustavommcv.ciscsmarthome.testing.FakeLinkSettings
import io.github.gustavommcv.ciscsmarthome.testing.FakeThemeSettings
import org.junit.Assert.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
  private val settings = FakeThemeSettings()
  private val link = FakeLinkSettings()
  private val connection = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
  private val viewModel = SettingsViewModel(settings, link, connection)

  @Test
  fun `the screen starts with the theme that is chosen`() {
    assertEquals(ThemeMode.Dark, SettingsViewModel(FakeThemeSettings(ThemeMode.Dark), link, connection).themeMode.value)
  }

  @Test
  fun `choosing a theme hands the choice over`() {
    viewModel.chooseTheme(ThemeMode.Light)
    assertEquals(listOf(ThemeMode.Light), settings.chosen)
  }

  @Test
  fun `what the screen shows follows what is chosen`() {
    viewModel.chooseTheme(ThemeMode.Dark)
    assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
  }

  @Test
  fun `a theme chosen somewhere else shows here too`() {
    settings.choose(ThemeMode.Light)
    assertEquals(ThemeMode.Light, viewModel.themeMode.value)
  }

  @Test
  fun `the screen starts with the link that is chosen, and the password that is saved`() {
    val model = SettingsViewModel(settings, FakeLinkSettings(LinkKind.CurrentNetwork, password = "secret-pass"), connection)
    assertEquals(LinkKind.CurrentNetwork, model.linkKind.value)
    assertEquals("secret-pass", model.boardPassword.value)
  }

  @Test
  fun `choosing a link hands the choice over`() {
    viewModel.chooseLink(LinkKind.CurrentNetwork)
    assertEquals(listOf(LinkKind.CurrentNetwork), link.chosen)
    assertEquals(LinkKind.CurrentNetwork, viewModel.linkKind.value)
  }

  @Test
  fun `a valid password is saved, and the screen is told so`() {
    assertTrue(viewModel.saveBoardPassword("secret-pass"))
    assertEquals(listOf("secret-pass"), link.saved)
    assertEquals("secret-pass", viewModel.boardPassword.value)
  }

  @Test
  fun `forgetting the password hands it over, and the screen is told there is none`() {
    viewModel.saveBoardPassword("secret-pass")
    viewModel.forgetBoardPassword()
    assertEquals(1, link.forgotten)
    assertNull(viewModel.boardPassword.value)
  }

  @Test
  fun `the screen follows how the connection is`() {
    assertEquals(ConnectionState.Connecting, viewModel.connection.value)
    connection.value = ConnectionState.Connected
    assertEquals(ConnectionState.Connected, viewModel.connection.value)
    connection.value = ConnectionState.Disconnected(ConnectionProblem.NetworkNotJoined)
    assertEquals(ConnectionState.Disconnected(ConnectionProblem.NetworkNotJoined), viewModel.connection.value)
  }

  @Test
  fun `a password that cannot be a Wi-Fi password is not saved, and the screen is told so`() {
    assertFalse(viewModel.saveBoardPassword("short"))
    assertFalse(viewModel.saveBoardPassword("senha-com-acento-é"))
    assertEquals(emptyList<String>(), link.saved)
    assertNull(viewModel.boardPassword.value)
  }
}
