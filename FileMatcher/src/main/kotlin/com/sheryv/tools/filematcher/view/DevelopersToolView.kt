package com.sheryv.tools.filematcher.view

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.Entry
import com.sheryv.tools.filematcher.model.ResultType
import com.sheryv.tools.filematcher.service.MinecraftService
import com.sheryv.tools.filematcher.service.RepositoryGenerator
import com.sheryv.tools.filematcher.service.RepositoryService
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.ViewUtils
import javafx.fxml.FXML
import javafx.scene.control.*
import java.io.File
import java.nio.file.Paths

class DevelopersToolView : BaseView() {
  
  override fun initialize() {
    super.initialize()
    if (Configuration.get().devTools.sourcePath != null) {
      tfPath.text = Configuration.get().devTools.sourcePath
    }
    if (Configuration.get().devTools.outputPath != null) {
      tfOutput.text = Configuration.get().devTools.outputPath
    }
    if (Configuration.get().devTools.bundlePreferredPath != null) {
      tfBundlePath.text = Configuration.get().devTools.bundlePreferredPath
    }
    
    btnPath.setOnAction {
      val initialDirectory = if (tfOutput.text.isNullOrBlank()) Configuration.get().devTools.sourcePath
          ?: SystemUtils.userDownloadDir() else tfPath.text
      DialogUtils.directoryDialog(btnPath.scene.window, initialDirectory = initialDirectory).ifPresent {
        tfPath.text = it.toAbsolutePath().toString()
        Configuration.get().devTools.sourcePath = it.toAbsolutePath().toString()
        Configuration.get().save()
      }
    }
    btnBundlePath.setOnAction {
      val initialDirectory = if (tfBundlePath.text.isNullOrBlank()) Configuration.get().devTools.bundlePreferredPath
          ?: SystemUtils.userDownloadDir() else tfBundlePath.text
      DialogUtils.directoryDialog(btnBundlePath.scene.window, initialDirectory = initialDirectory).ifPresent {
        tfBundlePath.text = it.toAbsolutePath().toString()
        Configuration.get().devTools.bundlePreferredPath = it.toAbsolutePath().toString()
        Configuration.get().save()
      }
    }
    btnOutput.setOnAction {
      val path = if (tfOutput.text.isNullOrBlank()) {
        Configuration.get().devTools.outputPath?.let { Paths.get(it) }
            ?: Paths.get(SystemUtils.userDownloadDir(), "repository.yaml")
      } else {
        Paths.get(tfOutput.text)
      }
      DialogUtils.saveFileDialog(btnPath.scene.window, initialFile = path.toAbsolutePath().toString()).ifPresent {
        tfOutput.text = it.toAbsolutePath().toString()
        Configuration.get().devTools.outputPath = it.toAbsolutePath().toString()
        Configuration.get().save()
      }
    }
    
    btnGenerate.setOnAction {
      generate()
    }
    
    btnCurseForge.setOnAction {
      if (tfOutput.text == null || !File(tfOutput.text).exists() || !File(tfOutput.text).isFile) {
        DialogUtils.dialog("'${tfOutput.text}' is not correct output file path", "Target path is incorrect or empty and it is required by this action", Alert.AlertType.ERROR, ButtonType.OK)
        return@setOnAction
      }
      DialogUtils.openFileDialog(btnCurseForge.scene.window, initialFile = Configuration.get().devTools.mcCursePath).ifPresent {
        Configuration.get().devTools.mcCursePath = it.toAbsolutePath().toString()
        Configuration.get().save()
        
        MinecraftService().fillDataFromCurseForgeJson(it.toFile(), tfOutput.text)
        DialogUtils.dialog("", "Completed", Alert.AlertType.INFORMATION, ButtonType.OK)
        val repo = RepositoryService().loadRepositoryFromFile(Paths.get(tfOutput.text))
        treeView.root = ViewUtils.toTreeItems(repo!!.bundles.last().versions.last().entries)
      }
    }
    
    btnInfo.setOnAction {
      DialogUtils.dialog("This option allows to replace some data in repository file pointed in Output field in " +
          "General tab. Uses CurseForge/Twitch modpack configuration file 'minecraftinstance.json' usually located at " +
          "\n'C:\\Users\\<your_username>\\Twitch\\Minecraft\\Instances\\<modpack_name>\\minecraftinstance.json' " +
          "(in Windows). \nIt loads download urls, project ID, file ID and fills them to selected repository." +
          " Matching is based on file names.", "Fill some data from CurseForge",
          Alert.AlertType.INFORMATION, ButtonType.OK)
    }
    
    cmFormat.items.setAll(listOf("YAML", "JSON"))
    cmFormat.selectionModel.select(0)
    
    MainView.initializeTreeTable(treeView)
  }
  
  private fun generate() {
    var file = tfPath.text?.let { SystemUtils.parseDirectory(it, null) }
    if (file == null) {
      DialogUtils.dialog("'${tfPath.text}' is not correct directory path", "Target directory is incorrect or empty", Alert.AlertType.ERROR, ButtonType.OK)
      return
    }
    val output = File(tfOutput.text)
    if (output.isDirectory) {
      DialogUtils.dialog("'${tfOutput.text}' is not correct output file path", "Target path is incorrect or empty", Alert.AlertType.ERROR, ButtonType.OK)
      return
    }
    try {
      lbProcessState.text = "Generating..."
      pbProcess.progress = ProgressBar.INDETERMINATE_PROGRESS
      listOf(btnBundlePath, btnOutput, btnPath, btnGenerate).forEach { it.isDisable = true }
      
      val service = RepositoryService()
      RepositoryGenerator(file) {
        lbProcessState.text = ""
        pbProcess.progress = 0.0
        listOf(btnBundlePath, btnOutput, btnPath, btnGenerate).forEach { it.isDisable = false }
    
        when (it.type) {
          ResultType.SUCCESS -> {
            service.saveToFile(it.data!!, File(tfOutput.text), cmFormat.selectionModel.selectedItem)
    
            Configuration.get().devTools.sourcePath = tfPath.text
            Configuration.get().devTools.outputPath = tfOutput.text
            Configuration.get().devTools.bundlePreferredPath = tfBundlePath.text
            Configuration.get().save()
            treeView.root = ViewUtils.toTreeItems(it.data.bundles.last().versions.last().entries)
          }
          ResultType.ERROR -> DialogUtils.textAreaDialog(
              "Details",
              it.error?.message.orEmpty(),
              "Error occurred when generating")
        }
      }.start()
    } catch (e: Exception) {
      DialogUtils.textAreaDialog(
          "Details",
          e.message.orEmpty(),
          "Error occurred when generating")
    }
  }
  
  @FXML
  lateinit var treeView: TreeTableView<Entry>
  
  @FXML
  lateinit var btnGenerate: Button
  
  @FXML
  lateinit var btnInfo: Button
  
  @FXML
  lateinit var cmFormat: ComboBox<String>
  
  @FXML
  lateinit var btnPath: Button
  
  @FXML
  lateinit var btnOutput: Button
  
  @FXML
  lateinit var btnBundlePath: Button
  
  @FXML
  lateinit var btnCurseForge: Button
  
  @FXML
  lateinit var tfPath: TextField
  
  @FXML
  lateinit var tfOutput: TextField
  
  @FXML
  lateinit var tfBundlePath: TextField
  
  @FXML
  lateinit var tfMcPath: TextField
  
  @FXML
  lateinit var tabPane: TabPane
  
  @FXML
  lateinit var pbProcess: ProgressBar
  
  @FXML
  lateinit var lbTreeState: Label
  
  @FXML
  lateinit var lbProcessState: Label
  
  @FXML
  lateinit var menuBar: MenuBar
}