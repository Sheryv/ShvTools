package com.sheryv.tools.videoconverter.subtitles

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.beEmpty
import org.junit.jupiter.api.Test
import java.nio.file.Path

class SrtSubtitlesParserTest {
  @Test
  fun render() {
    val text = readFile("subtitles.srt")
    val parsed = Subtitles.parse(text)
    
    text shouldNot beEmpty()
    parsed.render(StringBuilder()).toString().trimEnd() shouldBe text.trimEnd()
  }
  
  private fun readFile(file: String): String {
    return Thread.currentThread().contextClassLoader.getResourceAsStream(file).use { stream ->
      stream!!.bufferedReader().readText()
    }
  }
}
