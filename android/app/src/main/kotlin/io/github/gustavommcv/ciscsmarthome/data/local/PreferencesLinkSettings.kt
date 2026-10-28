package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import io.github.gustavommcv.ciscsmarthome.domain.LinkSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The choice of link and the password of the board's Wi-Fi in a small preferences file that only the app can read: the choice by the name of the link (a name
 * the app does not know reads as no choice, so a file from a later version never keeps the app from opening), the password as it was typed.
 */
class PreferencesLinkSettings(private val preferences: SharedPreferences) : LinkSettings {
  private val kindState = MutableStateFlow(loadKind())
  private val passwordState = MutableStateFlow(loadPassword())

  override val kind: StateFlow<LinkKind> = kindState.asStateFlow()

  override val boardPassword: StateFlow<String?> = passwordState.asStateFlow()

  override fun choose(kind: LinkKind) {
    preferences.edit { putString(KIND_KEY, kind.name) }
    kindState.value = kind
  }

  override fun saveBoardPassword(password: String) {
    preferences.edit { putString(PASSWORD_KEY, password) }
    passwordState.value = password
  }

  override fun forgetBoardPassword() {
    preferences.edit { remove(PASSWORD_KEY) }
    passwordState.value = null
  }

  private fun loadKind(): LinkKind {
    val name = runCatching { preferences.getString(KIND_KEY, null) }.getOrNull()
    return LinkKind.entries.firstOrNull { it.name == name } ?: LinkKind.Default
  }

  private fun loadPassword(): String? = runCatching { preferences.getString(PASSWORD_KEY, null) }.getOrNull()

  private companion object {
    const val KIND_KEY = "link"
    const val PASSWORD_KEY = "board-password"
  }
}
