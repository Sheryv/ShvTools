package com.sheryv.tools.filematcher.view

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.model.event.AbortEvent
import com.sheryv.tools.filematcher.model.event.ItemStateChangedEvent
import com.sheryv.tools.filematcher.service.*
import com.sheryv.tools.filematcher.utils.*
import com.sheryv.tools.lasso.util.OnChangeScheduledExecutor
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
  private lateinit var filterProvider: () -> List<ItemState>
  val updater = OnChangeScheduledExecutor("ViewUpdate_" + javaClass.simpleName, 400) {
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
    createMenu()
    updater.start()
    initializeTreeTable(treeView, context)
    
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
  
      if (inProgress) {
        btnDownload.text = "Abort"
      } else {
        btnDownload.text = "Download"
      }
    }
    
    btnLoad.isDisable = true
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
    
    cmBundle.selectionModel.selectedItemProperty().addListener { _, _, v ->
      if (v != null) {
        cmVersion.items.setAll(v.versions)
        cmVersion.selectionModel.select(0)
        val path = v.preferredBasePath.findPath()?.let { SystemUtils.parseDirectory(it, null) }
        context.basePath = path
        tfPath.text = path?.absolutePath
      }
    }
    cmVersion.selectionModel.selectedItemProperty().addListener { _, _, v ->
      if (v != null) {
        context = UserContext(context.repo, cmBundle.selectionModel.selectedItem?.id, v.versionId, context.basePath)
        updater.executeNow()
      } else {
        lbTreeState.text = ""
      }
      btnDownload.isDisable = v == null
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
              "Found problems when validating repository configuration")
        }
      }.start()
    }
    
    btnPath.setOnAction {
      DialogUtils.directoryDialog(treeView.scene.window, initialDirectory = tfPath.text).ifPresent {
        tfPath.text = it.toAbsolutePath().toString()
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
              DialogUtils.textAreaDialog("Details", r.error?.message.orEmpty() + "\n\n------\n" + ExceptionUtils.getStackTrace(r.error),
                  "Error verifying: ", Alert.AlertType.ERROR, wrapText = false)
  
          }
        }.start()
      }
    }
    
    btnDownload.setOnAction {
      if (state.inProgress.get()) {
        postEvent(AbortEvent())
        return@setOnAction
      }
  
      val directory = verifyAndGetDirectory()
      if (directory != null) {
        state.setMessage("Downloading files...")
        state.progessIndeterminate()
        context.basePath = directory
        FileSynchronizer(context, state) { r ->
          state.stop()
          when (r.type) {
            ResultType.ERROR ->
              DialogUtils.textAreaDialog("Details", r.error?.message.orEmpty() + "\n\n------\n" + ExceptionUtils.getStackTrace(r.error),
                  "Error while downloading", Alert.AlertType.ERROR, wrapText = false)
  
            ResultType.SUCCESS -> DialogUtils.dialog("", "Completed", Alert.AlertType.INFORMATION, ButtonType.OK)
          }
        }.start()
      }
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
  
  private fun initializeRepo(load: Repository) {
    context = UserContext(load)
    
    cmBundle.items.setAll(load.bundles)
    cmBundle.selectionModel.select(0)
    cmVersion.items.setAll(load.bundles[0].versions)
    cmVersion.selectionModel.select(0)
    
    lbLoadedRepo.text = "Loaded: " + cmRepositoryUrl.selectionModel.selectedItem
    renderFields()
  }
  
  private fun fillEntries(entries: List<Entry>) {
    val item = ViewUtils.toTreeItems(entries)
    ViewUtils.forEachTreeItem(item) { if (!it.isLeaf) it.isExpanded = true }
    treeView.root = item
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
    addDetailsRow("Update date", Utils.dateFormat(b.updateDate))
    addDetailsRow("Versions: aggregate old", (b.versioningMode == BundleMode.AGGREGATE_OLD).toEnglishWord())
    addDetailsRow("Bundle base url", b.getBaseUrl(r.baseUrl))
    addDetailsRow("Description", b.description)
    addDetailsRow("Version", v.versionName)
    addDetailsRow("Version ID", v.versionId.toString())
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
    filterProvider = { states.filter { it.isSelected }.map { it.userData as ItemState } }
    states.forEach { item ->
      item.setOnAction {
        val userData = item.userData as ItemState
        filterItems(states.filter { it.isSelected }.map { it.userData as ItemState }, item.isSelected, userData)
      }
    }
  
    val filters = mutableListOf(
        MenuItem("Only in progress states").apply {
          setOnAction {
            states.forEach {
              it.isSelected = (it.userData as ItemState).toModify
              filterItems()
            }
          }
        },
        MenuItem("None").apply {
          setOnAction {
            states.forEach {
              it.isSelected = false
              filterItems()
            }
          }
        },
        SeparatorMenuItem())
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
                  RepositoryService().loadRepositoryFromFile(treeView.scene.window)?.run { initializeRepo(this) }
                }
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
              Menu("Filter").apply {
                items.setAll(filters)
              },
              MenuItem("Clear filters").apply {
                setOnAction {
                  states.forEach { it.isSelected = true }
                  filterItems(ItemState.values().toList())
                }
                accelerator = KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN)
              },
              SeparatorMenuItem()
          )
        },
        Menu("Predefined repositories").apply {
          items.setAll(
              Menu("Minecraft").apply {
                items.setAll(
                    MenuItem("Xenypack - Modpack").apply { setOnAction { addRepositoryUrlToList("https://raw.githubusercontent.com/Detronit/xenypack-modpack/master/repository-xenypack.yaml") } }
                )
              }
          )
        },
        Menu("Info").apply {
          items.setAll(MenuItem("About").apply {
            setOnAction {
              val msg = "Created by Sheryv\nVersion: ${VersionUtils.loadVersionByModuleName("file-matcher-version")}\nWebsite: https://github.com/Sheryv/ShvTools"
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
  
  private fun filterItems(all: List<ItemState> = filterProvider(), selected: Boolean? = null, state: ItemState? = null) {
    if (!context.isFilled()) {
      return
    }
    
    val entries = context.getEntries()
    val filter = entries.filter { all.contains(it.state) || it.group }
    fillEntries(filter)
    updateStatusBar(entries)
  }
  
  private fun createDevToolsWindow() {
    ViewUtils.createWindow<DevelopersToolView>("developer-tools.fxml", "ShvFileMatcher - Developer Tools")
  }
  
  @Subscribe
  fun itemStateChangedEvent(e: ItemStateChangedEvent) {
    updateStatusBar(context.getEntries())
    updater.markChanged()
  }
  
  private fun updateStatusBar(list: List<Entry>, filteredStates: List<ItemState> = filterProvider()) {
    val entries = list.filter { !it.group }
    val filter = entries.filter { filteredStates.contains(it.state) }
    val synced = entries.count { it.state == ItemState.SYNCED }
    val skipped = entries.count { it.state == ItemState.SKIPPED }
    val download = entries.count { it.state.toModify }
    if (filter.size == entries.size) {
      lbTreeState.text = "Items: ${entries.size}; Synced: $synced, Skipped: $skipped, To download: $download"
    } else {
      val syncedFiltered = filter.count { it.state == ItemState.SYNCED }
      val skippedFiltered = filter.count { it.state == ItemState.SKIPPED }
      val downloadFiltered = filter.count { it.state.toModify }
      lbTreeState.text = "[Filtered/All] Items: ${filter.size}/${entries.size}; Synced: $syncedFiltered/$synced, Skipped: $skippedFiltered/$skipped, To download: $downloadFiltered/$download"
    }
  }
  
  companion object {
    @JvmStatic
    var instance: MainView? = null
    
    @JvmStatic
    fun initializeTreeTable(treeView: TreeTableView<Entry>, context: UserContext? = null) {
      treeView.columns.setAll(
          ViewUtils.createTreeColumn("Name", 300) { it.name },
          TreeTableColumn<Entry, String>("State").also {
            it.setCellValueFactory { if (it.value.value.group) SimpleStringProperty("") else it.value.value.stateProperty.asString() }
            it.cellFactory = ViewUtils.treeTableCellFactoryWithCustomCss<Entry>(setOf("bg-purple", "bg-yellow", "bg-green", "bg-red", "bg-grey", "bg-blue", "bg-orange")) {
              if (it.group) emptyList() else listOf(it.state.cssClass)
            }
            it.prefWidth = 210.0
          },
          ViewUtils.createTreeColumn("Path") { it.target.path?.findPath().orEmpty() },
          ViewUtils.createTreeColumn("Version") { it.version ?: if (it.group) "" else "-" },
          ViewUtils.createTreeColumn("Date") { if (!it.group) Utils.dateFormat(it.itemDate) else "" },
          ViewUtils.createTreeColumn("Override") { if (!it.group) (if (it.target.override) "Yes" else "No") else "" },
          ViewUtils.createTreeColumn("Category") { it.category.orEmpty() },
          ViewUtils.createTreeColumn("Tags") { it.tags?.joinToString { ", " }.orEmpty() },
          ViewUtils.createTreeColumn("ID") { it.id },
          ViewUtils.createTreeColumn("URL") { it.getSrcUrl(context?.getBundleOrNull()?.getBaseUrl(context.repo?.baseUrl)) }
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
        if (event.isPrimaryButtonDown && event.clickCount == 2 && treeView.selectionModel.selectedItem.value != null) {
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
          addDetailsRow("Src", item.getSrcUrl(context?.getBundleOrNull()?.getBaseUrl(context.repo?.baseUrl)), grid)
          addDetailsRow("Parent", item.parent, grid)
          addDetailsRow("Target Path", item.target.path?.findPath(), grid)
          addDetailsRow("Override", item.target.override.toEnglishWord(), grid)
          addDetailsRow("Target Path is absolute", item.target.absolute.toEnglishWord(), grid)
          addDetailsRow("Description", item.description, grid)
          addDetailsRow("Version", item.version, grid)
          addDetailsRow("Website", item.website, grid)
          addDetailsRow("Category", item.category, grid)
          addDetailsRow("Tags", item.tags?.joinToString(), grid, false)
          
          if (item.hashes?.hasAny() == true) {
            addDetailsHeader("Hashes", grid)
            if (item.hashes.md5 != null) {
              addDetailsRow("MD5", item.hashes.md5, grid)
            }
            if (item.hashes.sha1 != null) {
              addDetailsRow("SHA-1", item.hashes.sha1, grid)
            }
            if (item.hashes.sha256 != null) {
              addDetailsRow("SHA-256", item.hashes.sha256, grid)
            }
            if (item.hashes.crc32 != null) {
              addDetailsRow("CRC32", item.hashes.crc32, grid)
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
}