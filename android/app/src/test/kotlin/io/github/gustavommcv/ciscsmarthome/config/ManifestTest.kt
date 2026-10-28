package io.github.gustavommcv.ciscsmarthome.config

import io.github.gustavommcv.ciscsmarthome.di.AppContainer
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

/**
 * What the app asks of Android, and what it allows on the network. The board speaks plain HTTP, so
 * the only way to keep that from spreading is to allow it for the board's address and for nothing else.
 */
class ManifestTest {
  private val manifest = parse("src/main/AndroidManifest.xml")
  private val networkConfig = parse("src/main/res/xml/network_security_config.xml")

  private fun parse(path: String): Document =
    DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(File(path))

  private fun NodeList.elements(): List<Element> = (0 until length).map { item(it) as Element }

  private fun Element.android(name: String): String = getAttributeNS(ANDROID, name)

  private val application get() = manifest.getElementsByTagName("application").elements().single()

  @Test
  fun `the app asks for what reaching the board and watching it in the background need, and for nothing else`() {
    val permissions = manifest.getElementsByTagName("uses-permission").elements().map { it.android("name") }
    assertEquals(
      setOf(
        "android.permission.INTERNET",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.CHANGE_NETWORK_STATE",
      ),
      permissions.toSet(),
    )
    assertEquals("a permission is asked for twice", permissions.size, permissions.toSet().size)
  }

  @Test
  fun `the service is the one that watches the board, declared as a connected device and kept private`() {
    val service = manifest.getElementsByTagName("service").elements().single()
    assertEquals(".service.HomeService", service.android("name"))
    assertEquals("connectedDevice", service.android("foregroundServiceType"))
    assertEquals("false", service.android("exported"))
  }

  @Test
  fun `the only part of the app another app can start is the screen`() {
    val exported = manifest.getElementsByTagName("activity").elements().filter { it.android("exported") == "true" }
    assertEquals(listOf(".ui.MainActivity"), exported.map { it.android("name") })
    assertEquals(0, manifest.getElementsByTagName("receiver").length + manifest.getElementsByTagName("provider").length)
  }

  @Test
  fun `the application class is the one that wires the container`() {
    assertEquals(".CiscApplication", application.android("name"))
  }

  @Test
  fun `the application points to the network security config`() {
    assertEquals("@xml/network_security_config", application.android("networkSecurityConfig"))
  }

  @Test
  fun `the application does not allow plain HTTP everywhere`() {
    assertNotEquals("true", application.android("usesCleartextTraffic"))
  }

  @Test
  fun `plain HTTP is refused unless the config says otherwise`() {
    val base = networkConfig.getElementsByTagName("base-config").elements().single()
    assertEquals("false", base.getAttribute("cleartextTrafficPermitted"))
  }

  @Test
  fun `plain HTTP is allowed to the address of the board only`() {
    val allowed = networkConfig.getElementsByTagName("domain-config").elements()
    assertEquals(1, allowed.size)
    assertEquals("true", allowed.single().getAttribute("cleartextTrafficPermitted"))
    val domains = allowed.single().getElementsByTagName("domain").elements()
    assertEquals(listOf("192.168.4.1"), domains.map { it.textContent.trim() })
    assertEquals("false", domains.single().getAttribute("includeSubdomains"))
  }

  @Test
  fun `the address the app talks to is the one the config allows`() {
    val board = AppContainer.BOARD_URL.toHttpUrl()
    assertEquals("http", board.scheme)
    assertEquals("192.168.4.1", board.host)
  }

  @Test
  fun `the network the app joins is the one the firmware makes`() {
    val firmware = File("../../firmware/src/WifiConfig.h").readText()
    val name = Regex("""kAccessPointName\[\]\s*=\s*"([^"]+)"""").find(firmware)?.groupValues?.get(1)
    assertEquals(name, AppContainer.BOARD_SSID)
  }

  private companion object {
    const val ANDROID = "http://schemas.android.com/apk/res/android"
  }
}
