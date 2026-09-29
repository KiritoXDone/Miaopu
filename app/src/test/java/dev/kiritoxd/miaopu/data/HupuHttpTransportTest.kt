package dev.kiritoxd.miaopu.data

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class HupuHttpTransportTest {
    @Test fun cancellationCancelsCallAndDiscardsLateResponse() = runBlocking {
        val call = FakeCall()
        val task = async(start = CoroutineStart.UNDISPATCHED) { HupuHttpTransport.execute(call.request(), factory(call)) }
        task.cancelAndJoin()
        assertTrue(call.isCanceled())
        val body = TrackingBody()
        call.callback.onResponse(call, response(call, "late").newBuilder().body(body).build())
        assertTrue(task.isCancelled)
        assertTrue(body.closed)
    }

    @Test fun successfulResponsePreservesStatusAndBody() = runBlocking {
        val call = FakeCall()
        val task = async(start = CoroutineStart.UNDISPATCHED) { HupuHttpTransport.execute(call.request(), factory(call)) }
        call.callback.onResponse(call, response(call, "payload", 403))
        assertEquals(HupuHttpResponse(403, "payload"), task.await())
        assertFalse(call.isCanceled())
    }

    @Test fun networkFailureKeepsIOExceptionForAdapterMapping() = runBlocking {
        supervisorScope {
            val call = FakeCall()
            val task = async(start = CoroutineStart.UNDISPATCHED) { HupuHttpTransport.execute(call.request(), factory(call)) }
            val failure = IOException("offline")
            call.callback.onFailure(call, failure)
            try {
                task.await()
                fail("Expected network failure")
            } catch (actual: IOException) {
                // Coroutine stack-trace recovery may copy the exception across await().
                assertEquals(failure.javaClass, actual.javaClass)
                assertEquals(failure.message, actual.message)
            }
        }
    }

    private fun factory(call: Call) = object : Call.Factory {
        override fun newCall(request: Request): Call = call
    }

    private fun response(call: Call, body: String, code: Int = 200) = Response.Builder()
        .request(call.request()).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
        .body(body.toResponseBody()).build()

    private class TrackingBody : ResponseBody() {
        var closed = false
        private val source = object : ForwardingSource(Buffer().writeUtf8("late")) {
            override fun close() { closed = true; super.close() }
        }.buffer()
        override fun contentType(): MediaType? = null
        override fun contentLength(): Long = 4
        override fun source(): BufferedSource = source
    }

    private class FakeCall : Call {
        lateinit var callback: Callback
        private var cancelled = false
        override fun request(): Request = Request.Builder().url("https://example.invalid/").build()
        override fun enqueue(responseCallback: Callback) { callback = responseCallback }
        override fun cancel() { cancelled = true }
        override fun isCanceled(): Boolean = cancelled
        override fun isExecuted(): Boolean = ::callback.isInitialized
        override fun execute(): Response = error("Blocking execution is not used")
        override fun timeout(): Timeout = Timeout.NONE
        public override fun clone(): Call = FakeCall()
    }
}
