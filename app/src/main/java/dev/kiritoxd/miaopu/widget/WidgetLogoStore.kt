package dev.kiritoxd.miaopu.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.security.MessageDigest

internal class WidgetLogoStore(private val context: Context) {
    private val directory = File(context.cacheDir, "widget-logos-v2")

    fun cached(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        val file = fileFor(url)
        if (!file.isFile) return null
        val key = cacheKey(file)
        return synchronized(bitmaps) {
            bitmaps.get(key) ?: BitmapFactory.decodeFile(file.path)?.also { bitmaps.put(key, it) }
        }
    }

    suspend fun load(matches: List<WidgetMatch>): Boolean = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            directory.mkdirs()
            val changed = coroutineScope {
                matches.flatMap { it.teams }.mapNotNull { it.logoUrl }
                    .filter { it.startsWith("https://") }.distinct().take(6).map { url ->
                        async {
                            if (fileFor(url).isFile) return@async false
                            withTimeoutOrNull(8_000) {
                                val result = SingletonImageLoader.get(context).execute(
                                    ImageRequest.Builder(context).data(url).size(96).allowHardware(false).build(),
                                )
                                // Keep the decoded aspect ratio so ImageView can center wide flags and tall crests.
                                val bitmap = result.image?.toBitmap() ?: return@withTimeoutOrNull false
                                // Atomically replace a complete thumbnail; never expose a partial PNG to a renderer.
                                val temporary = File.createTempFile("logo", ".tmp", directory)
                                try {
                                    temporary.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                    val destination = fileFor(url)
                                    temporary.renameTo(destination).also { saved ->
                                        if (saved) synchronized(bitmaps) { bitmaps.put(cacheKey(destination), bitmap) }
                                    }
                                } finally {
                                    temporary.delete()
                                }
                            } ?: false
                        }
                    }.awaitAll().any { it }
            }
            directory.listFiles()?.filter { it.extension == "png" }?.sortedByDescending(File::lastModified)
                ?.drop(64)?.forEach(File::delete)
            changed
        }
    }

    private fun cacheKey(file: File): String = "${file.absolutePath}:${file.lastModified()}:${file.length()}"

    private companion object {
        val loadMutex = Mutex()
        val bitmaps = object : LruCache<String, Bitmap>(1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
        }
    }

    private fun fileFor(url: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$digest.png")
    }
}
