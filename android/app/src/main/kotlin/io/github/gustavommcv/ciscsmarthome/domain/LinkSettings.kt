package io.github.gustavommcv.ciscsmarthome.domain

import kotlinx.coroutines.flow.StateFlow

/** How the user chose to reach the board, and what the app needs to do it, kept for the next time the app opens. */
interface LinkSettings {
  /** The way in force: the user's choice, or [LinkKind.Default] before there is one. */
  val kind: StateFlow<LinkKind>

  /** The password of the Wi-Fi of the board, or null before the user gave it. It is kept as it was typed. */
  val boardPassword: StateFlow<String?>

  /** Keeps [kind] as the user's choice, and makes it the one in force at once. */
  fun choose(kind: LinkKind)

  /** Keeps [password] for joining the Wi-Fi of the board, and makes it the one in force at once. */
  fun saveBoardPassword(password: String)

  /** Throws the password away: there is none from now on, and the phone leaves the board's Wi-Fi until the user gives one. */
  fun forgetBoardPassword()
}
