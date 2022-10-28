package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common

import com.sheryv.tools.websitescraper.GlobalState
import com.sheryv.tools.websitescraper.ProcessingStates
import com.sheryv.tools.websitescraper.browser.BrowserDef
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.websitescraper.config.impl.streamingwebsite.VideoServerConfig
import com.sheryv.tools.websitescraper.process.base.ScraperDefinition
import com.sheryv.tools.websitescraper.process.base.SeleniumScraper
import com.sheryv.tools.websitescraper.process.base.model.SeleniumDriver
import com.sheryv.tools.websitescraper.process.base.model.SimpleStep
import com.sheryv.tools.websitescraper.process.base.model.Step
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.*
import com.sheryv.tools.websitescraper.utils.Utils
import com.sheryv.tools.websitescraper.utils.lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openqa.selenium.By
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

abstract class StreamingWebsiteBase(
  configuration: Configuration,
  browser: BrowserDef,
  def: ScraperDefinition<SeleniumDriver, StreamingWebsiteSettings>,
  driver: SeleniumDriver
) : SeleniumScraper<StreamingWebsiteSettings>(configuration, browser, def, driver) {
  lateinit var series: Series
  
  override fun getSteps(): List<Step<out Any, out Any>> {
    return listOf(
      SimpleStep("main", ::start),
    )
  }
  
  protected suspend fun start() {
    series = (if (Files.exists(Path.of(settings.outputPath))) {
      try {
        val current = Utils.jsonMapper.readValue(File(settings.outputPath), Series::class.java)
        if (current.season == settings.seasonNumber && settings.seriesName.equals(current.title, true)) {
          current.copy(title = settings.seriesName, seriesUrl = getSeriesLink())
        } else {
          null
        }
      } catch (e: Exception) {
        lg().error("Error while trying to deserialize current series", e)
        null
      }
    } else null) ?: Series(settings.seriesName, settings.seasonNumber, getMainLang(), getSeriesLink())
    
    
    driver.get(series.seriesUrl)
    driver.waitForVisibility(By.tagName("body"))
    title = driver.title
    
    val items: List<VideoData> = findEpisodeItems()
    var i = settings.searchStartIndex
    while (i <= items.size && i <= settings.searchStopIndex) {
      val item: VideoData = items[i - 1]
//      if (options.getRequiredIndexes() != null) {
//        if (!options.getRequiredIndexes().contains(item.getNum())) {
//          Gripper.log.info("Skipped " + item.toString())
//          i++
//          continue
//        }
//      }
      var downloadUrl: String? = null
      val err = mutableListOf<ErrorEntry>()
      try {
        goToEpisodePage(item)
        val allServers: List<VideoServer> = loadItemDataFromSummaryPageAndGetServers(item)
        if (allServers.isEmpty()) throw IllegalArgumentException(
          String.format(
            "No hosting found for: E%02d %s | %s%n",
            item.number,
            item.title,
            item.episodePageUrl
          )
        )
        val priorities: List<VideoServerConfig> = getPriorities(i)
        for (priority in priorities) {
          val servers = allServers
            .filter { it.serverName.lowercase() == priority.searchName.lowercase() }
            .take(settings.triesBeforeStreamingProviderChange)
          
          for (server in servers) {
            try {
              item.server = server
              goToExternalServerVideoPage(item)
              downloadUrl = findLoadedVideoDownloadUrl(item)
              if (!downloadUrl.isNullOrEmpty()) {
                break
              }
            } catch (e: Exception) {
              lg().error("Error while searching download url at ${server.serverName} [${server.index}] | ${server.videoPageExternalUrl}", e)
            }
          }
          if (!downloadUrl.isNullOrEmpty()) {
            break
          }
        }
      } catch (e: Exception) {
        lg().error("Error while searching download url", e)
      }
      if (downloadUrl.isNullOrEmpty()) {
        err.add(ErrorEntry(2, "Download url not found"))
      }
      val ep = Episode(
        item.title,
        item.number,
        downloadUrl.orEmpty(),
        item.episodePageUrl,
        item.server.type,
        item.server.format,
        err
      )
      val episodes = series.episodes.toMutableList()
      episodes.removeIf { it.number == ep.number }
      episodes.add(ep)
      episodes.sortBy { it.number }
      series = series.copy(episodes = episodes)
      Utils.jsonMapper.writeValue(File(settings.outputPath), series)
      lg().info("\n$ep\n")
      i++
    }
  }
  
  
  protected abstract suspend fun getMainLang(): String
  
  protected abstract suspend fun findEpisodeItems(serverIndex: String? = null): List<VideoData>
  
  protected abstract suspend fun loadItemDataFromSummaryPageAndGetServers(data: VideoData): List<VideoServer>
  
  protected abstract suspend fun findLoadedVideoDownloadUrl(data: VideoData): String?
  
  protected open suspend fun getSeriesLink() = getFullUrl(settings.seriesUrl)
  
  protected open suspend fun goToExternalServerVideoPage(data: VideoData) {
    driver.navigate().to(data.server.videoPageExternalUrl)
  }
  
  protected open suspend fun goToEpisodePage(data: VideoData) {
    lg().info("Opening ${data.episodePageUrl}")
    driver.navigate().to(getFullUrl(data.episodePageUrl))
  }
  
  
  protected open suspend fun initializeHostingAndGetUrl(server: VideoServer): String? {
    delay(500)
    
    if (support.waitForAttribute(By.cssSelector("video"), "src") == null) {
      try {
        val captacha = support.wait(By.cssSelector(".hcaptcha"))
        if (captacha != null) {
          GlobalState.processingState.set(ProcessingStates.PAUSED)
          runBlocking(Dispatchers.Main) {
            GlobalState.view.showMessageDialog("Captcha detected! Solve it and resume process.")
          }
          do {
            delay(500)
          } while (GlobalState.processingState.value == ProcessingStates.PAUSED)
        }
      } catch (e: Exception) {
        lg().error("At searching for captcha: {}", e.message)
      } finally {
        support.wait(By.cssSelector("iframe"), 3)?.also {
          driver.switchTo().frame(it)
          lg().debug("Switched to inner frame in {} [{}}]", server.serverName, server.index)
        }
      }
    }
    
    val serverHandler = streamingServersHandlers()[CommonVideoServers.forName(server.serverName)]!!
    val found = serverHandler.findVideoSrcUrl()
    if (found == null) {
      runBlocking(Dispatchers.Main) {
        GlobalState.view.showMessageDialog("Cannot find URL. Video not started?")
      }
    }
    
    return serverHandler.findVideoSrcUrl()
    
  }
  
  /*   private void runForErrOnly() throws Exception {
        beginStopLoading();
        driver.navigate().to(provider.getSeriesLink());
        Series series = Transformer.loadSeries(FileUtils.readFileInMemory(configuration.getDefaultFilePathWithEpisodesList()));
        List<Episode> episodes = series.getEpisodes();
        for (int i = 0; i < episodes.size(); i++) {
            Episode episode = episodes.get(i);
            if (episode.getError() != 0) {
                log.info("Loading for " + episode);
                Item item = new Item(episode.getPage(), episode.getName(), episode.getN());
                provider.goToEpisodePage(item);
                provider.openVideoPage(item, );

                String downloadLink = provider.findLoadedVideoDownloadUrl(item);
                int err = 0;
                if (downloadLink == null) {
                    err = 2;
                }
                Episode ep = new Episode(item.getLink(), item.getName(), item.getNum(), downloadLink, err, item.getType());
                series.getEpisodes().set(i, ep);
            }
        }
        String json = SerialisationUtils.toJsonPretty(series);
        FileUtils.saveFile(json, Paths.get(configuration.getDefaultFilePathWithEpisodesList()));
    }*/
  
  
  protected open fun getPriorities(indexOffset: Int): List<VideoServerConfig> {
    var offset = indexOffset
    val base: MutableList<VideoServerConfig> = settings.videoServerConfigs.filter { obj -> obj.enabled }.toMutableList()
    
    if (settings.numOfTopStreamingProvidersUsedSimultaneously <= 0) {
      return base
    }
    val top: List<VideoServerConfig> = base.take(settings.numOfTopStreamingProvidersUsedSimultaneously)
    base.removeAll(top)
    offset %= top.size
    val deque = ArrayDeque(top)
    for (i in 0 until offset) {
      val poll = deque.poll()
      deque.add(poll)
    }
    val result = deque.toMutableList()
    result.addAll(base)
    return result
  }
  
  
  open fun getFullUrl(url: String): String {
    if (url.startsWith("http:")
      || url.startsWith("https:")
      || url.startsWith("www")
    ) return url
    return if (!url.startsWith("/")) settings.websiteUrl + "/" + url else settings.websiteUrl + url
  }
  
  protected fun streamingServersHandlers(): Map<VideoServerDefinition, VideoServerHandler> {
    return mapOf(
      CommonVideoServers.HIGHLOAD to object : VideoServerHandler(CommonVideoServers.HIGHLOAD, driver, support) {},
      CommonVideoServers.VIDOZA to object : VideoServerHandler(CommonVideoServers.VIDOZA, driver, support) {},
      CommonVideoServers.UPSTREAM to object : VideoServerHandler(CommonVideoServers.UPSTREAM, driver, support) {},
    )
  }
}
