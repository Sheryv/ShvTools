package com.sheryv.tools.webcrawler.view.downloader

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.service.streamingwebsite.downloader.*
import com.sheryv.tools.webcrawler.utils.DialogUtils
import com.sheryv.tools.webcrawler.utils.ViewUtils
import com.sheryv.tools.webcrawler.utils.ViewUtils.TITLE
import com.sheryv.tools.webcrawler.view.OnChangeScheduledExecutor
import com.sheryv.util.Strings
import com.sheryv.util.fx.core.view.FxmlView
import com.sheryv.util.inMainThread
import com.sheryv.util.logging.log
import com.sheryv.util.subscribeEvent
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.Stage
import org.greenrobot.eventbus.Subscribe
import org.koin.core.component.get
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path


class DownloaderView : FxmlView("view/downloader.fxml") {
  override val config: Configuration = get()
  
  private lateinit var settings: StreamingWebsiteSettings
  private lateinit var scraper: CrawlerDef
  
  private val updater = OnChangeScheduledExecutor("ViewUpdate_" + javaClass.simpleName, 300) {
    inMainThread {
      update()
    }
  }
  
  init {
    this.title = "Downloading status - $TITLE"
  }
  
  override fun onViewCreated(stage: Stage) {
    super.onViewCreated(stage)
    stage.iconifiedProperty().addListener { obs, vo, vn ->
      if (vn) {
        updater.stop()
      } else {
        updater.start(300)
      }
    }
    updater.start(300)
    scraper = GlobalState.currentCrawler
    settings = scraper.findSettings(config) as StreamingWebsiteSettings
    
    btnAdd.setOnAction {
      val result =
        DialogUtils.inputDialog(
          "Add URL - " + ViewUtils.TITLE,
          null,
          listOf(
            "URL" to "http://sample.vodobox.net/skate_phantom_flex_4k/veryhigh/skate_phantom_flex_4k_1056_480p.m3u8",
            "File name" to Strings.generateId(6) + ".ts"
          ),
          Alert.AlertType.NONE
        )
      if (result.size == 2 && result.all { it.isNotBlank() }) {
        try {
          URL(result.first())
          val path = Path.of(result[1]).let {
            if (it.isAbsolute) {
              it
            } else {
              config.downloaderConfig.defaultDownloadDir.resolve(it)
            }
          }
          if (Files.exists(path.parent)) {
            Downloader.add(result[0], path)
            updater.markChanged()
          } else {
            log.warn("Dir ${path.parent} does not exists")
          }
        } catch (e: Exception) {
          log.warn("Incorrect url ${result[0]}", e)
        }
      }
    }
    
    val cf = config.downloaderConfig
    tfConcurrentDownloads.text = cf.concurrentDownloads.toString()
    tfMaxRetries.text = cf.maxRetries.toString()
    tfTempDirPath.text = cf.tempDirPath.toString()
    tfConnectionsPerFile.text = cf.connectionsPerFile.toString()
    tfDefaultDownloadDir.text = cf.defaultDownloadDir.toString()
    progress.progress = -1.0
    
    listOf(tfConcurrentDownloads, tfMaxRetries, tfTempDirPath, tfConcurrentDownloads).forEach {
      it.focusedProperty().addListener { obs, vo, vn ->
        if (!vn) {
          saveConfig()
        }
      }
    }
    tvList.items = FXCollections.observableArrayList()
    tvList.columns.setAll(
      column("File", 220.0) { it.fileName() },
      column("State", 100.0) { it.state.label },
      column("%", 50.0, alignRight = true) { it.progress()?.formatRatioAsPercent() ?: "-" },
      column("Size", 150.0, true) { it.progress()?.let { "${it.currentSize.formatted} / ${it.totalSize?.formatted ?: "-"}" } ?: "-" },
      column("Speed", 100.0, true) { it.progress()?.avgSpeed?.formatted ?: "-" },
      column("Parts", alignRight = true) { (it as? M3U8DownloadingTask)?.partsNumber() ?: "-" },
      column("ID") { it.id },
      column("URL", 180.0) { it.url },
      column("Output directory", 270.0) { it.output.parent.toAbsolutePath().toString() },
    )
    tvList.setRowFactory { tv ->
      val row: TableRow<DownloadingTask> = TableRow()
      val menu = ContextMenu()
      row.itemProperty().addListener { obs, vo, vn ->
        if (vn == null) {
          return@addListener
        }
        
        val items = mutableListOf(
          MenuItem("Copy URL").apply {
            setOnAction {
              val stringSelection = StringSelection(vn.url)
              Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, stringSelection)
            }
          },
          MenuItem("Open directory").apply {
            setOnAction {
              if (Files.exists(vn.output.parent.toAbsolutePath())) {
                Desktop.getDesktop().open(vn.output.parent.toFile())
              }
            }
          },
        )
        if (vn.state == DownloadingState.QUEUED) {
          items.add(0, MenuItem("Remove").apply {
            setOnAction {
              Downloader.removeFromQueue(vn)
            }
          })
        }
        if (vn.state.inBeingProcessed()) {
          items.add(0, MenuItem("Stop").apply {
            setOnAction {
              vn.stop()
            }
          })
        }
        if (vn.state == DownloadingState.STOPPED) {
          items.add(0, MenuItem("Restart").apply {
            setOnAction {
              Downloader.add(vn.url, vn.output)
            }
          })
        }
        menu.items.setAll(items)
      }
      row.contextMenu = menu
      row
    }
    btnStart.setOnAction {
      if (Downloader.isRunning()) {
        Downloader.stopScheduler()
        btnStart.text = "Resume"
      } else {
        Downloader.startScheduler()
        btnStart.text = "Hold new downloads"
      }
    }
    if (Downloader.isRunning()) {
      btnStart.text = "Hold new downloads"
    }
    
    subscribeEvent<DownloadingStateChanged> {
      updater.markChanged()
    }
    update()
  }
  
  private fun saveConfig() {
    val cf = config.downloaderConfig
    config.downloaderConfig = DownloaderConfig(
      Path.of(tfTempDirPath.text),
      tfConcurrentDownloads.text.toIntOrNull() ?: cf.concurrentDownloads,
      tfConnectionsPerFile.text.toIntOrNull() ?: cf.connectionsPerFile,
      tfMaxRetries.text.toIntOrNull() ?: cf.maxRetries,
      Path.of(tfDefaultDownloadDir.text)
    )
    config.save()
  }
  
  private fun update() {
    val tasks = Downloader.tasks()

//    if (tasks.any {!it.started || it.parts.any { !it.process.isComplete }}) {
//      updater.markChanged()
//    }
//    val started = tasks.filter { it.state.inBeingProcessed() }.mapNotNull { it.progress() }
//    progress.progress = started.sumOf { it.currentRatio } / started.size
    
    progress.isVisible = tasks.any { it.state.inBeingProcessed() }
    
    tvList.items.setAll(tasks.reversed())
    tvList.refresh()
  }
  
  private fun <T> column(
    name: String,
    width: Double = 80.0,
    alignRight: Boolean = false,
    value: (DownloadingTask) -> T
  ): TableColumn<DownloadingTask, T> {
    return TableColumn<DownloadingTask, T>(name).apply {
      if (alignRight) {
        this.style = "-fx-alignment: CENTER-RIGHT;"
      }
      prefWidth = width
      setCellValueFactory { o -> ReadOnlyObjectWrapper(value(o.value)) }
    }
  }
  
  @FXML
  lateinit var btnAdd: Button
  
  @FXML
  lateinit var btnStart: Button
  
  @FXML
  lateinit var tfTempDirPath: TextField
  
  @FXML
  lateinit var tfMaxRetries: TextField
  
  @FXML
  lateinit var tfConnectionsPerFile: TextField
  
  @FXML
  lateinit var tfConcurrentDownloads: TextField
  
  @FXML
  lateinit var tfDefaultDownloadDir: TextField
  
  @FXML
  lateinit var progress: ProgressBar
  
  @FXML
  lateinit var tvList: TableView<DownloadingTask>
}
