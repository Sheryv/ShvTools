package com.sheryv.tools.webcrawler.process.base

interface ListGroup {
  fun label(): String
  fun id(): String
  fun parent(): ListGroup? = null
}
