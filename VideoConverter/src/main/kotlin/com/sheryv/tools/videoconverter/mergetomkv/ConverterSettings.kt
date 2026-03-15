package com.sheryv.tools.videoconverter.mergetomkv

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.fx.lib.observable
import com.sheryv.util.fx.lib.observablePath
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class ConverterSettings(sources: List<SourceSettings> = listOf<SourceSettings>()) {
  var workingDir: Path = Path.of("").toAbsolutePath()
    private set
  var outputDir: Path = Path.of("output")
    private set
  var videoPathPattern: String = ".*"
    private set
  var directorySearchDepth: Int = 4
    private set
  var parallelProcessing: Int = 2
    private set
  var examplePath: String = "Example file S01E01.mp4"
    private set
  
  var sources: ObservableList<SourceSettings> = FXCollections.observableArrayList(sources)
  
  @JsonIgnore
  val workingDirProperty = observablePath(ConverterSettings::workingDir)
  
  @JsonIgnore
  val outputDirProperty = observablePath(ConverterSettings::outputDir)
  
  @JsonIgnore
  val videoPathPatternProperty = observable(ConverterSettings::videoPathPattern)
  
  @JsonIgnore
  val directorySearchDepthProperty = observable(ConverterSettings::directorySearchDepth)
  
  @JsonIgnore
  val examplePathProperty = observable(ConverterSettings::examplePath)
  
  fun copy(): ConverterSettings {
    val map = SerialisationUtils.jsonMapper.convertValue(this, Map::class.java)
    return SerialisationUtils.jsonMapper.convertValue(map, ConverterSettings::class.java)
  }
  
  fun save() {
    SerialisationUtils.toJson(currentPath.toFile(), this)
  }
  
  companion object {
    fun read(): ConverterSettings {
      val settings: ConverterSettings
      if (Files.exists(currentPath)) {
        settings = SerialisationUtils.fromJson(currentPath.toFile(), ConverterSettings::class.java)
      } else {
        settings = ConverterSettings()
        settings.save()
      }
      while (settings.sources.size < SOURCES_NUMBER) {
        settings.sources.add(SourceSettings())
      }
      while (settings.sources.size > SOURCES_NUMBER) {
        settings.sources.removeLast()
      }
      return settings
    }
    
    const val SOURCES_NUMBER = 8
    
    var currentPath: Path = Path.of("config.json")
  }
}


class SourceSettings(
  var pathPattern: String = "",
  var defaultTimeOffset: Double = 0.0,
  var language: String = "",
  var textFileEncoding: String = StandardCharsets.UTF_8.name(),
) {
  @JsonIgnore
  val pathPatternProperty = observable(SourceSettings::pathPattern)
  
  @JsonIgnore
  val defaultTimeOffsetProperty = observable(SourceSettings::defaultTimeOffset)
  
  @JsonIgnore
  val languageProperty = observable(SourceSettings::language)
  
  @JsonIgnore
  val textFileEncodingProperty = observable(SourceSettings::textFileEncoding)
  
  override fun toString(): String {
    return "[$pathPattern] $defaultTimeOffset, $language, $textFileEncoding"
  }
}
