package com.sheryv.tools.webcrawler.config.impl

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.model.Format
import com.sheryv.tools.webcrawler.view.settings.*

class FilmwebSettings(
  name: String,
  websiteUrl: String,
  outputPath: String,
  outputFormat: Format,
) : SettingsBase(name, websiteUrl, outputPath, outputFormat) {
  
  override fun copy(name: String,
                    websiteUrl: String,
                    outputPath: String,
                    outputFormat: Format): SettingsBase {
    return FilmwebSettings(name, websiteUrl, outputPath, outputFormat)
  }
  
  override fun buildSettingsPanelDef(): Pair<List<SettingsViewRow<*>>, SettingsPanelReader> {
    val savePathRow = TextInputSettingsRow("Save path", outputPath)
    return Pair(
      listOf(
        savePathRow,
      )
    ) {
      FilmwebSettings(name, websiteUrl, savePathRow.readValue(), outputFormat)
    }
  }
  
}
