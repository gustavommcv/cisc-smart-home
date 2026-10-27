package io.github.gustavommcv.ciscsmarthome.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.Home
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowService
import org.robolectric.annotation.Config

/** The service that keeps the app in touch with the board, against a fake repository. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class HomeServiceTest {
  private val app: TestApplication = ApplicationProvider.getApplicationContext()
  private val repository get() = app.repository
  private val notificationManager get() = app.getSystemService(NotificationManager::class.java)
  private val shadowNotifications get() = shadowOf(notificationManager)
  private var running: ServiceController<HomeService>? = null

  @After
  fun tearDown() {
    running?.destroy()
  }

  // ---- helpers ------------------------------------------------------------------

  private fun idle() = shadowOf(Looper.getMainLooper()).idle()

  private fun start(): HomeService {
    val controller = Robolectric.buildService(HomeService::class.java).create().startCommand(0, 1)
    running = controller
    idle()
    return controller.get()
  }

  private fun text(@androidx.annotation.StringRes id: Int, vararg args: Any) = app.getString(id, *args)

  private fun Notification.title(): String? = extras.getString(Notification.EXTRA_TITLE)

  private fun Notification.body(): String? = extras.getString(Notification.EXTRA_TEXT)

  /** The notifications of the gas alert channel, as the user would see them. */
  private fun leakNotifications(): List<Notification> = shadowNotifications.allNotifications.filter { it.channelId == "gas_alert" }

  private fun leak(count: Long, area: String = "kitchen", boot: String = "aaaa1111") = GasAlert(BootId(boot), AreaId(area), count)

  // ---- staying alive in the foreground --------------------------------------------

  @Test
  fun `it runs in the foreground, with a notice that says what the app is doing`() {
    val service = start()
    val notice = shadowOf(service).lastForegroundNotification
    assertNotNull(notice)
    assertEquals("monitoring", notice.channelId)
    assertEquals(text(R.string.app_name), notice.title())
    assertEquals(text(R.string.connection_connecting), notice.body())
    assertTrue(shadowOf(service).isLastForegroundNotificationAttached)
  }

  @Test
  fun `the notice follows the state of the connection`() {
    val service = start()
    val id = shadowOf(service).lastForegroundNotificationId

    repository.connection.value = ConnectionState.Connected
    idle()
    assertEquals(text(R.string.connection_connected), shadowNotifications.getNotification(id).body())

    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    idle()
    assertEquals(text(R.string.connection_unreachable), shadowNotifications.getNotification(id).body())
  }

  @Test
  fun `it starts as a connected-device service, the kind the manifest declares`() {
    val service = start()
    // What Android was told, which a service that is declared one way and started another would get wrong.
    val method = ShadowService::class.java.getDeclaredMethod("getForegroundServiceType").apply { isAccessible = true }
    assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE, method.invoke(shadowOf(service)))
  }

  @Test
  fun `the notice that stays cannot be swiped away`() {
    val notice = shadowOf(start()).lastForegroundNotification
    assertTrue(notice.flags and Notification.FLAG_ONGOING_EVENT != 0)
  }

  @Test
  fun `if Android stops the service for memory it is started again`() {
    val controller = Robolectric.buildService(HomeService::class.java).create()
    running = controller
    assertEquals(Service.START_STICKY, controller.get().onStartCommand(null, 0, 1))
  }

  @Test
  fun `it keeps the repository in step with the board, once, however many times it is started`() {
    val controller = Robolectric.buildService(HomeService::class.java).create().startCommand(0, 1)
    running = controller
    controller.startCommand(0, 2)
    idle()
    assertEquals(1, repository.syncsStarted)
  }

  @Test
  fun `stopping the service stops the sync`() {
    start()
    assertFalse(repository.syncCancelled)
    running!!.destroy()
    running = null
    idle()
    assertTrue(repository.syncCancelled)
  }

  // ---- announcing a leak -------------------------------------------------------------

  @Test
  fun `a gas leak is announced, with the area in Portuguese`() {
    start()
    repository.announce(leak(count = 1, area = "kitchen"))
    idle()
    val notice = leakNotifications().single()
    assertEquals(text(R.string.alert_gas_title), notice.title())
    assertEquals(text(R.string.alert_gas_text, text(R.string.area_kitchen)), notice.body())
  }

  @Test
  fun `the bedroom has its name in Portuguese too`() {
    start()
    repository.announce(leak(count = 1, area = "bedroom"))
    idle()
    assertEquals(text(R.string.alert_gas_text, text(R.string.area_bedroom)), leakNotifications().single().body())
  }

  @Test
  fun `an area the app has no name for is announced by the name the board gave it`() {
    repository.home.value = Home(BootId("aaaa1111"), 1_000, listOf(Area(AreaId("garage"), "Garage", emptyList())))
    start()
    repository.announce(leak(count = 1, area = "garage"))
    idle()
    assertEquals(text(R.string.alert_gas_text, "Garage"), leakNotifications().single().body())
  }

  @Test
  fun `an area nobody knows is announced by its id`() {
    start()
    repository.announce(leak(count = 1, area = "attic"))
    idle()
    assertEquals(text(R.string.alert_gas_text, "attic"), leakNotifications().single().body())
  }

  @Test
  fun `each leak is a notification of its own`() {
    start()
    repository.announce(leak(count = 1))
    repository.announce(leak(count = 2))
    idle()
    assertEquals(2, leakNotifications().size)
  }

  @Test
  fun `the same count on another boot is another leak`() {
    start()
    repository.announce(leak(count = 1, boot = "aaaa1111"))
    repository.announce(leak(count = 1, boot = "bbbb2222"))
    idle()
    assertEquals(2, leakNotifications().size)
  }

  @Test
  fun `a leak that came before the service was listening is still announced`() {
    repository.announce(leak(count = 3))
    start()
    assertEquals(1, leakNotifications().size)
  }

  @Test
  fun `touching the notification opens the app`() {
    start()
    repository.announce(leak(count = 1))
    idle()
    val intent = shadowOf(leakNotifications().single().contentIntent).savedIntent
    assertEquals("io.github.gustavommcv.ciscsmarthome.ui.MainActivity", intent.component?.className)
  }

  @Test
  fun `a leak notification goes away when touched`() {
    start()
    repository.announce(leak(count = 1))
    idle()
    assertTrue(leakNotifications().single().flags and Notification.FLAG_AUTO_CANCEL != 0)
  }

  // ---- the channels --------------------------------------------------------------------

  @Test
  fun `the alert channel is loud and sounds like an alarm`() {
    start()
    val channel = notificationManager.getNotificationChannel("gas_alert")
    assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    assertEquals(AudioAttributes.USAGE_ALARM, channel.audioAttributes.usage)
    assertTrue(channel.shouldVibrate())
    assertEquals(text(R.string.channel_gas_alert), channel.name.toString())
  }

  @Test
  fun `the channel of the notice that stays is quiet`() {
    start()
    val channel = notificationManager.getNotificationChannel("monitoring")
    assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
    assertEquals(text(R.string.channel_monitoring), channel.name.toString())
  }

  @Suppress("unused") private val context: Context = app
}
