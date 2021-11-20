package com.sheryv.tools.websitescrapper

import com.sheryv.tools.websitescrapper.browser.*
import com.sheryv.tools.websitescrapper.config.Configuration
import com.sheryv.tools.websitescrapper.process.Runner
import com.sheryv.tools.websitescrapper.process.ScraperRegistry
import com.sheryv.tools.websitescrapper.process.base.ScraperDef
import com.sheryv.tools.websitescrapper.process.base.model.SDriver
import com.sheryv.tools.websitescrapper.process.filmweb.FilmwebScraperDef
import com.sheryv.tools.websitescrapper.utils.DialogUtils
import com.sheryv.tools.websitescrapper.utils.Utils
import com.sheryv.tools.websitescrapper.utils.lg
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.Pane
import java.nio.file.Path
import java.time.format.DateTimeFormatter

class MainView : BaseView() {
  lateinit var registry: ScraperRegistry
  lateinit var browsers: BrowserRegistry
  
  override fun initialize() {
    try {
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
    registry = ScraperRegistry.DEFAULT
    registry.register(FilmwebScraperDef())
    browsers = BrowserRegistry.DEFAULT
    BrowserType.prepareDefaults().forEach { browsers.register(it) }
  }
  
  private fun init() {
    registry.all().forEach { cbScraper.items.add(it as ScraperDef<in SDriver>?) }
    val scraper = Configuration.get().scrapper
    val first = cbScraper.items.firstOrNull { it.id == scraper }
    if (scraper != null && first != null) {
      cbScraper.selectionModel.select(first)
    } else {
      cbScraper.selectionModel.selectFirst()
    }
    
    val selected = registry.get(cbScraper.selectionModel.selectedItem.id)!!
    tfSavePath.text = Configuration.get().savePath ?: Path.of(
      SystemUtils.userDownloadDir(),
      "${selected.outputFile}-${Utils.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.${selected.outputFormat.extension}"
    ).toAbsolutePath().toString()
    
    btnStart.setOnAction {
      saveConfig()
      val browser: BrowserDef = if (isCustomBrowser()) {
        BrowserDef(
          BrowserType.OTHER,
          tfBrowserPath.text,
          DriverDef(cbBrowserDriverType.selectionModel.selectedItem, tfBrowserDriverPath.text)
        )
      } else {
        browsers.get(cbBrowser.selectionModel.selectedItem)!!
      }
      val selectedScraper = cbScraper.selectionModel.selectedItem!!
      try {
        Runner(Configuration.get(), browser, selectedScraper).start()
      } catch (e: Exception) {
        lg().error("Running error", e)
        DialogUtils.textAreaDialog(
          "Details", e.stackTraceToString(),
          "Error occurred while scraping", Alert.AlertType.ERROR, false, ButtonType.OK
        )
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
      tfBrowserPath.text = b?.binaryPath ?: Configuration.get().browserPath
      tfBrowserDriverPath.text = b?.driverDef?.path ?: Configuration.get().browserDriverPath
      cbBrowserDriverType.selectionModel.select(b?.driverDef?.type ?: Configuration.get().browserDriverType)
      
      lbDriverVersion.text = if (!flag) {
        b?.type?.defaultDriverDef?.type?.propertyNameForVersion?.let { "Driver version is "+Configuration.property(it) } ?: ""
      } else ""
      
    }
    
    chkUseUserProfile.isSelected = Configuration.get().useUserProfile ?: false
    tfBrowserPath.text = Configuration.get().browserPath.orEmpty()
    tfBrowserDriverPath.text = Configuration.get().browserDriverPath.orEmpty()
    cbBrowserDriverType.items.addAll(DriverType.values())
    cbBrowserDriverType.selectionModel.select(Configuration.get().browserDriverType ?: DriverType.CHROME)
    cbBrowser.items.addAll(browsers.names())
    cbBrowser.items.add(BrowserType.OTHER)
    val browser = Configuration.get().browser
    if (browser != null && cbBrowser.items.contains(browser)) {
      cbBrowser.selectionModel.select(browser)
    } else {
      cbBrowser.selectionModel.selectFirst()
    }
    saveConfig()
  }
  
  private fun saveConfig() {
    Configuration.get().savePath = tfSavePath.text.takeIf { it.isNotBlank() }
    Configuration.get().browser = cbBrowser.selectionModel.selectedItem
    if (isCustomBrowser()) {
      Configuration.get().browserPath = tfBrowserPath.text?.takeIf { it.isNotBlank() }
      Configuration.get().browserDriverPath = tfBrowserDriverPath.text?.takeIf { it.isNotBlank() }
      Configuration.get().browserDriverType = cbBrowserDriverType.selectionModel.selectedItem
    }
    Configuration.get().scrapper = cbScraper.selectionModel.selectedItem.id
    Configuration.get().useUserProfile = chkUseUserProfile.isSelected
    Configuration.get().save()
  }
  
  private fun isCustomBrowser() = cbBrowser.selectionModel.selectedItem == BrowserType.OTHER
  
  @FXML
  private lateinit var tfSavePath: TextField
  
  @FXML
  private lateinit var tfBrowserPath: TextField
  
  @FXML
  private lateinit var tfBrowserDriverPath: TextField
  
  @FXML
  private lateinit var btnStart: Button
  
  @FXML
  private lateinit var btnSavePath: Button
  
  @FXML
  private lateinit var cbScraper: ComboBox<ScraperDef<in SDriver>>
  
  @FXML
  private lateinit var cbBrowser: ComboBox<BrowserType>
  
  @FXML
  private lateinit var cbBrowserDriverType: ComboBox<DriverType>
  
  @FXML
  private lateinit var taLogs: TextArea
  
  @FXML
  private lateinit var lbState: Label
  
  @FXML
  private lateinit var lbDriverVersion: Label
  
  @FXML
  private lateinit var vLinks: Pane
  
  @FXML
  private lateinit var chkUseUserProfile: CheckBox
}
