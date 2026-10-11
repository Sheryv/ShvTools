package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver.CommonVideoServers
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.DirectUrl
import com.sheryv.tools.webcrawler.service.subsdownload.SubsOpensubtitlesAPIDownload.SearchResultDetails
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
  
  
  @Test
  fun sort(){
    val l = listOf(1 to false, 2 to false, 3 to true, 4 to false, 5 to true)
    
    
    println(l.sortedWith(compareBy<Pair<Int, Boolean>> { it.second }.thenByDescending { it.first ?: 0 }))
  }
  
}
