package com.sheryv.tools.webcrawler.process.base

enum class Groups(private val label: String) : ListGroup {
  GENERAL("General"),
  STREAMING_WEBSITE("Streaming websites")
  ;
  
  override fun label(): String = label
  override fun id(): String = name
}
