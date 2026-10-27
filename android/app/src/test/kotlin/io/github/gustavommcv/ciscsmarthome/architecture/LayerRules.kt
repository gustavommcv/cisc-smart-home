package io.github.gustavommcv.ciscsmarthome.architecture

/** The layers of the app. See docs/arquitetura.md. */
enum class Layer {
  Domain,
  Data,
  Ui,
  Service,
  Di,
  /** The files in the base package itself: the Application class, which wires the container. */
  Root,
}

/**
 * Which layers may import which. It exists to keep the dependency rule true as the code grows:
 * the domain knows nothing of Android, of the screens or of the data, and the screens and the
 * service reach the data only through the domain's interfaces, wired by the container.
 */
object LayerRules {
  private const val BASE = "io.github.gustavommcv.ciscsmarthome"
  private val basePath = BASE.replace('.', '/')

  // What each layer may NOT import. Everything else is allowed.
  private val forbidden: Map<Layer, List<String>> =
    mapOf(
      Layer.Domain to listOf("android.", "androidx.", "$BASE.ui.", "$BASE.data.", "$BASE.service.", "$BASE.di."),
      Layer.Data to listOf("$BASE.ui.", "$BASE.service.", "$BASE.di."),
      Layer.Ui to listOf("$BASE.data.", "$BASE.service."),
      Layer.Service to listOf("$BASE.data.", "$BASE.ui."),
      Layer.Di to emptyList(),
      Layer.Root to emptyList(),
    )

  private val layerByFolder = mapOf("domain" to Layer.Domain, "data" to Layer.Data, "ui" to Layer.Ui, "service" to Layer.Service, "di" to Layer.Di)

  /** The layer of a source file, from its path under src/main/kotlin; null when it is in no layer. */
  fun layerOf(relativePath: String): Layer? {
    if (!relativePath.startsWith("$basePath/")) return null
    val inside = relativePath.removePrefix("$basePath/")
    if ('/' !in inside) return Layer.Root
    return layerByFolder[inside.substringBefore('/')]
  }

  /** The imports of [source] that [layer] may not have, in the order they appear. */
  fun violations(layer: Layer, source: String): List<String> {
    val rejected = forbidden.getValue(layer)
    return source
      .lineSequence()
      .map { it.trim() }
      .filter { it.startsWith("import ") }
      .map { it.removePrefix("import ").substringBefore(" as ").trim() }
      .filter { imported -> rejected.any { imported.startsWith(it) } }
      .toList()
  }
}
