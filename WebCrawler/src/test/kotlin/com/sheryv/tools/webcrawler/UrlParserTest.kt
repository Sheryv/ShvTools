package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver.CommonVideoServers
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
  
  @Test
  fun findStreamUrl() {
    val result = CommonVideoServers.VOE.isUrlMatchingRequestWithM3U8Manifest("https://cdn-s9axncnqpmolwhjf.edgeon-bandwidth.com/engine/hls2/01/09450/bcexc8bkipr6_,n,.urlset/master.m3u8?t=wt_rUo7j98n6yIfAm2QHoEFVzsFJfhYwyPftWU35dC0&s=1773583594&e=14400&f=47254114&node=BCeQachLSBhn9CjtJ8a5NmjiOwQOySRQZJFN9sWfb0k=&i=77.65&sp=2500&asn=212163&q=n&rq=KWoMzZwm1wQX18dQvVC0fL7fKuLDm8EdhksnNlFi")
    
    assertTrue(result)
  }
}
