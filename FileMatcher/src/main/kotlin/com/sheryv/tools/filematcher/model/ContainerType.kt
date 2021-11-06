package com.sheryv.tools.filematcher.model

const val CONTAINER_TYPE_PROVIDED = "PROVIDED"
const val CONTAINER_TYPE_LINK = "LINK"

enum class ContainerType(val code: String) {
  PROVIDED(CONTAINER_TYPE_PROVIDED),
  LINK(CONTAINER_TYPE_LINK)
}
