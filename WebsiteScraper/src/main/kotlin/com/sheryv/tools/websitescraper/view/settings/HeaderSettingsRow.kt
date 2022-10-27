package com.sheryv.tools.websitescraper.view.settings

import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.layout.Region
import javafx.scene.layout.VBox

class HeaderSettingsRow(name: String) : SettingsViewRow<Nothing>(name, false) {
  override fun buildPart(): Region {
    val label = Label(name)
    return VBox(label, Separator())
  }
}
