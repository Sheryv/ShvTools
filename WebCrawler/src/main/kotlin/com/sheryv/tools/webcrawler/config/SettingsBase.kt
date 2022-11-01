package com.sheryv.tools.webcrawler.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sheryv.tools.webcrawler.process.base.ScraperDef
import com.sheryv.tools.webcrawler.process.base.model.Format
import com.sheryv.tools.webcrawler.view.settings.SettingsPanelReader
import com.sheryv.tools.webcrawler.view.settings.SettingsViewRow
import java.net.URL

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class SettingsBase(
  val name: String,
  val websiteUrl: String,
  val outputPath: String,
  val outputFormat: Format,
) {
  
  abstract fun copy(
    name: String = this.name,
    websiteUrl: String = this.websiteUrl,
    outputPath: String = this.outputPath,
    outputFormat: Format = this.outputFormat
  ): SettingsBase
  
  override fun toString(): String {
    val url = URL(websiteUrl)
    return name + " - " + url.host
  }
  
  abstract fun buildSettingsPanelDef(): Pair<List<SettingsViewRow<*>>, SettingsPanelReader>
  
  open fun validate(def: ScraperDef) {
    URL(websiteUrl)
  }
}
