package io.github.gustavommcv.ciscsmarthome.ui.theme

import java.io.File
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The logo of the app: the icon on the home screen of the phone, the icon in the status bar and the mark on the first screen. They are one drawing. */
class LauncherIconTest {
  // Unit tests run from the module folder, as ContractTest relies on.
  private val res = File("src/main/res")

  private fun read(path: String): String = File(res, path).readText()

  private fun attribute(source: String, name: String): String? = Regex("""android:$name="([^"]*)"""").find(source)?.groupValues?.get(1)

  private fun pathData(path: String): List<String> = Regex("""android:pathData="([^"]*)"""").findAll(read(path)).map { it.groupValues[1] }.toList()

  /** The points a path goes through, for a path made of absolute commands, in which every number belongs to a pair. */
  private fun points(data: String): List<Pair<Double, Double>> {
    assertTrue("a path with relative or arc commands cannot be read here: $data", data.none { it.isLetter() && it !in "MLCQZ" })
    val numbers = Regex("""-?\d+(\.\d+)?""").findAll(data).map { it.value.toDouble() }.toList()
    assertEquals("odd number of coordinates in $data", 0, numbers.size % 2)
    return numbers.chunked(2).map { it[0] to it[1] }
  }

  private val layers = listOf("ic_launcher_background", "ic_launcher_foreground", "ic_launcher_monochrome")

  @Test
  fun `the icon and its round twin are the same adaptive icon, with a layer for themed icons`() {
    listOf("ic_launcher", "ic_launcher_round").forEach { name ->
      val source = read("mipmap-anydpi/$name.xml")
      assertTrue("$name background", source.contains("""<background android:drawable="@drawable/ic_launcher_background" />"""))
      assertTrue("$name foreground", source.contains("""<foreground android:drawable="@drawable/ic_launcher_foreground" />"""))
      assertTrue("$name monochrome", source.contains("""<monochrome android:drawable="@drawable/ic_launcher_monochrome" />"""))
    }
  }

  @Test
  fun `each layer of the icon is a vector of 108 dp on a 108 grid`() {
    layers.forEach { layer ->
      val source = read("drawable/$layer.xml")
      assertEquals("$layer width", "108dp", attribute(source, "width"))
      assertEquals("$layer height", "108dp", attribute(source, "height"))
      assertEquals("$layer viewportWidth", "108", attribute(source, "viewportWidth"))
      assertEquals("$layer viewportHeight", "108", attribute(source, "viewportHeight"))
    }
  }

  @Test
  fun `the mark stays inside the safe zone of an adaptive icon, a circle of 66 dp, whatever the mask`() {
    listOf("ic_launcher_foreground", "ic_launcher_monochrome").forEach { layer ->
      pathData("drawable/$layer.xml").forEach { data ->
        points(data).forEach { (x, y) -> assertTrue("$layer has a point at ($x, $y), out of the circle of radius 33", hypot(x - 54, y - 54) <= 33.0) }
      }
    }
  }

  @Test
  fun `the layer for themed icons draws the same house, with the shield cut out of it`() {
    val colored = pathData("drawable/ic_launcher_foreground.xml")
    val (house, shield, check) = colored
    val mono = pathData("drawable/ic_launcher_monochrome.xml")
    assertTrue("the house", mono[0].startsWith(house))
    assertTrue("the shield", mono[0].contains(shield))
    assertEquals("the check", check, mono[1])
    assertEquals("evenOdd", attribute(read("drawable/ic_launcher_monochrome.xml"), "fillType"))
  }

  @Test
  fun `the notification icon is a 24 dp silhouette, in white only`() {
    val source = read("drawable/ic_notification.xml")
    assertEquals("24dp", attribute(source, "width"))
    assertEquals("24dp", attribute(source, "height"))
    val colors = Regex("""android:(?:fillColor|strokeColor)="([^"]*)"""").findAll(source).map { it.groupValues[1].uppercase() }.toSet()
    assertEquals(setOf("#FFFFFFFF"), colors)
  }

  @Test
  fun `the notification icon is the house of the logo`() {
    val house = pathData("drawable/ic_launcher_foreground.xml").first()
    assertTrue(pathData("drawable/ic_notification.xml").first().startsWith(house))
  }

  @Test
  fun `the mark of the first screen is the drawing of the icon, so the two cannot drift apart`() {
    val logo = read("drawable/ic_logo.xml")
    assertEquals("108", attribute(logo, "viewportWidth"))
    pathData("drawable/ic_launcher_foreground.xml").forEach { data -> assertTrue("the logo lacks a part of the icon: ${data.take(24)}", pathData("drawable/ic_logo.xml").contains(data)) }
  }

  @Test
  fun `no raster icon is left, so the placeholder of the template cannot come back`() {
    val rasters = res.listFiles { file -> file.isDirectory && file.name.startsWith("mipmap") && file.name != "mipmap-anydpi" }.orEmpty().flatMap { it.listFiles().orEmpty().toList() }
    assertEquals("raster icons that the adaptive icon makes useless: ${rasters.map { it.path }}", emptyList<File>(), rasters)
  }
}
