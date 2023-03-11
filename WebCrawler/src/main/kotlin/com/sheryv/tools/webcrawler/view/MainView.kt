package com.sheryv.tools.webcrawler.view

import com.sheryv.tools.webcrawler.BaseView
import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.MainApplication
import com.sheryv.tools.webcrawler.ProcessingStates
import com.sheryv.tools.webcrawler.browser.BrowserTypes
import com.sheryv.tools.webcrawler.browser.DriverTypes
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.Runner
import com.sheryv.tools.webcrawler.process.base.CrawlerDef
import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.base.event.FetchedDataExternalChangeEvent
import com.sheryv.tools.webcrawler.process.base.event.FetchedDataStatusChangedEvent
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.service.Registry
import com.sheryv.tools.webcrawler.service.SystemSupport
import com.sheryv.tools.webcrawler.service.streamingwebsite.generator.MetadataGenerator
import com.sheryv.tools.webcrawler.service.streamingwebsite.idm.IDMService
import com.sheryv.tools.webcrawler.utils.*
import com.sheryv.tools.webcrawler.utils.ViewUtils.TITLE
import com.sheryv.tools.webcrawler.view.jdownloader.JDownloaderView
import com.sheryv.tools.webcrawler.view.search.SearchWindow
import com.sheryv.tools.webcrawler.view.settings.SettingsPanelBuilder
import com.sheryv.tools.webcrawler.view.settings.SettingsPanelReader
import com.sheryv.util.ChangeListener
import com.sheryv.util.HttpSupport
import com.sheryv.util.VersionUtils
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.*
import javafx.stage.Modality
import javafx.util.Duration
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.awt.EventQueue
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JFrame
import kotlin.io.path.*


class MainView : BaseView(), ViewActionsProvider {
  private lateinit var registry: Map<String, CrawlerDef>
  private var selected: CrawlerDef? = null
  private lateinit var config: Configuration
  private lateinit var settingsReader: SettingsPanelReader
  
  init {
    GlobalState.view = this
  }
  
  override fun onViewCreated() {
    try {
      config = Configuration.get()
      registry = Registry.get().crawlers().associate { it.id() to it as CrawlerDef }
      prepareRegistry()
      init()
    } catch (e: Exception) {
      DialogUtils.textAreaDialog(
        "Details", e.stackTraceToString(), TITLE,
        "Error occurred while starting application", Alert.AlertType.ERROR, false, ButtonType.OK
      )
      throw e
    }
  }
  
  private fun prepareRegistry() {
//    registry = ScraperRegistry.fill(ScraperRegistry.DEFAULT)
//    browsers = BrowserRegistry.fill(BrowserRegistry.DEFAULT)
  }
  
  
  private fun init() {
    eventsAttach(this)
    stage.title = TITLE
    config.crawler
    tvScrapers.root = TreeItem()
    tvScrapers.root.children.addAll(CrawlerListItem.fromDefs(config, registry.values).map { it.toTreeItem() })
    tvScrapers.root.children.forEach { it.isExpanded = true }
    tvScrapers.isShowRoot = false
    tvScrapers.selectionModel.selectionMode = SelectionMode.SINGLE
    
    val streamingRelatedMenus = streamingMenu()
    
    tvScrapers.selectionModel.selectedItemProperty().addListener { _, _, n ->
      if (n.isLeaf) {
        registry.values.forEach { eventsDetach(it) }
        selected = registry.get(n.value.id)!!
        eventsAttach(selected!!)
        GlobalState.currentCrawler = selected!!
        showSettingsForSelectedScraper()

        if (selected!!.findSettings(config) is StreamingWebsiteSettings) {
          if (!menu.menus.contains(streamingRelatedMenus)) {
            menu.menus.add(streamingRelatedMenus)
          }
        } else {
          menu.menus.remove(streamingRelatedMenus)
        }
//        tfSavePath.text = config.savePath ?: Path.of(
//          SystemUtils.userDownloadDir(),
//          "${settings.outputPath}-${Utils.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.${settings.outputFormat.extension}"
//        ).toAbsolutePath().toString()
        postEvent(FetchedDataExternalChangeEvent())
      }
    }
    
    val crawler = config.crawler
    val configured = tvScrapers.root.children.flatMap { it.children }.firstOrNull { it.value.id == crawler }
    if (crawler != null && configured != null) {
      tvScrapers.selectionModel.select(configured)
    } else {
      tvScrapers.selectionModel.select(ViewUtils.findFirstLeafInTree(tvScrapers.root))
    }
    btnStart.setOnAction { startOrPauseProcess() }
    btnPause.setOnAction { startOrPauseProcess() }
    btnStop.setOnAction { GlobalState.processingState.set(ProcessingStates.STOPPING) }
    btnStop.isVisible = false
    btnPause.isVisible = false
    progressProcess.isVisible = false
    GlobalState.processingState.addListener { o, n ->
      inViewThread {
        
        when (n!!) {
          ProcessingStates.RUNNING -> {
            btnStop.isVisible = true
            progressProcess.isVisible = true
            btnStart.text = "Pause"
            btnStart.styleClass.add("btn-info")
            btnStart.styleClass.remove("btn-success")
          }
          ProcessingStates.PAUSED -> {
            progressProcess.isVisible = false
            btnStart.text = "Resume"
            btnStart.isDisable = false
          }
          ProcessingStates.PAUSING -> {
            btnStart.isDisable = true
          }
          ProcessingStates.STOPPING -> {
            btnStart.isDisable = true
            btnStop.isVisible = false
          }
          ProcessingStates.IDLE -> {
            postEvent(FetchedDataExternalChangeEvent())
            btnStop.isVisible = false
            progressProcess.isVisible = false
            btnStart.isDisable = false
            btnStart.text = "Start"
            btnStart.styleClass.add("btn-success")
            btnStart.styleClass.remove("btn-info")
          }
        }
      }
    }
    
    cbBrowserDriverType.selectionModel.selectedItemProperty().addListener { _, _, n ->
      if (n != null) {
        config.browserSettings.currentBrowser().selectedDriver = n
        tfBrowserDriverPath.text = config.browserSettings.currentBrowser().currentDriver().path.toAbsolutePath().toString()
      }
    }
    
    cbBrowser.selectionModel.selectedItemProperty().addListener { _, o, n ->
      val flag = isCustomBrowser()
      val prev = config.browserSettings.currentBrowser()
      tfBrowserPath.isEditable = flag
      btnSelectBrowserExec.isDisable = !flag
//      tfBrowserDriverPath.isEditable = flag
//      cbBrowserDriverType.isDisable = !flag
      config.browserSettings.selected = n
      val b = config.browserSettings.currentBrowser()
      if (b.binaryPath != null) {
        tfBrowserPath.text = b.binaryPath!!.toAbsolutePath().toString()
      } else {
        config.browserSettings.currentBrowser().binaryPath = prev.binaryPath
      }
      val driver = b.selectedDriver
      cbBrowserDriverType.items.setAll(b.drivers.map { it.type })
      cbBrowserDriverType.selectionModel.select(driver)
    }
    val pause = PauseTransition(Duration.seconds(1.0))
    tfBrowserPath.textProperty().addListener { _, _, n ->
      config.browserSettings.currentBrowser().binaryPath = Path.of(n)
      lbStatusOfSelectedBrowser.text = ""
      pause.onFinished = EventHandler {
        findBrowserVersion(n)
      }
      pause.playFromStart()
    }
    
    val pause2 = PauseTransition(Duration.seconds(1.0))
    tfBrowserDriverPath.textProperty().addListener { _, _, n ->
      config.browserSettings.currentBrowser().currentDriver().path = Path.of(n)
      lbDriverVersion.text = ""
      pause2.onFinished = EventHandler {
        lbDriverVersion.text = if (config.browserSettings.currentBrowser().currentDriver().path.isExecutable())
          "File exists"
        else
          "Path is incorrect"
      }
      pause2.playFromStart()
    }
    
    chkUseUserProfile.isSelected = config.browserSettings.useUserProfile
    cbBrowser.items.addAll(config.browserSettings.configs.map { it.type })
    cbBrowser.selectionModel.select(config.browserSettings.selected)
    
    btnSelectDriver.setOnAction {
      DialogUtils.openFileDialog(stage, initialFile = tfBrowserDriverPath.text.takeIf { it.isNotBlank() })?.also {
        tfBrowserDriverPath.text = it.toAbsolutePath().toString()
      }
    }
    
    btnSelectBrowserExec.setOnAction {
      DialogUtils.openFileDialog(stage, initialFile = tfBrowserPath.text.takeIf { it.isNotBlank() })?.also {
        tfBrowserPath.text = it.toAbsolutePath().toString()
      }
    }
    
    vLinks.children.filterIsInstance<Pane>().flatMap { it.children }.filterIsInstance<Hyperlink>().map {
      it.setOnMouseClicked { e -> SystemSupport.get.openLink(it.text) }
    }
    
    
    menu.menus.addAll(
      0, listOf(
        Menu("File").apply {
          items.addAll(
            MenuItem("Save").apply {
              accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
              setOnAction {
                saveConfig()
              }
            },
            SeparatorMenuItem(),
            MenuItem("Exit").apply {
              accelerator = KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)
              setOnAction { Platform.exit() }
            }
          )
        },
        Menu("Process").apply {
          items.addAll(
            MenuItem("Start / Pause").apply {
              accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN)
              setOnAction { startOrPauseProcess() }
            },
            MenuItem("Start for failed episodes only").apply {
              setOnAction { startOrPauseProcess(true) }
            },
            MenuItem("Terminate").apply {
              accelerator = KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN)
              setOnAction { GlobalState.processingState.set(ProcessingStates.STOPPING) }
            },
          )
        },
        Menu("Tools").apply {
          items.addAll(
            MenuItem("Run script in opened browser").apply {
              accelerator = KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN)
              setOnAction { openRunScriptWindow() }
            },
            SeparatorMenuItem(),
            MenuItem("Logs"),
            SeparatorMenuItem(),
            CheckMenuItem("Pause processing on next step").apply {
              accelerator = KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN)
              setOnAction { GlobalState.pauseOnNextStep = this.isSelected }
            },
            MenuItem("Copy options from other crawler").apply {
              accelerator = KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
              setOnAction {
                val type = selected!!.settingsClass
                val source = config.settings.filter { it.crawlerId != selected!!.id() && type.isInstance(it) }
                DialogUtils.choiceDialog("Copy options from other scraper", source, "Choose source scraper")?.also {
                  config.updateSettings(it.copy())
                  showSettingsForSelectedScraper()
                }
              }
            },
          )
        },
        Menu("Info").apply {
          items.setAll(
            MenuItem("Show configuration file location").apply {
              setOnAction {
                DialogUtils.messageCopyableDialog(
                  Path.of(Configuration.FILE).toAbsolutePath().toString(),
                  "Location of Configuration file",
                )
              }
            },
            MenuItem("About").apply {
              setOnAction {
                val msg =
                  "Created by Sheryv\nVersion: ${VersionUtils.loadVersionByModuleName("web-crawler-version")}\nWebsite: https://github.com/Sheryv/ShvTools"
                DialogUtils.messageCopyableDialog(msg, "About info")
              }
            })
        },
      )
    )
  }
  
  private fun startOrPauseProcess(runOnlyForFailedEpisodes: Boolean = false) {
    if (selected != null) {
      when (GlobalState.processingState.value) {
        ProcessingStates.IDLE -> {
          GlobalState.processingState.set(ProcessingStates.RUNNING)
          
          val browser = config.browserSettings.currentBrowser()
          val runner = Runner(config, browser, selected!!, ProcessParams(runOnlyForFailedEpisodes))
          val settings = settingsReader.invoke()
          inBackground {
            try {
              runner.prepare(settings)
              saveConfig()
              runner.start()
            } catch (e: Exception) {
              lg().error("Running error", e)
              inViewThread {
                DialogUtils.textAreaDialog(
                  "Details", e.stackTraceToString(), TITLE,
                  "Error occurred while scraping", Alert.AlertType.ERROR, false, ButtonType.OK
                )
              }
            } finally {
              GlobalState.processingState.set(ProcessingStates.IDLE)
            }
          }
        }
        ProcessingStates.RUNNING -> GlobalState.processingState.set(ProcessingStates.PAUSING)
        ProcessingStates.PAUSED -> GlobalState.processingState.set(ProcessingStates.RUNNING)
        else -> {}
      }
    }
  }
  
  private fun showSettingsForSelectedScraper() {
    val settings = selected!!.findSettings(config)
    lbSelected.text = selected.toString()
    val parts = SettingsPanelBuilder(settings).build()
    settingsReader = parts.second
    vbOptions.children.setAll(parts.first)
  }
  
  private fun saveConfig() {
    try {
      synchronized(this) {
        val s = settingsReader()
        s.validate(selected!!)
        config.updateSettings(s)
        config.crawler = selected!!.attributes.id
        config.browserSettings.useUserProfile = chkUseUserProfile.isSelected
        config.save()
      }
    } catch (e: Exception) {
      DialogUtils.textAreaDialog(
        "One or more values is incorrect", e.message + "\n\n" + e.stackTraceToString(), TITLE,
        "Error while saving configuration for ${selected!!.findSettings(config)}", Alert.AlertType.ERROR, true, ButtonType.OK
      )
    }
  }
  
  private fun isCustomBrowser() = cbBrowser.selectionModel.selectedItem == BrowserTypes.OTHER
  
  override fun showMessageDialog(message: String) {
    val header = if (GlobalState.processingState.value == ProcessingStates.RUNNING) {
      "Processing crawler ${selected!!.attributes.name} - $TITLE"
    } else {
      TITLE
    }
    DialogUtils.dialog(message, header, owner = stage, buttons = arrayOf(ButtonType.OK))
  }
  
  private fun openSearchEpisodesWindow() {
    if (selected!!.findSettings(config) !is StreamingWebsiteSettings) {
      DialogUtils.dialog("This feature is only available for video streaming scrapers")
      return
    }
    EventQueue.invokeLater {
//      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      val f = JFrame()
      f.title = "Search and fill episodes links"
      f.contentPane.add(SearchWindow().init(config, selected!!.findSettings(config) as StreamingWebsiteSettings).mainPanel)
      f.setSize(1200, 750)
      f.isVisible = true
      f.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent?) {
          f.dispose()
        }
      })
    }
  }
  
  private fun openJDownloaderGeneratorView() {
    try {
      MainApplication.createWindow<JDownloaderView>("view/jdownloader-generate.fxml", "Generate file for JDownloader 2 import - $TITLE")
    } catch (e: Exception) {
      lg().error("Error while opening dialog JDownloader 2 import", e)
      DialogUtils.textAreaDialog(
        "Details", e.message + "\n\n" + e.stackTraceToString(), TITLE,
        "Error while opening dialog JDownloader 2 import", Alert.AlertType.ERROR, true, ButtonType.OK
      )
    }
  }
  
  private fun findBrowserVersion(path: String) {
    inBackground {
      val p = Path.of(path)
      val ver = if (p.exists() && p.isRegularFile()) {
        SystemSupport.get.getFileVersion(p).takeIf { it.isNotEmpty() }?.let { "Recognized version is: ${it.first()}" }
      } else null
      inViewThread {
        lbStatusOfSelectedBrowser.text = ver ?: "Provided path is incorrect"
      }
    }
  }
  
  private fun streamingMenu(): Menu {
    return Menu("Streaming website crawler").apply {
      items.setAll(
        Menu("Process fetched links").apply {
          items.setAll(
            MenuItem("Tv Show search tool").apply {
              accelerator = KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN)
              setOnAction {
                openSearchEpisodesWindow()
              }
            },
            SeparatorMenuItem(),
            MenuItem("Send links to IDM (Internet Download Manager)").apply {
              accelerator = KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
              setOnAction {
                inBackground {
                  try {
                    val settings = selected!!.findSettings(config)
                    val (done, all) = IDMService(config).addToIDM(
                      Utils.jsonMapper.readValue(
                        settings.outputPath.toFile(),
                        Series::class.java
                      )
                    )
                    inViewThread {
                      DialogUtils.messageDialog("Successfully added to IDM $done episodes out of $all")
                    }
                  } catch (e: Exception) {
                    lg().error("Error while sending to IDM", e)
                    inViewThread {
                      DialogUtils.textAreaDialog(
                        "Details", e.message + "\n\n" + e.stackTraceToString(), TITLE,
                        "Error while sending to IDM", Alert.AlertType.ERROR, true, ButtonType.OK
                      )
                    }
                  }
                }
              }
            },
            MenuItem("Generate file for JDownloader 2 import").apply {
              accelerator = KeyCodeCombination(KeyCode.J, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
              setOnAction { openJDownloaderGeneratorView() }
            },
          )
        },
        SeparatorMenuItem(),
        MenuItem("Generate .nfo and metadata files for found episodes").apply {
          setOnAction {
            inBackground {
              try {
                val settings = selected!!.findSettings(config) as StreamingWebsiteSettings
                val series = Utils.jsonMapper.readValue(
                  settings.outputPath.toFile(),
                  Series::class.java
                )
                MetadataGenerator(settings).generateNfoMetadata(series)
                val seriesDir = Path.of(settings.downloadDir).resolve(series.generateDirectoryPathForSeason())
                Files.copy(settings.outputPath, seriesDir.resolve("web_crawler_metadata.json"), StandardCopyOption.REPLACE_EXISTING)
                
                if (seriesDir.parent.listDirectoryEntries().none { it.name == "poster.jpg" || it.name == "poster.png" }) {
                  val http = HttpSupport()
                  series.posterUrl?.also {
                    val format = it.substringAfterLast('.', "jpg")
                    http.stream(HttpSupport.getRequest(series.posterUrl)).use {
                      Files.copy(it, seriesDir.parent.resolve("poster.$format"), StandardCopyOption.REPLACE_EXISTING)
                    }
                  }
                }
                
                inViewThread {
                  DialogUtils.messageDialog("Successfully generated metadata in directory ${seriesDir.toAbsolutePath()}")
                }
              } catch (e: Exception) {
                lg().error("Error while generating", e)
                inViewThread {
                  DialogUtils.textAreaDialog(
                    "Details", e.message + "\n\n" + e.stackTraceToString(), TITLE,
                    "Error while generating", Alert.AlertType.ERROR, true, ButtonType.OK
                  )
                }
              }
            }
          }
        }
      )
      
    }
  }
  
  private fun openRunScriptWindow() {
    
    val alert = Alert(Alert.AlertType.NONE, "", ButtonType.CLOSE)
    alert.initModality(Modality.NONE)
    alert.title = "Run JavaScript in currently opened browser - $TITLE"
    val btn = Button("Execute script")
    btn.styleClass.add("btn-success")
    val msg = Label("There is no running process!")
    msg.maxWidth = Double.MAX_VALUE
    
    btn.isDisable = GlobalState.runningProcess.value == null
    msg.isVisible = GlobalState.runningProcess.value == null
    
    val listener: ChangeListener<SeleniumCrawler<SettingsBase>> = ChangeListener { _, n ->
      btn.isDisable = n == null
      msg.isVisible = n == null
    }
    
    val inputArea = TextArea(config.lastUserScript).apply {
      isEditable = true
      isWrapText = false
      styleClass.add("mono")
      maxWidth = Double.MAX_VALUE
      maxHeight = Double.MAX_VALUE
      minWidth = 100.0
      promptText = "Your JavaScript goes here..."
    }
    val outputArea = TextArea().apply {
      isEditable = false
      isWrapText = false
      styleClass.add("mono")
      maxWidth = Double.MAX_VALUE
      maxHeight = Double.MAX_VALUE
      minWidth = 100.0
      promptText = "Execution output is shown here"
    }
    
    val split = SplitPane(inputArea, outputArea)
    
    val helpBtn = Button("Help")
    helpBtn.setOnAction {
      DialogUtils.textAreaDialog(
        "",
        """
        All scripts are executed by browser driver in current driver state.
        If driver is inside iframe then they will be executed in that iframe.
        To see values in right panel the script have to return value using 'return' statement and this tool adds 'return' statements automatically to last expression in script.
        
        Examples:
        
        //statement without return value
        document.querySelector('.title').setAttribute('style', 'color: green')
        
        //return title
        document.querySelector('.title').textContent
        
        //click button
        document.querySelector('nav a').click()
        """.trimIndent(), "Help for running scripts - $TITLE"
      )
    }
    val bottomRow = HBox(btn, msg, helpBtn)
    HBox.setHgrow(msg, Priority.ALWAYS)
    bottomRow.alignment = Pos.CENTER_LEFT
    bottomRow.spacing = 10.0
    val expContent = GridPane()
    expContent.vgap = 10.0
    expContent.maxWidth = Double.MAX_VALUE
    expContent.add(split, 0, 0)
    expContent.add(bottomRow, 0, 1)
    expContent.minHeight = 250.0
    expContent.prefHeight = 450.0
    expContent.minWidth = 400.0
    expContent.prefWidth = 1200.0
    alert.isResizable = true
    GridPane.setVgrow(split, Priority.ALWAYS)
    GridPane.setHgrow(split, Priority.ALWAYS)
    GridPane.setHgrow(bottomRow, Priority.ALWAYS)
    
    alert.dialogPane.content = expContent
    MainApplication.appendStyleSheets(alert.dialogPane.scene)
    
    inputArea.textProperty().addListener { _, _, n -> config.lastUserScript = n }
    
    btn.setOnAction {
      GlobalState.runningProcess.value?.takeIf { inputArea.text.isNotBlank() }?.also {
        val scriptRaw = inputArea.text.trim()
        val lines = scriptRaw.lines()
        var last = lines.last()
        val lastExpression = last.split(';').last().trim()
        if (!lastExpression.startsWith("return ")) {
          val semi = last.indexOf(';')
          last = "\n" + if (semi >= 0) {
            last.take(semi + 1) + " return " + last.substring(semi + 1)
          } else {
            "return $last"
          }
        }
        
        val script = lines.take(lines.size - 1).joinToString("\n") + last
        lg().debug("Executing:\n$script")
        val res = it.driver.executeScript(script)
        outputArea.appendText("[${LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_TIME)}]: ${res.toString()}\n")
        outputArea.scrollTop = Double.MAX_VALUE
      }
    }
    GlobalState.runningProcess.addListener(listener)
    alert.showAndWait()
    GlobalState.runningProcess.removeListener(listener)
  }
  
  @Subscribe(threadMode = ThreadMode.MAIN)
  internal fun onFetchedDataStatusChangedEvent(e: FetchedDataStatusChangedEvent) = inViewThread {
    taCrawlerStatus.text = e.statusText
  }
  
  @FXML
  private lateinit var tfSavePath: TextField
  
  @FXML
  private lateinit var tfBrowserPath: TextField
  
  @FXML
  private lateinit var tfBrowserDriverPath: TextField
  
  @FXML
  private lateinit var btnStart: Button
  
  @FXML
  private lateinit var btnPause: Button
  
  @FXML
  private lateinit var btnStop: Button
  
  @FXML
  private lateinit var btnSelectDriver: Button
  
  @FXML
  private lateinit var btnSelectBrowserExec: Button
  
  @FXML
  private lateinit var tvScrapers: TreeView<CrawlerListItem>
  
  @FXML
  private lateinit var cbBrowser: ComboBox<BrowserTypes>
  
  @FXML
  private lateinit var cbBrowserDriverType: ComboBox<DriverTypes>
  
  @FXML
  private lateinit var taLogs: TextArea
  
  @FXML
  private lateinit var lbState: Label
  
  @FXML
  private lateinit var lbStatusOfSelectedBrowser: Label
  
  @FXML
  private lateinit var lbSelected: Label
  
  @FXML
  private lateinit var lbDriverVersion: Label
  
  @FXML
  private lateinit var vLinks: Pane
  
  @FXML
  private lateinit var vbOptions: VBox
  
  @FXML
  private lateinit var chkUseUserProfile: CheckBox
  
  @FXML
  private lateinit var menu: MenuBar
  
  @FXML
  private lateinit var progressProcess: ProgressIndicator
  
  @FXML
  private lateinit var taCrawlerStatus: TextArea
  
  @FXML
  private lateinit var vbSharedOptions: VBox
}
