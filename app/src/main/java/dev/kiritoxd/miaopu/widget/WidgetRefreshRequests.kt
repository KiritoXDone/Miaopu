package dev.kiritoxd.miaopu.widget

import java.util.UUID

internal enum class WidgetRefreshStatus { IDLE, WAITING, RUNNING, FAILED }

internal data class WidgetRefreshRequest(val id: String, val manual: Boolean)

/** Distinguish queued work from a running fetch, and promote only queued background work. */
internal class WidgetRefreshRequests {
    private var active: Any? = null
    private var activeId: String? = null
    private var completedId: String? = null
    private var failed = false
    private var resultStatus: WidgetRefreshStatus? = null

    @Synchronized
    fun enqueue(
        manual: Boolean,
        supportsExpedited: Boolean,
        pending: () -> WidgetRefreshRequest?,
        schedule: (WidgetRefreshRequest, Boolean) -> Boolean,
    ) {
        if (active != null) return
        val queued = pending()?.takeUnless { it.id == completedId }
        if (queued != null && (!manual || queued.manual)) {
            failed = false
            return
        }
        val request = WidgetRefreshRequest(UUID.randomUUID().toString(), manual)
        val accepted = (manual && supportsExpedited && schedule(request, true)) || schedule(request, false)
        failed = !accepted
    }

    @Synchronized
    fun started(token: Any, requestId: String) {
        active = token
        activeId = requestId
        failed = false
        resultStatus = null
    }

    @Synchronized
    fun finishing(token: Any, failed: Boolean) {
        if (active === token) resultStatus = if (failed) WidgetRefreshStatus.FAILED else WidgetRefreshStatus.IDLE
    }

    @Synchronized
    fun stopped(requestId: String, reschedule: Boolean): Boolean {
        val token = active ?: return false
        if (activeId != requestId) return false
        return finished(token, reschedule = reschedule)
    }

    @Synchronized
    fun finished(token: Any, failed: Boolean = false, reschedule: Boolean = false): Boolean {
        if (active !== token) return false
        // JobScheduler may briefly keep a finished job visible after jobFinished().
        completedId = if (reschedule) null else activeId
        active = null
        activeId = null
        resultStatus = null
        this.failed = failed
        return true
    }

    @Synchronized
    fun status(pending: () -> WidgetRefreshRequest?): WidgetRefreshStatus = when {
        active != null -> resultStatus ?: WidgetRefreshStatus.RUNNING
        failed -> WidgetRefreshStatus.FAILED
        pending()?.let { it.id != completedId } == true -> WidgetRefreshStatus.WAITING
        else -> WidgetRefreshStatus.IDLE
    }
}
