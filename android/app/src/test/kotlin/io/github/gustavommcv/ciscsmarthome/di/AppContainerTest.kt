package io.github.gustavommcv.ciscsmarthome.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppContainerTest {
  @Test
  fun `a new container has not heard from the board yet`() {
    val container = AppContainer(ApplicationProvider.getApplicationContext<Context>())
    assertEquals(ConnectionState.Connecting, container.homeRepository.connection.value)
    assertNull(container.homeRepository.home.value)
  }

  @Test
  fun `a new container joins the network of the board, and has no password for it yet`() {
    val container = AppContainer(ApplicationProvider.getApplicationContext<Context>())
    assertEquals(LinkKind.BoardWifi, container.linkSettings.kind.value)
    assertNull(container.linkSettings.boardPassword.value)
  }

  @Test
  fun `the address of the board ends with a slash, so paths are added to it`() {
    assertTrue(AppContainer.BOARD_URL.endsWith("/"))
  }
}
