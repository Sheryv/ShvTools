package com.sheryv.tools.filematcher.view

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.service.FileMatcher
import com.sheryv.tools.filematcher.service.FileSynchronizer
import com.sheryv.tools.filematcher.service.RepositoryService
import com.sheryv.tools.filematcher.service.Validator
import com.sheryv.tools.filematcher.utils.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority

class MainView : BaseView() {
  private var context: UserContext = UserContext()
  
  @FXML
  fun initialize() {
    val buttons = {
      mapOf("copy" to Button("Copy").apply { padding = Insets(0.0, 5.0, 0.0, 5.0) })
    }
    treeView.columns.setAll(
        ViewUtils.createTreeColumn("Name", 300) { it.name },
        TreeTableColumn<Entry, String>("State").apply {
          setCellValueFactory { if (it.value.value.isGroup()) SimpleStringProperty("") else it.value.value.stateProperty.asString() }
          prefWidth = 210.0
        },
        ViewUtils.createTreeColumn("Path") { it.target.path?.findPath().orEmpty() },
        ViewUtils.createTreeColumn("Version") { it.version ?: if (it.isGroup()) "" else "-" },
        ViewUtils.createTreeColumn("Date") { if (!it.isGroup()) Utils.dateFormat(it.itemDate) else "" },
        ViewUtils.createTreeColumn("Override") { if (!it.isGroup()) (if (it.target.override) "Yes" else "No") else "" },
        ViewUtils.createTreeColumn("Category") { it.category.orEmpty() },
        ViewUtils.createTreeColumn("Tags") { it.tags?.joinToString { ", " }.orEmpty() },
        ViewUtils.createTreeColumn("ID") { it.id },
        TreeTableColumn<Entry, String>("URL").also {
          it.cellFactory = ViewUtils.buttonsInTreeTableCellFactory(buttons) { treeItem, map ->
            val e = treeItem.value!!
            map.values.first().setOnAction {
              val content = ClipboardContent()
              content.putString(e.getSrcUrl(context.getBundle()!!.getBaseUrl(context.repo!!.baseUrl)))
              Clipboard.getSystemClipboard().setContent(content)
            }
            return@buttonsInTreeTableCellFactory if (e.isGroup()) emptyList() else map.values
          }
        }
    )
    
    treeView.selectionModel.selectedItems.addListener { item: ListChangeListener.Change<out TreeItem<Entry>> ->
      if (item.list.size == 0 || item.list.size == 1 && !item.list[0].value.isGroup()) {
      } else if (item.list.size == 1 && item.list[0].value.isGroup()) {
      } else {
      }
    }
    
    val item = ViewUtils.toTreeItems(BundleUtils.createExample().bundles.first().versions.first().entries)
    ViewUtils.forEachTreeItem(item) { if (!it.isLeaf) it.isExpanded = true }
    treeView.root = item
    
    
    btnLoad.isDisable = true
    btnDownload.isDisable = true
    btnVerify.isDisable = true
    val recentRepositories = Configuration.get().recentRepositories
    cmRepositoryUrl.items = FXCollections.observableList(recentRepositories.toMutableList())
//    cmRepositoryUrl.items.addListener { c: ListChangeListener.Change<out String> ->  c.}
    cmRepositoryUrl.selectionModel.selectedItemProperty().addListener { _, _, v ->
      btnLoad.isDisable = !(v != null && v.isNotBlank())
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
          recentRepositories.add(0, res)
          if (recentRepositories.size >= 30) {
            recentRepositories.removeAt(recentRepositories.size - 1)
          }
          Configuration.get().save()
          cmRepositoryUrl.items.clear()
          cmRepositoryUrl.items.addAll(recentRepositories)
          cmRepositoryUrl.selectionModel.select(0)
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
        fillEntries(context.getEntries())
      }
      btnDownload.isDisable = v == null
      btnVerify.isDisable = v == null
    }
    
    btnLoad.setOnAction {
      try {
        val load = RepositoryService().loadRepositoryConfig(cmRepositoryUrl.selectionModel.selectedItem)
        context = UserContext(load)
        
        cmBundle.items.setAll(load.bundles)
        cmBundle.selectionModel.select(0)
        cmVersion.items.setAll(load.bundles[0].versions)
        cmVersion.selectionModel.select(0)
        
        lbLoadedRepo.text = "Loaded: " + cmRepositoryUrl.selectionModel.selectedItem
        renderFields()
      } catch (e: ValidationError) {
        DialogUtils.textAreaDialog(
            "Following problems were found at: \n${cmRepositoryUrl.selectionModel.selectedItem}",
            e.toLongText(),
            "Found problems when validating repository configuration")
      }
    }
    
    btnPath.setOnAction {
      DialogUtils.directoryDialog(treeView.scene.window, initialDirectory = tfPath.text).ifPresent {
        tfPath.text = it.toAbsolutePath().toString()
      }
    }
    
    btnVerify.setOnAction {
      if (tfPath.text.isNullOrBlank()) {
        DialogUtils.dialog("", "Target directory is empty", Alert.AlertType.ERROR, ButtonType.OK)
        return@setOnAction
      }
      
      val directory = SystemUtils.parseDirectory(tfPath.text, null)
      if (directory == null) {
        DialogUtils.dialog("", "Directory not found: ${tfPath.text.orEmpty()}", Alert.AlertType.ERROR, ButtonType.OK)
      } else {
        context.basePath = directory
        FileMatcher(context).verifyLocal()
      }
    }
    
    btnDownload.setOnAction {
      if (tfPath.text.isNullOrBlank()) {
        DialogUtils.dialog("", "Target directory is empty", Alert.AlertType.ERROR, ButtonType.OK)
        return@setOnAction
      }
      
      val directory = SystemUtils.parseDirectory(tfPath.text, null)
      if (directory == null) {
        DialogUtils.dialog("", "Directory not found: ${tfPath.text.orEmpty()}", Alert.AlertType.ERROR, ButtonType.OK)
      } else {
        context.basePath = directory
        FileSynchronizer(context).synchronize()
      }
    }
  }
  
  private fun fillEntries(entries: List<Entry>) {
    val item = ViewUtils.toTreeItems(entries)
    ViewUtils.forEachTreeItem(item) { if (!it.isLeaf) it.isExpanded = true }
    treeView.root = item
  }
  
  
  private fun renderFields() {
    gridDetails.children.clear()
    val r = context.repo!!
    addDetailsHeader("Repository")
    
    addDetailsRow("Name", r.codeName)
    addDetailsRow("BaseUrl", r.baseUrl)
    addDetailsRow("Version", r.version)
    addDetailsRow("Website", r.website)
    addDetailsRow("Title", r.title)
    addDetailsRow("Author", r.author)
    addDetailsRow("Update date", Utils.dateFormat(r.updateDate), false)
    
    addDetailsHeader("Additional fields")
    
    r.additionalFields.forEach { (t, u) ->
      addDetailsRow(t, u)
    }
    gridDetails.rowConstraints.forEach { it.vgrow = Priority.ALWAYS }
  }
  
  private fun addDetailsHeader(text: String) {
    val lk = Label(text)
    lk.styleClass.add("additional-fields-header")
    lk.maxWidth = Double.MAX_VALUE
    gridDetails.add(lk, 0, gridDetails.rowCount, 3, 1)
  }
  
  private fun addDetailsRow(key: String, value: String?, withSeparator: Boolean = true) {
    val height = 25.0
    
    val lk = Label(key)
    lk.minHeight = height
    lk.prefHeight = height
    val lv = if (value != null && Validator().url(value).isOk()) {
      Hyperlink(value).apply {
        maxHeight = height
        prefHeight = height
        setOnAction { SystemUtils.openLink(value) }
      }
    } else {
      Label(value.orEmpty())
    }
    
    val row = gridDetails.rowCount
    gridDetails.add(lk, 0, row, 1, 1)
    gridDetails.add(lv, 1, row, 1, 1)
    
    if (!value.isNullOrBlank()) {
      val cp = Button("Copy")
      cp.maxHeight = height - 10
      cp.prefHeight = height - 10
      cp.setOnAction {
        lg().debug("Click $value")
        val content = ClipboardContent()
        content.putString(value)
        Clipboard.getSystemClipboard().setContent(content)
      }
      gridDetails.add(cp, 2, row, 1, 1)
    }
    
    if (withSeparator) {
      gridDetails.add(Separator(Orientation.HORIZONTAL).apply { maxHeight = 5.0 }, 0, row + 2, 3, 1);
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
}