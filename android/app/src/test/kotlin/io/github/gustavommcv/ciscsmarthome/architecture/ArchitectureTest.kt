package io.github.gustavommcv.ciscsmarthome.architecture

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Applies the layer rules to the real source of the app. */
class ArchitectureTest {
  // Gradle runs unit tests with the module folder (app/) as the working directory.
  private val sourceRoot = File("src/main/kotlin")

  private val sourceFiles: List<File> = sourceRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

  private fun relativePath(file: File) = file.relativeTo(sourceRoot).invariantSeparatorsPath

  @Test
  fun `the test finds the source of the app`() {
    assertTrue("no Kotlin files under ${sourceRoot.absolutePath}", sourceFiles.isNotEmpty())
  }

  @Test
  fun `every file is in a known layer`() {
    val unknown = sourceFiles.map(::relativePath).filter { LayerRules.layerOf(it) == null }
    assertTrue("files outside the layers (domain, data, ui, service, di): $unknown", unknown.isEmpty())
  }

  @Test
  fun `no layer depends on one it should not`() {
    val problems =
      sourceFiles.flatMap { file ->
        val layer = LayerRules.layerOf(relativePath(file)) ?: return@flatMap emptyList()
        LayerRules.violations(layer, file.readText()).map { "${relativePath(file)} (${layer.name}) imports $it" }
      }
    assertTrue("layer rules broken:\n${problems.joinToString("\n")}", problems.isEmpty())
  }
}
