package io.github.gustavommcv.ciscsmarthome.domain

/**
 * The ways the app has of reaching the board, as the user sees them. A way to come (through the cloud, through a VPN) is a new entry
 * here and a new link in the data layer; nothing else in the app has to know.
 */
enum class LinkKind {
  /** The app joins the Wi-Fi the board makes, by itself, and talks to the board through it. The phone's own Wi-Fi is left alone as long as it can be. */
  BoardWifi,

  /** The app talks to the board through whatever network the phone is on: the board's own, a VPN, another one that reaches it. */
  CurrentNetwork;

  companion object {
    val Default = BoardWifi
  }
}
