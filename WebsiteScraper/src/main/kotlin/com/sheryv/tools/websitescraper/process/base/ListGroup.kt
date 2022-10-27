package com.sheryv.tools.websitescraper.process.base

interface ListGroup {
  fun label(): String
  fun id(): String
  fun parent(): ListGroup? = null
}
