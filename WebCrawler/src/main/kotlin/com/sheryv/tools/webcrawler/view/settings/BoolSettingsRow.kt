package com.sheryv.tools.webcrawler.view.settings

import javafx.scene.control.CheckBox
import javafx.scene.layout.Region

class BoolSettingsRow(name: String, val value: Boolean) : SettingsViewRow<Boolean>(name, false) {
  private lateinit var checkBox: CheckBox
  
  override fun buildPart(): Region {
    checkBox = CheckBox(name)
    checkBox.isSelected = value
    return checkBox
  }
  
  override fun readValue(): Boolean {
    return checkBox.isSelected
  }
}
