package dev.kiritoxd.miaopu.ui

import dev.kiritoxd.miaopu.data.Esport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleRefreshFeedbackTest {
    @Test fun repeatedSuccessfulRefreshesEachReportSuccess() {
        val messages = mutableListOf<String>()
        val feedback = ScheduleRefreshFeedback { messages += it }
        repeat(2) { feedback.begin(listOf(Esport.LOL))(Esport.LOL, true) }
        assertEquals(listOf("刷新成功", "刷新成功"), messages)
    }

    @Test fun singleFailureReportsFailure() {
        val messages = mutableListOf<String>()
        ScheduleRefreshFeedback { messages += it }.begin(listOf(Esport.CS2))(Esport.CS2, false)
        assertEquals(listOf("刷新失败"), messages)
    }

    @Test fun batchWaitsForEveryResultRegardlessOfCompletionOrder() {
        val messages = mutableListOf<String>()
        val report = ScheduleRefreshFeedback { messages += it }.begin(listOf(Esport.LOL, Esport.CS2))
        report(Esport.CS2, true)
        assertTrue(messages.isEmpty())
        report(Esport.LOL, true)
        assertEquals(listOf("刷新成功"), messages)
    }

    @Test fun partialFailureIsNotReportedAsSuccess() {
        val messages = mutableListOf<String>()
        val report = ScheduleRefreshFeedback { messages += it }
            .begin(listOf(Esport.LOL, Esport.CS2, Esport.KOG))
        report(Esport.LOL, true)
        report(Esport.CS2, false)
        report(Esport.KOG, true)
        assertEquals(listOf("部分赛程刷新失败（1/3）"), messages)
    }

    @Test fun allFailedRequestsReportFailure() {
        val messages = mutableListOf<String>()
        val report = ScheduleRefreshFeedback { messages += it }.begin(listOf(Esport.LOL, Esport.CS2))
        report(Esport.LOL, false)
        report(Esport.CS2, false)
        assertEquals(listOf("刷新失败"), messages)
    }

    @Test fun aNewRefreshSuppressesLateResultsFromThePreviousRefresh() {
        val messages = mutableListOf<String>()
        val feedback = ScheduleRefreshFeedback { messages += it }
        val old = feedback.begin(listOf(Esport.LOL, Esport.CS2))
        old(Esport.LOL, false)
        val current = feedback.begin(listOf(Esport.CS2))
        old(Esport.CS2, false)
        assertTrue(messages.isEmpty())
        current(Esport.CS2, true)
        assertEquals(listOf("刷新成功"), messages)
    }

    @Test fun duplicateAndUnrelatedResultsCannotFinishTheBatchEarly() {
        val messages = mutableListOf<String>()
        val report = ScheduleRefreshFeedback { messages += it }.begin(listOf(Esport.LOL, Esport.CS2))
        report(Esport.LOL, true)
        report(Esport.LOL, false)
        report(Esport.KOG, false)
        assertTrue(messages.isEmpty())
        report(Esport.CS2, true)
        report(Esport.CS2, false)
        assertEquals(listOf("刷新成功"), messages)
    }
}
