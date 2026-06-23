package com.kiniot.uflex.features.therapy.data.remote.datasource

import com.kiniot.uflex.features.therapy.data.mapper.toDomain
import com.kiniot.uflex.features.therapy.data.remote.dto.LiveRepEventDto
import com.kiniot.uflex.features.therapy.domain.model.LiveRepEvent
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

/**
 * Live rep-progress stream from the EDGE gateway over SSE (a different host than the
 * backend, on the LAN). Best-effort and optimistic: the backend `/progress` poll remains
 * authoritative. The [baseUrl] and [pairingToken] are resolved per subscription from the
 * backend rendezvous endpoint; the token authenticates this stream. Singleton so the link
 * survives recomposition.
 */
interface EdgeProgressDataSource {
    /** Cold stream of live rep events for [serialNumber]; collecting opens the SSE connection. */
    fun observeProgress(baseUrl: String, pairingToken: String, serialNumber: String): Flow<LiveRepEvent>
}

@Singleton
class EdgeProgressDataSourceImpl @Inject constructor(
    @Named("edgeOkHttp") private val client: OkHttpClient,
    private val json: Json
) : EdgeProgressDataSource {

    override fun observeProgress(
        baseUrl: String,
        pairingToken: String,
        serialNumber: String
    ): Flow<LiveRepEvent> = callbackFlow {
        val url = baseUrl.trimEnd('/') +
            "/api/v1/movement-monitoring/progress-stream?serial_number=$serialNumber"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer $pairingToken")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                // Only the named `rep` event carries JSON; heartbeats are SSE comments (not delivered here).
                val event = runCatching { json.decodeFromString<LiveRepEventDto>(data) }.getOrNull()
                if (event != null) trySend(event.toDomain())
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                // Surface as flow completion/failure; the collector (ViewModel) falls back to polling.
                close(t)
            }
        }

        val source = EventSources.createFactory(client).newEventSource(request, listener)
        awaitClose { source.cancel() }
    }.flowOn(Dispatchers.IO)
}
