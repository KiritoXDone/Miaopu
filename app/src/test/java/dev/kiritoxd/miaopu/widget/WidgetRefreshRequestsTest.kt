package dev.kiritoxd.miaopu.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetRefreshRequestsTest {
    private class Scheduler(val supportsExpedited: Boolean = true) {
        val requests = WidgetRefreshRequests()
        var pending: WidgetRefreshRequest? = null
        var accept: (Boolean) -> Boolean = { true }
        val attempts = mutableListOf<Pair<WidgetRefreshRequest, Boolean>>()

        fun enqueue(manual: Boolean) = requests.enqueue(manual, supportsExpedited, { pending }) { request, expedited ->
            attempts += request to expedited
            accept(expedited).also { if (it) pending = request }
        }
        fun status() = requests.status { pending }
        fun start(): Any = Any().also { requests.started(it, requireNotNull(pending).id) }
    }

    @Test fun manualTapPromotesQueuedBackgroundRefresh() {
        val scheduler = Scheduler()
        scheduler.enqueue(manual = false)
        val background = scheduler.pending
        scheduler.enqueue(manual = true)
        assertEquals(listOf(false, true), scheduler.attempts.map { it.second })
        assertNotEquals(background?.id, scheduler.pending?.id)
        assertTrue(requireNotNull(scheduler.pending).manual)
        assertEquals(WidgetRefreshStatus.WAITING, scheduler.status())
    }

    @Test fun repeatedTapsAndPeriodicBroadcastsKeepOneQueuedManualRefresh() {
        val scheduler = Scheduler()
        scheduler.enqueue(manual = true)
        repeat(100) {
            scheduler.enqueue(manual = true)
            scheduler.enqueue(manual = false)
        }
        assertEquals(1, scheduler.attempts.size)
    }

    @Test fun tappingDuringFetchDoesNotCancelOrRestartIt() {
        val scheduler = Scheduler()
        scheduler.enqueue(manual = false)
        scheduler.start()
        scheduler.enqueue(manual = true)
        scheduler.enqueue(manual = false)
        assertEquals(1, scheduler.attempts.size)
        assertEquals(WidgetRefreshStatus.RUNNING, scheduler.status())
    }

    @Test fun exhaustedExpeditedQuotaFallsBackWithoutLosingTheRequest() {
        val scheduler = Scheduler()
        scheduler.accept = { expedited -> !expedited }
        scheduler.enqueue(manual = true)
        assertEquals(listOf(true, false), scheduler.attempts.map { it.second })
        assertEquals(scheduler.attempts[0].first, scheduler.attempts[1].first)
        assertEquals(WidgetRefreshStatus.WAITING, scheduler.status())
        scheduler.enqueue(manual = true)
        assertEquals(2, scheduler.attempts.size)
    }

    @Test fun schedulingFailureIsVisibleAndAnotherTapCanRetry() {
        val scheduler = Scheduler()
        scheduler.accept = { false }
        scheduler.enqueue(manual = true)
        assertEquals(WidgetRefreshStatus.FAILED, scheduler.status())
        scheduler.accept = { true }
        scheduler.enqueue(manual = true)
        assertEquals(WidgetRefreshStatus.WAITING, scheduler.status())
    }

    @Test fun olderAndroidSchedulesOneRegularManualRequest() {
        val scheduler = Scheduler(supportsExpedited = false)
        scheduler.enqueue(manual = true)
        assertEquals(listOf(false), scheduler.attempts.map { it.second })
        assertTrue(requireNotNull(scheduler.pending).manual)
    }

    @Test fun processRestartRecognizesAlreadyQueuedManualWork() {
        val scheduler = Scheduler()
        scheduler.pending = WidgetRefreshRequest("existing", manual = true)
        assertEquals(WidgetRefreshStatus.WAITING, scheduler.status())
        scheduler.enqueue(manual = true)
        assertTrue(scheduler.attempts.isEmpty())
    }

    @Test fun stoppedRunCannotClearTheStatusOfItsReplacement() {
        val scheduler = Scheduler()
        scheduler.enqueue(manual = true)
        val old = scheduler.start()
        assertTrue(scheduler.requests.stopped(requireNotNull(scheduler.pending).id, reschedule = true))
        assertEquals(WidgetRefreshStatus.WAITING, scheduler.status())
        val current = scheduler.start()
        assertFalse(scheduler.requests.finished(old, failed = true))
        assertEquals(WidgetRefreshStatus.RUNNING, scheduler.status())
        assertTrue(scheduler.requests.finished(current))
        assertEquals(WidgetRefreshStatus.IDLE, scheduler.status())
    }

    @Test fun justFinishedJobStillReportedByAndroidDoesNotSwallowNextTap() {
        val scheduler = Scheduler()
        scheduler.enqueue(manual = true)
        val completed = scheduler.pending
        scheduler.requests.finished(scheduler.start())
        // Android has not yet removed the completed JobInfo from getPendingJob().
        assertEquals(completed, scheduler.pending)
        assertEquals(WidgetRefreshStatus.IDLE, scheduler.status())
        scheduler.enqueue(manual = true)
        assertEquals(2, scheduler.attempts.size)
        assertNotEquals(completed?.id, scheduler.pending?.id)
        assertEquals(WidgetRefreshStatus.WAITING, scheduler.status())
    }

    @Test fun unexpectedFetchFailureIsClearedWhenRetryStarts() {
        val scheduler = Scheduler()
        scheduler.enqueue(manual = true)
        scheduler.requests.finished(scheduler.start(), failed = true)
        assertEquals(WidgetRefreshStatus.FAILED, scheduler.status())
        scheduler.enqueue(manual = true)
        scheduler.start()
        assertEquals(WidgetRefreshStatus.RUNNING, scheduler.status())
    }

    @Test fun resultRenderingKeepsWorkInFlightUntilTheWidgetHasBeenUpdated() {
        val scheduler = Scheduler()
        scheduler.enqueue(manual = true)
        val token = scheduler.start()
        scheduler.requests.finishing(token, failed = false)
        assertEquals(WidgetRefreshStatus.IDLE, scheduler.status())
        scheduler.enqueue(manual = true)
        assertEquals(1, scheduler.attempts.size)
        assertTrue(scheduler.requests.finished(token))
    }

    @Test fun stopCallbackForReplacedRequestCannotCancelTheCurrentFetch() {
        val scheduler = Scheduler()
        scheduler.enqueue(manual = false)
        val oldId = requireNotNull(scheduler.pending).id
        scheduler.enqueue(manual = true)
        scheduler.start()
        assertFalse(scheduler.requests.stopped(oldId, reschedule = true))
        assertEquals(WidgetRefreshStatus.RUNNING, scheduler.status())
    }
}
