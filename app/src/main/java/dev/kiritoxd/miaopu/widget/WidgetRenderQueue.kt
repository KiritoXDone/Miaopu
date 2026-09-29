package dev.kiritoxd.miaopu.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Collapse bursts, with at most one additional pass queued while a render is running. */
internal class WidgetRenderQueue(
    scope: CoroutineScope,
    windowMillis: Long = 150,
    render: suspend () -> Unit,
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    init {
        scope.launch {
            for (request in requests) {
                delay(windowMillis)
                while (requests.tryReceive().isSuccess) { /* use the latest persisted schedule */ }
                render()
            }
        }
    }
    fun request() { requests.trySend(Unit) }
}
