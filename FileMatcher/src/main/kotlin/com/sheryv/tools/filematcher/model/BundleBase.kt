package com.sheryv.tools.filematcher.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
  use = JsonTypeInfo.Id.DEDUCTION,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = Bundle::class, name = CONTAINER_TYPE_PROVIDED),
  JsonSubTypes.Type(value = BundleLink::class, name = CONTAINER_TYPE_LINK)
)
abstract class BundleBase(
  val id: String,
  val name: String = "",
) {
  
  override fun toString(): String {
    return name
  }
}
