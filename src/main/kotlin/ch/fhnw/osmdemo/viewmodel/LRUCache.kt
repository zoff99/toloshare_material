package ch.fhnw.osmdemo.viewmodel

import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.locks.SynchronizedObject
import io.ktor.utils.io.locks.synchronized


@OptIn(InternalAPI::class)
class LRUCache<K, V>(private val maxSize: Int, private val onRemovedFromCache: (K, V) -> Unit = { _, _ -> }) : MutableMap<K, V> {
    private val cache = LinkedHashMap<K, V>()

    private val lock = SynchronizedObject()

    override val size: Int get() = synchronized(lock) { cache.size }

    override fun get(key: K): V? = synchronized(lock) {
        cache[key]?.also {
            cache.remove(key)
            cache[key] = it
        }
    }

    override fun put(key: K, value: V): V? = synchronized(lock) {
        val previous = cache[key]
        cache.remove(key)
        cache[key] = value

        if (cache.size > maxSize) {
            repeat (cache.size - maxSize){
                val eldest = cache.keys.first()
                onRemovedFromCache(eldest, cache[eldest]!!)
                cache.remove(eldest)
            }
        }
        return previous
    }

    override fun clear()        = synchronized(lock) { cache.clear() }
    override fun isEmpty()      = synchronized(lock) { cache.isEmpty() }
    override fun remove(key: K) = synchronized(lock) { cache.remove(key) }

    override fun putAll(from: Map<out K, V>) = synchronized(lock) {
        from.forEach { put(it.key, it.value) }
    }

    override fun containsValue(value: V) = synchronized(lock) { cache.containsValue(value) }
    override fun containsKey(key: K)     = synchronized(lock) { cache.containsKey(key) }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = synchronized(lock) { cache.entries.toMutableSet() }

    override val keys: MutableSet<K>
        get() = synchronized(lock) { cache.keys.toMutableSet() }

    override val values: MutableCollection<V>
        get() = synchronized(lock) { cache.values.toMutableList() }

}
