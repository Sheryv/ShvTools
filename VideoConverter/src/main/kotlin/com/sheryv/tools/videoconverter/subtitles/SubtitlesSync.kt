package com.sheryv.tools.videoconverter.subtitles

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class SubtitlesSync {
  
  fun syncTimestampsUsingIndices(toSync: Path, reference: Path) {
    val toEdit = Subtitles.parse(toSync)
    val ref = Subtitles.parse(reference)
    
    if (toEdit.getStatements().isEmpty() || ref.getStatements().isEmpty()) {
      throw RuntimeException("Empty subtitles")
    }
    
    ref.replaceTextFrom(toEdit.getStatements())
    val fileName = toSync.nameWithoutExtension + "_synced." + toSync.extension
    Files.newBufferedWriter(toSync.parent?.let { it.resolve(fileName) } ?: Path.of(fileName)).use {
      ref.render(it)
    }
  }
}
