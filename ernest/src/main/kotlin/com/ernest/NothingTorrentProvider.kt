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

    // Category codes from apibay.org
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
        val imdb: String
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

    private fun ApibayTorrent.toSearchResponse(): SearchResponse? {
        val sizeBytes = this.size.toLongOrNull() ?: return null
        if (sizeBytes > maxSizeBytes) return null
        if ((this.seeders.toIntOrNull() ?: 0) < 1) return null

        val quality = detectQuality(this.name)
        val magnet = buildMagnet(this.info_hash, this.name)

        return newMovieSearchResponse(
            "${this.name} [$quality]",
            magnet,
            TvType.Movie
        ) {
            this.posterUrl = null
        }
    }

    // request.data carries the apibay category code (e.g. "200"), request.name is the display label
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
        // url here IS the magnet link (we pass it straight through from search/getMainPage)
        val displayName = Regex("dn=([^&]+)").find(url)
            ?.groupValues?.get(1)
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            ?: "Unknown Torrent"

        return newMovieLoadResponse(
            displayName,
            url,
            TvType.Movie,
            url
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
        // NOTE: verify this against current CloudStream torrent-engine docs before shipping.
        // As of recent CloudStream versions, magnet links are passed to the app's native
        // torrent engine by emitting an ExtractorLink whose url IS the magnet URI, with
        // type = ExtractorLinkType.MAGNET (or TORRENT depending on version).
        // If ExtractorLinkType.MAGNET doesn't exist in your SDK version, check
        // com.lagradost.cloudstream3.utils.ExtractorLinkType for the correct enum name.
        callback(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = data
            ) {
                this.quality = com.lagradost.cloudstream3.utils.Qualities.Unknown.value
            }
        )
        return true
    }
}