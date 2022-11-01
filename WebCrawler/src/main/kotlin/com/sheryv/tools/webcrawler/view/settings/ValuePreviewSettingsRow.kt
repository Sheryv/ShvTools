package com.sheryv.tools.webcrawler.view.settings

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox

class ValuePreviewSettingsRow(name: String, val value: String) : SettingsViewRow<Nothing>(name, false) {
  override fun buildPart(): Region {
    val field = TextField(value)
    field.isEditable = false
    field.maxWidth = Double.MAX_VALUE
    
    val vBox = VBox(Label(name), field)
    val hBox = HBox(vBox, copyButton { field.text })
    HBox.setHgrow(vBox, Priority.ALWAYS)
    hBox.spacing = 10.0
    hBox.alignment = Pos.CENTER
    return hBox
  }
}
