package com.sheryv.tools.filematcher.utils

import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import javafx.stage.Window
import java.lang.Double.MAX_VALUE
import java.nio.file.Path
import java.util.*


object DialogUtils {
  
  fun dialog(
      text: String,
      header: String? = null,
      type: Alert.AlertType = Alert.AlertType.WARNING,
      vararg buttons: ButtonType = arrayOf(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)
  ): Optional<ButtonType> {
    return Alert(type, text, *buttons).apply {
      if (header != null) {
        headerText = header
      }
      ViewUtils.appendStyleSheets(dialogPane.scene)
    }.showAndWait()
  }
  
  fun inputDialog(
      text: String,
      header: String? = null
  ): Optional<String> {
    val dialog = TextInputDialog()
    dialog.title = text
    dialog.headerText = header?.padEnd(60)
    ViewUtils.appendStyleSheets(dialog.dialogPane.scene)
    return dialog.showAndWait()
  }
  
  fun directoryDialog(
      owner: Window,
      text: String = "Choose directory",
      initialDirectory: String? = null
  ): Optional<Path> {
    val directoryChooser = DirectoryChooser()
    val dir = SystemUtils.parseDirectory(initialDirectory)
    directoryChooser.initialDirectory = dir
    directoryChooser.title = text
    val selectedDirectory = directoryChooser.showDialog(owner)
    return Optional.ofNullable(selectedDirectory?.toPath())
  }
  
  fun textAreaDialog(
      label: String,
      content: String,
      header: String? = null,
      type: Alert.AlertType = Alert.AlertType.WARNING,
      vararg buttons: ButtonType = arrayOf(ButtonType.OK)
  ): Optional<ButtonType> {
    
    val alert = Alert(type, label, *buttons)
    if (header != null) {
      alert.headerText = header
    }
    
    val lb = Label(label)
    
    val textArea = TextArea(content)
    textArea.isEditable = false
    textArea.isWrapText = true
    textArea.styleClass.add("mono")
    
    textArea.maxWidth = MAX_VALUE
    textArea.maxHeight = MAX_VALUE
    GridPane.setVgrow(textArea, Priority.ALWAYS)
    GridPane.setHgrow(textArea, Priority.ALWAYS)
    
    val expContent = GridPane()
    expContent.vgap = 10.0
    expContent.maxWidth = MAX_VALUE
    expContent.add(lb, 0, 0)
    expContent.add(textArea, 0, 1)
    expContent.minHeight = 450.0
    expContent.minWidth = 800.0
    
    alert.dialogPane.content = expContent
    ViewUtils.appendStyleSheets(alert.dialogPane.scene)
    return alert.showAndWait()
  }
  
}