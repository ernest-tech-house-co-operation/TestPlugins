package com.ernest

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.delay

class NothingTorrentProvider : MainAPI() {
    override var mainUrl = "https://apibay.org"
    override var name = "Nothing Torrent Provider"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    private val categories = mapOf(
        "Trending Movies" to "200",
        "Trending TV Shows" to "205",
        "Trending Anime" to "207"
    )

    private val trackers = listOf(
        "udp://tracker.coppersurfer.tk:6969/announce",
        "udp://tracker.leechers-paradise.org:6969/announce",
        "udp://9.rarbg.to:2920/announce",
        "udp://9.rarbg.me:2780/announce",
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://exodus.desync.com:6969/announce"
    )

    private val maxSizeBytes = 8L * 1024 * 1024 * 1024 // 8GB

    data class ApibayTorrent(
        val id: String,
        val name: String,
        val info_hash: String,
        val leechers: String,
        val seeders: String,
        val num_files: String,
        val size: String,
        val username: String,
        val added: String,
        val status: String,
        val category: String,
        val imdb: String? = null
    )

    private fun detectQuality(name: String): String {
        val n = name.lowercase()
        return when {
            n.contains("2160p") || n.contains("4k") || n.contains("uhd") -> "4K"
            n.contains("1080p") || n.contains("full hd") -> "1080p"
            n.contains("720p") || n.contains("hd") -> "720p"
            n.contains("480p") || n.contains("sd") -> "480p"
            n.contains("bdrip") || n.contains("bd rip") -> "BD-Rip"
            n.contains("brrip") || n.contains("br rip") -> "BR-Rip"
            n.contains("webrip") || n.contains("web-dl") -> "WEB-Rip"
            else -> "Unknown"
        }
    }

    private fun buildMagnet(infoHash: String, displayName: String): String {
        val dn = java.net.URLEncoder.encode(displayName, "UTF-8")
        val trackerParams = trackers.joinToString("") { "&tr=${java.net.URLEncoder.encode(it, "UTF-8")}" }
        return "magnet:?xt=urn:btih:$infoHash&dn=$dn$trackerParams"
    }

    // === NEW: wrap/unwrap helpers so the magnet never travels as a "relative" string ===
    private fun wrapMagnet(magnet: String): String {
        return "https://magnet.local/?m=${java.net.URLEncoder.encode(magnet, "UTF-8")}"
    }

    private fun unwrapMagnet(wrapped: String): String {
        val encoded = Regex("[?&]m=([^&]+)").find(wrapped)?.groupValues?.get(1)
            ?: return wrapped // already a raw magnet, e.g. old cached data
        return java.net.URLDecoder.decode(encoded, "UTF-8")
    }
    // ===================================================================================

    private fun ApibayTorrent.toSearchResponse(): SearchResponse? {
        val sizeBytes = this.size.toLongOrNull() ?: return null
        if (sizeBytes > maxSizeBytes) return null
        if ((this.seeders.toIntOrNull() ?: 0) < 1) return null

        val quality = detectQuality(this.name)
        val magnet = buildMagnet(this.info_hash, this.name)
        val wrapped = wrapMagnet(magnet) // CHANGED: pass the disguised url, not the raw magnet

        return newMovieSearchResponse(
            "${this.name} [$quality]",
            wrapped,
            TvType.Movie
        ) {
            this.posterUrl = null
        }
    }

    override var mainPage = mainPageOf(
        "200" to "Trending Movies",
        "205" to "Trending TV Shows",
        "207" to "Trending Anime"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val catCode = request.data
        val res = app.get("$mainUrl/precompiled/data_top100_$catCode.json")
        val torrents = parseJson<List<ApibayTorrent>>(res.text)

        val items = torrents.mapNotNull { it.toSearchResponse() }

        return newHomePageResponse(
            request.name,
            items,
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val res = app.get("$mainUrl/q.php?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
        val torrents = parseJson<List<ApibayTorrent>>(res.text)

        return torrents
            .sortedByDescending { it.seeders.toIntOrNull() ?: 0 }
            .mapNotNull { it.toSearchResponse() }
            .take(50)
    }

    override suspend fun load(url: String): LoadResponse {
        // CHANGED: url here is the *wrapped* url now, unwrap first to read dn= properly
        val magnet = unwrapMagnet(url)
        val displayName = Regex("dn=([^&]+)").find(magnet)
            ?.groupValues?.get(1)
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            ?: "Unknown Torrent"

        return newMovieLoadResponse(
            displayName,
            url,
            TvType.Movie,
            url // still the wrapped url, unwrapped later in loadLinks
        ) {
            this.posterUrl = null
            this.plot = "Streamed via magnet link through apibay.org"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val magnet = unwrapMagnet(data) // CHANGED: unwrap back to the real magnet: uri

        callback(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = magnet // real magnet, unmangled, ready to be type-inferred as MAGNET
            ) {
                this.quality = com.lagradost.cloudstream3.utils.Qualities.Unknown.value
            }
        )
        return true
    }
}