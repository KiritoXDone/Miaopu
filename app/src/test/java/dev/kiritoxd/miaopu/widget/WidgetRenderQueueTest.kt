package dev.kiritoxd.miaopu.widget

import kotlinx.coroutines.*
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRenderQueueTest {
    @Test fun burstDuringRenderQueuesOnlyOneAdditionalPass() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val releaseFirst = CompletableDeferred<Unit>()
        val secondFinished = CompletableDeferred<Unit>()
        var renders = 0
        try {
            val queue = WidgetRenderQueue(scope, windowMillis = 0) {
                renders++
                if (renders == 1) releaseFirst.await() else secondFinished.complete(Unit)
            }
            queue.request()
            assertEquals(1, renders)
            repeat(100) { queue.request() }
            assertEquals(1, renders)
            releaseFirst.complete(Unit)
            withTimeout(1_000) { secondFinished.await() }
            assertEquals(2, renders)
        } finally { scope.cancel() }
    }
}
