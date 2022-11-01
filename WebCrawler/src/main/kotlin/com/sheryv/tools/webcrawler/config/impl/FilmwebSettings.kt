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
