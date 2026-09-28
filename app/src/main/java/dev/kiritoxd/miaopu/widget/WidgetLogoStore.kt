package dev.kiritoxd.miaopu.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.security.MessageDigest

internal class WidgetLogoStore(private val context: Context) {
    private val directory = File(context.cacheDir, "widget-logos-v2")

    fun cached(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        val file = fileFor(url)
        return if (file.isFile) BitmapFactory.decodeFile(file.path) else null
    }

    suspend fun load(matches: List<WidgetMatch>) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        coroutineScope {
            matches.flatMap { it.teams }.mapNotNull { it.logoUrl }
                .filter { it.startsWith("https://") }.distinct().take(6).map { url ->
                    async {
                        if (fileFor(url).isFile) return@async
                        withTimeoutOrNull(8_000) {
                            val result = SingletonImageLoader.get(context).execute(
                                ImageRequest.Builder(context).data(url).size(96).allowHardware(false).build(),
                            )
                            // Keep the decoded aspect ratio so ImageView can center wide flags and tall crests.
                            val bitmap = result.image?.toBitmap() ?: return@withTimeoutOrNull
                            // Atomically replace a complete thumbnail; never expose a partial PNG to a renderer.
                            val temporary = File.createTempFile("logo", ".tmp", directory)
                            try {
                                temporary.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                temporary.renameTo(fileFor(url))
                            } finally {
                                temporary.delete()
                            }
                        }
                    }
                }.awaitAll()
        }
        directory.listFiles()?.filter { it.extension == "png" }?.sortedByDescending(File::lastModified)
            ?.drop(64)?.forEach(File::delete)
    }

    private fun fileFor(url: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$digest.png")
    }
}
