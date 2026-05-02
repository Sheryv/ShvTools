package com.sheryv.tools.webcrawler.utils

import com.sheryv.tools.webcrawler.view.settings.TableSettingsRow
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TreeItem
import javafx.scene.input.*
import javafx.util.Callback



object ViewUtils {
  const val TITLE = "Web crawler"

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
}
