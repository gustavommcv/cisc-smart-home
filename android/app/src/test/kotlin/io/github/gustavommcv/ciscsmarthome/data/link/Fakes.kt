package io.github.gustavommcv.ciscsmarthome.data.link

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow

/** The phone, asked to join a network: it writes down each request, and says what each test sends it to say. */
internal class FakeNetworkJoiner : NetworkJoiner {
  /** The name and password of each request, in order. */
  val requests = mutableListOf<Pair<String, String>>()

  /** How many requests are in force now, and the most there ever were at once. */
  var active = 0
    private set
  var mostActive = 0
    private set

  private val feeds = mutableListOf<Channel<JoinEvent>>()

  /** Says [event] on the request in force, the latest one. */
  fun send(event: JoinEvent) {
    feeds.last().trySend(event)
  }

  override fun join(ssid: String, password: String): Flow<JoinEvent> {
    requests += ssid to password
    val feed = Channel<JoinEvent>(Channel.UNLIMITED)
    feeds += feed
    return feed
      .receiveAsFlow()
      .onStart {
        active++
        mostActive = maxOf(mostActive, active)
      }
      .onCompletion { active-- }
  }
}

/** A link the test moves by hand, that says whether it is being kept up. */
internal class FakeBoardLink(initial: BoardRoute = BoardRoute.Joining) : BoardLink {
  private val state = MutableStateFlow(initial)

  override val route: StateFlow<BoardRoute> = state

  var running = false
    private set
  var started = 0
    private set

  fun go(route: BoardRoute) {
    state.value = route
  }

  override suspend fun keepUp(): Nothing {
    running = true
    started++
    try {
      awaitCancellation()
    } catch (e: CancellationException) {
      running = false
      throw e
    }
  }
}
