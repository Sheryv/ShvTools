package com.sheryv.tools.filematcher.view

import com.sheryv.tools.filematcher.utils.ViewUtils
import com.sun.javafx.scene.control.skin.resources.ControlResources
import javafx.beans.Observable
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.Callback

class CustomDialog<T>(
  val controls: List<Row>,
  resultConverter: Callback<ButtonType, T> = Callback { null },
  label: String? = null,
  title: String? = null,
  header: String? = null,
  vararg buttons: ButtonType = arrayOf(ButtonType.OK, ButtonType.CANCEL)
) : Dialog<T>() {
  
  private val grid: GridPane
  private val labelControl: Label
//  private val vBox: VBox
  
  private fun updateGrid() {
    grid.children.clear()
    var row = 0
    if (!labelControl.text.isNullOrBlank()) {
      grid.add(labelControl, 0, row++)
    }
    controls.forEach {
      
      val node = if (it.label != null) {
        VBox(Label(it.label), it.control.apply {
          setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)
        }).apply {
          isFillWidth = true
        }
      } else {
        it.control
      }.apply {
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)
      }
      grid.add(node, 0, row)
      GridPane.setHgrow(node, Priority.ALWAYS)
      GridPane.setFillWidth(node, true)
      row++
    }
    dialogPane.content = grid
  }
  
  init {
    val dialogPane = dialogPane
    
    // -- grid
    grid = GridPane()
    grid.hgap = 10.0
    grid.vgap = 10.0
    grid.maxWidth = Double.MAX_VALUE
    grid.alignment = Pos.CENTER_LEFT
    grid.minWidth = 400.0
    
    // -- label
    labelControl = Label(label)
    labelControl.maxWidth = Double.MAX_VALUE
    labelControl.maxHeight = Double.MAX_VALUE
    labelControl.styleClass.add("content")
    labelControl.isWrapText = true
    labelControl.prefWidth = 360.0
    labelControl.prefWidth = Region.USE_COMPUTED_SIZE
//    labelControl.textProperty().bind(dialogPane.contentTextProperty())

//    dialogPane.contentTextProperty().addListener { o: com.sheryv.util.Observable? -> updateGrid() }
    this.title = title ?: ControlResources.getString("Dialog.confirm.title")
    dialogPane.headerText = header ?: ControlResources.getString("Dialog.confirm.header")
    dialogPane.styleClass.add("text-input-dialog")
    dialogPane.buttonTypes.addAll(buttons)

//    vBox = VBox(5.0, *controls.toTypedArray())
    
    ViewUtils.appendStyleSheets(dialogPane.scene)
    (dialogPane.scene.window as Stage).icons.add(Image(javaClass.classLoader.getResourceAsStream("icons/app.png")))
    
    updateGrid()
    setResultConverter(resultConverter)
  }
  
  data class Row(val control: Region, val label: String? = null)
}
