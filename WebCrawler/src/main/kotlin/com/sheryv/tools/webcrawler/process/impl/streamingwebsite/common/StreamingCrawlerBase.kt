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
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.emitEvent
import com.sheryv.util.event.AsyncEvent
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
  
  override fun handleEvent(e: AsyncEvent) {
    when(e){
      is FetchedDataExternalChangeEvent -> onFetchedDataExternalChangeEvent(e)
    }
  }
  
  override fun createDefaultSettings(): StreamingWebsiteSettings {
    return StreamingWebsiteSettings(
      id(),
      downloadDir = SystemSupport.get.userDownloadDir.resolve(SystemSupport.get.removeForbiddenFileChars(attributes.id)).toString()
    )
  }
  
  protected fun onFetchedDataExternalChangeEvent(e: FetchedDataExternalChangeEvent) {
    val path = findSettings(Configuration.get()).outputPath
    if (!Files.exists(path)) {
      emitEvent(FetchedDataStatusChangedEvent(""))
      return
    }
    val series = SerialisationUtils.jsonMapper.readValue(path.toFile(), Series::class.java)
    emitEvent(FetchedDataStatusChangedEvent(series.formattedString()))
  }
}
