package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.Groups
import com.sheryv.tools.webcrawler.process.base.event.FetchedDataExternalChangeEvent
import com.sheryv.tools.webcrawler.process.base.event.FetchedDataStatusChangedEvent
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.service.SystemSupport
import com.sheryv.tools.webcrawler.utils.Utils
import com.sheryv.tools.webcrawler.utils.postEvent
import org.greenrobot.eventbus.Subscribe
import java.nio.file.Files

abstract class StreamingCrawlerBase(
  id: String,
  name: String,
  websiteUrl: String
) : CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>(
  CrawlerAttributes(id, name, websiteUrl, Groups.STREAMING_WEBSITE),
  StreamingWebsiteSettings::class.java
) {
  
  override fun createDefaultSettings(): StreamingWebsiteSettings {
    return StreamingWebsiteSettings(
      id(),
      downloadDir = SystemSupport.get.userDownloadDir.resolve(SystemSupport.get.removeForbiddenFileChars(attributes.id)).toString()
    )
  }
  
  
  @Subscribe
  internal fun onFetchedDataExternalChangeEvent(e: FetchedDataExternalChangeEvent) {
    val path = findSettings(Configuration.get()).outputPath
    if (!Files.exists(path)) {
      postEvent(FetchedDataStatusChangedEvent(""))
      return
    }
    val series = Utils.jsonMapper.readValue(path.toFile(), Series::class.java)
    var output = """Series title: ${series.title}
      |Season: ${series.season}
      |
      |Episodes: ${series.episodes.size}
      |
    """.trimMargin()
    
    output += series.episodes.joinToString("\n") {
      "${it.number.toString().padStart(2)}. ${
        it.title.padEnd(37).take(37)
      } | ${it.downloadUrl.toString()}"
    }
    postEvent(FetchedDataStatusChangedEvent(output))
  }
}
