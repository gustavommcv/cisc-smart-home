package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertModes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesLeakAlertSettingsTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val file = "leak-alert-settings-test"
  private val preferences get() = context.getSharedPreferences(file, Context.MODE_PRIVATE)
  private val kitchen = AreaId("kitchen")
  private val bedroom = AreaId("bedroom")

  private fun settings() = PreferencesLeakAlertSettings(preferences)

  @Before
  fun clean() {
    preferences.edit().clear().commit()
  }

  @Test
  fun `no room has a way of its own at first`() {
    assertEquals(LeakAlertModes(), settings().modes.value)
  }

  @Test
  fun `a way that is chosen is the way at once`() {
    val settings = settings()
    settings.choose(kitchen, LeakAlertMode.Notice)
    assertEquals(LeakAlertMode.Notice, settings.modes.value.of(kitchen))
  }

  @Test
  fun `the ways come back after the app restarts`() {
    settings().apply {
      choose(kitchen, LeakAlertMode.Off)
      choose(bedroom, LeakAlertMode.Notice)
    }
    // A new object over the same file is what the next run of the app gets.
    assertEquals(LeakAlertModes(mapOf(kitchen to LeakAlertMode.Off, bedroom to LeakAlertMode.Notice)), settings().modes.value)
  }

  @Test
  fun `a later choice replaces the earlier one`() {
    val settings = settings()
    settings.choose(kitchen, LeakAlertMode.Off)
    settings.choose(kitchen, LeakAlertMode.Notice)
    assertEquals(LeakAlertMode.Notice, settings().modes.value.of(kitchen))
  }

  @Test
  fun `choosing the alarm, which is the default, keeps nothing, so the room goes on following the default`() {
    val settings = settings()
    settings.choose(kitchen, LeakAlertMode.Off)
    settings.choose(kitchen, LeakAlertMode.Alarm)
    assertEquals(LeakAlertModes(), settings.modes.value)
    assertEquals(LeakAlertModes(), settings().modes.value)
    assertNull(preferences.getString("area:kitchen", null))
  }

  @Test
  fun `a way is kept under the room and the key of the way, so a file from an earlier version still reads`() {
    settings().choose(kitchen, LeakAlertMode.Notice)
    assertEquals("notice", preferences.getString("area:kitchen", null))
  }

  @Test
  fun `the choice for one room leaves the others alone`() {
    val settings = settings()
    settings.choose(kitchen, LeakAlertMode.Off)
    settings.choose(bedroom, LeakAlertMode.Notice)
    settings.choose(kitchen, LeakAlertMode.Alarm)
    assertEquals(LeakAlertModes(mapOf(bedroom to LeakAlertMode.Notice)), settings().modes.value)
  }

  @Test
  fun `what the file has that is not a way is ignored`() {
    preferences.edit().putInt("area:garage", 3).putString("area:attic", "siren").putString("area:", "off").putString("other:thing", "off").commit()
    assertEquals(LeakAlertModes(), settings().modes.value)
  }
}
