package org.app.nxplayer.core

data class Playlist(val id: Long, val name: String, val uris: List<String>, val isMusic: Boolean = false)

data class Bookmark(val positionMs: Long, val createdMs: Long)

/** Choices remembered per file when "Remember selections" is on. */
data class FileSelections(
    val audioTrack: String? = null,
    val subtitleTrack: String? = null,
    val decoder: String? = null,
    val aspect: String? = null,
    val zoom: String? = null,
    val speed: Float? = null,
    val subtitleSyncMs: Long? = null,
)

/**
 * All user activity records, kept in one immutable value so the Android layer can persist it
 * atomically and the export file can round-trip it.
 */
data class UserData(
    val history: Map<String, PlaybackRecord> = emptyMap(),
    val lastPlayedUri: String? = null,
    val lastPlayedPerFolder: Map<String, String> = emptyMap(),
    val playlists: List<Playlist> = emptyList(),
    val bookmarks: Map<String, List<Bookmark>> = emptyMap(),
    val selections: Map<String, FileSelections> = emptyMap(),
    val searchHistory: List<String> = emptyList(),
    val favourites: Set<String> = emptySet(),
) {
    fun recordPlayback(uri: String, folder: String, positionMs: Long, durationMs: Long, nowMs: Long): UserData {
        val finished = durationMs > 0 && durationMs - positionMs < ResumePolicy.EDGE_MS
        return copy(
            history = history + (uri to PlaybackRecord(if (finished) 0 else positionMs, nowMs, finished)),
            lastPlayedUri = uri,
            lastPlayedPerFolder = lastPlayedPerFolder + (folder to uri),
        )
    }

    fun clearHistory() = copy(history = emptyMap(), lastPlayedUri = null, lastPlayedPerFolder = emptyMap(), searchHistory = emptyList())

    fun addSearch(query: String) = if (query.isBlank()) this else copy(searchHistory = (listOf(query) + searchHistory.filter { it != query }).take(20))

    fun createPlaylist(name: String, isMusic: Boolean, nowMs: Long) = copy(playlists = playlists + Playlist(nowMs, name, emptyList(), isMusic))

    fun addToPlaylist(id: Long, uris: List<String>) = copy(playlists = playlists.map { p ->
        if (p.id == id) p.copy(uris = (p.uris + uris).distinct()) else p
    })

    fun removeFromPlaylist(id: Long, uri: String) = copy(playlists = playlists.map { p -> if (p.id == id) p.copy(uris = p.uris - uri) else p })
    fun renamePlaylist(id: Long, name: String) = copy(playlists = playlists.map { if (it.id == id) it.copy(name = name) else it })
    fun deletePlaylist(id: Long) = copy(playlists = playlists.filterNot { it.id == id })

    fun addBookmark(uri: String, positionMs: Long, nowMs: Long): UserData {
        val list = (bookmarks[uri].orEmpty().filterNot { abs(it.positionMs - positionMs) < 1000 } + Bookmark(positionMs, nowMs)).sortedBy { it.positionMs }
        return copy(bookmarks = bookmarks + (uri to list))
    }

    fun removeBookmark(uri: String, positionMs: Long) = copy(bookmarks = bookmarks + (uri to bookmarks[uri].orEmpty().filterNot { it.positionMs == positionMs }))

    fun toggleFavourite(uri: String) = copy(favourites = if (uri in favourites) favourites - uri else favourites + uri)

    /** Moves all per-file records from [from] to [to] (used after rename or private-folder moves). */
    fun moveFile(from: String, to: String): UserData = copy(
        history = history.rekey(from, to),
        lastPlayedUri = if (lastPlayedUri == from) to else lastPlayedUri,
        lastPlayedPerFolder = lastPlayedPerFolder.mapValues { if (it.value == from) to else it.value },
        playlists = playlists.map { p -> p.copy(uris = p.uris.map { if (it == from) to else it }) },
        bookmarks = bookmarks.rekey(from, to),
        selections = selections.rekey(from, to),
        favourites = favourites.map { if (it == from) to else it }.toSet(),
    )

    fun forgetFile(uri: String): UserData = copy(
        history = history - uri,
        lastPlayedUri = lastPlayedUri.takeIf { it != uri },
        lastPlayedPerFolder = lastPlayedPerFolder.filterValues { it != uri },
        playlists = playlists.map { it.copy(uris = it.uris - uri) },
        bookmarks = bookmarks - uri,
        selections = selections - uri,
        favourites = favourites - uri,
    )

    fun encode(): String = buildString {
        history.forEach { (u, r) -> line("H", u, r.positionMs, r.lastPlayedMs, r.finished) }
        lastPlayedUri?.let { line("L", it) }
        lastPlayedPerFolder.forEach { (f, u) -> line("F", f, u) }
        playlists.forEach { p -> line("P", p.id, p.name, p.isMusic, *p.uris.toTypedArray()) }
        bookmarks.forEach { (u, list) -> list.forEach { b -> line("B", u, b.positionMs, b.createdMs) } }
        selections.forEach { (u, s) -> line("S", u, s.audioTrack ?: "", s.subtitleTrack ?: "", s.decoder ?: "", s.aspect ?: "", s.zoom ?: "", s.speed ?: "", s.subtitleSyncMs ?: "") }
        searchHistory.forEach { line("Q", it) }
        favourites.forEach { line("V", it) }
    }

    companion object {
        private const val SEP = '\u001F'

        private fun StringBuilder.line(tag: String, vararg parts: Any) {
            append(tag)
            parts.forEach { append(SEP).append(it.toString().replace("\n", " ")) }
            append('\n')
        }

        fun decode(raw: String?): UserData {
            if (raw.isNullOrBlank()) return UserData()
            var d = UserData()
            val bookmarks = mutableMapOf<String, MutableList<Bookmark>>()
            for (line in raw.lineSequence()) {
                val p = line.split(SEP)
                runCatching {
                    when (p[0]) {
                        "H" -> d = d.copy(history = d.history + (p[1] to PlaybackRecord(p[2].toLong(), p[3].toLong(), p[4].toBoolean())))
                        "L" -> d = d.copy(lastPlayedUri = p[1])
                        "F" -> d = d.copy(lastPlayedPerFolder = d.lastPlayedPerFolder + (p[1] to p[2]))
                        "P" -> d = d.copy(playlists = d.playlists + Playlist(p[1].toLong(), p[2], p.drop(4), p[3].toBoolean()))
                        "B" -> bookmarks.getOrPut(p[1]) { mutableListOf() } += Bookmark(p[2].toLong(), p[3].toLong())
                        "S" -> d = d.copy(selections = d.selections + (p[1] to FileSelections(
                            p[2].ifEmpty { null }, p[3].ifEmpty { null }, p[4].ifEmpty { null }, p[5].ifEmpty { null },
                            p[6].ifEmpty { null }, p[7].toFloatOrNull(), p[8].toLongOrNull(),
                        )))
                        "Q" -> d = d.copy(searchHistory = d.searchHistory + p[1])
                        "V" -> d = d.copy(favourites = d.favourites + p[1])
                    }
                }
            }
            return d.copy(bookmarks = bookmarks)
        }
    }
}

private fun abs(v: Long) = if (v < 0) -v else v

private fun <V> Map<String, V>.rekey(from: String, to: String): Map<String, V> {
    val v = this[from] ?: return this
    return (this - from) + (to to v)
}
