package com.sheryv.tools.webcrawler.config.impl

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.EpisodeType
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.StreamQuality
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.VideoServerConfig
import com.sheryv.tools.webcrawler.process.base.CrawlerAttributes
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.view.settings.*
import java.nio.file.Path

class StreamingWebsiteSettings(
  crawlerId: String,
  outputPath: Path? = null,
  val downloadDir: String,
  val seriesName: String = "",
  val seasonNumber: Int = 1,
  val seriesUrl: String = "",
  var tmdbKey: String? = null,
  val idmExePath: String? = null,
  var jDownloaderWatchedDir: String? = null,
  val searchStartIndex: Int = 1,
  val searchStopIndex: Int = -1,
  val episodeCodeFormatter: String = "\${series_name} S\${season}E\${episode_number}",
  val episodeNameFormatter: String = " - \${episode_name}\${file_extension}",
  val triesBeforeStreamingProviderChange: Int = 3,
  val numOfTopStreamingProvidersUsedSimultaneously: Int = 3,
  val allowedEpisodeTypes: List<EpisodeType> = EpisodeType.all(),
  val allowedQualities: List<StreamQuality> = StreamQuality.all(),
  val videoServerConfigs: List<VideoServerConfig> = VideoServerConfig.all()
) : SettingsBase(crawlerId, outputPath) {
  
  override fun buildSettingsPanelDef(): Pair<List<SettingsViewRow<*>>, SettingsPanelReader> {
    val savePathRow = TextInputSettingsRow("Intermediate file path", outputPath.toString())
    val seriesNameRow = TextInputSettingsRow("Series name", seriesName)
    val seriesNumberRow = NumberRangeSettingRow("Season number", seasonNumber, 1, 100, showSlider = false)
    val seriesUrlRow = TextInputSettingsRow("Series URL address (can be relative)", seriesUrl)
    val download = TextInputSettingsRow("Download directory (parent)", downloadDir)
    val searchStart = NumberRangeSettingRow("Search start index", searchStartIndex, 1, 10000)
    val searchStop = NumberRangeSettingRow("Search stop index (set -1 to disable)", searchStopIndex, -1, 10000)
    val tmdb = TextInputSettingsRow("API key for TMDB database (themoviedb.org)", tmdbKey.orEmpty())
    val idm = TextInputSettingsRow("Path to IDM exe (Internet Download Manager integration)", idmExePath.orEmpty())
    val triesBeforeSwitch =
      NumberRangeSettingRow("Number of tries before switch for each streaming provider", triesBeforeStreamingProviderChange, 1, 51)
    val parallelProviders =
      NumberRangeSettingRow("Number of streaming provider used simultaneously", numOfTopStreamingProvidersUsedSimultaneously, 1, 51)
    val codeTemplate = TextInputSettingsRow("Episode code template", episodeCodeFormatter)
    val nameTemplate = TextInputSettingsRow("Episode name template", episodeNameFormatter)
    val providers = TableSettingsRow(
      "Streaming providers order (Drag and drop to change order)",
      buildListAndAddNew(videoServerConfigs, VideoServerConfig.all()).map {
        TableSettingsRow.RowDefinition(
          listOf(it.name, it.searchTerm),
          it.enabled
        )
      },
      listOf("Name", "Search term")
    )
    val episodeAudioTypes = TableSettingsRow(
      "Allowed audio type (Drag and drop to change order)",
      buildListAndAddNew(allowedEpisodeTypes, EpisodeType.all()).map {
        TableSettingsRow.RowDefinition(
          listOf(
            it.kind.toString().lowercase()
          ), it.enabled
        )
      },
      listOf("Name")
    )
    val qualitiesRow = TableSettingsRow(
      "Allowed stream quality",
      buildListAndAddNew(allowedQualities, StreamQuality.all()).map {
        TableSettingsRow.RowDefinition(
          listOf(it.kind.toString()),
          it.enabled
        )
      },
      listOf("Name"),
      false
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
        episodeAudioTypes,
        qualitiesRow
      )
    ) {
      val types = episodeAudioTypes.readValue()
      val qualities = qualitiesRow.readValue()
      val provs = providers.readValue()
      StreamingWebsiteSettings(
        crawlerId,
        Path.of(savePathRow.readValue()),
        download.readValue(),
        seriesNameRow.readValue(),
        seriesNumberRow.readValue(),
        seriesUrlRow.readValue(),
        tmdb.readValue(),
        idm.readValue(),
        jDownloaderWatchedDir,
        searchStart.readValue(),
        searchStop.readValue(),
        codeTemplate.readValue(),
        nameTemplate.readValue(),
        triesBeforeSwitch.readValue(),
        parallelProviders.readValue(),
        allowedEpisodeTypes.map { e ->
          e.changeActivation(types.first { it.cells.first() == e.kind.toString().lowercase() }.isEnabled()) as EpisodeType
        },
        allowedQualities.map { s ->
          s.changeActivation(qualities.first { it.cells.first() == s.kind.toString() }.isEnabled()) as StreamQuality
        },
        videoServerConfigs.map { s -> s.changeActivation(provs.first { it.cells.first() == s.name }.isEnabled()) as VideoServerConfig },
      )
    }
  }
  
  
  override fun validate(def: CrawlerDef) {
    require(searchStartIndex > 0) { "Search create index have to be greater than 0 and less than or equal to episodes count!" }
    require(!(searchStopIndex < searchStartIndex && searchStopIndex != -1)) { "Search stop index have to be greater than or equal to create index or equal to -1 for unlimited value" }
    require(episodeCodeFormatter.isNotBlank()) { "EpisodeCodeFormatter cannot be empty" }
    require(downloadDir.isNotBlank()) { "Download directory path cannot be empty" }
    require(seriesUrl.isNotBlank()) { "Series URL cannot be empty" }
    require(seriesName.isNotBlank()) { "Series Name cannot be empty" }
  }
  
  private fun <T : ApplicableEntry> buildListAndAddNew(userEntries: List<T>, all: List<T>): List<T> {
    val res = userEntries.toMutableList()
    res.addAll(all.filterNot { userEntries.contains(it) }.onEach { it.changeActivation(false) })
    return res
  }
  
  override fun copy(
    crawlerId: String,
    outputPath: Path
  ): SettingsBase {
    return StreamingWebsiteSettings(
      crawlerId,
      outputPath,
      downloadDir,
      seriesName,
      seasonNumber,
      seriesUrl,
      tmdbKey,
      idmExePath,
      jDownloaderWatchedDir,
      searchStartIndex,
      searchStopIndex,
      episodeCodeFormatter,
      episodeNameFormatter,
      triesBeforeStreamingProviderChange,
      numOfTopStreamingProvidersUsedSimultaneously,
      allowedEpisodeTypes,
      allowedQualities,
      videoServerConfigs
    )
  }
}
