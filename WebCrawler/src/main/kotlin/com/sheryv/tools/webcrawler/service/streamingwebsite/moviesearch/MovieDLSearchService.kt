package com.sheryv.tools.webcrawler.service.streamingwebsite.moviesearch

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.Runner
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.EpisodeAudioTypes
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.FileFormats
import com.sheryv.tools.webcrawler.service.streamingwebsite.downloader.Downloader
import com.sheryv.tools.webcrawler.service.streamingwebsite.idm.IDMService
import com.sheryv.tools.webcrawler.service.streamingwebsite.metadata.NfoMovieParser
import com.sheryv.util.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.asSequence

class MovieDLSearchService(
  private val settings: StreamingWebsiteSettings,
  private val configuration: Configuration,
  private val crawlerDef: CrawlerDef,
  private val params: ProcessParams
) {
  
  suspend fun searchUrlsForMoviesIn(dir: Path) {
    val browser = configuration.browserSettings.currentBrowser()
    val runner = Runner(configuration, browser, crawlerDef, params)
    
    try {
      val extensions = FileFormats.entries.map { it.extension }
      val parser = NfoMovieParser()
      val movies = Files.walk(dir, 6)
        .filter { Files.isRegularFile(it) && it.name == "movie.nfo" }
        .asSequence()
        .mapNotNull { nfo ->
          Files.list(nfo.parent).filter { it.extension in extensions }.findFirst().orElse(null)?.let { path ->
            parser.parse(nfo)?.let { MovieFromFile(it.title, it.tmdbId, path, it.year) }
          }
        }
        .toList()
      
      runner.prepare(settings)
      val urls = runner.searchAllMovies(movies)
      val idm = IDMService(configuration)
      urls.forEach { (file, value) ->
        val (url, server) = value
        val suffix = when (server.type) {
          EpisodeAudioTypes.DUBBING -> "[DUB]"
          EpisodeAudioTypes.LECTOR -> "[VO]"
          EpisodeAudioTypes.SUBS_GENERATED, EpisodeAudioTypes.SUBS -> "[SUB]"
          else -> ""
        }
        
        val target = file.path.resolveSibling(file.path.nameWithoutExtension + suffix + ".ts")
        if (Files.exists(target)) {
          FileUtils.renameAsBackup(target)
        }
        if (url.isStreaming) {
          Downloader.add(url, target)
        } else {
          idm.addUrl(url.url, target, settings)
        }
      }
      println()
    } catch (e: Exception) {
      throw e
    }
  }
}
