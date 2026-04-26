package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver

import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import kotlin.time.Duration.Companion.milliseconds


object CommonVideoServers {
  val ALL = listOf(
    VideoServerDefinition("voe", "Voe", listOf("voe.sx", "lauradaydo.com"), true, ::JWVideoServerHandler),
    VideoServerDefinition("vtube", "VTube", listOf("vtbe.to"), true, ::VTubeVideoServerHanlder),
    VideoServerDefinition("vidoza", "Vidoza", listOf("vidoza.net"), false),
    VideoServerDefinition("embedo", "Embedo", listOf("embedo.co"), true) { def, scraper ->
      HLSVideoServerHandler(
        def,
        scraper,
        "document.querySelector('#videerlay')?.click();document.querySelector('.vjs-control-bar button.vjs-play-control')?.click()",
        "document.querySelector('#videerlay') != null"
      )
    },
    VideoServerDefinition("userload", "Userload", listOf("userload.co"), true) { def, scraper ->
      HLSVideoServerHandler(
        def,
        scraper,
        "document.querySelector('#videooverlay')?.click()",
        "document.querySelector('#videooverlay') != null"
      )
    },
    VideoServerDefinition("streamtape", "Streamtape", listOf("streamtape.com"), true) { def, scraper ->
      HLSVideoServerHandler(
        def,
        scraper,
        "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()",
        "document.querySelector('.play-overlay') != null"
      )
    },
    VideoServerDefinition("vidguard", "Vidguard", listOf("embedv.net"), true, ::JWVideoServerHandler),
    //    "#player iframe",//    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"    ),
    VideoServerDefinition("vidstream", "Vidstream", listOf("vidstream.pro"), true, ::HLSVideoServerHandler),
    // ,   "#player iframe",//    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"    ),
    VideoServerDefinition("mycloud", "MyCloud", listOf("mcloud.to"), true, ::HLSVideoServerHandler),
    // ,   "#player iframe",//    "document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click();document.querySelector('.play-overlay')?.click()"    ),
    VideoServerDefinition("filemoon", "Filemoon", listOf("filemoon.sx"), true, ::HLSVideoServerHandler),
    VideoServerDefinition("luluvdo", "Luluvdo", listOf("luluvdo.com"), true, ::JWVideoServerHandler),
    VideoServerDefinition("vidmoly", "vidmoly", listOf("vidmoly.biz"), true, ::JWVideoServerHandler),
  )
}

private const val JW_PLAYER_ACTIVATION_SCRIPT =
  "document.querySelector('.jw-player .spin')?.click();document.querySelector('.jw-controls .jw-button-color')?.click();document.querySelector('button[data-plyr=play]')?.click();document.querySelector('.voe-play')?.click()"
private const val JW_PLAYER_CHECK_READY_SCRIPT =
  "document.querySelector('.jw-player .spin') != null"

class JWVideoServerHandler(def: VideoServerDefinition, scraper: StreamingWebsiteBase) :
  HLSVideoServerHandler(def, scraper, JW_PLAYER_ACTIVATION_SCRIPT, JW_PLAYER_CHECK_READY_SCRIPT) {
  
  override suspend fun findVideoSrcUrl(timeout: Int): String? {
    delay(2000.milliseconds)
    return super.findVideoSrcUrl(timeout)
  }
}


class VTubeVideoServerHanlder(def: VideoServerDefinition, scraper: StreamingWebsiteBase) :
  HLSVideoServerHandler(def, scraper, JW_PLAYER_ACTIVATION_SCRIPT, JW_PLAYER_CHECK_READY_SCRIPT, By.cssSelector("#pframe"))
