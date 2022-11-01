package com.sheryv.tools.webcrawler.view

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.process.base.CrawlerDefinition
import javafx.scene.control.TreeItem

data class CrawlerListItem(val name: String, val id: String, val children: List<CrawlerListItem> = emptyList()) {
  
  fun toTreeItem(): TreeItem<CrawlerListItem> {
    val treeItem = TreeItem(this)
    treeItem.children.addAll(children.map { it.toTreeItem() })
    return treeItem
  }
  
  override fun toString() = name
  
  companion object {
    fun fromDefs(configuration: Configuration, crawlers: Collection<CrawlerDefinition<*, *>>): List<CrawlerListItem> {
      return crawlers.groupBy { it.attributes.group }.map {
        CrawlerListItem(
          it.key.label(),
          it.key.id(),
          it.value.map { s -> CrawlerListItem(s.toString(), s.id()) })
      }
    }
  }
}
