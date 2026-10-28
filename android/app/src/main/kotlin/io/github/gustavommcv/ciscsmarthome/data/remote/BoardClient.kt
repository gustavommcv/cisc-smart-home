package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HistoryPage
import io.github.gustavommcv.ciscsmarthome.domain.Home
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * The REST side of docs/api.md: asks the board for the house, sends it commands and fetches its
 * history. Every call says what it came to as a [BoardResult] instead of throwing.
 *
 * It does not choose the network, the timeouts or the address: [http] and [baseUrl] come from
 * outside, so a test can point it at a fake board.
 */
class BoardClient(private val baseUrl: HttpUrl, private val http: OkHttpClient) : BoardApi {
  private val json: Json = BoardJson

  override suspend fun getHome(): BoardResult<Home> =
    call(request("api/v1/home"), ::parseHome)

  override suspend fun sendCommand(entityId: EntityId, value: CommandValue): BoardResult<Entity> {
    val url = baseUrl.newBuilder().addPathSegments("api/v1/entities").addPathSegment(entityId.value).addPathSegment("value").build()
    val body = buildJsonObject {
      put(
        "value",
        when (value) {
          is CommandValue.Flag -> JsonPrimitive(value.on)
          is CommandValue.Choice -> JsonPrimitive(value.option)
          // A whole number goes as one ("90", not "90.0"): it is what the board's step is made of.
          is CommandValue.Number -> if (value.value % 1.0 == 0.0) JsonPrimitive(value.value.toLong()) else JsonPrimitive(value.value)
        },
      )
    }
    val request = Request.Builder().url(url).put(body.toString().toRequestBody(JSON_MEDIA_TYPE)).build()
    return call(request) { text ->
      json.decodeFromString<EntityDto>(text).toDomain() ?: error("the board answered with an entity of a kind the app does not know")
    }
  }

  override suspend fun getHistory(after: Long?, limit: Int?): BoardResult<HistoryPage> {
    val url =
      baseUrl.newBuilder().addPathSegments("api/v1/history").apply {
        if (after != null) addQueryParameter("after", after.toString())
        if (limit != null) addQueryParameter("limit", limit.toString())
      }.build()
    return call(Request.Builder().url(url).get().build()) { json.decodeFromString<HistoryDto>(it).toDomain() }
  }

  private fun request(path: String) = Request.Builder().url(baseUrl.newBuilder().addPathSegments(path).build()).get().build()

  private suspend fun <T> call(request: Request, parse: (String) -> T): BoardResult<T> {
    val answer =
      try {
        // Reading the body waits on the network, so all of it is done off the caller's thread, which may be the main one.
        withContext(Dispatchers.IO) { http.newCall(request).await().use { response -> response.code to response.body.string() } }
      } catch (e: IOException) {
        return BoardResult.Failure(BoardError.Unreachable(e))
      }
    val (status, body) = answer
    if (status !in 200..299) return BoardResult.Failure(rejectionOf(status, body))
    return try {
      BoardResult.Success(parse(body))
    } catch (e: SerializationException) {
      unexpected("the answer is not what the contract describes: ${e.message}")
    } catch (e: IllegalStateException) {
      unexpected(e.message ?: "the answer cannot be used")
    }
  }

  private fun unexpected(detail: String) = BoardResult.Failure(BoardError.Unexpected(detail))

  private companion object {
    val JSON_MEDIA_TYPE = "application/json".toMediaType()
  }
}
