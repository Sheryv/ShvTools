package com.sheryv.tools.websitescraper.view

import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.process.base.ScraperDefinition
import javafx.scene.control.TreeItem

data class ScraperListItem(val name: String, val id: String, val children: List<ScraperListItem> = emptyList()) {
  
  fun toTreeItem(): TreeItem<ScraperListItem> {
    val treeItem = TreeItem(this)
    treeItem.children.addAll(children.map { it.toTreeItem() })
    return treeItem
  }
  
  override fun toString() = name
  
  companion object {
    fun fromDefs(configuration: Configuration, scrapers: Collection<ScraperDefinition<*, *>>): List<ScraperListItem> {
      return scrapers.groupBy { it.group }.map {
        ScraperListItem(
          it.key.label(),
          it.key.id(),
          it.value.map { s -> ScraperListItem(s.findSettings(configuration).toString(), s.id) })
      }
    }
  }
}
