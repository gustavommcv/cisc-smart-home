package io.github.gustavommcv.ciscsmarthome

import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.service.HomeService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
