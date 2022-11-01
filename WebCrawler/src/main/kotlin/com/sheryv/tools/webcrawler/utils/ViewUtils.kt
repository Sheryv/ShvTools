package com.sheryv.tools.webcrawler.utils

import com.sheryv.tools.webcrawler.view.settings.TableSettingsRow
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TreeItem
import javafx.scene.input.*
import javafx.util.Callback


private val SERIALIZED_MIME_TYPE = DataFormat("application/x-java-serialized-object")

object ViewUtils {
  const val TITLE = "Web crawler"
  
  fun saveToClipboard(text: String) {
    val content = ClipboardContent()
    content.putString(text)
    Clipboard.getSystemClipboard().setContent(content)
  }
  
  fun loadFromClipboard(): String? = Clipboard.getSystemClipboard().takeIf { it.hasString() }?.string
  
  fun <T> findFirstLeafInTree(root: TreeItem<T>): TreeItem<T>? {
    for (child in root.children) {
      if (child.isLeaf) {
        return child
      }
      if (child.children.isNotEmpty()) {
        val inner = findFirstLeafInTree(child)
        if (inner != null)
          return inner
      }
    }
    return null
  }
  
  fun tableDraggableRowFactory(): Callback<TableView<TableSettingsRow.RowDefinition>, TableRow<TableSettingsRow.RowDefinition>> {
    return Callback { tableView ->
      val row: TableRow<TableSettingsRow.RowDefinition> = TableRow()
      
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
      
      row.setOnDragDropped { event ->
        val db: Dragboard = event.dragboard
        if (db.hasContent(SERIALIZED_MIME_TYPE)) {
          val draggedIndex = db.getContent(SERIALIZED_MIME_TYPE) as Int
          val draggedPerson: TableSettingsRow.RowDefinition = tableView.items.removeAt(draggedIndex)
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
      row
    }
  }
}
