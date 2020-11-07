package com.sheryv.tools.filematcher.utils

import com.sheryv.tools.filematcher.model.Entry
import com.sheryv.tools.filematcher.model.TargetPath
import com.sheryv.tools.filematcher.view.BaseView
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableCell
import javafx.scene.control.TreeTableColumn
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.stage.Stage
import javafx.util.Callback

object ViewUtils {
  
  fun createTreeColumn(name: String, preferredWidth: Int = 0, mapper: (Entry) -> String): TreeTableColumn<Entry, String> {
    return TreeTableColumn<Entry, String>(name).apply {
      setCellValueFactory { SimpleStringProperty(mapper(it.value.value)) }
      if (preferredWidth > 0)
        prefWidth = preferredWidth.toDouble()
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
    val root: TreeItem<Entry> = TreeItem(BundleUtils.createGroup("ROOT__BundleInvisibleGroup", "", target = TargetPath()))
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
          if (empty || treeTableRow.isEmpty || treeTableRow.treeItem?.value == null) {
            graphic = null
            text = null
          } else {
            val treeItem = treeTableRow.treeItem!!
            
            val result = build.invoke(treeItem, buttonsMap)
            
            if (result.isNotEmpty()) {
              hBox.children.clear()
              hBox.children.addAll(result)
              graphic = hBox
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
//            text = valueMapper.invoke(treeTableRow.treeItem.value)
          text = item
          graphic = null
        }
      }
    }
  }
  
  fun <T : BaseView> createWindow(fxml: String, title: String, stage: Stage = Stage()): T {
    val loader = FXMLLoader(javaClass.classLoader.getResource(fxml))
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
}