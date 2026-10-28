package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesThemeSettingsTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val file = "theme-settings-test"
  private val preferences get() = context.getSharedPreferences(file, Context.MODE_PRIVATE)

  private fun settings() = PreferencesThemeSettings(preferences)

  @Before
  fun clean() {
    preferences.edit().clear().commit()
  }

  @Test
  fun `with nothing chosen the app follows the phone`() {
    assertEquals(ThemeMode.System, settings().mode.value)
  }

  @Test
  fun `a choice is the mode at once`() {
    val settings = settings()
    settings.choose(ThemeMode.Dark)
    assertEquals(ThemeMode.Dark, settings.mode.value)
  }

  @Test
  fun `each choice comes back after the app restarts`() {
    ThemeMode.entries.forEach { mode ->
      settings().choose(mode)
      // A new object over the same file is what the next run of the app gets.
      assertEquals(mode, settings().mode.value)
    }
  }

  @Test
  fun `a later choice replaces the earlier one`() {
    val settings = settings()
    settings.choose(ThemeMode.Dark)
    settings.choose(ThemeMode.Light)
    assertEquals(ThemeMode.Light, settings().mode.value)
  }

  @Test
  fun `the choice is kept by the name of the mode, so a file from an earlier version still reads`() {
    settings().choose(ThemeMode.Dark)
    assertEquals("Dark", preferences.getString("theme", null))
  }

  @Test
  fun `a value the app does not know is the same as no choice`() {
    preferences.edit().putString("theme", "Sepia").commit()
    assertEquals(ThemeMode.System, settings().mode.value)
  }

  @Test
  fun `a value of another kind is the same as no choice`() {
    preferences.edit().putInt("theme", 3).commit()
    assertEquals(ThemeMode.System, settings().mode.value)
  }
}
