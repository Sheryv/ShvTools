package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.sheryv.tools.filematcher.model.event.ItemEnableChangedEvent
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.lg
import com.sheryv.tools.filematcher.utils.postEvent
import com.sheryv.util.logging.log
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import java.lang.IllegalStateException
import java.time.OffsetDateTime

class Entry(
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
  val updateDate: OffsetDateTime? = null,
  var hashes: Hash? = null,
  val category: String? = null,
  val fileSize: Long? = null,
  val tags: List<String>? = null,
  val additionalFields: Map<String, String?> = emptyMap(),
  var enabled: Boolean = true,
  @JsonIgnore
  val linkedItemBundleId: String? = null,
  @JsonIgnore
  val linkedItemBundleVersionId: Long? = null,
  @JsonIgnore
  val linkedItemId: String? = null,
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
  var enabledProperty = SimpleBooleanProperty(enabled)
  
  @JsonIgnore
  var previousUpdateDate = updateDate
  
  init {
    enabledProperty.addListener { o, _, n ->
      val e = this
      log.debug("listener> can change: ${e.enabled != n} | new: $n | $name")
      if (e.enabled != n) {
        enabled = n
        postEvent(ItemEnableChangedEvent(this))
      }
    }
  }
  
  fun updateBindings() {
    enabledProperty.set(enabled)
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
        val versionPath = version.versionUrlPart
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
  
  fun copy(
    id: String = this.id,
    name: String = this.name,
    src: String = this.src,
    version: String? = this.version,
    type: ItemType = this.type,
    target: TargetPath = this.target.copy(matching = this.target.matching.copy()),
    parent: String? = this.parent,
    group: Boolean = this.group,
    website: String? = this.website,
    description: String = this.description,
    itemDate: OffsetDateTime? = this.itemDate,
    updateDate: OffsetDateTime? = this.updateDate,
    hashes: Hash? = this.hashes,
    category: String? = this.category,
    fileSize: Long? = this.fileSize,
    tags: List<String>? = this.tags,
    additionalFields: Map<String, String?> = this.additionalFields.toMap(),
    enabled: Boolean = this.enabled,
    linkedItemBundleId: String? = this.linkedItemBundleId,
    linkedItemBundleVersionId: Long? = this.linkedItemBundleVersionId,
    linkedItemId: String? = this.linkedItemId,
    state: ItemState = this.state
  ): Entry {
    return Entry(
      id,
      name,
      src,
      version,
      type,
      target,
      parent,
      group,
      website,
      description,
      itemDate,
      updateDate,
      hashes,
      category,
      fileSize,
      tags,
      additionalFields,
      enabled,
      linkedItemBundleId,
      linkedItemBundleVersionId,
      linkedItemId,
    ).also {
      it.state = state
      it.previousUpdateDate = this.updateDate
    }
  }
  
  
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
  
  override fun toString(): String {
    return "Entry(id='$id', name='$name', src='$src', version=$version, parent=$parent, group=$group, state=$state, enabled=$enabled)"
  }
  
  
}
