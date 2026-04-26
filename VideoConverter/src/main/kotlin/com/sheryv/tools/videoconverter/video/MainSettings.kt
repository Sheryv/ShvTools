package com.sheryv.tools.videoconverter.video

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.fx.lib.observable
import com.sheryv.util.fx.lib.observablePath
import com.sheryv.util.fx.lib.staticProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.nio.file.Files
import java.nio.file.Path

class MainSettings(sources: List<SourceSettings> = listOf<SourceSettings>()) {
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
  var outputExtension: String = "mkv"
    private set
  
  var sources: ObservableList<SourceSettings> = FXCollections.observableArrayList(sources)
  
  @JsonIgnore
  val workingDirProperty = observablePath(MainSettings::workingDir)
  
  @JsonIgnore
  val outputDirProperty = observablePath(MainSettings::outputDir)
  
  @JsonIgnore
  val videoPathPatternProperty = observable(MainSettings::videoPathPattern)
  
  @JsonIgnore
  val directorySearchDepthProperty = observable(MainSettings::directorySearchDepth)
  
  @JsonIgnore
  val examplePathProperty = observable(MainSettings::examplePath)
  
  fun copy(): MainSettings {
    val map = SerialisationUtils.jsonMapper.convertValue(this, Map::class.java)
    return SerialisationUtils.jsonMapper.convertValue(map, MainSettings::class.java)
  }
  
  fun save() {
    SerialisationUtils.toJson(currentPath.toFile(), this)
  }
  
  companion object {
    fun read(): MainSettings {
      val settings: MainSettings
      if (Files.exists(currentPath)) {
        settings = SerialisationUtils.fromJson(currentPath.toFile(), MainSettings::class.java)
      } else {
        settings = MainSettings()
        settings.save()
      }
      while (settings.sources.size < SOURCES_NUMBER) {
        settings.sources.add(SourceSettings(settings.sources.size + 1))
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
  val index: Int,
  type: SourceType = SourceType.SUBTITLES,
  pathPattern: String = "",
  defaultTimeOffset: Double = 0.0,
  language: String = "",
  audioType: SourceAudioType = SourceAudioType.DUBBING,
) {
  var type = type
    private set
  var pathPattern = pathPattern
    private set
  var defaultTimeOffset = defaultTimeOffset
    private set
  var language = language
    private set
  var audioType = audioType
    private set
  
  @JsonIgnore
  val indexProperty = staticProperty(index)
  
  @JsonIgnore
  val typeProperty = observable(SourceSettings::type)
  
  @JsonIgnore
  val pathPatternProperty = observable(SourceSettings::pathPattern)
  
  @JsonIgnore
  val defaultTimeOffsetProperty = observable(SourceSettings::defaultTimeOffset)
  
  @JsonIgnore
  val languageProperty = observable(SourceSettings::language)
  
  @JsonIgnore
  val audioTypeProperty = observable(SourceSettings::audioType)
  
  override fun toString(): String {
    return "#${indexProperty.value} $type [$pathPattern] $defaultTimeOffset, $language, $audioType"
  }
}
