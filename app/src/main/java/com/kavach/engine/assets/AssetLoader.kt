package com.kavach.engine.assets

import android.content.Context

object AssetLoader {

    /** Reads a text file from the app's assets folder. */
    fun loadText(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    /** Reads raw bytes from the app's assets folder. */
    fun loadBytes(context: Context, path: String): ByteArray =
        context.assets.open(path).use { it.readBytes() }

    /** Lists all files under the given assets sub-directory. */
    fun list(context: Context, dir: String): Array<String> =
        context.assets.list(dir) ?: emptyArray()
}
