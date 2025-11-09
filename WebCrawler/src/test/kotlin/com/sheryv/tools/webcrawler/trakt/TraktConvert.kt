package com.sheryv.tools.webcrawler.trakt

import com.fasterxml.jackson.annotation.JsonProperty
import com.sheryv.tools.webcrawler.RunFilmwebScraper
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.service.videosearch.TmdbApi
import com.sheryv.util.SerialisationUtils
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneOffset


class TraktConvert {
  @Test
  fun fillIds() {
    val items = SerialisationUtils.fromJson<List<ProcessingResult>>(Files.readString(Path.of("processed.json")))
    val key = (Configuration.get().settings.first { it.crawlerId == "filman" } as StreamingWebsiteSettings).tmdbKey
    val api = TmdbApi(key)
    val out = mutableListOf<ProcessingResult>()
    out.addAll(items)
    try {
      for ((i, item) in out.withIndex()) {
        
        try {
          if (item.type == "serial") {
            if (item.imdbId != null &&
              ((item.watchedSingleEpisodes?.all { it.imdbId != null } == true)
                  || (item.episodesWatched?.current ?: 0) == 0)
            ) {
              continue
            }
            val result = api.searchTv(item.title, item.year).firstOrNull() ?: throw RuntimeException("not found for ${item.title}")
            println("Found ${result.name} [${result.popularity}] ${result.overview.take(100)}")
            val tv = api.getTvSeries(result.id)
            val seasonSizes = tv.seasons.sortedBy { it.season }.filter { it.season > 0 }.map { it.episodesCount }
            out[i] = item.copy(tmdbId = result.id.toInt(), imdbId = tv.externalIds["imdb_id"].toString(), seasonSizes = seasonSizes)
            out[i].watchedSingleEpisodes = findEpisodeIds(out[i])
          } else {
            if (item.imdbId != null) {
              continue
            }
            val result = api.searchMovie(item.title, item.year).firstOrNull() ?: throw RuntimeException("not found for ${item.title}")
            println("Found ${result.name} [${result.popularity}] ${result.overview.take(100)}")
            val imdbId = api.getImdbIdForMovie(result.id)
            out[i] = item.copy(tmdbId = result.id.toInt(), imdbId = imdbId)
          }
        } catch (e: Exception) {
          println("Error loading item ${item.title}")
          e.printStackTrace()
          out[i] = item
        }
      }
    } finally {
      Files.writeString(Path.of("processed.json"), SerialisationUtils.toJson(out))
    }
  }
  
  fun findEpisodeIds(item: ProcessingResult): List<Episode> {
    val key = (Configuration.get().settings.first { it.crawlerId == "filman" } as StreamingWebsiteSettings).tmdbKey
    val api = TmdbApi(key)
    return item.getEpisodesToSearch().map { toSearch ->
      if (toSearch.imdbId != null) {
        return@map toSearch
      }
      val imdbId = api.getEpisodeId(item.tmdbId!!.toLong(), toSearch.season, toSearch.episodeNum)
      toSearch.copy(imdbId = imdbId)
    }
  }
  
  @Test
  fun generateImport() {
    val items = SerialisationUtils.fromJson<List<ProcessingResult>>(Files.readString(Path.of("processed.json")))
    val import = mutableListOf<TraktImport>()
    val csv = StringBuilder("imdb_id,watched_at,watchlisted_at,rating,rated_at").appendLine()
    for (item in items) {
      if (item.imdbId == null || item.imdbId == "null"){
        continue
      }
      if (!item.isFavorite()) {
        import.add(
          TraktImportRate(
            item.imdbId,
            item.watchDate()?.atStartOfDay(ZoneOffset.UTC)?.toString(),
            item.watchlistedDate()?.atStartOfDay(ZoneOffset.UTC)?.toString(),
            item.rate?.current,
            if (item.rate != null) item.rateDate?.atStartOfDay(ZoneOffset.UTC)?.toString() else null
          )
        )
        csv.appendLine("${item.imdbId},${item.watchDate()?.atStartOfDay(ZoneOffset.UTC)?.toString().orEmpty()}," +
            "${ item.watchlistedDate()?.atStartOfDay(ZoneOffset.UTC)?.toString().orEmpty()},${item.rate?.current ?: ""}," +
            "${if (item.rate != null) item.rateDate?.atStartOfDay(ZoneOffset.UTC)?.toString() else ""}")
        item.watchedSingleEpisodes?.filter { it.imdbId != null }?.forEach { episode ->
          val watchedAt =
            item.rateDate?.atStartOfDay(ZoneOffset.UTC)?.toString() ?: item.defaultWatchDate().atStartOfDay(ZoneOffset.UTC)?.toString()
          import.add(
            TraktImportRate(
              episode.imdbId!!,
              watchedAt,
              null,
              null,
              null
            )
          )
          csv.appendLine("${episode.imdbId},${watchedAt ?: ""},,,")
        }
      }
    }
    
    
    Files.writeString(Path.of("trakt_to_import.json"), SerialisationUtils.toJson(import))
    Files.writeString(Path.of("trakt_to_import.csv"), csv.toString())
    
    println()
    
  }
  
  @Test
  fun updateTraktSeasonWatchStatus() {
    val props = System.getProperties()
    props.load(Files.newBufferedReader(Path.of("secrets.properties")))
    
    val trakt = TraktV2(props.getProperty("TRAKT_CLIENT")!!, props.getProperty("TRAKT_SECRET")!!, "google.com")
    auth(trakt)
    
    val api = trakt.seasons()
    val sync = trakt.sync()
    val items = SerialisationUtils.fromJson<List<ProcessingResult>>(Files.readString(Path.of("processed.json")))
    for (item in items) {
      if (item.type == "serial" && item.getWatchedSeasons()?.let { !it.isEmpty() } == true) {
        for (season in item.getWatchedSeasons()!!) {
          val episodes = api.season(item.imdbId, season, Extended.EPISODES).execute().body()
          val watchDate =
            (item.rateDate?.atStartOfDay(ZoneOffset.UTC) ?: item.defaultWatchDate().atStartOfDay(ZoneOffset.UTC)).toOffsetDateTime()
              .toEpochSecond()
          val mapped = episodes!!.map {
            SyncEpisode().watchedAt(
              OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(watchDate),
                org.threeten.bp.ZoneOffset.UTC
              )
            ).id(EpisodeIds.imdb(it.ids.imdb))
          }
          //        scrobble.startWatching()
          try {
            val respo = sync.addItemsToWatchedHistory(SyncItems().episodes(mapped)).execute()
            val result = respo.body()!!
            val resultString =
              "existing: ${mapStats(result.existing)}\n added: ${mapStats(result.added)}\n deleted: ${mapStats(result.deleted)}\n not_found: ${
                mapStats(result.not_found)
              }"
            println("Added history to '${item.title}' for season $season | records ${mapped.size} | result: \n$resultString")
          } catch (e: Exception) {
            println("Cannot add history to '${item.title}' for season $season | records ${mapped.size} | ${e.message}")
            e.printStackTrace()
          }
        }
      }
    }
  }
  
  private fun auth(trakt: TraktV2) {
    val token = System.getProperty("TRAKT_TOKEN").takeIf { it.isNotBlank() }
    
    if (token != null) {
      trakt.accessToken(token)
    } else {
      val deviceCodeResponse = trakt.generateDeviceCode()
      val deviceCode = deviceCodeResponse.takeIf { it.isSuccessful }?.body()
      if (deviceCode != null) {
        
        println("Open ${deviceCode.verification_url}")
        println("Code ${deviceCode.user_code}")
        
        val interval = deviceCode.interval!! * 1000L
        val expiryTime = System.currentTimeMillis() + (deviceCode.expires_in!! * 1000L)
        
        var accessToken: AccessToken? = null
        
        while (System.currentTimeMillis() < expiryTime) {
          try {
            sleep(interval)
            val tokenResponse = trakt.exchangeDeviceCodeForAccessToken(deviceCode.device_code!!)
            
            if (tokenResponse.isSuccessful && tokenResponse.body() != null) {
              accessToken = tokenResponse.body()!!
              break
            } else {
              println("Access token request failed | ${tokenResponse.errorBody()?.string()}")
            }
          } catch (e: Exception) {
            e.printStackTrace()
            break
          }
        }
        
        if (accessToken != null) {
          println("Tokens: \n ${accessToken.access_token}\n ${accessToken.refresh_token}")
          trakt.accessToken(accessToken.access_token)
        } else {
          throw RuntimeException("Access token polling failed")
        }
      } else {
        throw RuntimeException("Device code cannot be obtained | ${deviceCodeResponse.errorBody()?.string()}")
      }
    }
  }
  
  
  private fun mapStats(stats: SyncStats?): String {
    if (stats == null) return ""
    return "movies=${stats.movies}, show=${stats.shows}, seasons=${stats.seasons}, episodes=${stats.episodes}"
  }
  
  private fun mapStats(stats: SyncErrors?): String {
    if (stats == null) return ""
    return "movies=${stats.movies?.map { it.ids?.imdb }}, show=${stats.shows?.map { it.ids?.imdb }}, seasons=${stats.seasons?.map { it.episodes?.map { it.ids?.imdb } }}, episodes=${stats.episodes?.map { it.ids?.imdb }}"
  }
  
  @Test
  fun episodesToSearchShouldReturn() {
    var res = ProcessingResult("", false, "a", null, "serial", 2020, RunFilmwebScraper.Ratio(3, 15), seasonSizes = listOf(10, 5))
    var ep = res.getEpisodesToSearch()
    assertEquals(3, ep.size)
    assertEquals(Episode(1, 1), ep[0])
    assertEquals(Episode(1, 2), ep[1])
    assertEquals(Episode(1, 3), ep[2])
    
    res = ProcessingResult("", false, "a", null, "serial", 2020, RunFilmwebScraper.Ratio(12, 15), seasonSizes = listOf(10, 5))
    ep = res.getEpisodesToSearch()
    assertEquals(2, ep.size)
    assertEquals(Episode(2, 1), ep[0])
    assertEquals(Episode(2, 2), ep[1])
    
    res = ProcessingResult("", false, "a", null, "serial", 2020, RunFilmwebScraper.Ratio(10, 15), seasonSizes = listOf(10, 5))
    ep = res.getEpisodesToSearch()
    assertEquals(0, ep.size)
    
    res = ProcessingResult("", false, "a", null, "serial", 2020, RunFilmwebScraper.Ratio(15, 15), seasonSizes = listOf(10, 5))
    ep = res.getEpisodesToSearch()
    assertEquals(0, ep.size)
  }
  
  open class TraktImport(@JsonProperty("imdb_id") open val imdbId: String)
  
  data class TraktImportRate(
    @JsonProperty("imdb_id")
    override val imdbId: String,
    @JsonProperty("watched_at")
    val watchedAt: String? = null,
    @JsonProperty("watchlisted_at")
    val watchlistedAt: String?,
    val rating: Int?,
    @JsonProperty("rated_at")
    val ratedAt: String?,
  ) : TraktImport(imdbId)
}
