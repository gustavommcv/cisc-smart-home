package io.github.gustavommcv.ciscsmarthome.ui.theme

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Material Symbols the app draws are all the same kind: 24 dp, on the 960 grid the set is drawn on, with something to draw. */
class SymbolIconsTest {
  // Unit tests run from the module folder, as ContractTest relies on.
  private val symbols =
    File("src/main/res/drawable").listFiles { file -> file.name.startsWith("ic_") && !file.name.startsWith("ic_launcher") && file.name != "ic_notification.xml" && file.name != "ic_logo.xml" }.orEmpty().sortedBy { it.name }

  private fun attribute(source: String, name: String): String? = Regex("""android:$name="([^"]*)"""").find(source)?.groupValues?.get(1)

  @Test
  fun `the app has symbols to check`() {
    assertTrue("no symbol was found: ${File("src/main/res/drawable").absolutePath}", symbols.size >= 6)
  }

  @Test
  fun `every symbol is 24 dp wide and high`() {
    symbols.forEach { file ->
      val source = file.readText()
      assertEquals("${file.name} width", "24dp", attribute(source, "width"))
      assertEquals("${file.name} height", "24dp", attribute(source, "height"))
    }
  }

  @Test
  fun `every symbol is drawn on the 960 grid of Material Symbols`() {
    symbols.forEach { file ->
      val source = file.readText()
      assertEquals("${file.name} viewportWidth", "960", attribute(source, "viewportWidth"))
      assertEquals("${file.name} viewportHeight", "960", attribute(source, "viewportHeight"))
    }
  }

  @Test
  fun `every symbol draws something`() {
    symbols.forEach { file -> assertTrue("${file.name} has no path", attribute(file.readText(), "pathData").orEmpty().length > 20) }
  }
}
