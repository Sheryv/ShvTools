package com.sheryv.tools.filematcher.view

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.model.event.AbortEvent
import com.sheryv.tools.filematcher.model.event.ItemStateChangedEvent
import com.sheryv.tools.filematcher.service.*
import com.sheryv.tools.filematcher.utils.*
import com.sheryv.tools.lasso.util.OnChangeScheduledExecutor
import com.sheryv.util.FileUtils
import com.sheryv.util.VersionUtils
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.input.*
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.exception.ExceptionUtils
import org.greenrobot.eventbus.Subscribe
import java.io.File

class MainView : BaseView() {
  private var context: UserContext = UserContext()
  private var state: ViewProgressState = ViewProgressState()
  private val filters: MutableMap<String, (Entry) -> Boolean> = mutableMapOf()
  private var hideEmptyGroups: Boolean = false
  private var expandAll: Boolean = false
  private var menusToBlock: MutableList<MenuItem> = mutableListOf()
  val updater = OnChangeScheduledExecutor("ViewUpdate_" + javaClass.simpleName, 200) {
    if (context.isFilled()) {
      withContext(Dispatchers.Main) {
        filterItems()
      }
    }
  }
  
  
  init {
    instance = this
  }
  
  override fun initialize() {
    super.initialize()
    try {
      init()
    } catch (e: Exception) {
      DialogUtils.textAreaDialog(
        "Details", e.stackTraceToString(),
        "Error occurred while starting application", Alert.AlertType.ERROR, false, ButtonType.OK
      )
      throw e;
    }
  }
  
  private fun init() {
    createMenu()
    updater.start()
    initializeTreeTable(treeView)
    
    treeView.selectionModel.selectedItems.addListener { item: ListChangeListener.Change<out TreeItem<Entry>> ->
      if (item.list.size == 0 || item.list.size == 1 && !item.list[0].value.group) {
      } else if (item.list.size == 1 && item.list[0].value.group) {
      } else {
      }
    }
    
    treeView.root = TreeItem()
    pbIndicator.isVisible = false
    lbProcessState.textProperty().bind(state.messageProp)
    pbProcess.progressProperty().bind(state.progress)
    state.onChange { inProgress, progress ->
      cmRepositoryUrl.isDisable = inProgress
      cmBundle.isDisable = inProgress
      cmVersion.isDisable = inProgress
      btnPath.isDisable = inProgress
      btnAddRepository.isDisable = inProgress
      btnVerify.isDisable = inProgress
      btnLoad.isDisable = inProgress
      tfPath.isDisable = inProgress
      pbIndicator.isVisible = inProgress
      btnRemoveRepository.isDisable = inProgress
      
      menusToBlock.forEach { it.isDisable = inProgress }
      
      if (inProgress) {
        btnDownload.text = "Abort"
      } else {
        btnDownload.text = "Download"
      }
    }
    
    btnLoad.isDisable = true
    btnRemoveRepository.isDisable = true
    btnDownload.isDisable = true
    btnVerify.isDisable = true
    val recentRepositories = Configuration.get().recentRepositories
    cmRepositoryUrl.items = FXCollections.observableList(recentRepositories.toMutableList())
//    cmRepositoryUrl.items.addListener { c: ListChangeListener.Change<out String> ->  c.}
    cmRepositoryUrl.selectionModel.selectedItemProperty().addListener { _, _, v ->
      if (v != null) {
        btnLoad.isDisable = v.isBlank()
        if (recentRepositories.first() != v) {
          recentRepositories.remove(v)
          recentRepositories.add(0, v)
          Configuration.get().save()
        }
        btnRemoveRepository.isDisable = v.isBlank()
      }
    }
    cmRepositoryUrl.selectionModel.select(0)
    
    btnAddRepository.setOnAction {
      var s = "New repository"
      var h = "Insert url for repository file"
      do {
        val res = DialogUtils.inputDialog(s, h).orElse(null)
          ?: return@setOnAction
        
        val ok = Validator().url(res).isOk()
        if (ok) {
          addRepositoryUrlToList(res)
          return@setOnAction
        }
        s = "Error"
        h = "Incorrect repository url. Please insert another. Example:\nhttp://google.com/repo.json"
      } while (!ok)
    }
    
    btnRemoveRepository.setOnAction {
      removeRepositoryUrlFromList()
    }
    
    cmBundle.selectionModel.selectedItemProperty().addListener { _, _, v ->
      if (v != null) {
        cmVersion.items.setAll(v.versions)
        cmVersion.selectionModel.select(0)
        val path = v.preferredBasePath.findPath()?.let { SystemUtils.parseDirectory(it, null) }
        context.basePath = path?.toPath()?.toAbsolutePath()?.toFile()
        tfPath.text = path?.toPath()?.toAbsolutePath()?.toString()
      }
    }
    cmVersion.selectionModel.selectedItemProperty().addListener { _, _, v ->
      if (v != null) {
        treeView.root = TreeItem()
        updater.executeNow()
        context = UserContext(context.repo, cmBundle.selectionModel.selectedItem?.id, v.versionId, context.basePath)
        updater.executeNow()
        renderFields()
      } else {
        lbTreeState.text = ""
      }
      btnVerify.isDisable = v == null
    }
    
    btnLoad.setOnAction {
      state.setMessage("Loading repo...")
      state.progessIndeterminate()
      RepositoryLoader(cmRepositoryUrl.selectionModel.selectedItem) {
        state.stop()
        when (it.type) {
          ResultType.SUCCESS -> {
            initializeRepo(it.data!!)
          }
          ResultType.ERROR -> DialogUtils.textAreaDialog(
            "Following problems were found at: \n${cmRepositoryUrl.selectionModel.selectedItem}",
            it.error?.message.orEmpty(),
            "Found problems when validating repository configuration"
          )
        }
      }.start()
    }
    
    btnPath.setOnAction {
      DialogUtils.directoryDialog(treeView.scene.window, initialDirectory = tfPath.text).ifPresent {
        tfPath.text = it.toAbsolutePath().toString()
        btnDownload.isDisable = true
      }
    }
    
    btnVerify.setOnAction {
      val directory = verifyAndGetDirectory()
      if (directory != null) {
        state.setMessage("Verification...")
        state.progessIndeterminate()
        context.basePath = directory
        FileMatcher(context) { r ->
          state.stop()
          when (r.type) {
            ResultType.ERROR ->
              DialogUtils.textAreaDialog(
                "Details", r.error?.message.orEmpty() + "\n\n------\n" + ExceptionUtils.getStackTrace(r.error),
                "Error verifying: ", Alert.AlertType.ERROR, wrapText = false
              )
            ResultType.SUCCESS -> btnDownload.isDisable = false
          }
        }.start()
      }
    }
    
    btnDownload.setOnAction {
      if (state.inProgress.get()) {
        postEvent(AbortEvent())
        return@setOnAction
      }
      
      val matcher = FileMatcher(context)
      
      val toDelete = context.getEntries().flatMap { e ->
        val file = matcher.getEntryDir(e).resolve(e.name).toFile()
        e.target.matching.lastMatches.filter { it != file }
      }
      
      var allow = true
      var deleteOld = false
      if (toDelete.isNotEmpty()) {
        val dialog = DialogUtils.textAreaDialog(
          "Do you want to delete them? " +
              "\nChoose 'No' to only download new files and do not delete anything. " +
              "\nChoose 'Yes' to delete old files specified in the following list and download new ones.",
          toDelete.joinToString("\n") { it.absolutePath },
          "The old files exist in selected directory",
          buttons = arrayOf(ButtonType.CANCEL, ButtonType.YES, ButtonType.NO)
        )
        deleteOld = dialog.isPresent && dialog.get() == ButtonType.YES
        allow = deleteOld || dialog.isPresent && dialog.get() == ButtonType.NO
      }
      
      if (allow) {
        val directory = verifyAndGetDirectory()
        if (directory != null) {
          state.setMessage("Downloading files...")
          state.progessIndeterminate()
          context.basePath = directory
          FileSynchronizer(context, state, deleteOld) { r ->
            state.setMessage("Download completed")
            state.stop()
            when (r.type) {
              ResultType.ERROR ->
                DialogUtils.textAreaDialog(
                  "Details", r.error?.message.orEmpty() + "\n\n------\n" + ExceptionUtils.getStackTrace(r.error),
                  "Error while downloading", Alert.AlertType.ERROR, wrapText = false
                )
              
              ResultType.SUCCESS -> {
                if (stage.isFocused) {
                  DialogUtils.dialog("", "Download completed", Alert.AlertType.INFORMATION, ButtonType.OK)
                }
              }
            }
          }.start()
        }
      }
    }
    
    btnClearSearch.tooltip = Tooltip("Clear search value")
    btnClearSearch.setOnAction {
      tfSearch.text = ""
    }
    tfSearch.textProperty().addListener { obs, prev, new ->
      if (new.isBlank()) {
        filters.remove(NAME_FILTER)
      } else {
        filters[NAME_FILTER] = { it.name.contains(new, true) || it.group }
      }
      updater.markChanged()
    }
  }
  
  private fun addRepositoryUrlToList(url: String) {
    val recentRepositories = Configuration.get().recentRepositories
    if (recentRepositories.contains(url)) {
      recentRepositories.remove(url)
    }
    recentRepositories.add(0, url)
    if (recentRepositories.size >= 30) {
      recentRepositories.removeAt(recentRepositories.size - 1)
    }
    Configuration.get().save()
    cmRepositoryUrl.items.clear()
    cmRepositoryUrl.items.addAll(recentRepositories)
    cmRepositoryUrl.selectionModel.select(0)
  }
  
  private fun removeRepositoryUrlFromList() {
    val selectedItem = cmRepositoryUrl.selectionModel.selectedItem
    cmRepositoryUrl.items.remove(selectedItem)
    
    val recentRepositories = Configuration.get().recentRepositories
    recentRepositories.remove(selectedItem)
    Configuration.get().save()
    cmRepositoryUrl.selectionModel.select(0)
  }
  
  private fun initializeRepo(load: Repository) {
    context = UserContext(load)
    treeView.root = TreeItem()
    
    cmBundle.items.setAll(load.bundles)
    cmBundle.selectionModel.select(0)
    cmVersion.items.setAll(load.bundles[0].versions.sortedByDescending { it.versionId })
    cmVersion.selectionModel.select(0)
    
    lbLoadedRepo.text = "Loaded: " + cmRepositoryUrl.selectionModel.selectedItem
  }
  
  private fun renderFields() {
    gridDetails.children.clear()
    val r = context.repo!!
    addDetailsHeaderLocal("Repository")
    
    addDetailsRow("Name", r.codeName)
    addDetailsRow("BaseUrl", r.baseUrl)
    addDetailsRow("Version", r.repositoryVersion)
    addDetailsRow("Website", r.website)
    addDetailsRow("Title", r.title)
    addDetailsRow("Author", r.author)
    addDetailsRow("Update date", Utils.dateFormat(r.updateDate), withSeparator = false)
    
    val b = context.getBundle()
    val v = context.getVersion()
    addDetailsHeaderLocal("Bundle")
    addDetailsRow("Name", b.name)
    addDetailsRow("ID", b.id)
    addDetailsRow("Bundle update date", Utils.dateFormat(b.updateDate))
    addDetailsRow("Versions: aggregate old", (b.versioningMode == BundleMode.AGGREGATE_OLD).toEnglishWord())
    addDetailsRow("Bundle base url", b.getBaseUrl(r.baseUrl))
    addDetailsRow("Description", b.description)
    addDetailsRow("Bundle spec source", b.specSource)
    addDetailsRow("Version", v.versionName)
    addDetailsRow("Version ID", v.versionId.toString())
    addDetailsRow("Version update date", Utils.dateFormat(v.updateDate))
    addDetailsRow("Version URL part", v.versionUrlPart)
    addDetailsRow("Version spec source", v.specSource)
    addDetailsRow("Release notes", v.changesDescription, withSeparator = false)
    
    
    addDetailsHeaderLocal("Additional fields: Repository")
    
    r.additionalFields.forEach { (t, u) ->
      addDetailsRow(t, u)
    }
    
    addDetailsHeaderLocal("Additional fields: Bundle")
    
    b.additionalFields.forEach { (t, u) ->
      addDetailsRow(t, u)
    }
    v.additionalFields.forEach { (t, u) ->
      addDetailsRow(t, u)
    }
    gridDetails.rowConstraints.forEach { it.vgrow = Priority.ALWAYS }
  }
  
  private fun addDetailsHeaderLocal(text: String) {
    addDetailsHeader(text, gridDetails)
  }
  
  private fun addDetailsRow(key: String, value: String?, withSeparator: Boolean = true) {
    addDetailsRow(key, value, gridDetails, withSeparator)
  }
  
  private fun verifyAndGetDirectory(): File? {
    if (tfPath.text.isNullOrBlank()) {
      DialogUtils.dialog("", "Target directory is empty", Alert.AlertType.ERROR, ButtonType.OK)
      return null
    }
    
    val directory = SystemUtils.parseDirectory(tfPath.text, null)
    if (directory == null) {
      DialogUtils.dialog("", "Directory not found: ${tfPath.text.orEmpty()}", Alert.AlertType.ERROR, ButtonType.OK)
      return null
    }
    return directory
  }
  
  private fun createMenu() {
    val states = ItemState.values().mapIndexed { i, state ->
      val keyCode = KeyCode.values().first { it.code == KeyCode.DIGIT1.code + i }
      CheckMenuItem(state.label).apply {
        userData = state
        isSelected = true
        accelerator = KeyCodeCombination(keyCode, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN)
      }
    }
    states.forEach { item ->
      item.setOnAction {
        checkStateFilters(states)
      }
    }
    
    val filtersMenuItems = mutableListOf(
      MenuItem("Only in progress states").apply {
        setOnAction {
          states.forEach {
            it.isSelected = (it.userData as ItemState).toModify
          }
          checkStateFilters(states)
        }
      },
      MenuItem("None").apply {
        setOnAction {
          states.forEach {
            it.isSelected = false
          }
          checkStateFilters(states)
        }
      },
      MenuItem("All").apply {
        setOnAction {
          states.forEach {
            it.isSelected = true
          }
          checkStateFilters(states)
        }
      },
      SeparatorMenuItem()
    )
      .apply { addAll(states) }
    
    menuBar.menus.setAll(listOf(
      Menu("File").apply {
        items.setAll(
          MenuItem("Developer tools").apply {
            setOnAction { createDevToolsWindow() }
            accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN)
          },
          SeparatorMenuItem(),
          MenuItem("Load repository from file").apply {
            accelerator = KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN)
            setOnAction {
              
              state.setMessage("Loading repo...")
              state.progessIndeterminate()
              DialogUtils.openFileDialog(treeView.scene.window, initialFile = Configuration.get().lastLoadedRepoFile).ifPresent {
                Configuration.get().lastLoadedRepoFile = it.toAbsolutePath().toString()
                Configuration.get().save()
                
                RepositoryFileLoader(it) {
                  state.stop()
                  when (it.type) {
                    ResultType.SUCCESS -> {
                      initializeRepo(it.data!!)
                    }
                    ResultType.ERROR -> DialogUtils.textAreaDialog(
                      "Details", it.error?.stackTraceToString().orEmpty(),
                      "Error occurred while loading repository", Alert.AlertType.ERROR, false, ButtonType.OK
                    )
                  }
                }.start()
              }
            }
            menusToBlock.add(this)
          },
          SeparatorMenuItem(),
          MenuItem("Exit").apply {
            accelerator = KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)
            setOnAction { Platform.exit() }
          }
        )
      },
      Menu("Items").apply {
        items.setAll(
          MenuItem("Search by name").apply {
            setOnAction {
              tfSearch.requestFocus()
            }
            accelerator = KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN)
          },
          SeparatorMenuItem(),
          Menu("Filter").apply {
            items.setAll(filtersMenuItems)
          },
          MenuItem("Clear filters").apply {
            setOnAction {
              filters.clear()
              tfSearch.text = ""
              states.forEach { it.isSelected = true }
              updater.markChanged()
            }
            accelerator = KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN)
          },
          SeparatorMenuItem(),
          CheckMenuItem("Hide empty groups").apply {
            setOnAction {
              hideEmptyGroups = isSelected
              filterItems(true)
            }
          },
          SeparatorMenuItem(),
          CheckMenuItem("Keep all entries expanded after change").apply {
            setOnAction {
              expandAll = isSelected
              filterItems(true)
            }
          },
        )
        menusToBlock.add(this)
      },
      Menu("Predefined repositories").apply {
        items.setAll(
          Menu("Minecraft").apply {
            items.setAll(
              MenuItem("Xenypack - Modpack").apply { setOnAction { addRepositoryUrlToList("https://raw.githubusercontent.com/Detronit/xenypack-modpack/master/repository-xenypack.yaml") } }
            )
          },
//          MenuItem("Test").apply {
//            setOnAction {
//
//            }
////            { c, c2 ->
////              if (c.selectionModel.selectedItem == "two") {
////                c2.items.setAll(kotlin.collections.listOf("other", "2.4"))
////                c2.selectionModel.select(0)
////              }
////            }
//            accelerator = KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN)
//          }
        )
        menusToBlock.add(this)
      },
      Menu("Info").apply {
        items.setAll(MenuItem("About").apply {
          setOnAction {
            val msg =
              "Created by Sheryv\nVersion: ${VersionUtils.loadVersionByModuleName("file-matcher-version")}\nWebsite: https://github.com/Sheryv/ShvTools"
            val textArea = TextArea(msg)
            textArea.isEditable = false
            textArea.isWrapText = true
            textArea.prefRowCount = 5
            val gridPane = GridPane()
            gridPane.maxWidth = Double.MAX_VALUE
            gridPane.add(textArea, 0, 0)
            val alert = Alert(Alert.AlertType.NONE, msg, ButtonType.OK)
            alert.dialogPane.content = gridPane
            ViewUtils.appendStyleSheets(alert.dialogPane.content.scene)
            alert.title = "About"
            alert.showAndWait()
          }
        })
      }
    )
    )
  }
  
  private fun checkStateFilters(checks: List<CheckMenuItem>) {
    val selection = checks.filter { it.isSelected }.map { it.userData as ItemState }
    if (selection.size == ItemState.values().size) {
      filters.remove(STATE_FILTER)
    } else {
      filters[STATE_FILTER] = { selection.contains(it.state) || it.group }
    }
    updater.markChanged()
  }
  
  private fun filterItems(refresh: Boolean = false) {
    if (!context.isFilled()) {
      return
    }
    
    val entries = context.getEntries()
    val filtersList = filters.values
    
    val filterResult: List<Entry> = if (filtersList.isNotEmpty())
      entries.filter { e -> filtersList.all { it(e) } }
    else {
      entries
    }
    var count = 0
    ViewUtils.forEachTreeItem(treeView.root) { count++ }
    if (refresh || filterResult != entries || treeView.root.isLeaf || count != filterResult.size) {
      fillEntries(filterResult)
    }
    updateStatusBar(entries, if (filtersList.isEmpty()) null else filterResult)
  }
  
  
  private fun fillEntries(entries: List<Entry>) {
    val item = ViewUtils.toTreeItems(entries)
    if (hideEmptyGroups) {
      val toRemove = mutableSetOf<TreeItem<Entry>>()
      ViewUtils.forEachTreeItem(item) {
        it.isExpanded = getExpansion(it)
        var current = it
        var prev: TreeItem<Entry>? = null
        do {
          val empty = current.value.group && current.children.all { it.value.group }
          
          if ((current.parent == null || !empty) && prev != null) {
            toRemove.add(prev)
          } else if (empty) {
            prev = current
          }
          if (current.parent == null) {
            break
          }
          current = current.parent
        } while (empty)
        
      }
      
      toRemove.forEach { it.parent.children.remove(it) }
    } else {
      ViewUtils.forEachTreeItem(item) { it.isExpanded = getExpansion(it) }
    }

//    ViewUtils.forEachTreeItem(item) { if (!it.isLeaf) it.isExpanded = true }
    treeView.root = item
  }
  
  private fun getExpansion(item: TreeItem<Entry>): Boolean {
    if (expandAll && item.value != null && item.value.group) return true
    if (item.value != null && item.value.group) {
      return ViewUtils.findInTree(treeView.root) { it.value.id == item.value.id }?.isExpanded ?: false
    }
    return false
  }
  
  private fun createDevToolsWindow() {
    ViewUtils.createWindow<DevelopersToolView>("developer-tools.fxml", "ShvFileMatcher - Developer Tools")
  }
  
  @Subscribe
  fun itemStateChangedEvent(e: ItemStateChangedEvent) {
    updater.markChanged()
  }
  
  private fun updateStatusBar(all: List<Entry>, filtered: List<Entry>? = null) {
    val entries = all.filter { !it.group }
    val entriesSize = Utils.fileSizeFormat(entries.mapNotNull { it.fileSize }.sum())
    val synced = entries.count { it.state == ItemState.SYNCED }
    val syncedSize =
      Utils.fileSizeFormat(entries.asSequence().filter { it.state == ItemState.SYNCED }.mapNotNull { it.fileSize }
        .sum())
    val skipped = entries.count { it.state == ItemState.SKIPPED }
    val skippedSize =
      Utils.fileSizeFormat(entries.asSequence().filter { it.state == ItemState.SKIPPED }.mapNotNull { it.fileSize }
        .sum())
    val download = entries.count { it.state.toModify }
    val downloadSize =
      Utils.fileSizeFormat(entries.asSequence().filter { it.state.toModify }.mapNotNull { it.fileSize }.sum())
    
    if (filtered == null || filtered.size == entries.size) {
      lbTreeState.text =
        "Items: ${entries.size} [$entriesSize]; Synced: $synced [$syncedSize], Skipped: $skipped [$skippedSize], To download: $download [$downloadSize]"
    } else {
      val filtered = filtered.filter { !it.group }
      val filterSize = Utils.fileSizeFormat(filtered.mapNotNull { it.fileSize }.sum())
      val syncedFiltered = filtered.count { it.state == ItemState.SYNCED }
      val skippedFiltered = filtered.count { it.state == ItemState.SKIPPED }
      val downloadFiltered = filtered.count { it.state.toModify }
      val downloadFilteredSize =
        Utils.fileSizeFormat(filtered.asSequence().filter { it.state.toModify }.mapNotNull { it.fileSize }.sum())
      lbTreeState.text =
        "[Filtered/All] Items: ${filtered.size}/${entries.size} [$filterSize/$entriesSize]; Synced: $syncedFiltered/$synced [$syncedSize], Skipped: $skippedFiltered/$skipped [$skippedSize], To download: $downloadFiltered/$download [$downloadFilteredSize/$downloadSize]"
    }
  }
  
  companion object {
    @JvmStatic
    lateinit var instance: MainView
    
    private const val STATE_FILTER = "STATE_FILTER"
    private const val NAME_FILTER = "NAME_FILTER"
    
    @JvmStatic
    fun initializeTreeTable(treeView: TreeTableView<Entry>) {
      treeView.columns.setAll(
        ViewUtils.createTreeColumn("Name", 300) { it.name },
        TreeTableColumn<Entry, String>("State").also {
          it.setCellValueFactory { if (it.value.value.group) SimpleStringProperty("") else it.value.value.stateProperty.asString() }
          it.cellFactory = ViewUtils.treeTableCellFactoryWithCustomCss<Entry>(
            setOf(
              "bg-purple",
              "bg-yellow",
              "bg-green",
              "bg-red",
              "bg-grey",
              "bg-blue",
              "bg-orange"
            )
          ) {
            if (it.group) emptyList() else listOf(it.state.cssClass)
          }
          it.prefWidth = 210.0
        },
        ViewUtils.createTreeColumn("Path") { it.target.directory?.findPath().orEmpty() },
        ViewUtils.createTreeColumn("Version") { it.version ?: if (it.group) "" else "-" },
        ViewUtils.createTreeColumn("Date", 100) { if (!it.group) Utils.dateFormat(it.itemDate) else "" },
        ViewUtils.createTreeColumn(
          "Size", 60,
          alignRight = true
        ) { if (!it.group) Utils.fileSizeFormat(it.fileSize) else "" },
        ViewUtils.createTreeColumn("Override") {
          " " + (if (!it.group) it.target.override.toEnglishWord() else "") +
              (it.target.matching.getPattern()?.let { " ($it)" } ?: "")
        },
        ViewUtils.createTreeColumn("Category") { it.category.orEmpty() },
        ViewUtils.createTreeColumn("Tags") { it.tags?.joinToString { ", " }.orEmpty() },
        ViewUtils.createTreeColumn("ID", 100) { it.id },
        ViewUtils.createTreeColumn("URL", 150) {
          if (it.group) "" else it.getSrcUrlOrNull(instance.context) ?: it.src
        },
        ViewUtils.createTreeColumn("Description", 150) { it.description },
      )

//        treeView.columns.add(TreeTableColumn<Entry, String>("URL").also {


//          it.cellFactory = ViewUtils.buttonsInTreeTableCellFactory(buttons) { treeItem, map ->
//            val e = treeItem.value!!
//            map.values.first().setOnAction {
//              val content = ClipboardContent()
//              content.putString(e.getSrcUrl(context.getBundle()!!.getBaseUrl(context.repo!!.baseUrl)))
//              Clipboard.getSystemClipboard().setContent(content)
//            }
//            return@buttonsInTreeTableCellFactory if (e.group) emptyList() else map.values
//          }
//        })
      
      treeView.onMousePressed = EventHandler { event ->
        if (event.isPrimaryButtonDown && event.clickCount == 2 && treeView.selectionModel?.selectedItem?.value != null) {
          val item = treeView.selectionModel.selectedItem.value
          val alert = Alert(Alert.AlertType.INFORMATION, "Details", ButtonType.OK)
          alert.headerText = item.name
          alert.title = "ShvFileMatcher - Item details - ${item.name}"
          val grid = GridPane()
          grid.maxWidth = java.lang.Double.MAX_VALUE
          grid.columnConstraints.setAll(
            ColumnConstraints(150.0),
            ColumnConstraints(10.0, 100.0, Double.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true),
            ColumnConstraints(10.0, 60.0, Double.MAX_VALUE, Priority.NEVER, HPos.CENTER, true)
          )
          
          addDetailsHeader("Details", grid)
          addDetailsRow("ID", item.id, grid)
          addDetailsRow("Name", item.name, grid)
          addDetailsRow("Src", item.getSrcUrl(instance.context), grid)
          addDetailsRow("Parent", item.parent, grid)
          addDetailsRow("Target Path", item.target.directory?.findPath(), grid)
          addDetailsRow("Override", item.target.override.toEnglishWord(), grid)
          addDetailsRow("File matching pattern", item.target.matching.getPattern(), grid)
          addDetailsRow("Size", Utils.fileSizeFormat(item.fileSize), grid)
          addDetailsRow("Target Path is absolute", item.target.absolute.toEnglishWord(), grid)
          addDetailsRow("Description", item.description, grid)
          addDetailsRow("Version", item.version, grid)
          addDetailsRow("Website", item.website, grid)
          addDetailsRow("Category", item.category, grid)
          addDetailsRow("Tags", item.tags?.joinToString(), grid, false)
          
          if (item.hashes?.hasAny() == true) {
            val h = item.hashes!!
            addDetailsHeader("Hashes", grid)
            if (h.md5 != null) {
              addDetailsRow("MD5", h.md5, grid)
            }
            if (h.sha1 != null) {
              addDetailsRow("SHA-1", h.sha1, grid)
            }
            if (h.sha256 != null) {
              addDetailsRow("SHA-256", h.sha256, grid)
            }
            if (h.crc32 != null) {
              addDetailsRow("CRC32", h.crc32, grid)
            }
          }
          
          addDetailsHeader("Additional fields: Item", grid)
          item.additionalFields.forEach { (t, u) ->
            addDetailsRow(t, u, grid)
          }
          val scroll = ScrollPane(grid)
          scroll.isFitToWidth = true
          scroll.isFitToHeight = true
          scroll.prefViewportHeight = 450.0
          scroll.prefViewportWidth = 800.0
          grid.padding = Insets(5.0, 10.0, 10.0, 10.0)
          alert.dialogPane.content = scroll
          ViewUtils.appendStyleSheets(alert.dialogPane.scene)
          alert.isResizable = true
          alert.showAndWait()
        } else if (event.isSecondaryButtonDown) {
          val item = treeView.selectionModel.selectedItem
          
          if (item != null) {
            treeView.contextMenu?.hide()
            if (item.value.state != ItemState.SKIPPED) {
              treeView.contextMenu = ContextMenu(
                MenuItem("Set state to SKIPPED").apply {
                  setOnAction {
                    item.value.state = ItemState.SKIPPED
                    postEvent(ItemStateChangedEvent(item.value))
                  }
                },
              )
            } else {
              treeView.contextMenu = ContextMenu(
                MenuItem("Set state to UNKNOWN").apply {
                  setOnAction {
                    item.value.state = ItemState.UNKNOWN
                    postEvent(ItemStateChangedEvent(item.value))
                  }
                },
              )
            }
            treeView.contextMenu.items.add(MenuItem("Export to CSV").apply {
              setOnAction {
                if (item.value.group) {
                  exportToCsv(item.children.map { it.value }.filter { !it.group }, item.value.name)
                } else {
                  exportToCsv(listOf(item.value), item.value.name)
                }
              }
            })
            treeView.contextMenu.show(item.graphic, event.screenX, event.screenY)
          }
        }
      }
    }
    
    private fun exportToCsv(items: List<Entry>, groupName: String = "") {
      val lastIndexOf = groupName.lastIndexOf('.')
      val name = if (lastIndexOf > 0 && groupName.length - lastIndexOf < 6) {
        groupName.take(lastIndexOf)
      } else {
        groupName
      }.let {
        var res = it
        SystemUtils.fileNameForbiddenChars().forEach { res = res.replace(it, '_') }
        res
      }
      
      DialogUtils.inputDialog("Export to CSV", "Choose CSV column separator. Default is semicolon (;)").ifPresent {
        val sep = it.ifBlank { ";" }
        DialogUtils.saveFileDialog(instance!!.stage, "Export to CSV", "export-$name.csv").ifPresent {
          FileUtils.writeFileStream(it).use { bw ->
            bw.appendLine(
              "ID,Name,Override,File matching pattern,Size,Category,Tags,Description,SRC,Date,Version,Website".replace(
                ",",
                sep
              )
            )
            items.forEach { i ->
              bw.append(i.id).append(sep)
                .append(i.name).append(sep)
                .append(i.target.override.toString()).append(sep)
                .append(i.target.matching.getPattern().orEmpty()).append(sep)
                .append(i.fileSize?.toString().orEmpty()).append(sep)
                .append(i.category.orEmpty()).append(sep)
                .append(i.tags?.joinToString(",").orEmpty()).append(sep)
                .append(i.description).append(sep)
                .append(i.src).append(sep)
                .append(i.itemDate?.toString().orEmpty()).append(sep)
                .append(i.version.orEmpty()).append(sep)
                .append(i.website.orEmpty())
                .appendLine()
            }
          }
        }
      }
    }
    
    
    private fun addDetailsHeader(text: String, pane: GridPane) {
      val lk = Label(text)
      lk.styleClass.add("additional-fields-header")
      lk.maxWidth = Double.MAX_VALUE
      pane.add(lk, 0, pane.rowCount, 3, 1)
    }
    
    private fun addDetailsRow(key: String, value: String?, pane: GridPane, withSeparator: Boolean = true) {
      val height = 25.0
      
      val lk = Label(key)
      lk.minHeight = height
      lk.prefHeight = height
      lk.tooltip = Tooltip(key)
      val lv = if (value != null && Validator().url(value).isOk()) {
        Hyperlink(value).apply {
          maxHeight = height
          prefHeight = height
          setOnAction { SystemUtils.openLink(value) }
        }
      } else {
        Label(value.orEmpty())
      }
      lv.tooltip = Tooltip(value.orEmpty())
      
      val row = pane.rowCount
      pane.add(lk, 0, row, 1, 1)
      pane.add(lv, 1, row, 1, 1)
      
      if (!value.isNullOrBlank()) {
        val cp = Button("Copy")
        cp.maxHeight = height - 10
        cp.prefHeight = height - 10
        cp.setOnAction {
          val content = ClipboardContent()
          content.putString(value)
          Clipboard.getSystemClipboard().setContent(content)
        }
        pane.add(cp, 2, row, 1, 1)
      }
      
      if (withSeparator) {
        pane.add(Separator(Orientation.HORIZONTAL).apply { maxHeight = 5.0 }, 0, row + 2, 3, 1);
      }
    }
  }
  
  @FXML
  lateinit var treeView: TreeTableView<Entry>
  
  @FXML
  lateinit var lbLoadedRepo: Label
  
  @FXML
  lateinit var btnLoad: Button
  
  @FXML
  lateinit var tabPane: TabPane
  
  @FXML
  lateinit var btnAddRepository: Button
  
  @FXML
  lateinit var btnDownload: Button
  
  @FXML
  lateinit var btnVerify: Button
  
  @FXML
  lateinit var btnPath: Button
  
  @FXML
  lateinit var cmRepositoryUrl: ComboBox<String>
  
  @FXML
  lateinit var cmVersion: ComboBox<BundleVersion>
  
  @FXML
  lateinit var cmBundle: ComboBox<Bundle>
  
  @FXML
  lateinit var tfPath: TextField
  
  @FXML
  lateinit var gridDetails: GridPane
  
  @FXML
  lateinit var pbProcess: ProgressBar
  
  @FXML
  lateinit var pbIndicator: ProgressIndicator
  
  @FXML
  lateinit var lbTreeState: Label
  
  @FXML
  lateinit var lbProcessState: Label
  
  @FXML
  lateinit var menuBar: MenuBar
  
  @FXML
  lateinit var tfSearch: TextField
  
  @FXML
  lateinit var btnClearSearch: Button
  
  @FXML
  lateinit var btnRemoveRepository: Button
}
