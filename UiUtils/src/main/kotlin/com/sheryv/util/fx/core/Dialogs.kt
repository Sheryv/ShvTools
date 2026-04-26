package com.sheryv.util.fx.core

import com.sheryv.util.fx.core.app.AppConfiguration
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import java.lang.Double.MAX_VALUE
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name

class Dialogs(private val appConfiguration: AppConfiguration) {
  fun messageDialog(text: String, type: Alert.AlertType = Alert.AlertType.NONE) {
    dialog(text, type = type, buttons = arrayOf(ButtonType.OK))
  }
  
  fun messageCopyableDialog(text: String, label: String? = null, title: String = appConfiguration.name) {
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
    Styles.appendStyleSheets(alert.dialogPane.content.scene, appConfiguration)
    alert.title = title
    alert.isResizable = true
    alert.showAndWait()
  }
  
  fun dialog(
    text: String,
    title: String? = null,
    header: String? = null,
    type: Alert.AlertType = Alert.AlertType.WARNING,
    components: Pane? = null,
    owner: Stage? = null,
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
      this.title = title ?: owner?.title ?: appConfiguration.name
      Styles.appendStyleSheets(dialogPane.scene, appConfiguration)
      components?.also { dialogPane.content = it }
      isResizable = true
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
    
    Styles.appendStyleSheets(alert.dialogPane.scene, appConfiguration)
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
    Styles.appendStyleSheets(dialog.dialogPane.scene, appConfiguration)
    return dialog.showAndWait().orElse(null)
  }
  
  
  fun exceptionDialog(
    header: String,
    exception: Exception,
    title: String? = null,
  ) = textAreaDialog(
    "Details", exception.message + "\n\n" + exception.stackTraceToString(), title, header, Alert.AlertType.ERROR, true, false,
    ButtonType.CLOSE
  )
  
  fun textAreaDialog(
    label: String,
    content: String,
    title: String? = null,
    header: String? = null,
    type: Alert.AlertType = Alert.AlertType.WARNING,
    wrapText: Boolean = true,
    editable: Boolean = false,
    vararg buttons: ButtonType = arrayOf(ButtonType.OK)
  ): Pair<ButtonType?, String> {
    
    val alert = Alert(type, label, *buttons)
    if (header != null) {
      alert.headerText = header
    }
    alert.title = title ?: appConfiguration.name
    
    val lb = Label(label)
    
    val textArea = TextArea(content)
    textArea.isEditable = editable
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
    Styles.appendStyleSheets(alert.dialogPane.scene, appConfiguration)
    return alert.showAndWait().map { it to textArea.text }.orElse(Pair(null, textArea.text))
  }
  
  
  fun openFileDialog(
    owner: Stage,
    text: String = "Select file",
    initialFile: String? = null
  ): Path? {
    val directoryChooser = FileChooser()
    initialFile?.let {
      val dir = Paths.get(initialFile)
      if (!Files.exists(dir.parent)) {
        return@let null
      }
      if (Files.isDirectory(dir)) {
        directoryChooser.initialDirectory = dir.toFile()
      } else {
        directoryChooser.initialDirectory = dir.parent.toFile()
        directoryChooser.initialFileName = dir.name
      }
      dir.parent
    } ?: run {
      directoryChooser.initialDirectory = Path.of("").toAbsolutePath().toFile()
    }
    
    directoryChooser.title = text + " | " + owner.title
    val selectedFile = directoryChooser.showOpenDialog(owner)
    return selectedFile?.toPath()
  }
  
  fun openDirectoryDialog(
    owner: Stage,
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
    
    directoryChooser.title = text + " | " + owner.title
    val selectedDir = directoryChooser.showDialog(owner)
    return selectedDir?.toPath()
  }
  
  fun saveFileDialog(
    owner: Stage,
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
    directoryChooser.title = text + " | " + owner.title
    val selectedFile = directoryChooser.showSaveDialog(owner)
    return selectedFile?.toPath()
  }
}
