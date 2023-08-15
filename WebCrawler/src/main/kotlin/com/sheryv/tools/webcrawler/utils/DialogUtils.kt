package com.sheryv.tools.webcrawler.utils

import com.sheryv.tools.webcrawler.MainApplication
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Window
import java.lang.Double.MAX_VALUE
import java.nio.file.Path
import java.nio.file.Paths


object DialogUtils {
  
  fun messageDialog(text: String) {
    dialog(text, type = Alert.AlertType.NONE, buttons = arrayOf(ButtonType.OK))
  }
  
  fun messageCopyableDialog(text: String, label: String? = null, title: String = ViewUtils.TITLE) {
    val textArea = TextArea(text)
    textArea.isEditable = false
    textArea.isWrapText = true
    textArea.prefRowCount = text.lines().size
    GridPane.setHgrow(textArea, Priority.ALWAYS)
    GridPane.setVgrow(textArea, Priority.ALWAYS)
    val gridPane = GridPane()
    gridPane.maxWidth = Double.MAX_VALUE
    if (label != null) {
      gridPane.add(Label(label), 0, 0)
      gridPane.add(textArea, 0, 1)
    } else {
      gridPane.add(textArea, 0, 0)
    }
    val alert = Alert(Alert.AlertType.NONE, "", ButtonType.OK)
    alert.dialogPane.content = gridPane
    MainApplication.appendStyleSheets(alert.dialogPane.content.scene)
    alert.title = title
    alert.isResizable = true
    alert.showAndWait()
  }
  
  fun dialog(
    text: String,
    title: String = ViewUtils.TITLE,
    header: String? = null,
    type: Alert.AlertType = Alert.AlertType.WARNING,
    owner: Window? = null,
    vararg buttons: ButtonType = arrayOf(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)
  ): ButtonType? {
    return Alert(type, text, *buttons).apply {
      if (header != null) {
        headerText = header
        isResizable = true
        if (owner != null) {
          initModality(Modality.WINDOW_MODAL)
          initOwner(owner)
        }
      }
      this.title = title
      MainApplication.appendStyleSheets(dialogPane.scene)
    }.showAndWait().orElse(null)
  }
  
  fun inputDialog(
    title: String,
    header: String? = null,
    rows: List<Pair<String, String>>,
    type: Alert.AlertType = Alert.AlertType.WARNING,
  ): List<String> {
    val alert = Alert(type, null, ButtonType.APPLY, ButtonType.CANCEL)
    if (header != null) {
      alert.headerText = header
    }
    alert.title = title
    
    val list = VBox(10.0)
    list.prefHeight = VBox.USE_COMPUTED_SIZE
    list.minHeight = 100.0
    list.maxHeight = MAX_VALUE
    list.minWidth = 700.0
    list.prefWidth = VBox.USE_COMPUTED_SIZE
    alert.isResizable = true
    
    val fields = mutableListOf<TextField>()
    
    rows.forEachIndexed { i, row ->
      
      val lb = Label(row.first)
      val tf = TextField(row.second)
      fields.add(tf)
      tf.maxWidth = MAX_VALUE
      tf.maxHeight = MAX_VALUE
      val box = VBox(lb, tf)
      box.prefHeight = VBox.USE_COMPUTED_SIZE
      box.minHeight = 10.0
      box.maxHeight = MAX_VALUE
      box.prefWidth = VBox.USE_COMPUTED_SIZE
      list.children.add(box)
    }
    
    alert.dialogPane.content = list
    
    MainApplication.appendStyleSheets(alert.dialogPane.scene)
    return when (alert.showAndWait().orElse(null)) {
      ButtonType.APPLY -> fields.map { it.text }
      else -> emptyList()
    }
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
    text: String = "Select file",
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
    text: String = "Select directory",
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
    title: String = ViewUtils.TITLE,
    header: String? = null,
    type: Alert.AlertType = Alert.AlertType.WARNING,
    wrapText: Boolean = true,
    vararg buttons: ButtonType = arrayOf(ButtonType.OK)
  ): ButtonType? {
    
    val alert = Alert(type, label, *buttons)
    if (header != null) {
      alert.headerText = header
    }
    alert.title = title
    
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
