package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import com.sheryv.tools.webcrawler.process.base.Groups
import com.sheryv.tools.webcrawler.process.base.model.SeleniumDriver
import com.sheryv.tools.webcrawler.service.SystemSupport

abstract class StreamingCrawlerBase(id: String,
                                    name: String,
                                    websiteUrl: String
) :  CrawlerDefinition<SeleniumDriver, StreamingWebsiteSettings>(CrawlerAttributes(id, name, websiteUrl, Groups.STREAMING_WEBSITE), StreamingWebsiteSettings::class.java) {
  
  override fun createDefaultSettings(): StreamingWebsiteSettings {
    return StreamingWebsiteSettings(
      id(),
      downloadDir = SystemSupport.get.userDownloadDir.resolve(SystemSupport.get.removeForbiddenFileChars(attributes.id)).toString()
    )
  }
}
