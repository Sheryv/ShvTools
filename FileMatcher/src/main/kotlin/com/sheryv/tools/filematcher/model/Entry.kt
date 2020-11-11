package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.Utils
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import java.net.URLEncoder
import java.time.OffsetDateTime

data class Entry(
    val id: String,
    val name: String,
    val src: String,
    val version: String? = null,
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val type: ItemType = ItemType.ITEM,
    val target: TargetPath = TargetPath(),
    val selected: Boolean = true,
    val parent: String? = null,
    val group: Boolean = false,
    val website: String? = null,
    val description: String = "",
    val itemDate: OffsetDateTime? = null,
    var hashes: Hash? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val additionalFields: Map<String, String?> = emptyMap(),
    @JsonIgnore
    val linkedItemBundleId: String? = null,
    @JsonIgnore
    val linkedItemBundleVersionId: Long? = null,
    @JsonIgnore
    val linkedItemId: String? = null

) {
  
  @JsonIgnore
  var state: ItemState = ItemState.UNKNOWN
    set(value) {
      field = value
      stateProperty.set(value)
    }
  
  @JsonIgnore
  var stateProperty = SimpleObjectProperty(state)
  
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