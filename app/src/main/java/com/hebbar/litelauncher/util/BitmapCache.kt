package com.hebbar.litelauncher.util

import android.graphics.Bitmap
import android.util.LruCache

object BitmapCache {
    private const val MAX_CACHE_SIZE_KB = 12 * 1024 // 12 MB max memory cap for bitmaps

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE_KB) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        if (get(key) == null && !bitmap.isRecycled) {
            cache.put(key, bitmap)
        }
    }

    fun clear() {
        cache.evictAll()
    }
}
