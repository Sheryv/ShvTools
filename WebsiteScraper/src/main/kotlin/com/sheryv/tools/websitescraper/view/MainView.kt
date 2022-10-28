package com.sheryv.tools.websitescraper.view

import com.sheryv.tools.websitescraper.*
import com.sheryv.tools.websitescraper.browser.*
import com.sheryv.tools.websitescraper.config.Configuration
import com.sheryv.tools.websitescraper.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.websitescraper.process.Runner
import com.sheryv.tools.websitescraper.process.ScraperRegistry
import com.sheryv.tools.websitescraper.process.base.ScraperDef
import com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.websitescraper.service.streamingwebsite.IDMService
import com.sheryv.tools.websitescraper.utils.*
import com.sheryv.tools.websitescraper.view.search.SearchWindow
import com.sheryv.tools.websitescraper.view.settings.SettingsPanelBuilder
import com.sheryv.tools.websitescraper.view.settings.SettingsPanelReader
import com.sheryv.util.VersionUtils
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import java.io.File
import java.nio.file.Path
import javax.swing.JFrame
import javax.swing.UIManager


class MainView : BaseView(), ViewActionsProvider {
  private var registry: ScraperRegistry = ScraperRegistry.fill(ScraperRegistry.DEFAULT)
  private var browsers: BrowserRegistry = BrowserRegistry.fill(BrowserRegistry.DEFAULT)
  private var selected: ScraperDef? = null
  private lateinit var config: Configuration
  private lateinit var settingsReader: SettingsPanelReader
  private val title = "WebsiteScraper"
  
  init {
    GlobalState.view = this
  }
  
  override fun onViewCreated() {
    try {
      config = Configuration.get()
      prepareRegistry()
      init()
    } catch (e: Exception) {
      DialogUtils.textAreaDialog(
        "Details", e.stackTraceToString(),
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
    stage.title = title
    config.scrapper
    tvScrapers.root = TreeItem()
    tvScrapers.root.children.addAll(ScraperListItem.fromDefs(config, registry.all()).map { it.toTreeItem() })
    tvScrapers.root.children.forEach { it.isExpanded = true }
    tvScrapers.isShowRoot = false
    tvScrapers.selectionModel.selectionMode = SelectionMode.SINGLE
    tvScrapers.selectionModel.selectedItemProperty().addListener { _, _, n ->
      if (n.isLeaf) {
        selected = registry.get(n.value.id)!!
        GlobalState.currentScrapper = selected!!
        showSettingsForSelectedScraper()

//        tfSavePath.text = config.savePath ?: Path.of(
//          SystemUtils.userDownloadDir(),
//          "${settings.outputPath}-${Utils.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.${settings.outputFormat.extension}"
//        ).toAbsolutePath().toString()
      }
    }
    
    val scraper = config.scrapper
    val configured = tvScrapers.root.children.flatMap { it.children }.firstOrNull { it.value.id == scraper }
    if (scraper != null && configured != null) {
      tvScrapers.selectionModel.select(configured)
    } else {
      tvScrapers.selectionModel.select(ViewUtils.findFirstLeafInTree(tvScrapers.root))
    }
    btnStart.setOnAction { startPauseProcess() }
    btnPause.setOnAction { startPauseProcess() }
    btnStop.isVisible = false
    btnPause.isDisable = true
    progressProcess.isVisible = false
    GlobalState.processingState.addListener { o, n ->
      when (n!!) {
        ProcessingStates.RUNNING -> {
          progressProcess.isVisible = true
          btnStart.isDisable = true
          btnPause.isDisable = false
        }
        ProcessingStates.PAUSED -> progressProcess.isVisible = false
        ProcessingStates.IDLE -> {
          progressProcess.isVisible = false
          btnStart.isDisable = false
          btnPause.isDisable = true
        }
      }
    }
    
    cbBrowser.selectionModel.selectedItemProperty().addListener { _, o, n ->
      val flag = isCustomBrowser()
      tfBrowserPath.isEditable = flag
      tfBrowserDriverPath.isEditable = flag
      cbBrowserDriverType.isDisable = !flag
      if (o == BrowserType.OTHER && n != BrowserType.OTHER) {
        saveConfig()
      }
      val b = browsers.get(cbBrowser.selectionModel.selectedItem)
      tfBrowserPath.text = b?.binaryPath ?: config.browserPath
      tfBrowserDriverPath.text = b?.driverDef?.path ?: config.browserDriverPath
      cbBrowserDriverType.selectionModel.select(b?.driverDef?.type ?: config.browserDriverType)
      
      lbDriverVersion.text = if (!flag) {
        b?.type?.defaultDriverDef?.type?.propertyNameForVersion?.let { "Driver version is " + Configuration.property(it) } ?: ""
      } else ""
      
    }
    
    chkUseUserProfile.isSelected = config.useUserProfile ?: false
    tfBrowserPath.text = config.browserPath.orEmpty()
    tfBrowserDriverPath.text = config.browserDriverPath.orEmpty()
    cbBrowserDriverType.items.addAll(DriverType.values())
    cbBrowserDriverType.selectionModel.select(config.browserDriverType ?: DriverType.CHROME)
    cbBrowser.items.addAll(browsers.names())
    cbBrowser.items.add(BrowserType.OTHER)
    val browser = config.browser
    if (browser != null && cbBrowser.items.contains(browser)) {
      cbBrowser.selectionModel.select(browser)
    } else {
      cbBrowser.selectionModel.selectFirst()
    }
    
    vLinks.children.filter { it is Pane }.flatMap { (it as Pane).children }.filter { it is Hyperlink }.map {
      (it as Hyperlink).setOnMouseClicked { e -> SystemUtils.openLink(it.text) }
    }
    
    menu.menus.addAll(
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
            setOnAction { startPauseProcess() }
          },
          MenuItem("Stop").apply { accelerator = KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN) },
        )
      },
      Menu("Tools").apply {
        items.addAll(
          MenuItem("Logs"),
          SeparatorMenuItem(),
          MenuItem("Show configuration file location").apply {
            setOnAction {
              DialogUtils.inputDialog("Location of Configuration file ", null, Path.of(Configuration.FILE).toAbsolutePath().toString())
            }
          },
          MenuItem("Copy options from other scraper").apply {
            accelerator = KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
            setOnAction {
              val type = selected!!.settingsClass
              val source = config.settings.filter { it.key != selected!!.id }.filterValues { type.isInstance(it) }.values
              DialogUtils.choiceDialog("Copy options from other scraper", source, "Choose source scraper")?.also {
                config.settings[selected!!.id] = it
                showSettingsForSelectedScraper()
              }
            }
          },
          MenuItem("Send to IDM").apply {
            setOnAction {
              inBackground {
                try {
                  val settings = selected!!.findSettings(config)
                  val (done, all) = IDMService(config).addToIDM(Utils.jsonMapper.readValue(File(settings.outputPath), Series::class.java))
                  inViewThread {
                    DialogUtils.dialog("Added to IDM $done out of $all episodes")
                  }
                } catch (e: Exception) {
                  inViewThread {
                    DialogUtils.textAreaDialog(
                      "Details", e.message + "\n\n" + e.stackTraceToString(),
                      "Error while sending to IDM", Alert.AlertType.ERROR, true, ButtonType.OK
                    )
                  }
                }
              }
            }
          },
          MenuItem("Tv Show search tool").apply {
            setOnAction {
              openSearchEpisodesWindow()
            }
          },
        )
      },
      Menu("Info").apply {
        items.setAll(MenuItem("About").apply {
          setOnAction {
            val msg =
              "Created by Sheryv\nVersion: ${VersionUtils.loadVersionByModuleName("website-scraper-version")}\nWebsite: https://github.com/Sheryv/ShvTools"
            val textArea = TextArea(msg)
            textArea.isEditable = false
            textArea.isWrapText = true
            textArea.prefRowCount = 5
            val gridPane = GridPane()
            gridPane.maxWidth = Double.MAX_VALUE
            gridPane.add(textArea, 0, 0)
            val alert = Alert(Alert.AlertType.NONE, msg, ButtonType.OK)
            alert.dialogPane.content = gridPane
            MainApplication.appendStyleSheets(alert.dialogPane.content.scene)
            alert.title = "About"
            alert.showAndWait()
          }
        })
      }
    )
  }
  
  private fun startPauseProcess() {
    if (selected != null) {
      when (GlobalState.processingState.value) {
        ProcessingStates.IDLE -> {
          GlobalState.processingState.set(ProcessingStates.RUNNING)
          
          val browser: BrowserDef = if (isCustomBrowser()) {
            BrowserDef(
              BrowserType.OTHER,
              tfBrowserPath.text,
              DriverDef(cbBrowserDriverType.selectionModel.selectedItem, tfBrowserDriverPath.text)
            )
          } else {
            browsers.get(cbBrowser.selectionModel.selectedItem)!!
          }
          val runner = Runner(config, browser, selected!!)
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
                  "Details", e.stackTraceToString(),
                  "Error occurred while scraping", Alert.AlertType.ERROR, false, ButtonType.OK
                )
              }
            } finally {
              GlobalState.processingState.set(ProcessingStates.IDLE)
            }
          }
        }
        ProcessingStates.RUNNING -> GlobalState.processingState.set(ProcessingStates.PAUSED)
        ProcessingStates.PAUSED -> GlobalState.processingState.set(ProcessingStates.RUNNING)
      }
    }
  }
  
  private fun showSettingsForSelectedScraper() {
    val settings = selected!!.findSettings(config)
    lbSelected.text = settings.toString()
    val parts = SettingsPanelBuilder(settings).build()
    settingsReader = parts.second
    vbOptions.children.setAll(parts.first)
  }
  
  private fun saveConfig() {
    try {
      synchronized(this) {
        val s = settingsReader()
        s.validate(selected!!)
        config.settings[selected!!.id] = s
        config.browser = cbBrowser.selectionModel.selectedItem
        if (isCustomBrowser()) {
          config.browserPath = tfBrowserPath.text?.takeIf { it.isNotBlank() }
          config.browserDriverPath = tfBrowserDriverPath.text?.takeIf { it.isNotBlank() }
          config.browserDriverType = cbBrowserDriverType.selectionModel.selectedItem
        }
        config.scrapper = selected?.id
        config.useUserProfile = chkUseUserProfile.isSelected
        config.save()
      }
    } catch (e: Exception) {
      DialogUtils.textAreaDialog(
        "One or more values is incorrect", e.message + "\n\n" + e.stackTraceToString(),
        "Error while saving configuration for ${selected!!.findSettings(config)}", Alert.AlertType.ERROR, true, ButtonType.OK
      )
    }
  }
  
  private fun isCustomBrowser() = cbBrowser.selectionModel.selectedItem == BrowserType.OTHER
  
  override fun showMessageDialog(message: String) {
    val header = if (GlobalState.processingState.value == ProcessingStates.RUNNING) {
      "Scraping ${selected!!.findSettings(config).name} - $title"
    } else {
      title
    }
    DialogUtils.dialog(message, header, owner = stage, buttons = arrayOf(ButtonType.OK))
  }
  
  private fun openSearchEpisodesWindow() {
    if (selected!!.findSettings(config) !is StreamingWebsiteSettings) {
      DialogUtils.dialog("This feature is only available for video streaming scrapers")
      return
    }
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    val f = JFrame()
    f.title = "Search and fill episodes links"
    f.contentPane.add(SearchWindow().init(config, selected!!.findSettings(config) as StreamingWebsiteSettings).mainPanel)
    f.setSize(1200, 750)
    f.isVisible = true
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
  private lateinit var tvScrapers: TreeView<ScraperListItem>
  
  @FXML
  private lateinit var cbBrowser: ComboBox<BrowserType>
  
  @FXML
  private lateinit var cbBrowserDriverType: ComboBox<DriverType>
  
  @FXML
  private lateinit var taLogs: TextArea
  
  @FXML
  private lateinit var lbState: Label
  
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
}
