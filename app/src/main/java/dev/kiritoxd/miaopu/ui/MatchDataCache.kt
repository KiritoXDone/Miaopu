package dev.kiritoxd.miaopu.ui

/** Small, session-only cache. A monotonic clock avoids wall-clock adjustments extending entries. */
internal class MatchDataCache<K, V>(
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
    private val lifetimeMillis: Long = 30_000,
    private val capacity: Int = 16,
) {
    private data class Entry<V>(val value: V, val expiresAt: Long)
    private val entries = linkedMapOf<K, Entry<V>>()

    operator fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (now() >= entry.expiresAt) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    fun put(key: K, value: V) {
        entries.remove(key)
        entries[key] = Entry(value, now() + lifetimeMillis)
        while (entries.size > capacity) entries.remove(entries.keys.first())
    }

    fun clear() = entries.clear()
}
