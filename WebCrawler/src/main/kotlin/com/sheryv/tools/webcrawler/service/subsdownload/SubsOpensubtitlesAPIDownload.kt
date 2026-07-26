package com.sheryv.tools.webcrawler.service.subsdownload

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sheryv.tools.webcrawler.config.SecretsStore
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.FileFormats
import com.sheryv.tools.webcrawler.service.streamingwebsite.metadata.NfoMovieParser
import com.sheryv.tools.webcrawler.service.streamingwebsite.moviesearch.MovieFromFile
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.io.DataTransferProgressSummary
import com.sheryv.util.io.FileDownloader
import com.sheryv.util.io.HttpSupport
import com.sheryv.util.logging.log
import kotlinx.coroutines.delay
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpRequest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.asSequence
import kotlin.time.Duration.Companion.milliseconds

class SubsOpensubtitlesAPIDownload(private val secrets: SecretsStore) {
  
  private val userAgent = "shv-crawler"
  private val http = HttpSupport()
  private var token: String? = null
  
  
  suspend fun findAndDownloadForMoviesFromDir(inputDir: Path): Map<MovieFromFile, Path?> {
    val extensions = FileFormats.entries.map { it.extension }
    val parser = NfoMovieParser()
    
    val movies = Files.walk(inputDir, 2)
      .filter { Files.isRegularFile(it) && it.name == "movie.nfo" }
      .asSequence()
      .mapNotNull { nfo ->
        Files.list(nfo.parent).filter { it.extension in extensions }.findFirst().orElse(null)?.let { path ->
          parser.parse(nfo)?.let { MovieFromFile(it.title, it.tmdbId, path, it.year) }
        }
      }
      .toList()
    
    val result = mutableMapOf<MovieFromFile, Path?>()
    
    for (movie in movies) {
      result[movie] = null
      try {
        log.info("Searching subtitles for ${movie.title} [${movie.id}]")
        val output = movie.path.parent.resolve(movie.path.nameWithoutExtension + ".srt")
        val found = search(null, movie.id.toString())
        if (found.isNotEmpty()) {
          Files.writeString(movie.path.parent.resolve("subtitles_source.json"), SerialisationUtils.toJsonPretty(found))
        }
        
        val fileId = found.firstOrNull()?.files?.firstOrNull()?.fileId
        if (fileId == null) {
          log.error("Could not find any subtitles for ${movie.title} [${movie.id}]")
          delay(1010.milliseconds)
          continue
        }
        
        val progress = download(fileId, output)
        log.debug("Subtitles of size ${progress.totalSize.formatted} downloaded for ${movie.title} [${movie.id}]")
        result[movie] = output
      } catch (ex: Exception) {
        log.error("Error while searching for ${movie.title} [${movie.id}]", ex)
        delay(1010.milliseconds)
      }
    }
    return result
  }
  
  fun search(imdbId: String?, tmdbId: String?): List<SearchResultDetails> {
    require(imdbId != null || tmdbId != null) { "imdbId or tmdbId can not be null" }
    
    val params = mapOf(
      imdbId?.let { "imdb_id" to it } ?: ("tmdb_id" to tmdbId.toString()),
      "languages" to "pl",
    )
    return search(params)
  }
  
  fun searchMovie(query: String, year: Int): List<SearchResultDetails> {
    val params = mapOf(
      "query" to query,
      "year" to year.toString(),
      "languages" to "pl",
      "type" to "movie"
    ) //parent_imdb_id, query, year, season_number, episode_number, type=movie/episode
    return search(params)
  }
  
  fun search(params: Map<String, String>): List<SearchResultDetails> {
    var page = 0
    val results = mutableListOf<SearchResultDetails>()
    do {
      val query = renderQueryParams(params + ("page" to page.toString()))
      
      val response = http.send(
        HttpRequest.newBuilder()
          .GET()
          .uri(URI.create("https://api.opensubtitles.com/api/v1/subtitles?$query"))
          .header("User-Agent", userAgent)
          .header("Accept", "application/json")
          .header("Api-Key", secrets.openSubtitlesApiKey.require())
          .build()
      )
      
      log.debug("Found at $page: \n${SerialisationUtils.jsonMapper.readTree(response.body()).toPrettyString()}")
      
      val result = SerialisationUtils.fromJson<SearchResult>(response.body())
      results.addAll(result.data.map { it.attributes })
      page++
      
    } while (result.totalCount > result.perPage && result.page >= result.totalPages)
    
    return when {
      results.all { (it.ratings ?: 0) > 0 } -> results.sortedByDescending { it.ratings ?: 0 }
      results.all { it.newDownloadCount > 0 } -> results.sortedByDescending { it.newDownloadCount }
      else -> results.sortedByDescending { it.downloadCount }
    }
  }
  
  
  fun download(fileId: Int, output: Path): DataTransferProgressSummary {
    val params = mapOf(
      "file_id" to fileId.toString(),
//      "in_fps" to 25.0,
//      "out_fps" to 25.0,
//      "timeshift" to 0.0,
    )
    
    if (token == null) {
      login()
    }
    
    val response = http.send(
      HttpRequest.newBuilder()
        .uri(URI.create("https://api.opensubtitles.com/api/v1/download"))
        .POST(HttpRequest.BodyPublishers.ofString(SerialisationUtils.toJson(params)))
        .header("Content-Type", "application/json")
        .header("User-Agent", userAgent)
        .header("Accept", "application/json")
        .header("Authorization", "Bearer $token")
        .header("Api-Key", secrets.openSubtitlesApiKey.require())
        .build()
    )
    val result = SerialisationUtils.fromJson<DownloadResult>(response.body())
    log.info("Get download link from opensubtitles for file $fileId: ${result.link}\nMessage: ${result.message}\nRemaining downloads: ${result.remaining}\nResets at UTC: ${result.resetTimeUtc}")
    
    return FileDownloader(result.link, output).downloadSync()
  }
  
  
  private fun login(): LoginResult {
    
    val request = HttpRequest.newBuilder()
      .uri(URI.create("https://api.opensubtitles.com/api/v1/login"))
      .header("Content-Type", "application/json")
      .header("User-Agent", userAgent)
      .header("Accept", "application/json")
      .header("Api-Key", secrets.openSubtitlesApiKey.require())
      .method(
        "POST", HttpRequest.BodyPublishers.ofString(
          SerialisationUtils.toJson(
            mapOf("username" to secrets.openSubtitlesUsername.require(), "password" to secrets.openSubtitlesPassword.require())
          )
        )
      )
      .build()
    
    val response = http.send(request)
    if (response.statusCode() >= 220) {
      throw IllegalStateException("Error authenticating ${response.statusCode()} ${response.body()}")
    }
    val result = SerialisationUtils.fromJson<LoginResult>(response.body())
    if (result.user.allowedDownloads <= 0) {
      throw IllegalStateException("Error, user cannot download more subtitles today")
    }
    token = result.token
    return result
  }
  
  private fun renderQueryParams(params: Map<String, String>): String {
    return params.entries
      .sortedBy { it.key }
      .filter { it.key != "page" && it.value != "0" }
      .joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, Charsets.UTF_8)}" }
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class DownloadResult(
    val link: String,
    @param:JsonProperty("file_name")
    val fileName: String,
    val requests: Long,
    val remaining: Long,
    val message: String,
    @param:JsonProperty("reset_time")
    val resetTime: String,
    @param:JsonProperty("reset_time_utc")
    val resetTimeUtc: String,
  )
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class SearchResult(
    @param:JsonProperty("total_pages")
    val totalPages: Long,
    @param:JsonProperty("total_count")
    val totalCount: Long,
    @param:JsonProperty("per_page")
    val perPage: Long,
    val page: Long,
    val data: List<SearchResultRecord>,
  )
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class SearchResultRecord(
    val id: String,
    val type: String,
    val attributes: SearchResultDetails,
  )
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class SearchResultDetails(
    @param:JsonProperty("subtitle_id")
    val subtitleId: String,
    val language: String,
    @param:JsonProperty("download_count")
    val downloadCount: Long,
    @param:JsonProperty("new_download_count")
    val newDownloadCount: Long,
    @param:JsonProperty("hearing_impaired")
    val hearingImpaired: Boolean?,
    val hd: Boolean?,
    val fps: Long?,
    val votes: Long?,
    val ratings: Long?,
    @param:JsonProperty("from_trusted")
    val fromTrusted: Boolean?,
    @param:JsonProperty("foreign_parts_only")
    val foreignPartsOnly: Boolean?,
    @param:JsonProperty("upload_date")
    val uploadDate: String?,
    @param:JsonProperty("ai_translated")
    val aiTranslated: Boolean?,
    @param:JsonProperty("nb_cd")
    val nbCd: Long?,
    val slug: String?,
    @param:JsonProperty("machine_translated")
    val machineTranslated: Boolean?,
    val release: String?,
    val comments: String?,
    @param:JsonProperty("legacy_subtitle_id")
    val legacySubtitleId: Long?,
    @param:JsonProperty("legacy_uploader_id")
    val legacyUploaderId: Long?,
    val uploader: Uploader?,
    @param:JsonProperty("feature_details")
    val featureDetails: FeatureDetails?,
    val url: String?,
    @param:JsonProperty("related_links")
    val relatedLinks: List<RelatedLink>?,
    val files: List<File>,
  )
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class Uploader(
    @param:JsonProperty("uploader_id")
    val uploaderId: Long,
    val name: String?,
    val rank: String?,
  )
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class FeatureDetails(
    @param:JsonProperty("feature_id")
    val featureId: Long,
    @param:JsonProperty("feature_type")
    val featureType: String,
    val year: Long?,
    val title: String?,
    @param:JsonProperty("movie_name")
    val movieName: String?,
    @param:JsonProperty("imdb_id")
    val imdbId: Long?,
    @param:JsonProperty("tmdb_id")
    val tmdbId: Long?,
    @param:JsonProperty("season_number")
    val seasonNumber: Long?,
    @param:JsonProperty("episode_number")
    val episodeNumber: Long?,
    @param:JsonProperty("parent_imdb_id")
    val parentImdbId: Long?,
    @param:JsonProperty("parent_title")
    val parentTitle: String?,
    @param:JsonProperty("parent_tmdb_id")
    val parentTmdbId: Long?,
    @param:JsonProperty("parent_feature_id")
    val parentFeatureId: Long?,
  )
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class RelatedLink(
    val label: String?,
    val url: String?,
    @param:JsonProperty("img_url")
    val imgUrl: String?,
  )
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class File(
    @param:JsonProperty("file_id")
    val fileId: Int,
    @param:JsonProperty("cd_number")
    val cdNumber: Long?,
    @param:JsonProperty("file_name")
    val fileName: String?,
  )
  
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class LoginResult(
    val user: LoginResultUser,
    @param:JsonProperty("base_url")
    val baseUrl: String,
    val token: String,
    val status: Int,
  )
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class LoginResultUser(
    @param:JsonProperty("allowed_downloads")
    val allowedDownloads: Int,
    @param:JsonProperty("allowed_translations")
    val allowedTranslations: Int,
    val level: String,
//    @param:JsonProperty("user_id")
//    val user_id: Int,
//    @param:JsonProperty("ext_installed")
//    val ext_installed: Boolean,
//    val vip: Boolean,
  )
}
