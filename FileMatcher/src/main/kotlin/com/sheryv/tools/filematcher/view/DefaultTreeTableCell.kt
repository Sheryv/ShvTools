package com.sheryv.tools.filematcher.view

import javafx.scene.Node
import javafx.scene.control.Skin
import javafx.scene.control.TreeTableCell
import javafx.scene.control.skin.TreeTableCellSkin


class DefaultTreeTableCell<S, T> : TreeTableCell<S, T>() {
  
  protected override fun updateItem(item: T?, empty: Boolean) {
    if (item === getItem()) return
    super.updateItem(item, empty)
    if (item == null) {
      super.setText(null)
      super.setGraphic(null)
    } else if (item is Node) {
      super.setText(null)
      super.setGraphic(item as Node?)
    } else {
      super.setText(item.toString())
      super.setGraphic(null)
    }
  }
  
  protected override fun createDefaultSkin(): Skin<*>? {
    return DefaultTreeTableCellSkin<S, T>(this)
  }
  
  /**
   * TreeTableCellSkin that handles row graphic in its leftPadding, if
   * it is in the treeColumn of the associated TreeTableView.
   *
   *
   * It assumes that per-row graphics - including the graphic of the TreeItem, if any -
   * is folded into the TreeTableRow graphic and patches its leftLabelPadding
   * to account for the graphic width.
   *
   *
   *
   * Note: TableRowSkinBase seems to be designed to cope with variations of row
   * graphic - it has a method `graphicProperty()` that's always used
   * internally when calculating offsets in the treeColumn.
   * Subclasses override as needed, the layout code remains constant. The real
   * problem is the TreeTableCell hard-codes the TreeItem as the only graphic
   * owner.
   *
   */
  class DefaultTreeTableCellSkin<S, T>(treeTableCell: TreeTableCell<S, T>?) : TreeTableCellSkin<S, T>(treeTableCell) {
    /**
     * Overridden to adjust the padding returned by super for row graphic.
     */
    protected fun leftLabelPadding(): Double {
      var padding = 10.0 //super.leftLabelPadding()
      padding += rowGraphicPatch
      return padding
    }// start with row's graphic
    // correct for super's having added treeItem's graphic
    /**
     * Returns the patch for leftPadding if the tableRow has a graphic of
     * its own.
     *
     *
     *
     * Note: this implemenation is a bit whacky as it relies on super's
     * handling of treeItems graphics offset. A cleaner
     * implementation would override leftLabelPadding from scratch.
     *
     *
     * PENDING JW: doooooo it!
     *
     * @return
     */
    protected val rowGraphicPatch: Double
      protected get() {
        if (!isTreeColumn) return 0.0
        val graphic: Node? = skinnable.treeTableRow.graphic
        if (graphic != null) {
          val height = cellSize
          // start with row's graphic
          var patch: Double = graphic.prefWidth(height)
          // correct for super's having added treeItem's graphic
          val item = skinnable.treeTableRow.treeItem
          if (item.graphic != null) {
            val correct = item.graphic.prefWidth(height)
            patch -= correct
          }
          return patch
        }
        return 0.0
      }
    
    /**
     * Checks and returns whether our cell is attached to a treeTableView/column
     * and actually has a TreeItem.
     * @return
     */
    protected val isTreeColumn: Boolean
      protected get() {
        if (skinnable.isEmpty) return false
        val column = skinnable.tableColumn
        val view = skinnable.treeTableView
        return if (column == view.treeColumn) true else view.visibleLeafColumns.indexOf(column) == 0
      }
  }
}
