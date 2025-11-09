package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.DirectUrl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrlParserTest {
  
  @Test
  fun compareUrl() {
    val base = "https://github.com/MrMonkey42/stremio-addon-debrid-search?bv=123"
    val short = "https://github.com/MrMonkey42/stremio-addon-debrid-search"
    val a = DirectUrl(base)
    val b = DirectUrl(short)
    
    assertTrue(a.isSameUrl(base))
    assertTrue(b.isSameUrl(base))
    assertTrue(a.isSameUrl(short))
  }
}
