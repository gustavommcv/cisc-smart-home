package io.github.gustavommcv.ciscsmarthome.testing

import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import io.github.gustavommcv.ciscsmarthome.domain.LinkSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The choice of link and the password in memory, writing down what was chosen and saved. */
class FakeLinkSettings(initial: LinkKind = LinkKind.Default, password: String? = null) : LinkSettings {
  private val kindState = MutableStateFlow(initial)
  private val passwordState = MutableStateFlow(password)

  override val kind: StateFlow<LinkKind> = kindState

  override val boardPassword: StateFlow<String?> = passwordState

  val chosen = mutableListOf<LinkKind>()
  val saved = mutableListOf<String>()
  var forgotten = 0
    private set

  override fun choose(kind: LinkKind) {
    chosen += kind
    kindState.value = kind
  }

  override fun saveBoardPassword(password: String) {
    saved += password
    passwordState.value = password
  }

  override fun forgetBoardPassword() {
    forgotten++
    passwordState.value = null
  }
}
