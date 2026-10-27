package io.github.gustavommcv.ciscsmarthome.architecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val BASE = "io.github.gustavommcv.ciscsmarthome"

class LayerRulesTest {
  private fun violations(layer: Layer, vararg imports: String): List<String> =
    LayerRules.violations(layer, imports.joinToString("\n") { "import $it" })

  // ---- which layer a file belongs to -----------------------------------------

  @Test
  fun `a file belongs to the layer named by its first folder`() {
    assertEquals(Layer.Domain, LayerRules.layerOf("io/github/gustavommcv/ciscsmarthome/domain/Area.kt"))
    assertEquals(Layer.Data, LayerRules.layerOf("io/github/gustavommcv/ciscsmarthome/data/remote/Client.kt"))
    assertEquals(Layer.Ui, LayerRules.layerOf("io/github/gustavommcv/ciscsmarthome/ui/home/HomeScreen.kt"))
    assertEquals(Layer.Service, LayerRules.layerOf("io/github/gustavommcv/ciscsmarthome/service/Alerts.kt"))
    assertEquals(Layer.Di, LayerRules.layerOf("io/github/gustavommcv/ciscsmarthome/di/AppContainer.kt"))
  }

  @Test
  fun `a file in the base package itself is the root`() {
    assertEquals(Layer.Root, LayerRules.layerOf("io/github/gustavommcv/ciscsmarthome/CiscApplication.kt"))
  }

  @Test
  fun `a folder that is not a layer is not known`() {
    assertNull(LayerRules.layerOf("io/github/gustavommcv/ciscsmarthome/utils/Strings.kt"))
  }

  // ---- the domain depends on nothing of ours or of Android --------------------

  @Test
  fun `the domain may not import Android`() {
    assertEquals(listOf("android.content.Context"), violations(Layer.Domain, "android.content.Context"))
    assertEquals(listOf("androidx.compose.ui.Modifier"), violations(Layer.Domain, "androidx.compose.ui.Modifier"))
  }

  @Test
  fun `the domain may not import the other layers`() {
    for (other in listOf("ui", "data", "service", "di")) {
      assertEquals("domain -> $other", 1, violations(Layer.Domain, "$BASE.$other.Anything").size)
    }
  }

  @Test
  fun `the domain may import the language and its own package`() {
    assertTrue(
      violations(Layer.Domain, "kotlin.collections.List", "kotlinx.coroutines.flow.Flow", "java.time.Instant", "$BASE.domain.Area")
        .isEmpty()
    )
  }

  // ---- the other layers --------------------------------------------------------

  @Test
  fun `the data layer may use the domain and Android libraries but not the screens or the service`() {
    assertTrue(violations(Layer.Data, "$BASE.domain.Area", "androidx.room.Dao", "android.net.Network").isEmpty())
    assertEquals(1, violations(Layer.Data, "$BASE.ui.home.HomeScreen").size)
    assertEquals(1, violations(Layer.Data, "$BASE.service.Alerts").size)
    assertEquals(1, violations(Layer.Data, "$BASE.di.AppContainer").size)
  }

  @Test
  fun `the screens may use the domain and the container but not the data layer or the service`() {
    assertTrue(violations(Layer.Ui, "$BASE.domain.Area", "$BASE.di.AppContainer", "androidx.compose.ui.Modifier").isEmpty())
    assertEquals(1, violations(Layer.Ui, "$BASE.data.HomeRepositoryImpl").size)
    assertEquals(1, violations(Layer.Ui, "$BASE.service.Alerts").size)
  }

  @Test
  fun `the service may use the domain and the container but not the screens or the data layer`() {
    assertTrue(violations(Layer.Service, "$BASE.domain.Area", "$BASE.di.AppContainer", "android.app.Service").isEmpty())
    assertEquals(1, violations(Layer.Service, "$BASE.ui.MainActivity").size)
    assertEquals(1, violations(Layer.Service, "$BASE.data.HomeRepositoryImpl").size)
  }

  @Test
  fun `the container and the root may use every layer`() {
    val everything = listOf("domain", "data", "ui", "service", "di").map { "$BASE.$it.Anything" }.toTypedArray()
    assertTrue(violations(Layer.Di, *everything).isEmpty())
    assertTrue(violations(Layer.Root, *everything).isEmpty())
  }

  // ---- how imports are read -----------------------------------------------------

  @Test
  fun `an import with an alias is judged by what it imports`() {
    assertEquals(1, LayerRules.violations(Layer.Domain, "import android.content.Context as AndroidContext").size)
  }

  @Test
  fun `a comment that mentions an import is not an import`() {
    val source = """
      // import android.content.Context
      /* import androidx.compose.ui.Modifier */
      package $BASE.domain
    """.trimIndent()
    assertTrue(LayerRules.violations(Layer.Domain, source).isEmpty())
  }

  @Test
  fun `every violation is reported, not only the first`() {
    assertEquals(
      listOf("android.os.Bundle", "$BASE.ui.MainActivity"),
      violations(Layer.Domain, "android.os.Bundle", "kotlin.collections.List", "$BASE.ui.MainActivity"),
    )
  }
}
