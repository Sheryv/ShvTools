package com.sheryv.tools.webcrawler.config.impl

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.view.settings.SettingsPanelReader
import com.sheryv.tools.webcrawler.view.settings.SettingsViewRow
import com.sheryv.tools.webcrawler.view.settings.TextInputSettingsRow
import java.nio.file.Path

class FilmwebSettings(
  crawlerId: String,
  outputPath: Path? = null,
) : SettingsBase(crawlerId, outputPath) {
  
  override fun copy(
    crawlerId: String,
    outputPath: Path,
  ): SettingsBase {
    return FilmwebSettings(crawlerId, outputPath)
  }
  
  override fun buildSettingsPanelDef(): Pair<List<SettingsViewRow<*>>, SettingsPanelReader> {
    val savePathRow = TextInputSettingsRow("Save path", outputPath.toString())
    return Pair(
      listOf(
        savePathRow,
      )
    ) {
      FilmwebSettings(crawlerId, Path.of(savePathRow.readValue()))
    }
  }
  
}
