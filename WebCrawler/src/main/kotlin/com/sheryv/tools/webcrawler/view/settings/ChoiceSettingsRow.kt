package com.sheryv.tools.webcrawler.view.settings

import javafx.beans.property.Property
import javafx.scene.control.ComboBox
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region

class ChoiceSettingsRow(name: String, val value: String, val choices: Collection<String>, listener: Property<String>? = null) :
  SettingsViewRow<String>(name, listener = listener) {
  private lateinit var comboBox: ComboBox<String>
  
  override fun buildPart(): Region {
    comboBox = ComboBox<String>()
    comboBox.items.addAll(choices)
    comboBox.maxWidth = Double.MAX_VALUE
    if (comboBox.items.contains(value)) {
      comboBox.selectionModel.select(value)
    } else {
      comboBox.selectionModel.selectFirst()
    }
    listener?.bind(comboBox.selectionModel.selectedItemProperty())
    val hBox = HBox(comboBox, copyButton { comboBox.value })
    hBox.spacing = 10.0
    HBox.setHgrow(comboBox, Priority.ALWAYS)
    return hBox
  }
  
  override fun readValue(): String {
    return comboBox.value
  }
}
