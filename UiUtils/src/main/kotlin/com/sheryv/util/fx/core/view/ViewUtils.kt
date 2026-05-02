package com.sheryv.util.fx.core.view

import com.sheryv.util.EditableValue
import com.sheryv.util.inBackground
import com.sheryv.util.inMainContext
import com.sheryv.util.logging.log
import javafx.scene.control.Alert
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.input.*
import javafx.util.Callback
import java.awt.Desktop
import java.net.URI


fun <T> BaseView.withErrorHandler(block: () -> T): T? {
  try {
    return block()
  } catch (e: Exception) {
    log.error("Error in action", e)
    this.factory.dialogs.textAreaDialog(
      "Details",
      e.stackTraceToString(),
      "Cannot process action",
      type = Alert.AlertType.ERROR,
    )
  }
  return null
}

fun <T> BaseView.runActionInBackground(
  title: String,
  toUpdate: EditableValue<Boolean>? = null,
  action: suspend () -> T,
  onSuccess: suspend (T) -> Unit = {},
  onError: suspend (Exception) -> Boolean = { true }
) {
  inBackground(toUpdate) {
    try {
      val result = action()
      inMainContext {
        onSuccess(result)
      }
    } catch (e: Exception) {
      inMainContext {
        if (onError(e)) {
          log.error("Action failed: $title", e)
          factory.dialogs.exceptionDialog("$title failed", e)
        }
      }
    }
  }
}

object ViewUtils {
  fun saveToClipboard(text: String) {
    val content = ClipboardContent()
    content.putString(text)
    Clipboard.getSystemClipboard().setContent(content)
  }
  
  fun loadFromClipboard(): String? = Clipboard.getSystemClipboard().takeIf { it.hasString() }?.string
  
  fun openWebpage(uri: String): Boolean {
    val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
      try {
        desktop.browse(URI.create(uri))
        return true
      } catch (e: java.lang.Exception) {
        log.error("Error in open action", e)
      }
    }
    return false
  }
  
  fun <T> tableDraggableRowFactory(onCreate: (TableRow<T>) -> Unit = {}): Callback<TableView<T>, TableRow<T>> {
    return Callback { tableView ->
      val row: TableRow<T> = TableRow()
      
      row.setOnDragDetected { event ->
        if (!row.isEmpty) {
          val index: Int = row.index
          val db: Dragboard = row.startDragAndDrop(TransferMode.MOVE)
          db.dragView = row.snapshot(null, null)
          val cc = ClipboardContent()
          cc[SERIALIZED_MIME_TYPE] = index
          db.setContent(cc)
          event.consume()
        }
      }
      
      row.setOnDragOver { event ->
        val db: Dragboard = event.dragboard
        if (db.hasContent(SERIALIZED_MIME_TYPE)) {
          if (row.index != (db.getContent(SERIALIZED_MIME_TYPE) as Int).toInt()) {
            event.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            event.consume()
          }
        }
      }
      
      row.setOnDragEntered { event ->
        val db: Dragboard = event.dragboard
        if (db.hasContent(SERIALIZED_MIME_TYPE)) {
          val draggedIndex = db.getContent(SERIALIZED_MIME_TYPE) as Int
          if (row.index != draggedIndex) {
            row.styleClass.add("drag-target")
          }
        }
      }
      
      row.setOnDragExited { _ ->
        row.styleClass.remove("drag-target")
      }
      
      row.setOnDragDropped { event ->
        val db: Dragboard = event.dragboard
        if (db.hasContent(SERIALIZED_MIME_TYPE)) {
          val draggedIndex = db.getContent(SERIALIZED_MIME_TYPE) as Int
          val draggedPerson: T = tableView.items.removeAt(draggedIndex)
          val dropIndex: Int = if (row.isEmpty) {
            tableView.items.size
          } else {
            row.index
          }
          tableView.items.add(dropIndex, draggedPerson)
          event.isDropCompleted = true
          tableView.selectionModel.select(dropIndex)
          event.consume()
        }
      }
      onCreate(row)
      row
    }
  }
}

private val SERIALIZED_MIME_TYPE = DataFormat("application/x-java-serialized-object")
