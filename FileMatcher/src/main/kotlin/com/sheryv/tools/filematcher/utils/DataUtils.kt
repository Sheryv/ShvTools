package com.sheryv.tools.filematcher.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.nio.channels.Channels
import javax.net.ssl.HttpsURLConnection

object DataUtils {
  fun downloadFile(urlStr: String, file: File, isCancelled: () -> Boolean = { false }): File? {
    if (file.exists())
      file.delete()
    
    lg().info("Downloading ${file.name} from: $urlStr")
    var cancelled = false
    try {
      val url = URL(urlStr)
      val conn = url.openConnection()
      Channels.newChannel(conn.inputStream).use { rbc ->
        FileOutputStream(file).use { fos ->
          val len = 1024 * 1024
          var pos: Long = 0
          var done: Long
          
          do {
            if (isCancelled.invoke()) {
              cancelled = true
              break
            }
  
            done = fos.channel.transferFrom(rbc, pos, java.lang.Long.MAX_VALUE)
            pos += done
          } while (done >= len)
          lg().debug("Download finished ${file.name}")
        }
      }
    } finally {
      if (cancelled) {
        lg().warn("Downloading cancelled. Unfinished file '${file.absolutePath}' will be deleted")
        file.delete()
        return null
      }
    }
    return file
  }
  
  fun downloadText(url: String): String? {
    val response = StringBuilder()
    val website = URL(url)
    val connection = website.openConnection()
    BufferedReader(InputStreamReader(connection.getInputStream())).use { read ->
      var inputLine: String?
      inputLine = read.readLine()
      while (inputLine != null) {
        response.appendln(inputLine)
        inputLine = read.readLine()
      }
    }
    return response.toString()
  }
  
  fun <T> downloadAndParse(url: String, output: Class<T>): T {
    val indexOf = url.lastIndexOf('.')
    val extension = url.substring(indexOf + 1)
    
    val connection = URL(url).openConnection()
    val type = connection.getHeaderField("Content-Type")
    val mapper = if (type != null && type.contains("application/json") || extension == "json") {
      jsonMapper()
    } else {
      yamlMapper()
    }
    
    return mapper.readValue(BufferedInputStream(connection.getInputStream()), output)
  }
  
  fun isAbsoluteUrl(url: String) = url.startsWith("http:") || url.startsWith("https:")
  
  fun jsonMapper(): ObjectMapper {
    val map = ObjectMapper()
    map.configure(SerializationFeature.INDENT_OUTPUT, true)
    map.registerModule(KotlinModule())
    map.registerModule(JavaTimeModule())
    map.dateFormat = StdDateFormat()
    map.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    return map
  }
  
  fun yamlMapper(): ObjectMapper {
    val map = ObjectMapper(YAMLFactory())
    map.registerModule(KotlinModule())
    map.registerModule(JavaTimeModule())
    map.dateFormat = StdDateFormat()
    map.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    return map
  }
}