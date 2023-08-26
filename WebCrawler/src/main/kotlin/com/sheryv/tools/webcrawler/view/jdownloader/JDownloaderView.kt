package com.sheryv.tools.webcrawler.view.jdownloader

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.ConfigurationChangedEvent
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.service.SystemSupport
import com.sheryv.tools.webcrawler.service.streamingwebsite.jdownloader.JDownloaderCrawlerEntry
import com.sheryv.tools.webcrawler.utils.DialogUtils
import com.sheryv.tools.webcrawler.utils.ViewUtils
import com.sheryv.util.DateUtils
import com.sheryv.util.io.FileUtils
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.fx.core.view.FxmlView
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.Stage
import org.greenrobot.eventbus.Subscribe
import org.koin.core.component.get
import java.nio.file.Files
import java.nio.file.Path
import java.time.format.DateTimeFormatter

class JDownloaderView : FxmlView("view/jdownloader-generate.fxml") {
  override val config: Configuration = get()
  private lateinit var settings: StreamingWebsiteSettings
  private lateinit var scraper: CrawlerDef
  private lateinit var lastSeries: Series
  
  init {
    this.title = "Generate file for JDownloader 2 import - ${ViewUtils.TITLE}"
  }
  
  override fun onViewCreated(stage: Stage) {
    super.onViewCreated(stage)
    scraper = GlobalState.currentCrawler
    settings = scraper.findSettings(config) as StreamingWebsiteSettings
    
    btnOpenSelectDir.tooltip = Tooltip("Open directory selection dialog")
    chFilterToStreamingFilesOnly.isSelected = true
    chOverwritePackagizerRules.isSelected = true
    linkHelp.setOnMouseClicked { SystemSupport.get.openLink(linkHelp.text) }
    
    tfWatchedDir.text = settings.jDownloaderWatchedDir.orEmpty()
    
    btnOpenSelectDir.setOnAction { DialogUtils.openDirectoryDialog(stage)?.also { tfWatchedDir.text = it.toAbsolutePath().toString() } }
    
    btnGenerate.setOnAction { generate() }
    
    tfWatchedDir.textProperty().addListener { _, _, n -> settings.jDownloaderWatchedDir = n }
    
    loadEpisodes()
    chFilterToStreamingFilesOnly.selectedProperty().addListener { _, _, _ ->
      loadEpisodes()
    }
    chOverwritePackagizerRules.selectedProperty().addListener { _, _, _ ->
      loadEpisodes()
    }
  }
  
  private fun generate() {
    try {
      val filename = "${FileUtils.fixFileNameWithColonSupport(String.format("%s %02d", lastSeries.title, lastSeries.season))}_" +
          "${DateUtils.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.crawljob"
      Files.writeString(Path.of(tfWatchedDir.text, filename), taJsonList.text)
      DialogUtils.messageDialog("File generated. \nNow wait for next scan of JDownloader Folder Watch")
    } catch (e: Exception) {
      DialogUtils.textAreaDialog(
        "Details", e.message + "\n\n" + e.stackTraceToString(), ViewUtils.TITLE,
        "Error while generating file", Alert.AlertType.ERROR, true, false, ButtonType.OK
      )
    }
  }
  
  private fun loadEpisodes() {
    try {
      lastSeries = SerialisationUtils.jsonMapper.readValue(settings.outputPath.toFile(), Series::class.java)
      val filtered = lastSeries.episodes.asSequence()
        .filter { it.downloadUrl != null }
        .filter { chFilterToStreamingFilesOnly.isSelected && it.downloadUrl!!.isStreaming || !chFilterToStreamingFilesOnly.isSelected }
      
      taJsonList.text = SerialisationUtils.jsonMapper.writeValueAsString(filtered.map {
        val downloadDir = if (chOverwritePackagizerRules.isSelected)
          Path.of(settings.downloadDir).resolve(lastSeries.generateDirectoryPathForSeason())
        else
          Path.of(settings.downloadDir)
        JDownloaderCrawlerEntry(
          it.downloadUrl!!.base,
          it.generateFileName(lastSeries, settings),
          downloadDir.toAbsolutePath().toString(),
          FileUtils.fixFileNameWithColonSupport(String.format("%s %02d", lastSeries.title, lastSeries.season)),
          it.sourcePageUrl,
          overwritePackagizerEnabled = chOverwritePackagizerRules.isSelected
        )
      }.toList())
      taSimpleLinks.text =
        filtered.joinToString("\n") { String.format("%s#S%02dE%02d", it.downloadUrl!!.base, lastSeries.season, it.number) }
    } catch (e: Exception) {
      throw RuntimeException("Cannot read file with fetched links at '${settings.outputPath}'", e)
    }
  }
  
  @Subscribe
  fun onConfigurationChangedEvent(e: ConfigurationChangedEvent) {
  
  }
  
  
  @FXML
  private lateinit var btnGenerate: Button
  
  @FXML
  private lateinit var btnOpenSelectDir: Button
  
  @FXML
  private lateinit var chFilterToStreamingFilesOnly: CheckBox
  
  @FXML
  private lateinit var chOverwritePackagizerRules: CheckBox
  
  @FXML
  private lateinit var linkHelp: Hyperlink
  
  @FXML
  private lateinit var taJsonList: TextArea
  
  @FXML
  private lateinit var taSimpleLinks: TextArea
  
  @FXML
  private lateinit var tfWatchedDir: TextField
}
