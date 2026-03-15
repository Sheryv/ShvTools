@file:JvmName("MainLauncher")

package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.view.MainView
import com.sheryv.tools.webcrawler.view.downloader.DownloaderView
import com.sheryv.tools.webcrawler.view.jdownloader.JDownloaderView
import com.sheryv.tools.webcrawler.view.search.SearchView
import com.sheryv.tools.webcrawler.view.remoteclient.HttpServerView
import com.sheryv.util.fx.core.app.App
import com.sheryv.util.fx.core.app.AppConfiguration
import org.koin.core.module.dsl.factoryOf
import java.lang.management.ManagementFactory


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
      factoryOf(::HttpServerView)
      single(createdAtStart = true) { Configuration.get() }
      single<AppConfiguration> { Configuration.get() }
    }
  }
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
