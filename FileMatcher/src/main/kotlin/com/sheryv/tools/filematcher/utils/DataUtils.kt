package com.sheryv.tools.filematcher.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.*
import java.net.URL
import java.nio.channels.Channels
import javax.net.ssl.HttpsURLConnection

object DataUtils {
  fun downloadFile(urlStr: String, file: File): File? {
    if (file.exists())
      file.delete()
    
    var conn: HttpsURLConnection? = null
    try {
      val url = URL(urlStr)
      conn = url.openConnection() as HttpsURLConnection
      Channels.newChannel(conn.inputStream).use { rbc ->
        FileOutputStream(file).use { fos ->
          //conn = (HttpsURLConnection) url.openConnection();
          val len = conn.contentLengthLong
          var done: Long = 0
          do {
            done = fos.channel.transferFrom(rbc, done, java.lang.Long.MAX_VALUE)
          } while (done < len)
        }
      }
    } catch (ex: IOException) {
      ex.printStackTrace()
      return null
    } finally {
      if (conn != null)
        conn.disconnect()
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
      ObjectMapper()
    } else {
      ObjectMapper(YAMLFactory())
    }
    
    return mapper.readValue(BufferedInputStream(connection.getInputStream()), output)
  }
  
  fun isAbsoluteUrl(url: String) = url.startsWith("http:") || url.startsWith("https:")
}