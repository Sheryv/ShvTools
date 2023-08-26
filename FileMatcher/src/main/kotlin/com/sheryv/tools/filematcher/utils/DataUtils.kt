package com.sheryv.tools.filematcher.utils

import com.sheryv.util.io.FileUtils
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.logging.log
import java.io.*
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.channels.Channels
import java.time.Duration
import java.util.*
import java.util.regex.Pattern


object DataUtils {
  private val YAML_BLOCK_ENDS = listOf(" |", " >", "\\")
  private val YAML_IGNORED_LINES = listOf("#", "--")
  private val YAML_PATTERN_FOR_FIELD_NAME = Pattern.compile("""^((- )|(- \{\s*))?['"]?(?<field>\w+)['"]?:""")
  val PROPS_CACHE = mutableMapOf<String, Map<String, String>>()
  
  fun downloadFile(urlStr: String, file: File, isCancelled: () -> Boolean = { false }): File? {
    if (file.exists())
      file.delete()
    
    log.info("Downloading ${file.name} from: $urlStr")
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
          log.debug("Download finished ${file.name}")
        }
      }
    } finally {
      if (cancelled) {
        log.warn("Downloading cancelled. Unfinished file '${file.absolutePath}' will be deleted")
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
        response.appendLine(inputLine)
        inputLine = read.readLine()
      }
    }
    return response.toString()
  }
  
  fun <T> downloadAndParseOld(url: String, output: Class<T>): T {
    log.info("Downloading text from: $url")
    val indexOf = url.lastIndexOf('.')
    val extension = url.substring(indexOf + 1)
    
    val connection = URL(url).openConnection()
    val type = connection.getHeaderField("Content-Type")
    val mapper = if (type != null && type.contains("application/json") || extension == "json") {
      jsonMapper()
    } else {
      yamlMapper()
    }
    
    val result = mapper.readValue(BufferedInputStream(connection.getInputStream()), output)
    log.debug("Downloaded and mapped text from: $url")
    return result
  }
  
  fun <T> downloadAndParse(url: String, output: Class<T>): T {
    log.info("Downloading text from: $url")
    val t = System.currentTimeMillis()
    val indexOf = url.lastIndexOf('.')
    val extension = url.substring(indexOf + 1)
    
    val request: HttpRequest = HttpRequest.newBuilder()
      .uri(URI(url))
      .timeout(Duration.ofSeconds(30))
      .GET()
      .build()
    
    val client = HttpClient.newHttpClient()
    val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
    
    val json = response.headers().firstValue("Content-Type")
      .map { it.contains("application/json") }
      .orElse(extension == "json")
    val mapper = if (json) {
      jsonMapper()
    } else {
      yamlMapper()
    }
    
    println("before mapping " + (System.currentTimeMillis() - t))
    val result = mapper.readValue(response.body(), output)
    println("after mapping " + (System.currentTimeMillis() - t))
    log.debug("Downloaded and mapped text from: $url")
    return result
  }
  
  fun isAbsoluteUrl(url: String) = url.startsWith("http:") || url.startsWith("https:")
  
  fun buildUrlFromBase(part: String? = null, base: String? = null): String? {
    var res = base?.trim('/')?.takeIf { part == null || !isAbsoluteUrl(part) }
    if (!part.isNullOrBlank()) {
      res = (res?.plus("/") ?: "") + part.trim('/')
    }
    return res
  }
  
  fun jsonMapper() = SerialisationUtils.jsonMapper
  
  fun yamlMapper() = SerialisationUtils.yamlMapper
  
  fun appendCommentsToYamlFile(
    input: File,
    output: File,
    comments: Map<String, String>,
    allowCommentsRepeating: Boolean = true
  ) {
    check(input != output) { "Input and output cannot be the same file" }
    appendCommentsToYaml(
      FileUtils.readFileStream(input.toPath()),
      BufferedWriter(OutputStreamWriter(FileOutputStream(output))),
      comments,
      allowCommentsRepeating
    )
  }
  
  fun appendCommentsToYaml(
    reader: BufferedReader,
    writer: BufferedWriter,
    comments: Map<String, String>,
    allowCommentsRepeating: Boolean = true
  ) {
    val path = ArrayList<String>(10)
    val comments = comments.toMutableMap()
    writer.use { out ->
      var indentSize = 0
      var lastIndent = 0
      var inBlock = false
      reader.use { buff ->
        buff.lines().forEach { line ->
          val trimmed = line.trim()
          if (YAML_IGNORED_LINES.any { trimmed.startsWith(it) }) {
            out.appendLine(line)
            return@forEach
          }
          
          val currIndent = line.takeWhile { it == ' ' || it == '-' }.length
          if (indentSize == 0) {
            indentSize = currIndent
          }
          val indentIndex = if (indentSize == 0) 0 else currIndent / indentSize
          
          if (inBlock && indentIndex > lastIndent) {
            out.appendLine(line)
            return@forEach
          }
          if (inBlock) {
            inBlock = false
          }
          
          val matcher = YAML_PATTERN_FOR_FIELD_NAME.matcher(trimmed)
          if (matcher.find()) {
            val field = matcher.group("field")
            path.addOrSetWithFill(indentIndex, field, "")
            
            if (indentIndex < lastIndent) {
              do {
                path.removeLast()
              } while (path.size > indentIndex + 1)
            }
            
            lastIndent = indentIndex
            val key = path.joinToString("") { if (it.isEmpty()) "" else "$it." }.trimEnd('.')
//        println("Match: ${key.padStart(50)} | $line")
            if (comments.containsKey(key)) {
              val commentIndent = " ".repeat(indentIndex * indentSize)
              comments[key]!!.lines().forEach { comment ->
                out
                  .append(commentIndent)
                  .append("# ")
                  .appendLine(comment)
                
                if (!allowCommentsRepeating) {
                  comments.remove(key)
                }
//            println("  Comment: " + comment)
              }
            }
            
            if (YAML_BLOCK_ENDS.any { trimmed.endsWith(it) }) {
              inBlock = true
            }
          }
          out.appendLine(line)
        }
      }
    }
  }
  
  fun loadPropsFromResources(relativePath: String): Map<String, String> {
    val p = Properties()
    p.load(javaClass.classLoader.getResourceAsStream(relativePath))
    return p.map { it.key.toString() to it.value.toString() }.toMap()
  }
  
  private fun <E> MutableList<E>.addOrSetWithFill(index: Int, e: E, fill: E) {
    when {
      this.size == index -> this.add(e)
      this.size > index -> this[index] = e
      else -> {
        do {
          this.add(fill)
        } while (this.size < index)
        this.add(e)
      }
    }
  }
}
