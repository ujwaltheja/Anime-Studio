package com.kavach.engine.assets.model

import android.content.Context
import android.util.Log
import com.kavach.engine.assets.model.GLBLoader

/** Simple in-memory cache for loaded models. Avoids re-parsing the same file. */
object ModelCache {

    private val cache = mutableMapOf<String, LoadedModel>()

    fun load(context: Context, assetPath: String): LoadedModel? {
        cache[assetPath]?.let { return it }
        val model = GLBLoader.load(context, assetPath) ?: run {
            Log.w("ModelCache", "Could not load $assetPath")
            return null
        }
        cache[assetPath] = model
        return model
    }

    fun evict(assetPath: String) = cache.remove(assetPath)

    fun clear() = cache.clear()
}
