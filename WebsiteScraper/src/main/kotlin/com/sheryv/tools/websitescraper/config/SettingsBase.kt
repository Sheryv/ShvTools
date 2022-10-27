package com.sheryv.tools.websitescraper.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sheryv.tools.websitescraper.process.base.ScraperDef
import com.sheryv.tools.websitescraper.process.base.model.Format
import com.sheryv.tools.websitescraper.view.settings.SettingsPanelReader
import com.sheryv.tools.websitescraper.view.settings.SettingsViewRow
import com.sheryv.tools.websitescraper.view.settings.TextInputSettingsRow
import java.net.URL

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class SettingsBase(
  val name: String,
  val websiteUrl: String,
  val outputPath: String,
  val outputFormat: Format,
) {
  
  abstract fun copy(): SettingsBase
  
  override fun toString(): String {
    val url = URL(websiteUrl)
    return name + " - " + url.host
  }
  
  abstract fun buildSettingsPanelDef(): Pair<List<SettingsViewRow<*>>, SettingsPanelReader>
  
  open fun validate(def: ScraperDef) {
    URL(websiteUrl)
  }
}
