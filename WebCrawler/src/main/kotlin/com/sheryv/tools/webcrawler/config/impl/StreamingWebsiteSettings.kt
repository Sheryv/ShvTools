package com.sheryv.tools.webcrawler.config.impl

import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.EpisodeType
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.HistoryItem
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.StreamQuality
import com.sheryv.tools.webcrawler.config.impl.streamingwebsite.VideoServerConfig
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingCrawlerBase
import com.sheryv.tools.webcrawler.service.Registry
import com.sheryv.tools.webcrawler.view.settings.*
import com.sheryv.util.fx.lib.onChangeNotNull
import javafx.beans.property.SimpleStringProperty
import java.nio.file.Path
import java.time.Duration

data class StreamingWebsiteSettings(
  override val crawlerId: String,
  override val outputPath: Path,
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
  val videoServerConfigs: List<VideoServerConfig> = VideoServerConfig.all(),
  val skippedEpisodes: List<Int> = emptyList(),
  var history: List<HistoryItem> = emptyList(),
  val linkExpirationDuration: Duration = Duration.ofDays(1),
) : SettingsBase() {
  
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
          listOf(it.definition.label(), it.definition.domain(), it.id),
          it.enabled
        )
      },
      listOf("Name", "Domain", "ID")
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
    val skippedEpisodes = TableSettingsRow(
      "Skipped episodes indices",
      (1 until 31).map {
        TableSettingsRow.RowDefinition(
          listOf(it.toString()),
          this.skippedEpisodes.contains(it)
        )
      },
      listOf("Episode number"),
      false
    )
    return Pair(
      listOf(
        prepareHistoryDropdown(),
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
        qualitiesRow,
        skippedEpisodes,
      )
    ) {
      val types = episodeAudioTypes.readValue()
      val qualities = qualitiesRow.readValue()
      val provs = providers.readValue()
      val skipped = skippedEpisodes.readValue()
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
        buildListAndAddNew(allowedEpisodeTypes, EpisodeType.all()).map { e ->
          e.changeActivation(types.first { it.cells.first() == e.kind.toString().lowercase() }.isEnabled()) as EpisodeType
        },
        buildListAndAddNew(allowedQualities, StreamQuality.all()).map { s ->
          s.changeActivation(qualities.first { it.cells.first() == s.kind.toString() }.isEnabled()) as StreamQuality
        },
        buildListAndAddNew(videoServerConfigs, VideoServerConfig.all()).map { s ->
          s.changeActivation(provs.first { it.cells[2] == s.id }.isEnabled()) as VideoServerConfig
        },
        skipped.filter { it.isEnabled() }.map { it.cells.first().toInt() },
        history
      )
    }
  }
  
  
  override fun validate(def: CrawlerDef) {
    require(searchStartIndex > 0) { "Search create index have to be greater than 0 and less than or equal to episodes count!" }
    require(!(searchStopIndex < searchStartIndex && searchStopIndex != -1)) { "Search stop index have to be greater than or equal to create index or equal to -1 for unlimited value" }
    require(episodeCodeFormatter.isNotBlank()) { "EpisodeCodeFormatter cannot be empty" }
    require(downloadDir.isNotBlank()) { "Download directory path cannot be empty" }
    require(seriesUrl.isNotBlank()) { "Series URL cannot be empty" }
//    require(seriesName.isNotBlank()) { "Series Name cannot be empty" }
  }
  
  fun appendHistory(item: HistoryItem) {
    history = listOf(item) + history
    if (history.size > 30) {
      history = history.dropLast(1)
    }
  }
  
  private fun <T : ApplicableEntry> buildListAndAddNew(userEntries: List<T>, all: List<T>): List<T> {
    val res = userEntries.toMutableList()
    res.addAll(all.filterNot { userEntries.contains(it) }.onEach { it.changeActivation(false) })
    return res
  }
  
  private fun prepareHistoryDropdown(): ChoiceSettingsRow {
    val historyItems = history.map { it.toString() }
    val historyProp = SimpleStringProperty().also {
      it.onChangeNotNull { chosen ->
        if (chosen.isNotEmpty() && "-" != chosen && historyItems.indexOf(chosen) >= 0) {
          val item = history[historyItems.indexOf(chosen)]
          Registry.get().crawlers().filterIsInstance<StreamingCrawlerBase>().firstOrNull { it.id() == crawlerId }?.onLoadFromHistory(item)
        }
      }
    }
    return ChoiceSettingsRow("Load from history", "-", history.map { it.toString() } + "-", historyProp)
  }
  
  override fun copyAll(): SettingsBase = copy()
  
  @Suppress("RedundantOverride")
  override fun equals(other: Any?) = super.equals(other)
  
  @Suppress("RedundantOverride")
  override fun hashCode() = super.hashCode()
}
