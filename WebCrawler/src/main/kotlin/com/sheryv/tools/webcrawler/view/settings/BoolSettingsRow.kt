package com.sheryv.tools.webcrawler.view.settings

import javafx.beans.property.ObjectProperty
import javafx.scene.control.CheckBox
import javafx.scene.layout.Region

class BoolSettingsRow(name: String, val value: Boolean, listener: ObjectProperty<Boolean>? = null) : SettingsViewRow<Boolean>(name, false, listener) {
  private lateinit var checkBox: CheckBox
  
  override fun buildPart(): Region {
    checkBox = CheckBox(name)
    checkBox.isSelected = value
    listener?.bind(checkBox.selectedProperty())
    return checkBox
  }
  
  override fun readValue(): Boolean {
    return checkBox.isSelected
  }
}
