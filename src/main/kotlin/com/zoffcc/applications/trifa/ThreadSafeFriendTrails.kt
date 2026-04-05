package com.zoffcc.applications.trifa

import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ThreadSafeFriendTrails {
    private val friendLocations = ConcurrentHashMap<String, Deque<Location>>()

    companion object {
        private const val MAX_AGE_MS = 180 * 1000L
        private const val MAX_POSITIONS = 80
    }

    fun updateLocation(friendId: String, newLoc: Location) {
        // computeIfAbsent ensures we get the same Deque instance for the ID
        val history = friendLocations.computeIfAbsent(friendId) { ArrayDeque() }

        synchronized(history) {
            history.addFirst(newLoc)
            val cutoff = System.currentTimeMillis() - MAX_AGE_MS

            // Clean up old entries
            history.removeIf { it.time < cutoff }

            // Trim to max size
            while (history.size > MAX_POSITIONS) {
                history.removeLast()
            }
        }
    }

    fun getRecentPositions(friendId: String): List<Location> {
        val history = friendLocations[friendId] ?: return emptyList()
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS

        return synchronized(history) {
            history.filter { it.time >= cutoff }
                .take(MAX_POSITIONS)
        }
    }
}
