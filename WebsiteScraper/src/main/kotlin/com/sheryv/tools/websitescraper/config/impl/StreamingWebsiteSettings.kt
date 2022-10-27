package com.sheryv.tools.websitescraper.config.impl

import com.sheryv.tools.websitescraper.config.SettingsBase
import com.sheryv.tools.websitescraper.config.impl.streamingwebsite.EpisodeType
import com.sheryv.tools.websitescraper.config.impl.streamingwebsite.VideoServerConfig
import com.sheryv.tools.websitescraper.process.base.ScraperDef
import com.sheryv.tools.websitescraper.process.base.model.Format
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.CommonVideoServers
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.EpisodeTypes
import com.sheryv.tools.websitescraper.view.settings.*

class StreamingWebsiteSettings(
  name: String,
  websiteUrl: String,
  outputPath: String,
  outputFormat: Format,
  val downloadDir: String,
  val seriesName: String = "",
  val seasonNumber: Int = 1,
  val seriesUrl: String = "",
  var tmdbKey: String? = null,
  val idmExePath: String? = null,
  val searchStartIndex: Int = 1,
  val searchStopIndex: Int = -1,
  val episodeCodeFormatter: String = "\${series_name} S\${season}E\${episode_number}",
  val episodeNameFormatter: String = " - \${episode_name}\${file_extension}",
  val triesBeforeStreamingProviderChange: Int = 3,
  val numOfTopStreamingProvidersUsedSimultaneously: Int = 3,
  val allowedEpisodeTypes: List<EpisodeType> = EpisodeTypes.values().map { EpisodeType(it) },
  videoServerConfigs: List<VideoServerConfig> = emptyList()
) : SettingsBase(name, websiteUrl, outputPath, outputFormat) {
  
  val videoServerConfigs: List<VideoServerConfig> = videoServerConfigs
    get() = field.ifEmpty { defaultHostingsList() }
  
  
  override fun buildSettingsPanelDef(): Pair<List<SettingsViewRow<*>>, SettingsPanelReader> {
    val savePathRow = TextInputSettingsRow("Intermediate file path", outputPath)
    val seriesNameRow = TextInputSettingsRow("Series name", seriesName)
    val seriesNumberRow = NumberRangeSettingRow("Season number", seasonNumber, 1, 100, showSlider = false)
    val seriesUrlRow = TextInputSettingsRow("Series URL address (can be relative)", seriesUrl)
    val download = TextInputSettingsRow("Download directory", downloadDir)
    val searchStart = NumberRangeSettingRow("Search start index", searchStartIndex, 1, 10000)
    val searchStop = NumberRangeSettingRow("Search stop index (set -1 to disable)", searchStopIndex, -1, 10000)
    val tmdb = TextInputSettingsRow("API key for TMDB database (themoviedb.org)", tmdbKey.orEmpty())
    val idm = TextInputSettingsRow("Path to IDM exe", idmExePath.orEmpty())
    val triesBeforeSwitch =
      NumberRangeSettingRow("Number of tries before switch for each streaming provider", triesBeforeStreamingProviderChange, 1, 51)
    val parallelProviders =
      NumberRangeSettingRow("Number of streaming provider used simultaneously", numOfTopStreamingProvidersUsedSimultaneously, 1, 51)
    val codeTemplate = TextInputSettingsRow("Episode code template", episodeCodeFormatter)
    val nameTemplate = TextInputSettingsRow("Episode name template", episodeNameFormatter)
    val providers = TableSettingsRow(
      "Streaming providers order (Drag and drop to change)",
      videoServerConfigs.map { TableSettingsRow.RowDefinition(listOf(it.name, it.searchName), it.enabled) },
      listOf("Name", "Search term")
    )
    val episodeTypes = TableSettingsRow(
      "Allowed audio type (Drag and drop to change order)",
      allowedEpisodeTypes.map { TableSettingsRow.RowDefinition(listOf(it.kind.toString().lowercase()), it.enabled) },
      listOf("Name")
    )
    return Pair(
      listOf(
        seriesNameRow,
        seriesNumberRow,
        seriesUrlRow,
        savePathRow,
        download,
        searchStart,
        searchStop,
        tmdb,
        idm,
        triesBeforeSwitch,
        parallelProviders,
        codeTemplate,
        nameTemplate,
        providers,
        episodeTypes
      )
    ) {
      val types = episodeTypes.readValue()
      val provs = providers.readValue()
      StreamingWebsiteSettings(
        name, websiteUrl, savePathRow.readValue(), outputFormat,
        download.readValue(),
        seriesNameRow.readValue(),
        seriesNumberRow.readValue(),
        seriesUrlRow.readValue(),
        tmdb.readValue(),
        idm.readValue(),
        searchStart.readValue(),
        searchStop.readValue(),
        codeTemplate.readValue(),
        nameTemplate.readValue(),
        triesBeforeSwitch.readValue(),
        parallelProviders.readValue(),
        allowedEpisodeTypes.map { e -> e.copy(enabled = types.first { it.cells.first() == e.kind.toString().lowercase() }.enabled) },
        videoServerConfigs.map { s -> s.copy(enabled = provs.first { it.cells.first() == s.name }.enabled) }
      )
    }
  }
  
  
  override fun validate(def: ScraperDef) {
    require(searchStartIndex > 0) { "Search create index have to be greater than 0 and less than or equal to episodes count!" }
    require(!(searchStopIndex < searchStartIndex && searchStopIndex != -1)) { "Search stop index have to be greater than or equal to create index or equal to -1 for unlimited value" }
    require(episodeCodeFormatter.isNotBlank()) { "EpisodeCodeFormatter cannot be empty" }
    require(downloadDir.isNotBlank()) { "Download directory path cannot be empty" }
    require(seriesUrl.isNotBlank()) { "Series URL cannot be empty" }
    require(seriesName.isNotBlank()) { "Series Name cannot be empty" }
    require(outputPath.isNotBlank()) { "Output path cannot be empty" }
  }
  
  open fun defaultHostingsList(): List<VideoServerConfig> {
    return CommonVideoServers.values().map { it.toConfig() }
  }
  
  override fun copy(): SettingsBase {
    return StreamingWebsiteSettings(
      name,
      websiteUrl,
      outputPath,
      outputFormat,
      downloadDir,
      seriesName,
      seasonNumber,
      seriesUrl,
      tmdbKey,
      idmExePath,
      searchStartIndex,
      searchStopIndex,
      episodeCodeFormatter,
      episodeNameFormatter,
      triesBeforeStreamingProviderChange,
      numOfTopStreamingProvidersUsedSimultaneously,
      allowedEpisodeTypes,
      videoServerConfigs
    )
  }
}
