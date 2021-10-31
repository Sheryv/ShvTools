package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.Utils
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import java.lang.IllegalStateException
import java.net.URLEncoder
import java.time.OffsetDateTime

data class Entry(
  val id: String,
  val name: String,
  var src: String,
  val version: String? = null,
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val type: ItemType = ItemType.ITEM,
  val target: TargetPath = TargetPath(),
  val parent: String? = null,
  val group: Boolean = false,
  val website: String? = null,
  val description: String = "",
  val itemDate: OffsetDateTime? = null,
  var hashes: Hash? = null,
  val category: String? = null,
  val fileSize: Long? = null,
  val tags: List<String>? = null,
  val additionalFields: Map<String, String?> = emptyMap(),
  @JsonIgnore
  val linkedItemBundleId: String? = null,
  @JsonIgnore
  val linkedItemBundleVersionId: Long? = null,
  @JsonIgnore
  val linkedItemId: String? = null,
  var selected: Boolean = true
) {
  
  @JsonIgnore
  var state: ItemState = ItemState.UNKNOWN
    set(value) {
      field = value
      stateProperty.set(value)
    }
  
  @JsonIgnore
  var stateProperty = SimpleObjectProperty(state)
  
  init {
    state = if (selected) ItemState.UNKNOWN else ItemState.SKIPPED
  }
  
  @JsonIgnore
  fun getSrcUrl(context: UserContext?): String {
    if (!DataUtils.isAbsoluteUrl(src)) {
      if (context == null) {
        throw IllegalStateException("Null context while calculating URL for '$name' [$id]")
      }
      
      return getSrcUrl(context.getBundle(), context.getVersion(), context.repo?.baseUrl)
    }
    return src.trim('/')
  }
  
  @JsonIgnore
  fun getSrcUrlOrNull(context: UserContext?): String? {
    if (!DataUtils.isAbsoluteUrl(src)) {
      if (context?.repo == null || context.getBundleOrNull() == null) {
        return null
      }
      
      return getSrcUrl(context.getBundle(), context.getVersion(), context.repo?.baseUrl)
    }
    return src.trim('/')
  }
  
  @JsonIgnore
  fun getSrcUrl(bundle: Bundle, version: BundleVersion, repoBaseUrl: String?): String {
    var res = ""
    if (!DataUtils.isAbsoluteUrl(src)) {
      val baseUrl = bundle.getBaseUrl(repoBaseUrl)
      if (!baseUrl.isNullOrBlank()) {
        res += "$baseUrl/"
        val versionPath = version.versionPath
        if (!versionPath.isNullOrBlank()) {
          res += "$versionPath/"
        }
      } else {
        throw IllegalStateException("Cannot calculate URL for '$name' [$id]")
      }
    }
    res += src.trim('/')
    return res
  }
  
  @JsonIgnore
  fun isStdItem() = type == ItemType.ITEM
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as Entry
    
    if (id != other.id) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return id.hashCode()
  }
}
