package com.sheryv.tools.webcrawler.view.settings

import com.sheryv.tools.webcrawler.utils.DialogUtils
import com.sheryv.tools.webcrawler.utils.ViewUtils
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region


class TextInputSettingsRow(name: String, val text: String, val lines: Int = 1, val characterLimitPerLine: Int = 250) :
  SettingsViewRow<String>(name) {
  
  private lateinit var textField: TextInputControl
  
  override fun buildPart(): Region {
    val btn = Button()
    btn.graphic = Region()
    btn.styleClass.add("ic-more-vert")
    
    textField = if (lines > 1)
      TextArea(text).apply {
        prefRowCount = lines
        isWrapText = true
        minHeight = 12.0 * lines
      }
    else
      TextField(text)
    textField.textFormatter = TextFormatter<String> { if (it.controlNewText.length <= characterLimitPerLine * lines) it else null }
    
    val menu = ContextMenu(
      MenuItem("Copy").apply { setOnAction { ViewUtils.saveToClipboard(textField.text) } },
      MenuItem("Paste").apply { setOnAction { ViewUtils.loadFromClipboard()?.also { textField.text = it } } },
      MenuItem("Clear").apply { setOnAction { textField.clear() } },
      SeparatorMenuItem(),
      MenuItem("Choose directory path").apply {
        setOnAction {
          DialogUtils.openDirectoryDialog(textField.scene.window)?.let { textField.text = it.toAbsolutePath().toString() }
        }
      },
      MenuItem("Choose file path").apply {
        setOnAction {
          DialogUtils.saveFileDialog(textField.scene.window)?.let { textField.text = it.toAbsolutePath().toString() }
        }
      },
      SeparatorMenuItem(),
      MenuItem("Reset").also { it.setOnAction { textField.text = text } },
    )
    btn.tooltip = Tooltip("Click to show Menu")
    btn.setOnMouseClicked { menu.show((it.source as Control).parent, it.screenX, it.screenY) }
    val hBox = HBox(textField, btn)
    HBox.setHgrow(textField, Priority.ALWAYS)
    hBox.spacing = 10.0
    return hBox
  }
  
  override fun readValue(): String {
    return textField.text
  }
}
