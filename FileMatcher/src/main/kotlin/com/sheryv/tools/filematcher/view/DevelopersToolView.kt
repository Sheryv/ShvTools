package com.sheryv.tools.filematcher.view

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.service.*
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.ViewUtils
import com.sheryv.tools.filematcher.utils.lg
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.Pane
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern

class DevelopersToolView : BaseView() {
  
  private var context: DevContext? = null
    set(value) {
      field = value
      changeButtonEnable(value != null, true)
      lbRepository.text = value?.repo?.toUniqueString() ?: "-"
    }
  
  private lateinit var menuItemsBaseOnContext: List<MenuItem>
  
  override fun initialize() {
    super.initialize()
    if (Configuration.get().devTools.sourcePath != null) {
      tfPath.text = Configuration.get().devTools.sourcePath
    }
    if (Configuration.get().devTools.outputPath != null) {
      tfOutput.text = Configuration.get().devTools.outputPath
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
      val initialDirectory = if (tfBundlePath.text.isNullOrBlank()) Configuration.get().devTools.options.bundleBasePath
        ?: SystemUtils.userDownloadDir() else tfBundlePath.text
      DialogUtils.directoryDialog(btnBundlePath.scene.window, initialDirectory = initialDirectory).ifPresent {
        tfBundlePath.text = it.toAbsolutePath().toString()
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
    
    btnSave.setOnAction { save() }
    
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
          
          MinecraftService().fillDataFromCurseForgeJson(context!!.version, it.toFile(), tfOutput.text)
          DialogUtils.dialog("", "Completed", Alert.AlertType.INFORMATION, ButtonType.OK)
          treeView.refresh()
        }
    }
    
    btnTransformUrls.setOnAction {
      validateContext() ?: return@setOnAction
      MinecraftService().transformUrls(context!!.version)
      lbProcessState.text = "URLs fixed"
      treeView.refresh()
    }
    
    btnAddMatchingStrategyToMods.setOnAction {
      validateContext() ?: return@setOnAction
      val errors = MinecraftService().addMinecraftModsMatcher(context!!.version)
      updateView()
      DialogUtils.textAreaDialog(
        "File that were not matched are listed below",
        errors.joinToString("\n"),
        "Completed",
        Alert.AlertType.INFORMATION
      )
    }
    
    btnListItems.setOnAction {
      val sourceDir = validateSourceDir() ?: return@setOnAction
      val context = validateContext() ?: return@setOnAction
      
      if (context.version.entries.isNotEmpty()) {
        DialogUtils.dialog(
          "There are already loaded items in current list. Do you want to replace them or merge with new ones. " +
              "If you choose merge, all old items will leave in list (if they don't point to the same file in new list) " +
              "even if they don't exist in filesystem now.\n\nChoose Yes to replace old items\nChoose No to merge items",
          null,
          Alert.AlertType.INFORMATION,
          ButtonType.YES,
          ButtonType.NO
        )
      } else {
        Optional.of(ButtonType.YES)
      }.ifPresent {
        
        lbProcessState.text = "Listing files..."
        pbProcess.progress = ProgressBar.INDETERMINATE_PROGRESS
        changeButtonEnable(false)
        
        EntryGenerator(sourceDir, context, it == ButtonType.YES) {
          lbProcessState.text = ""
          pbProcess.progress = 0.0
          changeButtonEnable(true)
          
          when (it.type) {
            ResultType.SUCCESS -> {
              Configuration.get().devTools.sourcePath = sourceDir.absolutePath
              Configuration.get().save()
              context.version.entries = it.data!!
              updateView()
            }
            ResultType.ERROR -> DialogUtils.textAreaDialog(
              "Details",
              it.error?.message.orEmpty(),
              "Error occurred when listing items form filesystem"
            )
          }
          
        }.start()
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
    
    btnLoadFromFile.setOnAction {
      DialogUtils.openFileDialog(treeView.scene.window, initialFile = Configuration.get().lastLoadedRepoFile).ifPresent {
        Configuration.get().lastLoadedRepoFile = it.toAbsolutePath().toString()
        Configuration.get().save()
        changeButtonEnable(true)
  
        RepositoryFileLoader(it) {
          changeButtonEnable(false)
  
          when (it.type) {
            ResultType.SUCCESS -> {
              displayRepository(it.data!!)
            }
            ResultType.ERROR -> DialogUtils.textAreaDialog(
              "Details", it.error?.stackTraceToString().orEmpty(),
              "Error occurred while loading repository", Alert.AlertType.ERROR, false, ButtonType.OK
            )
          }
        }.start()
      }
    }
    
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
          MenuItem("Clear view / memory").apply {
            setOnAction {
              context = null
              updateView()
            }
          }
        ))
        menuItemsBaseOnContext = items.toList()
      },
      Menu("Tools").apply {
        items.setAll(
          listOf(
            MenuItem("Remove duplicates in directory").apply {
              setOnAction {
                removeDuplicates()
              }
            },
          )
        )
      }
    ))
    
    val s = Configuration.get().devTools.options
    cmFormat.items.setAll(SaveOptions.FORMATS)
    cmFormat.selectionModel.select(s.format)
    chkOverrideItems.isSelected = s.overrideExistingItems
    chkSplitVersions.isSelected = s.splitVersionsToFiles
    context = null
    updateView()
  }
  
  private fun updateHashes() {
    val sourceDir = validateSourceDir() ?: return
    val output = validateOutputFile() ?: return
    validateContext() ?: return
    if (!output.exists()) {
      DialogUtils.dialog(
        "File does not exists: '${output}'",
        "Output file is incorrect or empty",
        Alert.AlertType.ERROR,
        ButtonType.OK
      )
      return
    }
    ViewUtils.withErrorHandler {
      lbProcessState.text = "Updating..."
      pbProcess.progress = ProgressBar.INDETERMINATE_PROGRESS
      changeButtonEnable(false)
      
      EntryHashUpdater(sourceDir, context!!.repo) {
        lbProcessState.text = ""
        pbProcess.progress = 0.0
        changeButtonEnable(true)
        
        when (it.type) {
          ResultType.SUCCESS -> {
            Configuration.get().devTools.sourcePath = sourceDir.absolutePath
            Configuration.get().devTools.outputPath = output.absolutePath
            Configuration.get().save()
            displayRepository(context!!.repo)
          }
          ResultType.ERROR -> DialogUtils.textAreaDialog(
            "Details",
            it.error?.message.orEmpty(),
            "Error occurred when updating"
          )
        }
      }.start()
    }
    
    
  }
  
  private fun displayRepository(r: Repository, v: BundleVersion? = null) {
    var version = v
    var bundle: Bundle? = v?.bundle
    
    if (version == null) {
      if (r.bundles.size == 1 && r.bundles[0].versions.size == 1) {
        bundle = r.bundles[0]
        version = bundle.versions[0]
      } else {
        val bundles = r.bundles.map { it.toUniqueString() }
        val res = DialogUtils.twoComboDialog(
          "Choose bundle and version to display",
          "Bundle", "Version", { bundles },
          {
            if (it == null) {
              r.bundles[0].versions.sortedByDescending { it.versionId }.map { it.toUniqueString() }
            } else {
              r.bundles.first { b -> b.toUniqueString() == it.first }.versions.sortedByDescending { it.versionId }
                .map { it.toUniqueString() }
            }
          },
        )
        if (res.isPresent) {
          bundle = r.bundles.first { it.toUniqueString() == res.get().first }
          version = bundle.versions.first { it.toUniqueString() == res.get().second }
        }
      }
    }
    
    if (version != null && bundle != null) {
      context = DevContext(r, version)
    }
    updateView()
  }
  
  private fun updateView() {
    if (context != null) {
      val c = context!!
      tfOBundleId.text = c.bundle.id
      tfOBundleName.text = c.bundle.name
      tfOBundleUrl.text = c.bundle.baseItemUrl
      tfOVersionId.text = c.version.versionId.toString()
      tfOVersionName.text = c.version.versionName
      tfORepoUrl.text = c.repo.baseUrl
      tfORepoName.text = c.repo.codeName
      tfORepoTitle.text = c.repo.title
      tfBundlePath.text = c.bundle.preferredBasePath.systemAwareValue()
      treeView.root = ViewUtils.toTreeItems(c.version.entries)
    } else {
      val o = Configuration.get().devTools.options
      tfOBundleId.text = o.bundleId
      tfOBundleName.text = o.bundleName
      tfOBundleUrl.text = o.bundleUrl
      tfOVersionId.text = o.versionId.toString()
      tfOVersionName.text = o.versionName
      tfORepoUrl.text = o.repoUrl
      tfORepoName.text = o.repoName
      tfORepoTitle.text = o.repoTitle
      tfBundlePath.text = o.bundleBasePath
      treeView.root = TreeItem()
    }
  }
  
  private fun generate() {
    try {
      val options = resolveOptions()
      RepositoryGenerator(options) {
        
        when (it.type) {
          ResultType.SUCCESS -> {
            
            context = DevContext(it.data!!, it.data.bundles.first().versions.maxByOrNull { it.versionId }!!)
            treeView.root = ViewUtils.toTreeItems(context!!.version.entries)
          }
          ResultType.ERROR -> DialogUtils.textAreaDialog(
            "Details",
            it.error?.message.orEmpty(),
            "Error occurred when generating"
          )
        }
      }.start()
    } catch (e: Exception) {
      lg().error("Error occurred when generating", e)
      DialogUtils.textAreaDialog(
        "Details",
        e.message.orEmpty(),
        "Error occurred when generating"
      )
    }
  }
  
  private fun resolveOptions() = SaveOptions(
    tfOBundleId.text,
    tfOBundleName.text,
    tfOBundleUrl.text,
    tfOVersionId.text,
    tfOVersionName.text,
    tfORepoUrl.text,
    tfORepoName.text,
    tfORepoTitle.text,
    tfBundlePath.text ?: "",
    cmFormat.selectionModel.selectedItem,
    chkSplitVersions.isSelected,
    chkOverrideItems.isSelected
  )
  
  private fun save() {
    val context = validateContext() ?: return
    val output = validateOutputFile() ?: return
    val options = resolveOptions()
    changeButtonEnable(false)
    RepositorySaver(context, output, options) {
      changeButtonEnable(true)
      when (it.type) {
        ResultType.SUCCESS -> {
          Configuration.get().devTools.outputPath = output.absolutePath
          Configuration.get().devTools.options = options
          Configuration.get().save()
          val saved = it.data!!
          displayRepository(saved.repo, saved.version)
          lbProcessState.text = "Saved"
        }
        ResultType.ERROR -> DialogUtils.textAreaDialog(
          "Details",
          it.error?.message.orEmpty(),
          "Error occurred when saving"
        )
      }
    }.start()
  }
  
  private fun changeButtonEnable(enabled: Boolean, onlyContextBased: Boolean = false) {
    listOf(
      btnTransformUrls,
      btnAddMatchingStrategyToMods,
      btnTransformUrls,
      btnCurseForge,
      btnSave,
      btnListItems,
    ).forEach { it.isDisable = !enabled }
    
    if (!onlyContextBased) {
      listOf(
        btnGenerate,
        btnOutput,
        btnPath,
        btnGenerate,
        btnLoadFromFile,
        paneOptions
      ).forEach { it.isDisable = !enabled }
      listOf(tfOutput, tfPath).forEach { it.isEditable = enabled }
    }
    
    menuItemsBaseOnContext.forEach { it.isDisable = !enabled }
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
  
  private fun validateContext(): DevContext? {
    if (context == null) {
      DialogUtils.dialog("No repository loaded. Generate one or load from file")
    }
    return context
  }
  
  private val MOD_FULL_REGEX = Regex("([ +A-Za-z\\d_-]+(\\d+)?)[._-]([\\w.]+[_-])?(.*)")
  private val MOD_SHORT_REGEX = Regex("([ +A-Za-z\\d_-]+)([ ._-]\\w*\\d*)*")
  private val CHECK_MOD_FULL_REGEX = Regex("\\d\\.\\d")
  private fun removeDuplicates() {
    
    DialogUtils.directoryDialog(stage, initialDirectory = SystemUtils.userDownloadDir()).ifPresent {
      val files = it.toFile().listFiles()!!.filter { it.isFile }
      val patterns = files.map { file ->
        val matchResult = if (CHECK_MOD_FULL_REGEX.find(file.name) != null) {
          MOD_FULL_REGEX.matchEntire(file.name)
        } else {
          MOD_SHORT_REGEX.matchEntire(file.name)
        }
        check(matchResult != null) { "Cannot match $file" }
        Pattern.quote(matchResult!!.groupValues[1].trim()) + ".*" to mutableListOf<File>()
      }.toMap()
      patterns.forEach { p ->
        val pat = Regex(p.key)
        p.value.addAll(files.filter { pat.matches(it.name) }.sortedBy { it.lastModified() })
      }
      val toDelete = patterns.values.filter { it.size > 1 }.map { it.take(it.size - 1) }.flatten().toSortedSet()
      DialogUtils.textAreaDialog(
        "Confirm deletion",
        "Found ${toDelete.size} duplicates out of ${files.size} files in ${it.toAbsolutePath()}." +
            "\nDo you want to continue deleting following files:\n${toDelete.joinToString("\n") { it.name }}",
        buttons = arrayOf(ButtonType.YES, ButtonType.CANCEL)
      ).ifPresent {
        if (it == ButtonType.YES) {
          toDelete.forEach { it.delete() }
        }
      }
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
  lateinit var lbRepository: Label
  
  @FXML
  lateinit var menuBar: MenuBar
  
  @FXML
  lateinit var btnSave: Button
  
  @FXML
  lateinit var btnTransformUrls: Button
  
  @FXML
  lateinit var btnAddMatchingStrategyToMods: Button
  
  @FXML
  lateinit var btnLoadFromFile: Button
  
  @FXML
  lateinit var btnListItems: Button
  
  @FXML
  lateinit var chkSplitVersions: CheckBox
  
  @FXML
  lateinit var chkOverrideItems: CheckBox
  
  @FXML
  lateinit var tfOBundleId: TextField
  
  @FXML
  lateinit var tfOBundleName: TextField
  
  @FXML
  lateinit var tfOBundleUrl: TextField
  
  @FXML
  lateinit var tfOVersionId: TextField
  
  @FXML
  lateinit var tfOVersionName: TextField
  
  @FXML
  lateinit var tfORepoUrl: TextField
  
  @FXML
  lateinit var tfORepoName: TextField
  
  @FXML
  lateinit var tfORepoTitle: TextField
  
  @FXML
  lateinit var paneOptions: Pane
  
}
