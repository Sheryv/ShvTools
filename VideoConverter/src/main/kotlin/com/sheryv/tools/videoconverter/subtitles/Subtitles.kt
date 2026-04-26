package com.sheryv.tools.videoconverter.subtitles

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.math.min
import kotlin.streams.asSequence


interface Subtitles {
  fun getStatements(): List<SubtitleStatement>
  
  fun render(target: Appendable): Appendable
  
  fun renderWithoutTimestamps(target: Appendable): Appendable
  fun replaceTextFrom(newTexts: List<SubtitleStatement>): Pair<List<SubtitleStatement>, List<SubtitleStatement>>
  fun splitInParts(maxRecordsPerPart: Int): List<Subtitles>
  fun append(subtitles: List<Subtitles>)
  fun copy(): Subtitles
  
  val type: String
  
  companion object {
    fun parse(file: Path): Subtitles {
      return parse(Files.lines(file).asSequence(), file.extension)
    }
    
    fun parse(text: String, type: String = "srt"): Subtitles {
      return parse(text.lineSequence(), type)
    }
    
    fun parse(lines: Sequence<String>, type: String = "srt"): Subtitles {
      if (type == "srt") {
        return SrtSubtitlesParser().parse(lines)
      } else {
        throw RuntimeException("Subtitle type $type is not supported")
      }
    }
    
    fun merge(subtitles: List<Subtitles>): Subtitles? {
      if (subtitles.isEmpty()) {
        return null
      } else if (subtitles.size == 1) {
        return subtitles.first().copy()
      } else {
        val result = subtitles.first().copy()
        result.append(subtitles.drop(1))
        return result
      }
    }
  }
}

data class SubtitleStatement(val index: Int, val startTime: String, val endTime: String, val text: String)

private val TIMECODE_REGEX = Regex("""(\d{2}:\d{2}:\d{2},\d{3}) --> (\d{2}:\d{2}:\d{2},\d{3})""")

private class SrtSubtitlesParser() : Subtitles {
  constructor(allStatements: List<SubtitleStatement>) : this() {
    this.allStatements = allStatements
  }
  
  override val type: String = "srt"
  
  lateinit var allStatements: List<SubtitleStatement>
  
  fun parse(lines: Sequence<String>): Subtitles {
    val subtitles = mutableListOf<SubtitleStatement>()
    
    var currentIndex: Int? = null
    var startTime: String? = null
    var endTime: String? = null
    val textBuilder = StringBuilder()
    
    
    lines.forEachIndexed { index, lineRaw ->
      var line = lineRaw.trim()
      if (index == 0) {
        val bytes = line.encodeToByteArray()
        //BOM
        if (bytes.size > 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
          line = line.substring(1)
        }
      }
      when {
        // 1. Look for the Index
        line.toIntOrNull() != null && currentIndex == null -> {
          currentIndex = line.toInt()
        }
        
        // 2. Look for Timecodes
        TIMECODE_REGEX.matches(line) -> {
          val match = TIMECODE_REGEX.find(line)
          startTime = match?.groupValues?.get(1)
          endTime = match?.groupValues?.get(2)
        }
        
        // 3. Look for the Text (collecting until an empty line)
        line.isNotEmpty() && currentIndex != null -> {
          if (textBuilder.isNotEmpty()) textBuilder.append("\n")
          textBuilder.append(line)
        }
        
        // 4. End of a subtitle block
        line.isEmpty() && currentIndex != null -> {
          subtitles.add(
            SubtitleStatement(
              currentIndex,
              startTime ?: "",
              endTime ?: "",
              textBuilder.toString().trim()
            )
          )
          // Reset for next block
          currentIndex = null
          startTime = null
          endTime = null
          textBuilder.clear()
        }
      }
    }
    
    // Catch the last subtitle if the file doesn't end with a newline
    if (currentIndex != null) {
      subtitles.add(SubtitleStatement(currentIndex, startTime ?: "", endTime ?: "", textBuilder.toString()))
    }
    
    allStatements = subtitles
    return this
  }
  
  override fun getStatements(): List<SubtitleStatement> = allStatements
  
  override fun render(target: Appendable): Appendable {
    for (st in allStatements) {
      target
        .appendLine(st.index.toString())
        .append(st.startTime).append(" --> ").appendLine(st.endTime)
        .appendLine(st.text.trim())
        .appendLine()
    }
    return target
  }
  
  override fun renderWithoutTimestamps(target: Appendable): Appendable {
    for (st in allStatements) {
      target
        .appendLine(st.index.toString())
        .appendLine(st.text.trim())
        .appendLine()
    }
    return target
  }
  
  override fun replaceTextFrom(newTexts: List<SubtitleStatement>): Pair<List<SubtitleStatement>, List<SubtitleStatement>> {
    val missing = allStatements.filter { c -> newTexts.none { it.index == c.index } }
    val excess = newTexts.filter { c -> allStatements.none { it.index == c.index } }
    
    allStatements = allStatements
      .map { c -> newTexts.firstOrNull { it.index == c.index }?.let { c.copy(text = it.text) } ?: c }
    
    return missing to excess
  }
  
  override fun splitInParts(maxRecordsPerPart: Int): List<Subtitles> {
    val statements = mutableListOf<List<SubtitleStatement>>()
    var index = 0
    while (index < allStatements.size) {
      statements.add(allStatements.subList(index, min(index + maxRecordsPerPart, allStatements.size)))
      index += maxRecordsPerPart
    }
    
    return statements.map { SrtSubtitlesParser(it) }
  }
  
  
  override fun append(subtitles: List<Subtitles>) {
    require(subtitles.all { it.type == type }) { "All subtitles must be of type $type" }
    
    val result = ArrayList(allStatements)
    subtitles.forEach { result.addAll(it.getStatements()) }
    allStatements = result
  }
  
  override fun copy(): Subtitles = SrtSubtitlesParser(allStatements)
  
  override fun toString(): String {
    return "Subtitles(type=${type}, entries=${allStatements.size})"
  }
}

