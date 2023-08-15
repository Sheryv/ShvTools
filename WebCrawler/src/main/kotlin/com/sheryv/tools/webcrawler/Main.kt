@file:JvmName("MainLauncher")

package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.view.MainView
import com.sheryv.tools.webcrawler.view.downloader.DownloaderView
import com.sheryv.tools.webcrawler.view.jdownloader.JDownloaderView
import com.sheryv.tools.webcrawler.view.search.SearchView
import com.sheryv.util.fx.core.app.App
import com.sheryv.util.fx.lib.*
import com.sheryv.util.logging.log
import item
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import menu
import org.koin.core.module.dsl.factoryOf
import java.lang.management.ManagementFactory
import javax.swing.SwingUtilities


fun main(args: Array<String>) {
  println("Main init at " + (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().startTime))
//  System.setProperty("logback.debug", "true")

//  -Dprism.text=t2k
//  "-Djavafx.autoproxy.disable=true"
//  "-Djavafx.verbose=true"
//  System.setProperty("flatlaf.useWindowDecorations", "false")

  
  App.createAndStart<MainView>(args) {
    module {
      factoryOf(::MainView)
      factoryOf(::DownloaderView)
      factoryOf(::JDownloaderView)
      factoryOf(::SearchView)
      single(createdAtStart = true) { Configuration.get() }
    }
  }

//
//  Platform.startup {
//    log.debug("App init at ${ManagementFactory.getRuntimeMXBean().uptime}")
//
//    val root = VBox().apply {
//      paddingAll = 10.0
//      spacing = 10.0
//      isFillWidth = true
//      background = Background(BackgroundFill( Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY))
//
//      hbox {
//        isFillHeight = true
//        prefHeight = 30.0
//        spacing = 3.0
//
//        label("Title"){
//          hgrow = Priority.ALWAYS
//          maxWidth = Double.MAX_VALUE
//        }
//        button("_")
//        button("□")
//        button("x")
//      }
//
//      menubar {
//        menu("File") {
//          item("cvbvcb")
//        }
//        menu("Help") {
//          item("cvbvcb")
//        }
//      }
//
//      textfield("xzcvcxv")
//      button("123213")
//      progressbar(0.2)
//      tabpane {
//        tab("asdasd")
//        tab("vbcbv")
//      }
//      slider {
//
//      }
//    }
//    val stage = Stage()
//    stage.title = "Title"
//    stage.initStyle(StageStyle.UNIFIED)
//    val scene = Scene(root, 1000.0, 400.0)
//    scene.fill = Color.TRANSPARENT
//    stage.scene = scene
//    log.debug("App view created at ${ManagementFactory.getRuntimeMXBean().uptime}")
//
//    stage.show()
//    log.debug("App visible at ${ManagementFactory.getRuntimeMXBean().uptime}")
//  }
//

//  runBlocking {
//    M3U8Downloader.add(
//      "https://delivery-node-ynug3prrg0f4gget.voe-network.net/engine/hls2/01/04356/60nycdrz7ejk_n/index-v1-a1.m3u8?t=y9670yLb2B72Z3nCnXzqZaUIVzHzx-hp2ktAc0gyhrE&s=1681756394&e=14400&f=21781206&node=delivery-node-ynug3prrg0f4gget.voe-network.net&i=77.65&sp=4500&asn=212163\n",
////      "http://sample.vodobox.net/skate_phantom_flex_4k/veryhigh/skate_phantom_flex_4k_1056_480p.m3u8",
//      Path.of("D:\\test").resolve("${(System.currentTimeMillis() / 1000)}.ts")
//    )
//    M3U8Downloader.startScheduler()
//
//
//    while (M3U8Downloader.inProgress.isNotEmpty() || M3U8Downloader.queue.isNotEmpty()) {
//      M3U8Downloader.inProgress.forEach {
//        if (it.started) {
//          val p = it.progress()
//          val ratio = "%2.2f".format(p.currentRatio)
//          log.debug(
//            "Progress ${it.fileName()} [${it.id}] >> ${p.formatDownloaded().padStart(7)} $ratio% | speed ${p.formatSpeed()}"
//          )
//        }
//      }
//      delay(200)
//    }
//  }
//  M3U8Downloader.stopScheduler()
}


/*

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
val json =  Json { encodeDefaults = true }

@Serializable
data class Ms(val name: String, val mi: Mi)

@Serializable
data class Mi(val tag: String, val b: Boolean = true, val nnn: String? = null, val inte: Int = 2, val dub: Double = 1.2)

inline fun <reified T> toMap(obj: T): Map<String, Any?> {
  return jsonObjectToMap((json.encodeToJsonElement(obj) as JsonObject))
}

fun jsonObjectToMap(element: JsonObject): Map<String, Any?> {
  return element.entries.associate {
    it.key to extractValue(it.value)
  }
}

private fun extractValue(element: JsonElement): Any? {
  return when (element) {
    is JsonNull -> null
    is JsonPrimitive -> {
      if (element.isString)
        element.content
      else {
        element.doubleOrNull?.let {
          if (floor(it) == it){
            it.toLong()
          }else{
            it
          }
          
        } ?: element.booleanOrNull
      }
    }
    
    is JsonArray -> element.map { extractValue(it) }
    is JsonObject -> jsonObjectToMap(element)
  }
}

val i = Ms("zxczx", Mi("taggy", true))
val js = json.encodeToJsonElement(i) as JsonObject
val map = toMap(i)
val s = js.toString()
val frommap = json.encodeToJsonElement(map)
val i2 = json.decodeFromJsonElement<Ms>(frommap)
*/
