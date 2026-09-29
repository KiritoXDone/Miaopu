package dev.kiritoxd.miaopu.widget

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import dev.kiritoxd.miaopu.data.EsportSubscriptionStore
import dev.kiritoxd.miaopu.data.HupuAdapter
import dev.kiritoxd.miaopu.data.HupuCookieSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException

/** Network work is owned by JobScheduler, never by the receiver's short lifetime. */
class WidgetRefreshService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refresh: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        if (WidgetRenderer.ids(this).isEmpty()) return false
        val adapter = HupuAdapter(HupuCookieSession(this))
        refresh = scope.launch {
            var clearedRefreshing = false
            try {
                WidgetRenderer.renderAll(this@WidgetRefreshService, refreshing = true)
                val subscriptions = EsportSubscriptionStore(this@WidgetRefreshService).subscriptions()
                val store = WidgetScheduleStore(this@WidgetRefreshService)
                val concurrency = Semaphore(3)
                coroutineScope {
                    subscriptions.map { sport ->
                        async(Dispatchers.IO) {
                            concurrency.withPermit {
                                val startedAt = System.currentTimeMillis()
                                val schedule = try {
                                    adapter.getSchedule(sport).data
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    android.util.Log.w("MiaopuWidget", "Schedule unavailable for ${sport.businessId}", error)
                                    null
                                }
                                store.record(sport, schedule, startedAt)
                            }
                        }
                    }.awaitAll()
                }
                WidgetRenderer.renderAll(this@WidgetRefreshService)
                clearedRefreshing = true
                try {
                    if (WidgetLogoStore(this@WidgetRefreshService).load(store.state(System.currentTimeMillis()).matches)) {
                        WidgetRenderer.renderAll(this@WidgetRefreshService)
                    }
                } catch (_: IOException) {
                    // Score data is still usable when a thumbnail cannot be stored.
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("MiaopuWidget", "Widget refresh did not complete", error)
            } finally {
                // Remove an in-progress label even if the system stopped this job.
                if (!clearedRefreshing) withContext(NonCancellable + Dispatchers.IO) {
                    WidgetRenderer.renderAll(this@WidgetRefreshService)
                }
            }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        refresh?.cancel()
        return WidgetRenderer.ids(this).isNotEmpty()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        internal const val JOB_ID = 27102

        internal fun enqueue(context: Context) {
            if (WidgetRenderer.ids(context).isEmpty()) return
            val scheduler = context.getSystemService(JobScheduler::class.java)
            // Both sizes share one request; repeated taps do not restart an in-flight job.
            if (scheduler.getPendingJob(JOB_ID) != null) return
            scheduler.schedule(
                JobInfo.Builder(JOB_ID, ComponentName(context, WidgetRefreshService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(0)
                    .setBackoffCriteria(30_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .build(),
            )
        }
    }
}
