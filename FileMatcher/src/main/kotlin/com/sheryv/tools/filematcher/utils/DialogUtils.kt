package com.sheryv.tools.filematcher.utils

import com.sheryv.tools.filematcher.view.CustomDialog
import javafx.beans.value.ObservableValue
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
    initialFile: String? = null,
    extension: String? = null
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
    return Optional.ofNullable(selectedFile?.toPath())
  }
  
  fun textAreaDialog(
    label: String,
    content: String,
    header: String? = null,
    type: Alert.AlertType = Alert.AlertType.WARNING,
    wrapText: Boolean = true,
    editable: Boolean = false,
    vararg buttons: ButtonType = arrayOf(ButtonType.OK)
  ): Optional<Pair<ButtonType, String>> {
    
    val alert = Alert(type, label, *buttons)
    if (header != null) {
      alert.headerText = header
    }
    
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
    ViewUtils.appendStyleSheets(alert.dialogPane.scene)
    return alert.showAndWait().map { it to textArea.text }
  }
  
  fun twoComboDialog(
    title: String, label1: String, label2: String,
    first: (Pair<String, String>?) -> List<String>,
    second: (Pair<String, String>?) -> List<String>,
  ): Optional<Pair<String, String>> {
    
    val c1 = ComboBox<String>()
    c1.items.setAll(first(null))
    val c2 = ComboBox<String>()
    c2.items.setAll(second(null))
    
    val function: (observable: ObservableValue<out String>?, oldValue: String?, newValue: String?) -> Unit =
      { o, _, _ ->
        if (c1.selectionModel.selectedItem != null && c2.selectionModel.selectedItem != null) {
          val pair = c1.selectionModel.selectedItem to c2.selectionModel.selectedItem
          val first1 = first(pair)
          if (!first1.toTypedArray().contentEquals(c1.items.toTypedArray())) {
            c1.items.setAll(first1)
            c1.selectionModel.select(0)
          }
          val second1 = second(pair)
          if (!second1.toTypedArray().contentEquals(c2.items.toTypedArray())) {
            c2.items.setAll(second1)
            c2.selectionModel.select(0)
          }
        }
      }
    c1.selectionModel.selectedItemProperty().addListener(function)
    c2.selectionModel.selectedItemProperty().addListener(function)
    
    c1.selectionModel.select(0)
    c2.selectionModel.select(0)
    
    val d = CustomDialog(header = title, controls = listOf(
      CustomDialog.Row(c1, label1),
      CustomDialog.Row(c2, label2),
    ), resultConverter = {
      if (it == ButtonType.OK) {
        c1.selectionModel.selectedItem to c2.selectionModel.selectedItem
      } else null
    })
    return d.showAndWait().map { it }
  }
  
}
