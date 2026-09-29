package dev.kiritoxd.miaopu.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class HupuHttpResponse(val status: Int, val body: String)

/** A cancelled screen request closes its active call, including an in-progress body read. */
internal object HupuHttpTransport {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build()

    suspend fun execute(request: Request, calls: Call.Factory = client): HupuHttpResponse =
        suspendCancellableCoroutine { continuation ->
            val call = calls.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!continuation.isActive) {
                        response.close()
                        return
                    }
                    try {
                        val result = response.use {
                            HupuHttpResponse(it.code, it.body?.string().orEmpty())
                        }
                        continuation.resume(result)
                    } catch (error: IOException) {
                        continuation.resumeWithException(error)
                    }
                }
            })
        }
}
