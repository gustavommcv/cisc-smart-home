package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesLinkSettingsTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val file = "link-settings-test"
  private val preferences get() = context.getSharedPreferences(file, Context.MODE_PRIVATE)

  private fun settings() = PreferencesLinkSettings(preferences)

  @Before
  fun clean() {
    preferences.edit().clear().commit()
  }

  @Test
  fun `with nothing chosen the app joins the network of the board, and has no password`() {
    val settings = settings()
    assertEquals(LinkKind.BoardWifi, settings.kind.value)
    assertNull(settings.boardPassword.value)
  }

  @Test
  fun `a choice is the link at once`() {
    val settings = settings()
    settings.choose(LinkKind.CurrentNetwork)
    assertEquals(LinkKind.CurrentNetwork, settings.kind.value)
  }

  @Test
  fun `each choice comes back after the app restarts`() {
    LinkKind.entries.forEach { kind ->
      settings().choose(kind)
      // A new object over the same file is what the next run of the app gets.
      assertEquals(kind, settings().kind.value)
    }
  }

  @Test
  fun `the choice is kept by the name of the link, so a file from an earlier version still reads`() {
    settings().choose(LinkKind.CurrentNetwork)
    assertEquals("CurrentNetwork", preferences.getString("link", null))
  }

  @Test
  fun `the password is kept under the key the file has always had, so a file from an earlier version still reads`() {
    settings().saveBoardPassword("secret-pass")
    assertEquals("secret-pass", preferences.getString("board-password", null))
  }

  @Test
  fun `a value the app does not know is the same as no choice`() {
    preferences.edit().putString("link", "Carrier-pigeon").commit()
    assertEquals(LinkKind.BoardWifi, settings().kind.value)
  }

  @Test
  fun `a value of another kind is the same as no choice`() {
    preferences.edit().putInt("link", 3).commit()
    assertEquals(LinkKind.BoardWifi, settings().kind.value)
  }

  @Test
  fun `a saved password is the password at once`() {
    val settings = settings()
    settings.saveBoardPassword("secret-pass")
    assertEquals("secret-pass", settings.boardPassword.value)
  }

  @Test
  fun `the password comes back after the app restarts`() {
    settings().saveBoardPassword("secret-pass")
    assertEquals("secret-pass", settings().boardPassword.value)
  }

  @Test
  fun `a later password replaces the earlier one`() {
    val settings = settings()
    settings.saveBoardPassword("first-pass")
    settings.saveBoardPassword("second-pass")
    assertEquals("second-pass", settings().boardPassword.value)
  }

  @Test
  fun `the password is kept exactly as typed, spaces included`() {
    settings().saveBoardPassword(" two  words ")
    assertEquals(" two  words ", settings().boardPassword.value)
  }

  @Test
  fun `a password of another kind is the same as no password`() {
    preferences.edit().putInt("board-password", 3).commit()
    assertNull(settings().boardPassword.value)
  }

  @Test
  fun `a password that is forgotten is no password at once, and after the app restarts`() {
    val settings = settings()
    settings.saveBoardPassword("secret-pass")
    settings.forgetBoardPassword()
    assertNull(settings.boardPassword.value)
    assertNull(settings().boardPassword.value)
    assertNull(preferences.getString("board-password", null))
  }

  @Test
  fun `forgetting a password that was never given changes nothing`() {
    val settings = settings()
    settings.forgetBoardPassword()
    assertNull(settings.boardPassword.value)
    assertEquals(LinkKind.BoardWifi, settings.kind.value)
  }

  @Test
  fun `forgetting the password keeps the choice of link`() {
    val settings = settings()
    settings.choose(LinkKind.CurrentNetwork)
    settings.saveBoardPassword("secret-pass")
    settings.forgetBoardPassword()
    assertEquals(LinkKind.CurrentNetwork, settings().kind.value)
  }

  @Test
  fun `choosing a link keeps the password, and saving a password keeps the choice`() {
    val settings = settings()
    settings.saveBoardPassword("secret-pass")
    settings.choose(LinkKind.CurrentNetwork)
    settings.saveBoardPassword("other-pass")
    assertEquals(LinkKind.CurrentNetwork, settings().kind.value)
    assertEquals("other-pass", settings().boardPassword.value)
  }
}
