package com.sheryv.tools.webcrawler.view.settings

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.util.fx.core.view.ViewFactory
import javafx.scene.layout.Pane

class SettingsPanelBuilder(val settings: SettingsBase, val viewFactory: ViewFactory) {
  
  fun build(): Pair<List<Pane>, SettingsPanelReader> {
    val pair = settings.buildSettingsPanelDef(viewFactory)
    return pair.first.map {
      val pane = it.build()
      pane.maxWidth = Double.MAX_VALUE
      pane.prefWidth = Double.MAX_VALUE
      pane
    } to pair.second
  }
}

typealias SettingsPanelReader = () -> SettingsBase
