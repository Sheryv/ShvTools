package com.sheryv.util.fx.lib

import javafx.beans.property.ObjectProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.*
import kotlin.reflect.KFunction1

private val GridPaneRowIdKey = "shvtools.GridPaneRowId"
private val GridPaneParentObjectKey = "shvtools.GridPaneParentObject"

fun GridPane.row(title: String? = null, op: Pane.() -> Unit = {}) {
  properties[GridPaneRowIdKey] = if (properties.containsKey(GridPaneRowIdKey)) properties[GridPaneRowIdKey] as Int + 1 else 0
  
  // Allow the caller to add children to a fake pane
  val fake = Pane()
  fake.properties[GridPaneParentObjectKey] = this
  if (title != null) fake.children.add(Label(title))
  
  op(fake)
  
  // Create a new row in the GridPane and add the children added to the fake pane
  addRow(properties[GridPaneRowIdKey] as Int, *fake.children.toTypedArray())
}

/**
 * Removes the corresponding row to which this [node] belongs to.
 *
 * It does the opposite of the [GridPane.row] cleaning all internal state properly.
 *
 * @return the row index of the removed row.
 */
fun GridPane.removeRow(node: Node): Int {
  val rowIdKey = properties[GridPaneRowIdKey] as Int?
  if (rowIdKey != null) {
    when (rowIdKey) {
      0 -> properties.remove(GridPaneRowIdKey)
      else -> properties[GridPaneRowIdKey] = rowIdKey - 1
    }
  }
  val rowIndex = GridPane.getRowIndex(node) ?: 0
  val nodesToDelete = mutableListOf<Node>()
  children.forEach { child ->
    val childRowIndex = GridPane.getRowIndex(child) ?: 0
    if (childRowIndex == rowIndex) {
      nodesToDelete.add(child)
      // Remove row index property from the node
      GridPane.setRowIndex(child, null)
      GridPane.setColumnIndex(child, null)
    } else if (childRowIndex > rowIndex) {
      GridPane.setRowIndex(child, childRowIndex - 1)
    }
  }
  children.removeAll(nodesToDelete)
  return rowIndex
}

fun GridPane.removeAllRows() {
  children.forEach {
    GridPane.setRowIndex(it, null)
    GridPane.setColumnIndex(it, null)
  }
  children.clear()
  properties.remove(GridPaneRowIdKey)
}

fun GridPane.constraintsForColumn(columnIndex: Int): ColumnConstraints {
  while (columnConstraints.size <= columnIndex) columnConstraints.add(ColumnConstraints())
  return columnConstraints[columnIndex]
}

fun GridPane.constraintsForRow(rowIndex: Int): RowConstraints {
  while (rowConstraints.size <= rowIndex) rowConstraints.add(RowConstraints())
  return rowConstraints[rowIndex]
}

val Parent.gridpaneColumnConstraints: ColumnConstraints?
  get() {
    var cursor = this
    var next = parent
    while (next != null) {
      val gridReference = when {
        next is GridPane -> next to GridPane.getColumnIndex(cursor)?.let { it }
        // perhaps we're still in the row builder
        next.parent == null -> (next.properties[GridPaneParentObjectKey] as? GridPane)?.let {
          it to next.getChildList()?.indexOf(cursor)
        }
        else -> null
      }
      
      if (gridReference != null) {
        val (grid, columnIndex) = gridReference
        if (columnIndex != null && columnIndex >= 0) return grid.constraintsForColumn(columnIndex)
      }
      cursor = next
      next = next.parent
    }
    return null
  }

fun Parent.gridpaneColumnConstraints(op: ColumnConstraints.() -> Unit) = gridpaneColumnConstraints?.apply { op() }

fun ToolBar.spacer(prio: Priority = Priority.ALWAYS, op: Pane.() -> Unit = {}): Pane {
  val pane = Pane().apply {
    styleClass.add("spacer")
    hgrow = prio
  }
  op(pane)
  addChildIfPossible(pane)
  return pane
}

fun HBox.spacer(prio: Priority = Priority.ALWAYS, op: Pane.() -> Unit = {}) = Pane().apply { HBox.setHgrow(this, prio) }.attachTo(this, op)
fun VBox.spacer(prio: Priority = Priority.ALWAYS, op: Pane.() -> Unit = {}) = Pane().apply { VBox.setVgrow(this, prio) }.attachTo(this, op)

fun Parent.toolbar(vararg nodes: Node, op: ToolBar.() -> Unit = {}): ToolBar {
  val toolbar = ToolBar()
  if (nodes.isNotEmpty()) toolbar.items.addAll(nodes)
  toolbar.attachTo(this, op)
  return toolbar
}


@Deprecated("No need to wrap ToolBar children in children{} anymore. Remove the wrapper and all builder items will still be added as before.", ReplaceWith("no children{} wrapper"), DeprecationLevel.WARNING)
fun ToolBar.children(op: ToolBar.() -> Unit) = apply { op() }

fun Parent.hbox(spacing: Number? = null, alignment: Pos? = null, op: HBox.() -> Unit = {}): HBox {
  val hbox = HBox()
  if (alignment != null) hbox.alignment = alignment
  if (spacing != null) hbox.spacing = spacing.toDouble()
  return hbox.attachTo(this, op)
}

fun Parent.vbox(spacing: Number? = null, alignment: Pos? = null, op: VBox.() -> Unit = {}): VBox {
  val vbox = VBox()
  if (alignment != null) vbox.alignment = alignment
  if (spacing != null) vbox.spacing = spacing.toDouble()
  return vbox.attachTo(this, op)
}

fun ToolBar.separator(orientation: Orientation = Orientation.HORIZONTAL, op: Separator.() -> Unit = {}): Separator {
  val separator = Separator(orientation).also(op)
  addChildIfPossible(separator)
  return separator
}

fun Parent.separator(orientation: Orientation = Orientation.HORIZONTAL, op: Separator.() -> Unit = {}) = Separator(orientation).attachTo(this, op)

fun Parent.group(initialChildren: Iterable<Node>? = null, op: Group.() -> Unit = {}) = Group().apply { if (initialChildren != null) children.addAll(initialChildren) }.attachTo(this, op)
fun Parent.stackpane(initialChildren: Iterable<Node>? = null, op: StackPane.() -> Unit = {}) = StackPane().apply { if (initialChildren != null) children.addAll(initialChildren) }.attachTo(this, op)
fun Parent.gridpane(op: GridPane.() -> Unit = {}) = GridPane().attachTo(this, op)
fun Parent.pane(op: Pane.() -> Unit = {}) = Pane().attachTo(this, op)
fun Parent.flowpane(op: FlowPane.() -> Unit = {}) = FlowPane().attachTo(this, op)
fun Parent.tilepane(op: TilePane.() -> Unit = {}) = TilePane().attachTo(this, op)
fun Parent.borderpane(op: BorderPane.() -> Unit = {}) = BorderPane().attachTo(this, op)

@Suppress("UNCHECKED_CAST")
var Node.builderTarget: KFunction1<*, ObjectProperty<Node>>?
  get() = properties["shvtools.builderTarget"] as KFunction1<Any, ObjectProperty<Node>>?
  set(value) {
    properties["shvtools.builderTarget"] = value
  }

fun BorderPane.top(op: BorderPane.() -> Unit) = region(BorderPane::topProperty, op)
fun BorderPane.bottom(op: BorderPane.() -> Unit) = region(BorderPane::bottomProperty, op)
fun BorderPane.left(op: BorderPane.() -> Unit) = region(BorderPane::leftProperty, op)
fun BorderPane.right(op: BorderPane.() -> Unit) = region(BorderPane::rightProperty, op)
fun BorderPane.center(op: BorderPane.() -> Unit) = region(BorderPane::centerProperty, op)
internal fun BorderPane.region(region: KFunction1<BorderPane, ObjectProperty<Node>>?, op: BorderPane.() -> Unit) {
  builderTarget = region
  op()
  builderTarget = null
}

@Deprecated("Use top = node {} instead")
fun <T : Node> BorderPane.top(topNode: T, op: T.() -> Unit = {}): T {
  top = topNode
  return topNode.attachTo(this, op)
}

@Deprecated("Use bottom = node {} instead")
fun <T : Node> BorderPane.bottom(bottomNode: T, op: T.() -> Unit = {}): T {
  bottom = bottomNode
  return bottomNode.attachTo(this, op)
}

@Deprecated("Use left = node {} instead")
fun <T : Node> BorderPane.left(leftNode: T, op: T.() -> Unit = {}): T {
  left = leftNode
  return leftNode.attachTo(this, op)
}

@Deprecated("Use right = node {} instead")
fun <T : Node> BorderPane.right(rightNode: T, op: T.() -> Unit = {}): T {
  right = rightNode
  return rightNode.attachTo(this, op)
}

@Deprecated("Use center = node {} instead")
fun <T : Node> BorderPane.center(centerNode: T, op: T.() -> Unit = {}): T {
  center = centerNode
  return centerNode.attachTo(this, op)
}

fun Parent.titledpane(title: String? = null, node: Node? = null, collapsible: Boolean = true, op: (TitledPane).() -> Unit = {}): TitledPane {
  val titledPane = TitledPane(title, node)
  titledPane.isCollapsible = collapsible
  titledPane.attachTo(this, op)
  return titledPane
}

fun Parent.titledpane(title: ObservableValue<String>, node: Node? = null, collapsible: Boolean = true, op: (TitledPane).() -> Unit = {}): TitledPane {
  val titledPane = TitledPane("", node)
  titledPane.textProperty().bind(title)
  titledPane.isCollapsible = collapsible
  titledPane.attachTo(this, op)
  return titledPane
}

fun Parent.pagination(pageCount: Int? = null, pageIndex: Int? = null, op: Pagination.() -> Unit = {}): Pagination {
  val pagination = Pagination()
  if (pageCount != null) pagination.pageCount = pageCount
  if (pageIndex != null) pagination.currentPageIndex = pageIndex
  return pagination.attachTo(this, op)
}

fun Parent.scrollpane(fitToWidth: Boolean = true, fitToHeight: Boolean = true, op: ScrollPane.() -> Unit = {}): ScrollPane {
  val pane = ScrollPane()
  pane.isFitToWidth = fitToWidth
  pane.isFitToHeight = fitToHeight
  pane.attachTo(this, op)
  return pane
}

var ScrollPane.edgeToEdge: Boolean
  get() = styleClass.contains("edge-to-edge")
  set(value) {
    if (value) styleClass.add("edge-to-edge") else styleClass.remove("edge-to-edge")
  }

fun Parent.splitpane(orientation: Orientation = Orientation.HORIZONTAL, vararg nodes: Node, op: SplitPane.() -> Unit = {}): SplitPane {
  val splitpane = SplitPane()
  splitpane.orientation = orientation
  if (nodes.isNotEmpty())
    splitpane.items.addAll(nodes)
  splitpane.attachTo(this, op)
  return splitpane
}

@Deprecated("No need to wrap splitpane items in items{} anymore. Remove the wrapper and all builder items will still be added as before.", ReplaceWith("no items{} wrapper"), DeprecationLevel.WARNING)
fun SplitPane.items(op: (SplitPane.() -> Unit)) = op(this)

fun Parent.canvas(width: Double = 0.0, height: Double = 0.0, op: Canvas.() -> Unit = {}) =
  Canvas(width, height).attachTo(this, op)

fun Parent.anchorpane(vararg nodes: Node, op: AnchorPane.() -> Unit = {}): AnchorPane {
  val anchorpane = AnchorPane()
  if (nodes.isNotEmpty()) anchorpane.children.addAll(nodes)
  anchorpane.attachTo(this, op)
  return anchorpane
}

fun Parent.accordion(vararg panes: TitledPane, op: Accordion.() -> Unit = {}): Accordion {
  val accordion = Accordion()
  if (panes.isNotEmpty()) accordion.panes.addAll(panes)
  accordion.attachTo(this, op)
  return accordion
}

fun <T : Node> Accordion.fold(title: String? = null, node: T, expanded: Boolean = false, op: T.() -> Unit = {}): TitledPane {
  val fold = TitledPane(title, node)
  fold.isExpanded = expanded
  panes += fold
  op(node)
  return fold
}

@Deprecated("Properties added to the container will be lost if you add only a single child Node", ReplaceWith("Accordion.fold(title, node, op)"), DeprecationLevel.WARNING)
fun Accordion.fold(title: String? = null, op: Pane.() -> Unit = {}): TitledPane {
  val vbox = VBox().also(op)
  val fold = TitledPane(title, if (vbox.children.size == 1) vbox.children[0] else vbox)
  panes += fold
  return fold
}

fun Parent.region(op: Region.() -> Unit = {}) = Region().attachTo(this, op)


@Deprecated("Use the paddingRight property instead", ReplaceWith("paddingRight = p"))
fun Region.paddingRight(p: Double) { paddingRight = p }

var Region.paddingRight: Number
  get() = padding.right
  set(value) {
    padding = padding.copy(right = value.toDouble())
  }

@Deprecated("Use the paddingLeft property instead", ReplaceWith("paddingLeft = p"))
fun Region.paddingLeft(p: Double) { paddingLeft = p }

var Region.paddingLeft: Number
  get() = padding.left
  set(value) {
    padding = padding.copy(left = value)
  }

@Deprecated("Use the paddingTop property instead", ReplaceWith("paddingTop = p"))
fun Region.paddingTop(p: Double) { paddingTop = p }

var Region.paddingTop: Number
  get() = padding.top
  set(value) {
    padding = padding.copy(top = value)
  }

@Deprecated("Use the paddingBottom property instead", ReplaceWith("paddingBottom = p"))
fun Region.paddingBottom(p: Double) { paddingBottom = p }

var Region.paddingBottom: Number
  get() = padding.bottom
  set(value) {
    padding = padding.copy(bottom = value)
  }

@Deprecated("Use the paddingVertical property instead", ReplaceWith("paddingVertical = p"))
fun Region.paddingVertical(p: Double) { paddingVertical = p }

var Region.paddingVertical: Number
  get() = padding.vertical * 2
  set(value) {
    val half = value.toDouble() / 2.0
    padding = padding.copy(vertical = half)
  }

@Deprecated("Use the paddingHorizontal property instead", ReplaceWith("paddingHorizontal = p"))
fun Region.paddingHorizontal(p: Double) { paddingHorizontal = p }

var Region.paddingHorizontal: Number
  get() = padding.horizontal * 2
  set(value) {
    val half = value.toDouble() / 2.0
    padding = padding.copy(horizontal = half)
  }

@Deprecated("Use the paddingAll property instead", ReplaceWith("paddingAll = p"))
fun Region.paddingAll(p: Double) {
  paddingAll = p
}

var Region.paddingAll: Number
  get() = padding.all
  set(value) {
    padding = insets(value)
  }

fun Region.fitToParentHeight() {
  val parent = this.parent
  if (parent != null && parent is Region) {
    fitToHeight(parent)
  }
}

fun Region.fitToParentWidth() {
  val parent = this.parent
  if (parent != null && parent is Region) {
    fitToWidth(parent)
  }
}

fun Region.fitToParentSize() {
  fitToParentHeight()
  fitToParentWidth()
}

fun Region.fitToHeight(region: Region) {
  prefHeightProperty().bind(region.heightProperty())
}

fun Region.fitToWidth(region: Region) {
  prefWidthProperty().bind(region.widthProperty())
}

fun Region.fitToSize(region: Region) {
  fitToHeight(region)
  fitToWidth(region)
}
