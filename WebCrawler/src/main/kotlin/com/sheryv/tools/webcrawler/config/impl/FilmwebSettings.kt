package com.sheryv.tools.webcrawler.config.impl

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.view.settings.SettingsPanelReader
import com.sheryv.tools.webcrawler.view.settings.SettingsViewRow
import com.sheryv.tools.webcrawler.view.settings.TextInputSettingsRow
import java.nio.file.Path

data class FilmwebSettings(
  override val crawlerId: String,
  override val outputPath: Path,
) : SettingsBase() {
  override fun copyAll(): SettingsBase = copy()
  
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
  
  @Suppress("RedundantOverride")
  override fun equals(other: Any?) = super.equals(other)
  
  @Suppress("RedundantOverride")
  override fun hashCode() = super.hashCode()
}
