package com.sheryv.tools.websitescraper.utils

import com.sheryv.tools.websitescraper.MainApplication
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Window
import java.lang.Double.MAX_VALUE
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*


object DialogUtils {
  
  fun dialog(
    text: String,
    header: String? = null,
    type: Alert.AlertType = Alert.AlertType.WARNING,
    owner: Window? = null,
    vararg buttons: ButtonType = arrayOf(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)
  ): ButtonType? {
    return Alert(type, text, *buttons).apply {
      if (header != null) {
        headerText = header
        isResizable = true
        if (owner != null) initOwner(owner)
      }
      MainApplication.appendStyleSheets(dialogPane.scene)
    }.showAndWait().orElse(null)
  }
  
  fun inputDialog(
    title: String,
    header: String? = null,
    text: String? = null
  ): String? {
    val dialog = TextInputDialog(text)
    dialog.title = title
    dialog.headerText = header?.padEnd(60)
    dialog.isResizable = true
    MainApplication.appendStyleSheets(dialog.dialogPane.scene)
    return dialog.showAndWait().orElse(null)
  }
  
  fun <T> choiceDialog(
    title: String,
    list: Collection<T>,
    header: String? = null,
    default: T? = null
  ): T? {
    val dialog = ChoiceDialog<T>(default, list)
    dialog.title = title
    dialog.headerText = header?.padEnd(60)
    dialog.isResizable = true
    MainApplication.appendStyleSheets(dialog.dialogPane.scene)
    return dialog.showAndWait().orElse(null)
  }
  
  fun openFileDialog(
    owner: Window,
    text: String = "Choose file",
    initialFile: String? = null
  ): Path? {
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
      directoryChooser.initialDirectory = Path.of("").toAbsolutePath().toFile()
    }
    
    directoryChooser.title = text
    val selectedFile = directoryChooser.showOpenDialog(owner)
    return selectedFile?.toPath()
  }
  
  fun openDirectoryDialog(
    owner: Window,
    text: String = "Choose directory",
    initialDir: String? = null
  ): Path? {
    val directoryChooser = DirectoryChooser()
    initialDir?.let {
      val dir = Paths.get(initialDir).toFile()
      if (dir.exists()) {
        directoryChooser.initialDirectory = if (dir.isDirectory) dir
        else dir.parentFile
        dir
      } else {
        null
      }
    } ?: run {
      directoryChooser.initialDirectory = Path.of("").toAbsolutePath().toFile()
    }
    
    directoryChooser.title = text
    val selectedDir = directoryChooser.showDialog(owner)
    return selectedDir?.toPath()
  }
  
  fun saveFileDialog(
    owner: Window,
    text: String = "Save file as",
    initialFile: String? = null,
    extension: String? = null
  ): Path? {
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
      directoryChooser.initialDirectory = Path.of("").toAbsolutePath().toFile()
    }
    if (extension != null) {
      directoryChooser.extensionFilters.add(
        FileChooser.ExtensionFilter(
          extension.uppercase() + " files",
          "*.$extension"
        )
      )
    }
    directoryChooser.title = text
    val selectedFile = directoryChooser.showSaveDialog(owner)
    return selectedFile?.toPath()
  }
  
  fun textAreaDialog(
    label: String,
    content: String,
    header: String? = null,
    type: Alert.AlertType = Alert.AlertType.WARNING,
    wrapText: Boolean = true,
    vararg buttons: ButtonType = arrayOf(ButtonType.OK)
  ): ButtonType? {
    
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
    MainApplication.appendStyleSheets(alert.dialogPane.scene)
    return alert.showAndWait().orElse(null)
  }
  
}
