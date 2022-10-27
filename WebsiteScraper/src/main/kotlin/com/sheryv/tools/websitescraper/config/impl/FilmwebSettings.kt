package com.sheryv.tools.websitescraper.config.impl

import com.sheryv.tools.websitescraper.config.SettingsBase
import com.sheryv.tools.websitescraper.process.base.model.Format
import com.sheryv.tools.websitescraper.view.settings.*

class FilmwebSettings(
  name: String,
  websiteUrl: String,
  outputPath: String,
  outputFormat: Format,
) : SettingsBase(name, websiteUrl, outputPath, outputFormat) {
  
  override fun copy(): SettingsBase {
    return FilmwebSettings(this.name, this.websiteUrl, this.outputPath, this.outputFormat)
  }
  
  override fun buildSettingsPanelDef(): Pair<List<SettingsViewRow<*>>, SettingsPanelReader> {
    val savePathRow = TextInputSettingsRow("Save path", outputPath)
    return Pair(
      listOf(
        savePathRow,
        TextInputSettingsRow("asdasd", ""),
        TextInputSettingsRow(
          "asdasd", "The driver must be in the same version as the browser. Download links are provided " +
              "below. The driver must be in the same version as the browser. Download links are provided below.", 6
        ),
        BoolSettingsRow("asdasd", false),
        ChoiceSettingsRow("asdasd", "vb", listOf("1", "bcvnvsdf", "vb")),
        NumberRangeSettingRow("asdasd", 20, 0, 100),
        HeaderSettingsRow("asdasd"),
        
        ValuePreviewSettingsRow(
          "Streaming providers order (Drag and drop to change)",
          "The driver must be in the same version as the browser. Download links are provided below."
        ),
      )
    ) {
      FilmwebSettings(name, websiteUrl, savePathRow.readValue(), outputFormat)
    }
  }
  
}
