package com.sheryv.tools.videoconverter.video

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.fx.lib.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.nio.file.Files
import java.nio.file.Path

class MainSettings(sources: List<SourceSettings> = listOf<SourceSettings>()) {
  internal constructor(
    workingDir: Path,
    outputDir: Path,
//    videoPathPattern: String,
    directorySearchDepth: Int,
    parallelProcessing: Int,
    outputExtension: String,
    languageFilter: List<String>,
  ) : this() {
    this.workingDir = workingDir
    this.outputDir = outputDir
//    this.videoPathPattern = videoPathPattern
    this.directorySearchDepth = directorySearchDepth
    this.parallelProcessing = parallelProcessing
    this.outputExtension = outputExtension
    this.languageFilter = languageFilter
  }
  
  var workingDir: Path = Path.of("").toAbsolutePath()
    private set
  var outputDir: Path = Path.of("output")
    private set
//  var videoPathPattern: String = ".*"
//    private set
  var directorySearchDepth: Int = 4
    private set
  var parallelProcessing: Int = 2
    private set
  var examplePath: String = "Example file S01E01.mp4"
    private set
  var outputExtension: String = "mkv"
    private set
  var languageFilter: List<String> = emptyList()
    private set
  
  var sources: ObservableList<SourceSettings> = FXCollections.observableArrayList(sources)
  
  @JsonIgnore
  var additionalSources: ObservableList<SourceSettings> = this.sources.filtered { !it.main }
  
  @JsonIgnore
  val workingDirProperty = observablePath(MainSettings::workingDir)
  
  @JsonIgnore
  val outputDirProperty = observablePath(MainSettings::outputDir)
  
//  @JsonIgnore
//  val videoPathPatternProperty = observable(MainSettings::videoPathPattern)
  
  @JsonIgnore
  val directorySearchDepthProperty = observable(MainSettings::directorySearchDepth)
  
  @JsonIgnore
  val examplePathProperty = observable(MainSettings::examplePath)
  
  @JsonIgnore
  val languageFilterProperty = observable(MainSettings::languageFilter)
  
  @JsonIgnore
  val mainSourceProperty = listProperty(this.sources).mapObservable { it.first { it.main } }
  
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
        settings.sources.add(SourceSettings())
      }
      while (settings.sources.size > SOURCES_NUMBER) {
        settings.sources.removeLast()
      }
      if (settings.sources.none { it.main }) {
        settings.sources.add(0, SourceSettings(main = true, pathPattern = ".*", type = SourceType.ALL))
      }
      return settings
    }
    
    const val SOURCES_NUMBER = 7
    
    var currentPath: Path = Path.of("config.json")
  }
}


class SourceSettings(
  type: SourceType = SourceType.AUDIO,
  pathPattern: String = "",
  defaultTimeOffset: Double = 0.0,
  language: String = "",
  audioType: SourceAudioType = SourceAudioType.NOT_SPECIFIED,
  streamSelection: StreamSelection = StreamSelection.ALL,
  val main: Boolean = false,
) {
  var enabled = true
    private set
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
  var streamSelection = streamSelection
    private set
  
  @JsonIgnore
  val enabledProperty = observable(SourceSettings::enabled)
  
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
  
  @JsonIgnore
  val streamSelectionProperty = observable(SourceSettings::streamSelection)
  
  @JsonIgnore
  val mainProperty = staticProperty(main)
  
  override fun toString(): String {
    return "$type [$pathPattern] $defaultTimeOffset, $language, $audioType"
  }
}
