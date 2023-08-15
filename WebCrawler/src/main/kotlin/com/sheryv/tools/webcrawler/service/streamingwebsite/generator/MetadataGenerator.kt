package com.sheryv.tools.webcrawler.service.streamingwebsite.generator

import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.utils.lg
import com.sheryv.util.logging.log
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.format.DateTimeFormatter
import kotlin.io.path.exists

class MetadataGenerator(private val settings: StreamingWebsiteSettings) {
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  
  fun generateNfoMetadata(series: Series) {
    val seriesNfo = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <tvshow>
          <title>${series.title}</title>
          <originaltitle/>
          <showtitle>${series.title}</showtitle>
          <sorttitle/>
          <year>${series.releaseDate?.year ?: ""}</year>
          <ratings/>
          <userrating>0.0</userrating>
          <outline/>
          <plot/>
          <tagline/>
          <runtime>0</runtime>
          <mpaa/>
          <certification/>
          <imdbid>${series.imdbId.orEmpty()}</imdbid>
          <tmdbid>${series.tvdbId.orEmpty()}</tmdbid>
          <premiered/>
          <status>Unknown</status>
          <watched>false</watched>
          <playcount/>
          <studio/>
          <country/>
          <trailer/>
          <dateadded>${series.episodes.firstOrNull()?.created?.format(formatter).orEmpty()}</dateadded>
        </tvshow>
        """.trimIndent()
    val seriesFile = Path.of(settings.downloadDir).resolve(series.generateDirectoryPathForSeason().parent).resolve("tvshow.nfo")
    Files.createDirectories(seriesFile.parent)
    if (!seriesFile.exists()) {
      Files.writeString(seriesFile, seriesNfo, StandardOpenOption.CREATE)
    }
    
    for (episode in series.episodes) {
      val episodeNfo = """
              <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
              <episodedetails>
                <title>${episode.title}</title>
                <originaltitle/>
                <showtitle>${series.title}</showtitle>
                <season>${series.season}</season>
                <episode>${episode.number}</episode>
                <displayseason>-1</displayseason>
                <displayepisode>-1</displayepisode>
                <id/>
                <ratings/>
                <userrating>0.0</userrating>
                <plot/>
                <mpaa/>
                <premiered/>
                <aired/>
                <watched>false</watched>
                <playcount>0</playcount>
                <trailer/>
                <dateadded>${episode.created.format(formatter)}</dateadded>
                <epbookmark/>
                <code/>
              </episodedetails>
            """.trimIndent()
      
      val file = Path.of(settings.downloadDir)
        .resolve(series.generateDirectoryPathForSeason())
        .resolve(episode.generateFileName(series, settings, "nfo"))
      Files.createDirectories(file.parent)
      if (!file.exists()) {
        Files.writeString(file, episodeNfo, StandardOpenOption.CREATE)
        log.debug("Saved .nfo to $file")
      } else {
        log.debug("Skipped .nfo at $file")
      }
    }
  }
}
