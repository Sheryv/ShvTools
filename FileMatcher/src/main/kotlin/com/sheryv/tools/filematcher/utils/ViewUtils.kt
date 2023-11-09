package com.sheryv.tools.filematcher.utils

import com.sheryv.tools.filematcher.model.Entry
import com.sheryv.tools.filematcher.model.TargetPath
import com.sheryv.tools.filematcher.view.BaseView
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.lib.onChange
import com.sheryv.util.logging.log
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.beans.value.ObservableValueBase
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.stage.Stage
import javafx.util.Callback

object ViewUtils {
  
  const val title = "ShvFileMatcher"
  
  fun createTreeColumn(
    name: String,
    preferredWidth: Int = 0,
    alignRight: Boolean = false,
    cssClassMapper: ((Entry?) -> Map<String, Boolean>)? = null,
    onUpdateCell: ((Entry?) -> Unit)? = null,
    mapper: (Entry) -> String,
  ): TreeTableColumn<Entry, String> {
    return TreeTableColumn<Entry, String>(name).apply {
      setCellValueFactory { SimpleStringProperty(mapper(it.value.value)) }
      if (preferredWidth > 0)
        prefWidth = preferredWidth.toDouble()
      if (alignRight) {
        style = "-fx-alignment: CENTER-RIGHT;"
      }
      if (onUpdateCell != null || cssClassMapper != null) {
        setCellFactory {
          object : TreeTableCell<Entry, String>() {
            override fun updateItem(item: String?, empty: Boolean) {
              super.updateItem(item, empty);
              val entry: Entry?
              if (empty || item == null) {
                text = null
                graphic = null
                entry = null
              } else {
                entry = treeTableRow.treeItem?.value
                text = item.toString()
              }
              if (cssClassMapper != null) {
                cssClassMapper(entry).forEach { (key, add) ->
                  val contains = styleClass.contains(key)
                  if (contains && !add) {
                    styleClass.remove(key)
                  }
                  if (add && !contains) {
                    styleClass.add(key)
                  }
                }
              }
              if (onUpdateCell != null) {
                onUpdateCell(entry)
              }
            }
          }
        }
      }
    }
  }
  
  /*
  fun fromTreeItems(tree: TreeItem<Entry>): List<Entry> {
    val root = BundleUtils.createGroup("ROOT__BundleInvisibleGroup", "", target = TargetPath())
    fromTreeItems(tree, root)
    return root.entries
  }
  
  private fun fromTreeItems(entry: TreeItem<Entry>, child: Group) {
    val list = mutableListOf<Entry>()
    for (item in entry.children) {
      list.add(item.value)
      if (item.value is Group) {
        fromTreeItems(item, (item.value as Group))
      }
    }
    child.entries = list
  }
  */
  fun toTreeItems(entries: List<Entry>): TreeItem<Entry> {
    val root: TreeItem<Entry> =
      TreeItem(BundleUtils.createGroup("ROOT__BundleInvisibleGroup", "", target = TargetPath()))
    toTreeItems(entries.filter { it.parent == null }, entries, root)
    return root
  }
  
  private fun toTreeItems(singleLevel: List<Entry>, allEntries: List<Entry>, root: TreeItem<Entry>) {
    for (entry in singleLevel) {
      val item = TreeItem(entry)
      root.children.add(item)
      toTreeItems(BundleUtils.getFirstLevelChildren(entry.id, allEntries), allEntries, item)
    }
  }
  
  fun <T> forEachTreeItem(root: TreeItem<T>, callback: (TreeItem<T>) -> Unit) {
    for (item in root.children) {
      if (item.value == null) {
        continue
      }
      callback.invoke(item)
      forEachTreeItem(item, callback)
    }
  }
  
  fun <T> findInTree(root: TreeItem<T>, callback: (TreeItem<T>) -> Boolean): TreeItem<T>? {
    for (item in root.children) {
      if (item.value == null) {
        continue
      }
      if (callback.invoke(item)) {
        return item
      }
      
      val res = findInTree(item, callback)
      if (res != null) {
        return res
      }
    }
    return null
  }
  
  fun appendStyleSheets(scene: Scene) {
    if (scene.stylesheets.isEmpty()) {
      scene.stylesheets.add(javaClass.classLoader.getResource("style.css")?.toExternalForm())
      scene.stylesheets.add(javaClass.classLoader.getResource("dark.css")?.toExternalForm())
    }
  }
  
  
  fun <Item, Label, Control : Region> buttonsInTreeTableCellFactory(
    buttonsFactory: () -> Map<String, Control>,
    build: (TreeItem<Item>, Map<String, Control>) -> Collection<Control>
  ): Callback<TreeTableColumn<Item, Label>, TreeTableCell<Item, Label>> {
    return Callback<TreeTableColumn<Item, Label>, TreeTableCell<Item, Label>> {
      object : TreeTableCell<Item, Label>() {
        private val buttonsMap: Map<String, Control> = buttonsFactory.invoke()
        private val hBox = HBox().apply { spacing = 3.0 }
        
        override fun updateItem(item: Label, empty: Boolean) {
          super.updateItem(item, empty)
          if (empty || this.treeTableRow == null) {
            graphic = null
            text = null
          } else {
            val treeItem = treeTableRow.treeItem ?: return
            
            val result = build.invoke(treeItem, buttonsMap)
            
            if (result.isNotEmpty()) {
              hBox.children.setAll(result)
              graphic = hBox
            } else {
              graphic = null
            }
            text = null
          }
        }
      }
    }
  }
  
  fun <Item> treeTableCellFactoryWithCustomCss(
    allCssClasses: Set<String>,
    cssClassMapper: (Item) -> List<String>
  ): Callback<TreeTableColumn<Item, String>, TreeTableCell<Item, String>> {
    return Callback<TreeTableColumn<Item, String>, TreeTableCell<Item, String>> {
      object : TreeTableCell<Item, String>() {
        
        override fun updateItem(item: String?, empty: Boolean) {
          super.updateItem(item, empty)
          
          updateCss(empty)
//            text = valueMapper.invoke(treeTableRow.treeItem.value)
          text = item
          graphic = null
        }
        
        private fun updateCss(empty: Boolean) {
          if (empty || treeTableRow.isEmpty || treeTableRow.treeItem?.value == null) {
            graphic = null
            text = null
            styleClass.removeAll(allCssClasses)
          } else {
            val current = cssClassMapper.invoke(treeTableRow.treeItem.value)
            if (current.all { styleClass.contains(it) }) {
              styleClass.removeAll(allCssClasses.filter { !current.contains(it) })
            } else {
              styleClass.removeAll(allCssClasses)
              styleClass.addAll(current)
            }
          }
        }
      }
    }
  }
  
  fun <Item, Column> tableCellFactoryWithCustomCss(
    allCssClasses: Set<String>,
    cssClassMapper: (Item) -> List<String>
  ): Callback<TableColumn<Item, Column>, TableCell<Item, Column>> {
    return Callback<TableColumn<Item, Column>, TableCell<Item, Column>> {
      object : TableCell<Item, Column>() {
        
        override fun updateItem(item: Column?, empty: Boolean) {
          super.updateItem(item, empty)
          
          updateCss(empty)
//            text = valueMapper.invoke(treeTableRow.treeItem.value)
          text = item?.toString()
          graphic = null
        }
        
        private fun updateCss(empty: Boolean) {
          if (empty || tableRow.isEmpty || tableRow.item == null) {
            graphic = null
            text = null
            styleClass.removeAll(allCssClasses)
          } else {
            val current = cssClassMapper.invoke(tableRow.item)
            if (current.all { styleClass.contains(it) }) {
              styleClass.removeAll(allCssClasses.filter { !current.contains(it) })
            } else {
              styleClass.removeAll(allCssClasses)
              styleClass.addAll(current)
            }
          }
        }
      }
    }
  }
  
  
  fun <T : BaseView> createWindow(fxml: String, title: String, stage: Stage = Stage()): T {
    val loader = FXMLLoader(javaClass.classLoader.getResource(fxml))
    stage.icons.add(Image(javaClass.classLoader.getResourceAsStream("icons/app.png")))
    val root: Parent = loader.load()
    val controller = loader.getController<T>()
    controller.initStage(stage)
    stage.title = title
    val scene = Scene(root, 900.0, 600.0)
    stage.scene = scene
    appendStyleSheets(stage.scene)
    stage.show()
    return controller
  }
  
  fun <T : SimpleView> createWindow(view: T, stage: Stage = Stage()): T {
    val scene = Scene(view.root,1300.0, 600.0)
    stage.scene = scene
    appendStyleSheets(stage.scene)
    stage.titleProperty().bind(view.titleProperty)
    view.iconProperty.onChange {
      stage.icons.clear()
      stage.icons.add(it)
    }
    view.onViewCreated(stage)
    view.onViewReady()
    stage.show()
    view.onViewShown()
    return view
  }
  
  fun <T> withErrorHandler(block: () -> T): T? {
    try {
      return block()
    } catch (e: Exception) {
      log.error("Error in action", e)
      DialogUtils.textAreaDialog(
        "Details",
        e.stackTraceToString(),
        "Cannot process action",
        Alert.AlertType.ERROR,
      )
    }
    return null
  }
  
  fun <T> obs(item: TreeItem<Entry>, value: T): ObservableValue<T> {
    return object : ObservableValueBase<T>() {
      val v = value
      override fun getValue(): T {
        return v
//        return mapper(item) // mapper: (TreeItem<Entry>) -> T
      }
    }
  }
}
