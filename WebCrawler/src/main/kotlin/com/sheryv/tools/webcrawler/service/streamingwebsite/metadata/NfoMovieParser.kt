package com.sheryv.tools.webcrawler.service.streamingwebsite.metadata

import com.sheryv.util.logging.log
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class NfoMovieParser {
  
  fun parse(path: Path): NfoMovie? {
    return parse(Files.readString(path))
  }
  
  fun parse(xmlString: String): NfoMovie? {
    try {// 1. Initialize the Document Builder
      val factory = DocumentBuilderFactory.newInstance()
      val builder = factory.newDocumentBuilder()
      
      // 2. Parse the input string into a Document
      val input = ByteArrayInputStream(xmlString.toByteArray())
      val doc = builder.parse(input)
      
      // 3. Normalize the XML structure
      doc.documentElement.normalize()
      
      // 4. Extract helper function to get text content by tag name
      fun getTagValue(tagName: String): String {
        return doc.getElementsByTagName(tagName).item(0).textContent
      }
      
      // 5. Handle the element with attributes (uniqueid)
//    val uniqueIdNode = doc.getElementsByTagName("uniqueid").item(0) as Element
//    val uniqueId = uniqueIdNode.textContent
//    val uniqueIdType = uniqueIdNode.getAttribute("type")
      
      return NfoMovie(
        title = getTagValue("title"),
        originalTitle = getTagValue("originaltitle"),
        year = getTagValue("year").toInt(),
        tmdbId = getTagValue("tmdbid").toInt(),
      )
    } catch (e: Exception) {
      log.error("Error parsing movie xml", e)
    }
    return null
  }
}

data class NfoMovie(
  val title: String,
  val originalTitle: String,
  val year: Int,
  val tmdbId: Int,
)
