package com.sheryv.tools.videoconverter.subtitles

import com.sheryv.tools.videoconverter.ai.GeminiClient
import com.sheryv.util.CoreUtils
import com.sheryv.util.Strings
import com.sheryv.util.logging.log
import kotlinx.coroutines.delay
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.time.DurationUnit
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

class SubtitlesTranslator(val apiKey: String) {
  
  
  suspend fun translateWithGemini(
    file: Path,
    mediaTitle: String = "",
    mediaType: MediaType = MediaType.TV_SHOW,
    year: Int? = null,
    language: String = "Polish"
  ): Result? {
    val client = GeminiClient(apiKey)
    val variables = mapOf(
      "type" to mediaType,
      "lang" to language,
      "name" to ": $mediaTitle",
      "year" to year?.let { " ($it)" }?.takeIf { mediaTitle.isNotEmpty() }.orEmpty()
    )
    val prompt = Strings.getTemplater(variables, "&{", "}").replace(PROMPT)
    val englishSubtitles = Subtitles.parse(file)
    if (englishSubtitles.getStatements().isEmpty()) {
      log.error("File {} contains no valid subtitles", file.absolutePathString())
      return null
    }
    
    val parts = englishSubtitles.splitInParts(100)
    val missing = mutableListOf<Int>()
    val excess = mutableListOf<Int>()
    
    log.info("Translating {} parts of {} '{}' to {} | {}", parts.size, mediaType, mediaTitle, language, file.absolutePathString())
    parts.forEachIndexed { index, part ->
      
      var repeats = 5
      var httpFailureRepeats = 5
      var httpFailure = false
      do {
        try {
          log.debug(
            "Sending request for part {}, entries {}-{} of {}",
            index,
            part.getStatements().first().index,
            part.getStatements().last().index,
            file.name
          )
          val output: TimedValue<String> = measureTimedValue {
            client.generate(prompt + "\n\n" + part.renderWithoutTimestamps(StringBuilder()).toString())
            //            .onEach {
            //            log.trace("MODEL OUTPUT: {}", it)
            //          }
          }
          
          val parsed = Subtitles.parse(output.value, part.type).getStatements()
          val (m, e) = part.replaceTextFrom(parsed)
          
          log.debug(
            "From model got {} characters in {} ms for part {} of {} [m: {}, e: {}]",
            output.value.length,
            output.duration.toLong(DurationUnit.MILLISECONDS),
            index,
            file.name,
            m.size,
            e.size
          )
          val del = CoreUtils.calculateBackoffDelay(3 - repeats, 1000)
          if (m.size > 1) {
            log.error(
              "Incorrect response from model: \n========================================================\n{}" +
                  "\n========================================================", output.value
            )
            repeats -= 1
            delay(del)
          } else {
            repeats = 0
          }
          if (repeats > 0) {
            log.info("Repeating incorrect request in {} s", del.inWholeSeconds)
          } else {
            missing.addAll(m.map { it.index })
            excess.addAll(e.map { it.index })
          }
          httpFailure = false
          httpFailureRepeats = 5
          
        } catch (e: Exception) {
          log.error("Error processing part {} of {}", index, file.name, e)
          val del = CoreUtils.calculateBackoffDelay(5 - httpFailureRepeats, 5000)
          delay(del)
          httpFailureRepeats -= 1
          httpFailure = true
          
          if (httpFailureRepeats > 0) {
            log.info("Repeating failed request in {} s", del.inWholeSeconds)
          }
        }
        
      } while ((!httpFailure && repeats > 0) || (httpFailure && httpFailureRepeats > 0))
    }
//    val translated = merged.render().trim()
//    Files.writeString(file.resolveSibling(file.nameWithoutExtension + "[PL]." + file.extension), translated)
    val merged = Subtitles.merge(parts)!!
    log.info("Translated {} entries of {} | Missing: {} | Excess: {}", merged.getStatements().size, file.name, missing.size, excess.size)
    return Result(merged, missing, excess)
  }
  
  data class Result(val translated: Subtitles, val missingEntries: List<Int>, val excessEntries: List<Int>) {
    val isFullyTranslated = missingEntries.isEmpty() && excessEntries.isEmpty()
  }
  
  enum class MediaType(val label: String) {
    TV_SHOW("TV Show"),
    MOVIE("Movie"),
  }
}

private val PROMPT = """
    **Role:** You are a professional subtitle translator specializing in English-to-&{lang} localization for television.
    
    **Task:** Translate the provided English subtitle text into &{lang}. Subtitles come from &{type}&{name}&{year}
    
    **Strict Constraints:**
    
    1.  **Format Preservation:** Keep the index numbers on their own lines exactly as they appear. Don't merge or split lines. Output should have the same line number as input
    2.  **HTML Tags:** Preserve all formatting tags, such as `<i>` and `</i>`.
    3.  **Dialogue Markers:** Keep the dashes `-` used for indicating different speakers.
    4.  **Tone:** Use natural, conversational &{lang} appropriate for a TV drama/procedural.
    5.  **Character Limits:** Try to keep the &{lang} translation concise so it fits comfortably on a screen within a similar timeframe as the English.
    6.  **Output:** Return only translated subtitles.
    
    **Input Text:**
    """.trimIndent()
