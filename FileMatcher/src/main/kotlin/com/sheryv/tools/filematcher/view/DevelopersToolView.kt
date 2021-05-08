package com.sheryv.tools.filematcher.view

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.BundleVersion
import com.sheryv.tools.filematcher.model.Entry
import com.sheryv.tools.filematcher.model.Repository
import com.sheryv.tools.filematcher.model.ResultType
import com.sheryv.tools.filematcher.service.MinecraftService
import com.sheryv.tools.filematcher.service.RepositoryGenerator
import com.sheryv.tools.filematcher.service.RepositoryHashUpdater
import com.sheryv.tools.filematcher.service.RepositoryService
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.ViewUtils
import com.sheryv.tools.filematcher.utils.lg
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.scene.control.*
import java.io.File
import java.nio.file.Paths
import java.time.OffsetDateTime

class DevelopersToolView : BaseView() {
  
  private val saveBtnVisibility: BooleanProperty = SimpleBooleanProperty(true)
  private var repository: Repository? = null
    set(value) {
      field = value
      saveBtnVisibility.set(value == null)
    }
  
  private var version: BundleVersion? = null
  
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
    
    btnSave.setOnAction {
      repository?.let { save(it) }
    }
    btnSave.disableProperty().bind(saveBtnVisibility)
    
    btnCurseForge.setOnAction {
      if (tfOutput.text == null || !File(tfOutput.text).exists() || !File(tfOutput.text).isFile) {
        DialogUtils.dialog(
          "'${tfOutput.text}' is not correct output file path",
          "Target path is incorrect or empty and it is required by this action",
          Alert.AlertType.ERROR,
          ButtonType.OK
        )
        return@setOnAction
      }
      DialogUtils.openFileDialog(btnCurseForge.scene.window, initialFile = Configuration.get().devTools.mcCursePath)
        .ifPresent {
          Configuration.get().devTools.mcCursePath = it.toAbsolutePath().toString()
          Configuration.get().save()
          
          MinecraftService().fillDataFromCurseForgeJson(version!!, it.toFile(), tfOutput.text)
          DialogUtils.dialog("", "Completed", Alert.AlertType.INFORMATION, ButtonType.OK)
          treeView.refresh()
        }
    }
    
    btnTransformUrls.setOnAction {
      if (repository != null) {
        MinecraftService().transformUrls(version!!)
        lbProcessState.text = "URLs fixed"
        treeView.refresh()
      }
    }
    
    btnAddMatchingStrategyToMods.setOnAction {
      if (repository != null) {
        val errors = MinecraftService().addMinecraftModsMatcher(version!!)
        displayRepository()
        DialogUtils.textAreaDialog(
          "File that were not matched are listed below",
          errors.joinToString("\n"),
          "Completed",
          Alert.AlertType.INFORMATION
        )
      }
    }
    
    btnInfo.setOnAction {
      DialogUtils.dialog(
        "This option allows to replace some data in repository file pointed in Output field in " +
            "General tab. Uses CurseForge/Twitch modpack configuration file 'minecraftinstance.json' usually located at " +
            "\n'C:\\Users\\<your_username>\\Twitch\\Minecraft\\Instances\\<modpack_name>\\minecraftinstance.json' " +
            "(in Windows). \nIt loads download urls, project ID, file ID and fills them to selected repository." +
            " Matching is based on file names.", "Fill some data from CurseForge",
        Alert.AlertType.INFORMATION, ButtonType.OK
      )
    }
    
    cmFormat.items.setAll(listOf("YAML", "JSON"))
    cmFormat.selectionModel.select(0)
    
    MainView.initializeTreeTable(treeView)
    treeView.columns.add(TreeTableColumn<Entry, String>("Action").also {
      it.cellFactory = ViewUtils.buttonsInTreeTableCellFactory(
        { mapOf("setUrl" to Button("Set Url").apply { padding = Insets(0.0, 5.0, 0.0, 5.0) }) },
        { item, map ->
          if (!item.value.group) {
            listOf(map["setUrl"]!!.apply {
              setOnAction {
                DialogUtils.inputDialog("ShvFileMatcher - Set URL", "URL for entry '${item.value.name}'")
                  .let { item.value.src = it.get() }; treeView.refresh()
              }
            })
          } else {
            emptyList()
          }
        })
    })
    
    menuBar.menus.setAll(listOf(
      Menu("Operations").apply {
        items.setAll(listOf(
          MenuItem("Update hashes").apply {
            setOnAction {
              updateHashes()
            }
          },
          MenuItem("Load from file").apply {
            setOnAction {
              val output = validateOutputFile() ?: return@setOnAction
              repository = RepositoryService().loadRepositoryFromFile(output)
              displayRepository()
            }
          }
        ))
      }
    ))
  }
  
  private fun updateHashes() {
    val sourceDir = validateSourceDir() ?: return
    val output = validateOutputFile() ?: return
    if (!output.exists()) {
      DialogUtils.dialog(
        "File does not exists: '${output}'",
        "Output file is incorrect or empty",
        Alert.AlertType.ERROR,
        ButtonType.OK
      )
      return
    }
    try {
      lbProcessState.text = "Updating..."
      pbProcess.progress = ProgressBar.INDETERMINATE_PROGRESS
      changeButtonEnable(false)
      
      val service = RepositoryService()
      RepositoryHashUpdater(sourceDir, repository ?: service.loadRepositoryFromFile(output)) {
        lbProcessState.text = ""
        pbProcess.progress = 0.0
        changeButtonEnable(true)
        
        when (it.type) {
          ResultType.SUCCESS -> {
            Configuration.get().devTools.sourcePath = sourceDir.absolutePath
            Configuration.get().devTools.outputPath = output.absolutePath
            Configuration.get().save()
            repository = it.data
            displayRepository()
          }
          ResultType.ERROR -> DialogUtils.textAreaDialog(
            "Details",
            it.error?.message.orEmpty(),
            "Error occurred when updating"
          )
        }
      }.start()
    } catch (e: Exception) {
      DialogUtils.textAreaDialog(
        "Details",
        e.message.orEmpty(),
        "Error occurred when updating"
      )
    }
    
    
  }
  
  private fun displayRepository() {
    if (repository != null) {
      val bundle = repository!!.bundles.maxByOrNull { it.updateDate ?: OffsetDateTime.MIN }!!
      version = bundle.versions.last()
      treeView.root = ViewUtils.toTreeItems(version!!.entries)
    } else {
      treeView.root = TreeItem()
    }
  }
  
  private fun generate() {
    var sourceDir = validateSourceDir() ?: return
    
    try {
      lbProcessState.text = "Generating..."
      pbProcess.progress = ProgressBar.INDETERMINATE_PROGRESS
      changeButtonEnable(false)
      
      RepositoryGenerator(sourceDir) {
        lbProcessState.text = ""
        pbProcess.progress = 0.0
        changeButtonEnable(true)
        
        when (it.type) {
          ResultType.SUCCESS -> {
            
            Configuration.get().devTools.sourcePath = sourceDir.absolutePath
            Configuration.get().devTools.bundlePreferredPath = tfBundlePath.text
            Configuration.get().save()
            repository = it.data
            version = it.data!!.bundles.first().versions.last()
            treeView.root = ViewUtils.toTreeItems(version!!.entries)
          }
          ResultType.ERROR -> DialogUtils.textAreaDialog(
            "Details",
            it.error?.message.orEmpty(),
            "Error occurred when generating"
          )
        }
      }.start()
    } catch (e: Exception) {
      DialogUtils.textAreaDialog(
        "Details",
        e.message.orEmpty(),
        "Error occurred when generating"
      )
    }
  }
  
  private fun save(repo: Repository) {
    val output = validateOutputFile() ?: return
    val service = RepositoryService()
    service.saveToFile(repo, output, cmFormat.selectionModel.selectedItem)
    Configuration.get().devTools.outputPath = output.absolutePath
    Configuration.get().save()
    lbProcessState.text = "Saved"
  }
  
  private fun changeButtonEnable(enabled: Boolean) {
    listOf(btnBundlePath, btnOutput, btnPath, btnGenerate).forEach { it.isDisable = !enabled }
  }
  
  
  private fun validateSourceDir(): File? {
    var file = tfPath.text?.let { SystemUtils.parseDirectory(it, null) }
    if (file == null) {
      DialogUtils.dialog(
        "'${tfPath.text}' is not correct directory path",
        "Source directory is incorrect or empty",
        Alert.AlertType.ERROR,
        ButtonType.OK
      )
      return null
    }
    return file
  }
  
  private fun validateOutputFile(): File? {
    val output = File(tfOutput.text)
    if (output.exists() && !output.isFile) {
      DialogUtils.dialog(
        "'${tfOutput.text}' is not correct output file path",
        "Output path is incorrect or empty",
        Alert.AlertType.ERROR,
        ButtonType.OK
      )
      return null
    }
    return output
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
  
  @FXML
  lateinit var btnSave: Button
  
  @FXML
  lateinit var btnTransformUrls: Button
  
  @FXML
  lateinit var btnAddMatchingStrategyToMods: Button
}
