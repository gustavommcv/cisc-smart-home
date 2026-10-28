package io.github.gustavommcv.ciscsmarthome

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.data.local.PreferencesNameBook
import io.github.gustavommcv.ciscsmarthome.data.local.PreferencesThemeSettings
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import io.github.gustavommcv.ciscsmarthome.service.HomeService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CiscApplicationTest {
  private val app: CiscApplication = ApplicationProvider.getApplicationContext()

  @Test
  fun `the application starts nothing by itself`() {
    assertNull(shadowOf(app).nextStartedService)
  }

  @Test
  fun `starting the monitoring starts the service`() {
    app.startMonitoring()
    assertEquals(HomeService::class.java.name, shadowOf(app).nextStartedService.component?.className)
  }

  @Test
  fun `the choice of theme is one object for the whole app, so a change in the settings reaches the screen`() {
    assertSame(app.themeSettings, app.themeSettings)
  }

  @Test
  fun `the choice of theme is kept in the file theme-settings, so an update of the app keeps what the user chose`() {
    app.themeSettings.choose(ThemeMode.Dark)
    val file = app.getSharedPreferences("theme-settings", Context.MODE_PRIVATE)
    assertEquals(ThemeMode.Dark, PreferencesThemeSettings(file).mode.value)
  }

  @Test
  fun `the names of the user are one book for the whole app, so a name given in a room reaches the alarm`() {
    assertSame(app.nameBook, app.nameBook)
  }

  @Test
  fun `the names are kept in the file user-names, so an update of the app keeps what the user named`() {
    app.nameBook.renameArea(AreaId("kitchen"), "Copa")
    val file = app.getSharedPreferences("user-names", Context.MODE_PRIVATE)
    assertEquals("Copa", PreferencesNameBook(file).names.value.areas.getValue(AreaId("kitchen")))
  }
}
