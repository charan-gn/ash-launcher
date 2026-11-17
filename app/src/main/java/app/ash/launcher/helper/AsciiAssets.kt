package app.ash.launcher.helper

import android.content.Context

object AsciiAssets {

    private val cache = mutableMapOf<String, List<String>>()

    fun lines(context: Context, name: String): List<String> = cache.getOrPut(name) {
        runCatching {
            context.assets.open("ascii/$name.txt").bufferedReader().use { reader ->
                reader.readLines()
                    .map { it.trimEnd() }
                    .dropWhile { it.isEmpty() }
                    .dropLastWhile { it.isEmpty() }
            }
        }.getOrDefault(emptyList())
    }
}
