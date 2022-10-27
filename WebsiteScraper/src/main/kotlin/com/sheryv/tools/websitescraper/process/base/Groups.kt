package com.sheryv.tools.websitescraper.process.base

enum class Groups(private val label: String) : ListGroup {
  GENERAL("General"),
  STREAMING_WEBSITE("Streaming websites")
  ;
  
  override fun label(): String = label
  override fun id(): String = name
}
