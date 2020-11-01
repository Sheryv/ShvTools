package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.Utils
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import java.time.OffsetDateTime

open class Entry(
    val id: String,
    val name: String,
    val src: String,
    val version: String? = null,
    val type: ItemType = ItemType.ITEM,
    val target: TargetPath = TargetPath(),
    val selected: Boolean = true,
    val website: String? = null,
    val description: String = "",
    val itemDate: OffsetDateTime? = null,
    val hashes: Hash? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val linkedItemBundleId: String? = null,
    val linkedItemBundleVersionId: Long? = null,
    val linkedItemId: String? = null

) {
  
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  var state: ItemState = ItemState.UNKNOWN
    set(value) {
      field = value
      stateProperty.set(value)
    }
  
  @JsonIgnore
  var stateProperty = SimpleObjectProperty(state)
  
  @JsonIgnore
  fun isGroup(): Boolean {
    val g = type == ItemType.GROUP
    if (g && this !is Group) {
      throw IllegalStateException("Defined as Group but not Group class")
    }
    return g
  }
  
  @JsonIgnore
  fun getSrcUrl(bundleBase: String?): String {
    var res = ""
    if (!DataUtils.isAbsoluteUrl(src) && !bundleBase.isNullOrBlank()) {
      res += "$bundleBase/"
    }
    res += src.trim('/')
    return res
  }
  
  @JsonIgnore
  fun isStdItem() = type == ItemType.ITEM
}