package io.github.gustavommcv.ciscsmarthome.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
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
import io.github.gustavommcv.ciscsmarthome.testing.houseOfTheMaquete
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowService

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
  private fun leakNotifications(): List<Notification> = shadowNotifications.allNotifications.filter { it.channelId == "gas_alarm" }

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

  // ---- stopping the monitoring, which is the user's to do --------------------------------

  private fun stopIntent() = Intent(app, HomeService::class.java).setAction("io.github.gustavommcv.ciscsmarthome.action.STOP_MONITORING")

  /** What touching "stop" on the notice does: the service gets the intent the notice carries. */
  private fun HomeService.stopFromTheNotice(): Int = onStartCommand(stopIntent(), 0, 2)

  @Test
  fun `the notice that stays has a button to stop monitoring`() {
    val action = shadowOf(start()).lastForegroundNotification.actions.single()
    assertEquals(text(R.string.notice_stop_monitoring), action.title.toString())
  }

  @Test
  fun `the button asks the service to stop, and does not open the app`() {
    val button = shadowOf(shadowOf(start()).lastForegroundNotification.actions.single().actionIntent)
    assertTrue(button.isServiceIntent)
    val intent = button.savedIntent
    assertEquals(HomeService::class.java.name, intent.component?.className)
    assertEquals("io.github.gustavommcv.ciscsmarthome.action.STOP_MONITORING", intent.action)
  }

  @Test
  fun `the button is the only action of the notice`() {
    assertEquals(1, shadowOf(start()).lastForegroundNotification.actions.size)
  }

  @Test
  fun `stopping from the notice stops the sync`() {
    val service = start()
    service.stopFromTheNotice()
    idle()
    assertTrue(repository.syncCancelled)
  }

  @Test
  fun `stopping from the notice takes the notice away and stops the service`() {
    val service = start()
    service.stopFromTheNotice()
    idle()
    assertTrue(shadowOf(service).isForegroundStopped)
    assertFalse(shadowOf(service).isLastForegroundNotificationAttached)
    assertTrue(shadowOf(service).isStoppedBySelf)
  }

  @Test
  fun `Android does not start the service again after the user stopped it`() {
    assertEquals(Service.START_NOT_STICKY, start().stopFromTheNotice())
  }

  @Test
  fun `stopping from the notice closes the app, so nothing of it is left on the screen`() {
    start().stopFromTheNotice()
    idle()
    assertEquals(1, app.appsClosed)
  }

  @Test
  fun `stopping from the notice takes the warning of a lost board away, for nobody is watching on purpose`() {
    val service = start()
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    pass(61)
    assertEquals(1, lostNotifications().size)
    service.stopFromTheNotice()
    idle()
    assertEquals(0, lostNotifications().size)
  }

  @Test
  fun `stopping from the notice leaves the warning of a leak that is going on`() {
    val service = start()
    repository.announce(leak(count = 1))
    idle()
    service.stopFromTheNotice()
    idle()
    assertEquals(1, leakNotifications().size)
  }

  @Test
  fun `stopping from the notice takes the countdown with it`() {
    val service = start()
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    idle()
    service.stopFromTheNotice()
    pass(120)
    assertEquals(0, lostNotifications().size)
  }

  @Test
  fun `a leak after the user stopped is not warned of, for nothing is listening`() {
    val service = start()
    service.stopFromTheNotice()
    idle()
    repository.announce(leak(count = 1))
    idle()
    assertEquals(0, leakNotifications().size)
  }

  @Test
  fun `starting the monitoring again after it was stopped keeps the repository in step again`() {
    start().stopFromTheNotice()
    idle()
    running!!.destroy()
    start()
    assertEquals(2, repository.syncsStarted)
  }

  @Test
  fun `a stop that comes with nothing running does no harm`() {
    val controller = Robolectric.buildService(HomeService::class.java).create()
    running = controller
    controller.get().stopFromTheNotice()
    idle()
    assertEquals(0, repository.syncsStarted)
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
  fun `a room the board names in English is announced in Portuguese all the same`() {
    repository.home.value = house("emergency", alerts = 1)
    start()
    repository.announce(leak(count = 1, area = "kitchen"))
    idle()
    assertEquals(text(R.string.alert_gas_text, text(R.string.area_kitchen)), leakNotifications().single().body())
  }

  @Test
  fun `a room the user named is announced by the name the user gave it, before ours`() {
    app.names.renameArea(AreaId("kitchen"), "Cozinha da vó")
    start()
    repository.announce(leak(count = 1, area = "kitchen"))
    idle()
    assertEquals(text(R.string.alert_gas_text, "Cozinha da vó"), leakNotifications().single().body())
  }

  @Test
  fun `a room the user named, that the app has no words for, is announced by the user's name, not the board's`() {
    repository.home.value = Home(BootId("aaaa1111"), 1_000, listOf(Area(AreaId("garage"), "Garage", emptyList())))
    app.names.renameArea(AreaId("garage"), "Oficina")
    start()
    repository.announce(leak(count = 1, area = "garage"))
    idle()
    assertEquals(text(R.string.alert_gas_text, "Oficina"), leakNotifications().single().body())
  }

  @Test
  fun `a room named after the service started is announced by the new name`() {
    start()
    app.names.renameArea(AreaId("kitchen"), "Copa")
    repository.announce(leak(count = 1, area = "kitchen"))
    idle()
    assertEquals(text(R.string.alert_gas_text, "Copa"), leakNotifications().single().body())
  }

  @Test
  fun `a room that goes back to the default name is announced by ours again`() {
    app.names.renameArea(AreaId("kitchen"), "Copa")
    app.names.renameArea(AreaId("kitchen"), null)
    start()
    repository.announce(leak(count = 1, area = "kitchen"))
    idle()
    assertEquals(text(R.string.alert_gas_text, text(R.string.area_kitchen)), leakNotifications().single().body())
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

  // ---- repeating until someone acts ----------------------------------------------------

  @Test
  fun `the warning of a leak repeats its sound until it is opened or cancelled`() {
    start()
    repository.announce(leak(count = 1))
    idle()
    assertTrue(leakNotifications().single().flags and Notification.FLAG_INSISTENT != 0)
  }

  // ---- stopping by itself when the board says the leak is over -------------------------

  private fun house(state: String, alerts: Long, boot: String = "aaaa1111") = houseOfTheMaquete(BootId(boot), 1_000, state, alerts)

  @Test
  fun `the warning goes away by itself when the board says the leak is over`() {
    start()
    repository.announce(leak(count = 1))
    repository.home.value = house("emergency", alerts = 1)
    idle()
    assertEquals(1, leakNotifications().size)

    // Someone pressed the button of the maquete once the gas was gone.
    repository.home.value = house("normal", alerts = 1)
    idle()
    assertEquals(0, leakNotifications().size)
  }

  @Test
  fun `the warning stays while the board still says emergency`() {
    start()
    repository.announce(leak(count = 1))
    repository.home.value = house("emergency", alerts = 1)
    idle()
    repository.home.value = house("emergency", alerts = 1).copy(uptimeMs = 2_000)
    idle()
    assertEquals(1, leakNotifications().size)
  }

  @Test
  fun `a home from before the leak does not take its warning away`() {
    start()
    repository.home.value = house("normal", alerts = 0)
    idle()
    repository.announce(leak(count = 1))
    idle()
    assertEquals(1, leakNotifications().size)
  }

  @Test
  fun `a house that says nothing about gas leaves the warning alone`() {
    start()
    repository.announce(leak(count = 1))
    idle()
    repository.home.value = Home(BootId("aaaa1111"), 1_000, emptyList())
    idle()
    assertEquals(1, leakNotifications().size)
  }

  @Test
  fun `the warnings of all the leaks that ended go away`() {
    start()
    repository.announce(leak(count = 1))
    repository.announce(leak(count = 2))
    idle()
    assertEquals(2, leakNotifications().size)
    repository.home.value = house("normal", alerts = 2)
    idle()
    assertEquals(0, leakNotifications().size)
  }

  @Test
  fun `a new leak is warned of again after the last one ended`() {
    start()
    repository.announce(leak(count = 1))
    repository.home.value = house("normal", alerts = 1)
    idle()
    repository.announce(leak(count = 2))
    idle()
    assertEquals(1, leakNotifications().size)
  }

  @Test
  fun `a warning of another boot is left alone`() {
    start()
    repository.announce(leak(count = 1, boot = "aaaa1111"))
    idle()
    repository.home.value = house("normal", alerts = 5, boot = "bbbb2222")
    idle()
    assertEquals(1, leakNotifications().size)
  }

  @Test
  fun `the notice that stays is not taken away with the warning`() {
    val service = start()
    repository.announce(leak(count = 1))
    repository.home.value = house("normal", alerts = 1)
    idle()
    assertNotNull(shadowNotifications.getNotification(shadowOf(service).lastForegroundNotificationId))
  }

  // ---- a house nobody is watching ----------------------------------------------------------

  private fun lostNotifications(): List<Notification> = shadowNotifications.allNotifications.filter { it.channelId == "connection_lost" }

  private fun pass(seconds: Long) = shadowOf(Looper.getMainLooper()).idleFor(seconds, TimeUnit.SECONDS)

  @Test
  fun `a board that stays out of reach for a minute is a house nobody is watching`() {
    start()
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    pass(61)
    val notice = lostNotifications().single()
    assertEquals(text(R.string.alert_lost_title), notice.title())
    assertEquals(text(R.string.connection_unreachable), notice.body())
  }

  @Test
  fun `the warning does not take the place of the notice that stays`() {
    val service = start()
    val id = shadowOf(service).lastForegroundNotificationId
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    pass(61)
    assertEquals(1, lostNotifications().size)
    assertEquals("monitoring", shadowNotifications.getNotification(id).channelId)
  }

  @Test
  fun `it does not warn before the minute is over`() {
    start()
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    pass(50)
    assertEquals(0, lostNotifications().size)
  }

  @Test
  fun `a board that comes back in time is not warned about`() {
    start()
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    pass(40)
    repository.connection.value = ConnectionState.Connected
    pass(120)
    assertEquals(0, lostNotifications().size)
  }

  @Test
  fun `a board that goes away again starts the minute again`() {
    start()
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    pass(40)
    repository.connection.value = ConnectionState.Connected
    pass(5)
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    // 70 s since the first time it went away, but only 25 s since the last time it did.
    pass(25)
    assertEquals(0, lostNotifications().size)
  }

  @Test
  fun `the warning goes away when the board is heard again`() {
    start()
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    pass(61)
    assertEquals(1, lostNotifications().size)
    repository.connection.value = ConnectionState.Connected
    idle()
    assertEquals(0, lostNotifications().size)
  }

  @Test
  fun `the warning says why the board is out of reach`() {
    start()
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.TooManyClients)
    pass(61)
    assertEquals(text(R.string.connection_too_many_clients), lostNotifications().single().body())
  }

  @Test
  fun `the channel of that warning is loud, but it is not the alarm of a leak`() {
    start()
    val channel = notificationManager.getNotificationChannel("connection_lost")
    assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    assertEquals(AudioAttributes.USAGE_NOTIFICATION, channel.audioAttributes.usage)
    assertEquals(text(R.string.channel_connection_lost), channel.name.toString())
  }

  @Test
  fun `stopping the service takes the countdown with it`() {
    start()
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    idle()
    running!!.destroy()
    running = null
    pass(120)
    assertEquals(0, lostNotifications().size)
  }

  // ---- the channels --------------------------------------------------------------------

  @Test
  fun `the alert channel is loud and sounds like an alarm`() {
    start()
    val channel = notificationManager.getNotificationChannel("gas_alarm")
    assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    assertEquals(AudioAttributes.USAGE_ALARM, channel.audioAttributes.usage)
    assertTrue(channel.shouldVibrate())
    assertEquals(text(R.string.channel_gas_alert), channel.name.toString())
  }

  @Test
  fun `the alert has a sound of its own, not the alarm ringtone of the phone, which is the sound of the alarm clock`() {
    start()
    val channel = notificationManager.getNotificationChannel("gas_alarm")
    assertEquals(Uri.parse("android.resource://${app.packageName}/${R.raw.gas_alarm}"), channel.sound)
    assertNotEquals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), channel.sound)
  }

  @Test
  fun `the alert vibrates in three strong pulses, like the three beeps of its sound`() {
    start()
    val pattern = notificationManager.getNotificationChannel("gas_alarm").vibrationPattern
    assertEquals(listOf(0L, 400L, 150L, 400L, 150L, 400L, 600L), pattern.toList())
  }

  @Test
  fun `the channel the alert used to have, with the alarm ringtone, is taken away, for a channel keeps its sound for good`() {
    notificationManager.createNotificationChannel(NotificationChannel("gas_alert", "old", NotificationManager.IMPORTANCE_HIGH))
    start()
    assertNull(notificationManager.getNotificationChannel("gas_alert"))
  }

  @Test
  fun `starting the service again keeps the channel as it is`() {
    start()
    running!!.destroy()
    start()
    assertNotNull(notificationManager.getNotificationChannel("gas_alarm"))
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
