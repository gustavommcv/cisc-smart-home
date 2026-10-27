package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HistoryPage
import io.github.gustavommcv.ciscsmarthome.domain.Home
import kotlinx.coroutines.flow.Flow

/** What the app asks of the board over REST. [BoardClient] is the real one; the tests of whoever uses it give a fake. */
interface BoardApi {
  suspend fun getHome(): BoardResult<Home>

  /** Asks an entity to become [value]. The result is the entity as the board says it ended up. */
  suspend fun sendCommand(entityId: EntityId, value: CommandValue): BoardResult<Entity>

  /** The stored samples, oldest first. [after] keeps only those with a greater uptime; [limit] caps how many. */
  suspend fun getHistory(after: Long? = null, limit: Int? = null): BoardResult<HistoryPage>
}

/** What the board announces on its own. [BoardEventStream] is the real one. */
interface BoardEvents {
  fun updates(): Flow<BoardStreamUpdate>
}
