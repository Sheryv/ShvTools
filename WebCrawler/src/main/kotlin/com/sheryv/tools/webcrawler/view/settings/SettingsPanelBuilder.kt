package com.sheryv.tools.webcrawler.view.settings

import com.sheryv.tools.webcrawler.config.SettingsBase
import javafx.scene.layout.Pane

class SettingsPanelBuilder(val settings: SettingsBase) {
  
  fun build(): Pair<List<Pane>, SettingsPanelReader> {
    val pair = settings.buildSettingsPanelDef()
    return pair.first.map {
      val pane = it.build()
      pane.maxWidth = Double.MAX_VALUE
      pane.prefWidth = Double.MAX_VALUE
      pane
    } to pair.second
  }
}

typealias SettingsPanelReader = () -> SettingsBase
