package com.sheryv.tools.webcrawler.view.settings

import com.sheryv.tools.webcrawler.utils.ViewUtils
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.layout.Region


class TableSettingsRow(
  name: String,
  val items: List<RowDefinition>,
  val columns: List<String> = emptyList(),
  val orderable: Boolean = true,
  listener: ObjectProperty<List<TableSettingsRow.RowDefinition>>? = null
) :
  SettingsViewRow<List<TableSettingsRow.RowDefinition>>(name, listener = listener) {
  
  private lateinit var table: TableView<RowDefinition>
  
  override fun buildPart(): Region {
    val cols = columns.takeIf { it.isNotEmpty() }
      ?: items.takeIf { items.isNotEmpty() }?.let { (1..items.first().cells.size).map { "Column $it" } }
      ?: columns
    require(items.all { it.cells.size == cols.size }) { "Number of cells for each row have to equal column number" }
    
    table = TableView<RowDefinition>()
    table.items.addAll(items)
    if (orderable) {
      table.rowFactory = ViewUtils.tableDraggableRowFactory()
    }
    
    table.columns.add(TableColumn<RowDefinition, Boolean>("Enabled").apply {
      setCellValueFactory { it.value.enabledProperty }
      setCellFactory { CheckBoxTableCell() }
    })
    table.columns.addAll(cols.mapIndexed { i, c ->
      TableColumn<RowDefinition, String>(c).apply {
        setCellValueFactory {
          ReadOnlyStringWrapper(it.value.cells[i])
        }
      }
    })
    listener?.bind(table.itemsProperty())
    table.minHeight = 25 + 25.0 * (items.size.coerceAtMost(8))
    table.isEditable = true
    return table
  }
  
  class RowDefinition(val cells: List<String>, enabled: Boolean = true) {
    internal val enabledProperty = SimpleBooleanProperty(enabled)
    
    fun isEnabled() = enabledProperty.value
  }
  
  override fun readValue(): List<RowDefinition> {
    return table.items.toList()
  }
}
