package dev.kiritoxd.miaopu.ui

import dev.kiritoxd.miaopu.data.Esport

/** Reports one result for the latest manual refresh, after all its requests finish. */
internal class ScheduleRefreshFeedback(private val onMessage: (String) -> Unit) {
    private val requests = TargetRequestGate()

    fun begin(esports: List<Esport>): (Esport, Boolean) -> Unit {
        val token = requests.begin("schedule-refresh")
        val pending = esports.toMutableSet()
        val total = pending.size
        var failures = 0
        return complete@{ esport, succeeded ->
            if (!requests.isCurrent(token) || !pending.remove(esport)) return@complete
            if (!succeeded) failures++
            if (pending.isEmpty() && requests.complete(token)) {
                onMessage(when (failures) {
                    0 -> "刷新成功"
                    total -> "刷新失败"
                    else -> "部分赛程刷新失败（$failures/$total）"
                })
            }
        }
    }
}
