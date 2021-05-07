package com.sheryv.tools.filematcher.utils

import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Window
import java.io.File
import java.lang.Double.MAX_VALUE
import java.nio.file.Path
import java.nio.file.Paths
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
        isResizable = true
      }
      ViewUtils.appendStyleSheets(dialogPane.scene)
    }.showAndWait()
  }
  
  fun inputDialog(
    title: String,
    header: String? = null
  ): Optional<String> {
    val dialog = TextInputDialog()
    dialog.title = title
    dialog.headerText = header?.padEnd(60)
    dialog.isResizable = true
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
  
  fun openFileDialog(
    owner: Window,
    text: String = "Choose file",
    initialFile: String? = null
  ): Optional<Path> {
    val directoryChooser = FileChooser()
    initialFile?.let {
      val dir = Paths.get(initialFile).toFile()
      directoryChooser.initialDirectory = dir.parentFile
      directoryChooser.initialFileName = dir.name
      if (!dir.parentFile.exists()) {
        return@let null
      }
      dir.parentFile
    } ?: run {
      directoryChooser.initialDirectory = File(SystemUtils.userDownloadDir())
    }
    
    directoryChooser.title = text
    val selectedFile = directoryChooser.showOpenDialog(owner)
    return Optional.ofNullable(selectedFile?.toPath())
  }
  
  fun saveFileDialog(
    owner: Window,
    text: String = "Save file as",
    initialFile: String? = null
  ): Optional<Path> {
    val directoryChooser = FileChooser()
    initialFile?.run {
      val dir = Paths.get(initialFile).toFile()
      if (dir.isDirectory) {
        directoryChooser.initialDirectory = dir
      } else {
        directoryChooser.initialDirectory = dir.parentFile
        directoryChooser.initialFileName = dir.name
      }
    } ?: run {
      directoryChooser.initialDirectory = File(SystemUtils.userDownloadDir())
    }
    directoryChooser.title = text
    val selectedFile = directoryChooser.showSaveDialog(owner)
    return Optional.ofNullable(selectedFile?.toPath())
  }
  
  fun textAreaDialog(
    label: String,
    content: String,
    header: String? = null,
    type: Alert.AlertType = Alert.AlertType.WARNING,
    wrapText: Boolean = true,
    vararg buttons: ButtonType = arrayOf(ButtonType.OK)
  ): Optional<ButtonType> {
    
    val alert = Alert(type, label, *buttons)
    if (header != null) {
      alert.headerText = header
    }
    
    val lb = Label(label)
    
    val textArea = TextArea(content)
    textArea.isEditable = false
    textArea.isWrapText = wrapText
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
    alert.isResizable = true
    
    alert.dialogPane.content = expContent
    ViewUtils.appendStyleSheets(alert.dialogPane.scene)
    return alert.showAndWait()
  }
  
}
