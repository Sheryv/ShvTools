package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
  use = JsonTypeInfo.Id.DEDUCTION,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(
  JsonSubTypes.Type(value = BundleVersion::class, name = CONTAINER_TYPE_PROVIDED),
  JsonSubTypes.Type(value = BundleVersionLink::class, name = CONTAINER_TYPE_LINK)
)
abstract class BundleVersionBase(
  val versionId: Long,
  val versionName: String = "",
) {
  
  override fun toString(): String {
    return "$versionName [$versionId]"
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as BundleVersionBase
    
    if (versionId != other.versionId) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return versionId.hashCode()
  }
  
  
}
