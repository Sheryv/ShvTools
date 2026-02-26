package com.sheryv.tools.webcrawler.service.streamingwebsite.idm

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Episode
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.util.logging.log
import kotlinx.coroutines.delay
import java.nio.file.Path

class IDMService(val configuration: Configuration) {
  
  suspend fun addToIDM(series: Series): Pair<Int, Int> {
    val settings = GlobalState.currentCrawler.findSettings(configuration) as StreamingWebsiteSettings
    var c = 0
    val episodes = series.episodes.filter { settings.searchStartIndex <= it.number && it.number <= settings.searchStopIndex }
    for (episode in episodes) {
      if (episode.errors.isEmpty() && episode.downloadUrl?.isDirect == true) {
        addSingle(series, episode, settings)
        c++
        delay(1000)
      } else {
        log.info("No download link for $episode")
      }
    }
    return c to episodes.size
  }
  
  fun addSingle(series: Series, episode: Episode, settings: StreamingWebsiteSettings) {
    val idmExePath: String = settings.idmExePath!!
    require(idmExePath.isNotBlank()) { "Path to IDM exe cannot be empty" }
    require(episode.downloadUrl?.isDirect == true) { "Only direct download urls are supported by IDM" }
    val ex = String.format(
      "\"%s\" /n /f \"%s\" /p \"%s\"" +
          " /a /d %s",
      idmExePath,
      episode.generateFileName(series, settings),
      Path.of(settings.downloadDir).resolve(series.generateDirectoryPathForSeason()),
      episode.downloadUrl!!.url
    )
    
    try {
      log.info("\n> IDM {} {}", episode.number, ex)
      Runtime.getRuntime().exec(ex)
    } catch (e: Exception) {
      log.error("Error while adding to IDM", e)
    }
  }
  
  fun openWebsiteWithHlsStreams() {
    //language=html
    val html = """
          <!DOCTYPE html>
          <html>
          <head>
          <meta charset=utf-8 />
          <title>WebCrawler Videos</title>
            
          
            <link href="https://unpkg.com/video.js/dist/video-js.css" rel="stylesheet">
            <script src="https://unpkg.com/video.js/dist/video.js"></script>
            <script src="https://unpkg.com/videojs-contrib-hls/dist/videojs-contrib-hls.js"></script>
            
          </head>
          <body>
            <video id="my_video_1" class="video-js vjs-fluid vjs-default-skin" controls preload="auto"
            data-setup='{}'>
              <source src="https://cdn3.wowza.com/1/ejBGVnFIOW9yNlZv/cithRSsv/hls/live/playlist.m3u8" type="application/x-mpegURL">
            </video>
            
          <script>
          var player = videojs('my_video_1');
          player.play();
          </script>
          
          </body>
          </html>
    """.trimIndent()
    
  }
}
