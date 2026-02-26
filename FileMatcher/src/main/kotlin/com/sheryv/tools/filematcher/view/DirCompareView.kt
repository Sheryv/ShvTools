package com.sheryv.tools.filematcher.view


import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.Hashing
import com.sheryv.tools.filematcher.utils.ViewUtils
import com.sheryv.tools.filematcher.view.DirPane.*
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.lib.*
import com.sheryv.util.ie
import com.sheryv.util.inBackground
import com.sheryv.util.inMainContext
import com.sheryv.util.logging.log
import com.sheryv.util.unit.BinarySize
import javafx.beans.binding.Bindings
import javafx.beans.property.ListProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.TableView.TableViewSelectionModel
import javafx.scene.layout.Priority
import javafx.stage.Stage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk


class DirCompareView(override val config: Configuration) : SimpleView() {
  
  private val pattern = stringProperty("([ +A-Za-z\\d_-]+)([ ._-]\\w*\\d*)*.*")
  private val method = objectProperty(ComparisonMethod.PATTERN)
  private val left = DirPane("Left")
  private val right = DirPane("Right")
//  private val trigger = triggerObs()
  
  fun dirPane(data: DirPane, parent: Parent) = parent.hbox(10) {
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
    paddingAll = 5.0
    
    data.path.onChangeNotNull {
      if (data.name == "Left") {
        config.dirComparisonLeftPath = Path.of(it)
      } else {
        config.dirComparisonRightPath = Path.of(it)
      }
      config.save()
//      updateMatchers()
    }
    
    vbox(alignment = Pos.CENTER_LEFT) {
      val isLoading = booleanProperty(false)
      var job: Job? = null
      
      hgrow = Priority.ALWAYS
      label(data.name + " dir")
      hbox {
        textfield(data.path) {
          hgrow = Priority.ALWAYS
        }
        button("...").setOnAction {
          DialogUtils.directoryDialog(this.scene.window, initialDirectory = data.path.value)
            .ifPresent { data.path.value = it.toAbsolutePath().toString() }
        }
        button(isLoading.map { it.ie("Cancel", "Load") }).setOnAction {
          if (job == null) {
            job = inBackground(isLoading.asEditable()) {
              data.loadFromFilesystem()
            }
          } else {
            job!!.cancel()
            job = null
          }
        }
      }
      
      TableView(data.files).attachTo(this).apply {
        this.selectionModel.selectionMode = SelectionMode.MULTIPLE
        
        data.selectionModel = selectionModel
        styleClass.add("mono")
        vgrow = Priority.ALWAYS
        
        column("Name", 450) { it.name }
        column("Size", alignRight = true) { BinarySize.format(it.size) }
        columnBound("Status", 100) { it.match.mapObservableOrDefault(FileRecordCmpStatus.NOT_MATCHED) { it?.status } }.also {
          it.cellFactory = ViewUtils.tableCellFactoryWithCustomCss<FileRecord, FileRecordCmpStatus>(
            setOf(
//              "bg-purple",
//              "bg-yellow",
              "bg-green",
//              "bg-red",
              "bg-grey",
              "bg-blue",
              "bg-orange"
            )
          ) {
            listOf(
              when (it.match.value?.status) {
                FileRecordCmpStatus.IDENTICAL -> "bg-blue"
                FileRecordCmpStatus.MATCHED -> "bg-green"
                FileRecordCmpStatus.NOT_MATCHED -> "bg-orange"
                else -> "bg-grey"
              }
            )
          }
        }
        columnBound("Matched fragments") { f ->
          f.match.mapObservable { it?.phrases?.let { if (data == left) it.first.orEmpty() else it.second.orEmpty() } ?: "" }
        }
        columnBound("Same size") { f ->
          f.match.mapObservable { if (it?.sameSize == true) "Yes" else "No" }
        }
        column("ModID") { f -> f.minecraftModId.orEmpty() }
        column("Mod Name") { f -> f.minecraftModName.orEmpty() }
      }
      hbox {
        vgrow = Priority.SOMETIMES
        label {
          hgrow = Priority.ALWAYS
          maxWidth = Double.MAX_VALUE
          
          bind(Bindings.createStringBinding({
            val size = BinarySize.format(data.files.sumOf { it.size })
            val count = data.files.size
            val selected = data.selectedFiles.size
            val matched = data.files.count { it.match.value?.status == FileRecordCmpStatus.MATCHED || it.match.value?.status == FileRecordCmpStatus.IDENTICAL }
            "Selected $selected/$count, Matched $matched, Total $size"
          }, data.files, data.selectedFiles))
        }
        MenuButton(
          "", null,
          item("Select all") { data.selectAll() },
          item("Select none") { data.selectNone() },
          item("Select matched") { data.select { it.match.value?.status == FileRecordCmpStatus.MATCHED } },
          item("Select identical") { data.select { it.match.value?.status == FileRecordCmpStatus.IDENTICAL } },
          item("Select matched & identical") { data.select { it.match.value?.status == FileRecordCmpStatus.IDENTICAL || it.match.value?.status == FileRecordCmpStatus.MATCHED } },
          item("Select not matched") { data.select { it.match.value?.status == FileRecordCmpStatus.NOT_MATCHED } },
          item("Invert selection") { data.invertSelect() },
          SeparatorMenuItem(),
          item("Copy selected to clipboard") {
            val selected = data.selectedFiles
            Toolkit.getDefaultToolkit().systemClipboard.setContents(
              FileTransferable(selected.map { it.path.toFile() })
            ) { _, _ -> }
            log.info("Copied {}:\n{}", selected.size, selected.joinToString("\n\t") { it.path.fileName.toString() })
          },
          item("Delete selected") {
            val selected = data.selectedFiles.toList()
            DialogUtils.dialog(
              "Do yot really want to delete ${selected.size} files from ${
                selected.firstOrNull()?.path?.parent?.toAbsolutePath()?.toString().orEmpty()
              }",
              type = Alert.AlertType.NONE,
              buttons = arrayOf(ButtonType.YES, ButtonType.CANCEL)
            )
              .filter { it == ButtonType.YES }
              .ifPresent {
                for (file in selected) {
                  Files.delete(file.path)
                }
                log.info("Deleted {}:\n{}", selected.size, selected.joinToString("\n\t") { it.path.fileName.toString() })
              }
          },
        ).attachTo(this).also {
          it.minHeight = 25.0
          it.prefHeight = 25.0
          it.styleClass.add("ic-more-vert")
        }
      }
      
    }
  }
  
  override val root: Parent by lazy {
    createRoot {
      paddingAll = 5.0
      splitpane(Orientation.HORIZONTAL) {
        vgrow = Priority.ALWAYS
        dirPane(left, this)
        dirPane(right, this)
      }
      titledpane("Parameters") {
        hbox(25) {
          vbox(10) {
            hgrow = Priority.ALWAYS
            vbox(2) {
              label("Comparison method")
              combo(ComparisonMethod.entries.asObservable(), method)
            }
            vbox(2) {
              label("Pattern")
              hbox {
                hgrow = Priority.ALWAYS
                
                textfield(pattern) {
                  hgrow = Priority.ALWAYS
                  styleClass.add("mono")
                }
//                button("Update pattern").setOnAction {
//
//                }
                button("Run compare").setOnAction {
                  updateMatchers()
                }
              }
            }
          }
          vbox(5) {
            label("Actions")
            vbox(5) {
              hgrow = Priority.ALWAYS
              button("test button with name") {
                maxWidth = Double.MAX_VALUE
                setOnAction {
                }
              }
              button("-") {
                maxWidth = Double.MAX_VALUE
                setOnAction {
                }
              }
            }
          }
        }
      }
      hbox {
        label("-")
      }
    }
  }
  
  override fun onViewCreated(stage: Stage) {
    super.onViewCreated(stage)
    title = "Directory compare"
    left.path.set(config.dirComparisonLeftPath?.toAbsolutePath()?.toString())
    right.path.set(config.dirComparisonRightPath?.toAbsolutePath()?.toString())
  }
  
  private fun updateMatchers() {
    if (left.path.value == null || right.path.value == null) {
      return
    }
    for (f in left.files) {
      f.match.value = null
    }
    for (f in right.files) {
      f.match.value = null
    }
    
    val method = this.method.value
    
    log.info("Updating lists")
    val regex = Regex(pattern.value)
    for (l in left.files) {
      for (r in right.files) {
        if (r.match.value?.status?.hasMatch == true && l.match.value?.status?.hasMatch == true) {
          continue
        }
        
        val match = l.matches(method, regex, r)
        if (l.match.value?.status?.hasMatch != true) {
          l.match.value = match
        }
        if (r.match.value?.status?.hasMatch != true) {
          r.match.value = match
        }
      }
    }
    left.selectedFiles.clear()
    right.selectedFiles.clear()
  }
//    trigger.fire()
}


private fun item(text: String, action: EventHandler<ActionEvent>): MenuItem {
  return MenuItem(text).also { it.onAction = action }
}


class DirPane(
  val name: String,
  pathArg: Path? = null
) {
  val path: StringProperty = stringProperty(pathArg?.toAbsolutePath()?.toString())
  private val modIdRegex = Regex("""modId *= *"(\S+)"""")
  private val modNameRegex = Regex("""displayName *= *"(\S+)"""")
  
  var files: ObservableList<FileRecord> = FXCollections.synchronizedObservableList(FXCollections.observableArrayList())
  
  internal lateinit var selectionModel: TableViewSelectionModel<FileRecord>
  
  val selectedFiles
    get() = selectionModel.selectedItems
  
  //
  init {
//    files.onChange {
//      selectedFiles.clear()
//    }
  }
  
  fun selectAll() = selectionModel.selectAll()
  
  fun selectNone() = selectionModel.clearSelection()
  
  fun invertSelect() {
    val old = selectedFiles.toList()
    for (v in files.withIndex()) {
      if (old.contains(v.value)) {
        selectionModel.clearSelection(v.index)
      } else {
        selectionModel.select(v.index)
      }
    }
  }
  
  fun select(filter: (FileRecord) -> Boolean) {
    files.withIndex().forEach {
      if (filter(it.value)) {
        selectionModel.select(it.index)
      } else {
        selectionModel.clearSelection(it.index)
      }
    }
  }
  
  suspend fun loadFromFilesystem() {
    val dir = path.takeIf { it.value != null }?.let { Path.of(it.value) }
    if (dir?.isDirectory() == true) {
      inMainContext {
        files.clear()
      }

//      val flow = callbackFlow<FileRecord> {
      val flow = dir.walk().asFlow().filter { it.isRegularFile() }
        .map {
          var modId: String? = null
          var modName: String? = null
          if (it.extension == "jar") {
            try {
              
              FileSystems.newFileSystem(it).use { fileSystem ->
                (fileSystem.getPath("META-INF/mods.toml").takeIf(Files::exists) ?: fileSystem.getPath("META-INF/neoforge.mods.toml")
                  .takeIf(Files::exists))
                  ?.let(Files::readString)
              }?.also {
                modId = modIdRegex.find(it)?.let { it.groupValues[1] }
                modName = modNameRegex.find(it)?.let { it.groupValues[1] }
              }
            } catch (e: Exception) {
              log.error("Cannot read mod file $it", e)
            }
          }
          
          FileRecord(dir.relativize(it).toString(), it.toAbsolutePath(), Files.size(it), Hashing.crc32(it), modId, modName)
        }
      
      flow.collect {
        inMainContext {
          files.add(it)
        }
      }
      
    }
  }
  
  data class FileRecord(
    val name: String,
    val path: Path,
    val size: Long,
    val crc: String,
    val minecraftModId: String? = null,
    val minecraftModName: String? = null
  ) {
    val match = objectProperty<FileRecordMatch>(null)
    
    fun matches(method: ComparisonMethod, regex: Regex, other: FileRecord): FileRecordMatch {
      when (method) {
        ComparisonMethod.PATTERN -> {
          val currentMatch = regex.matchEntire(path.fileName.toString())
          
          if (crc == other.crc) {
            return FileRecordMatch(null, size == other.size, true, false)
          }
          
          if (currentMatch?.groups?.isNotEmpty() == true) {
            val otherMatch = regex.matchEntire(other.path.fileName.toString())
            
            if (otherMatch?.groups?.isNotEmpty() == true) {
              if (currentMatch.groups[1]?.value?.equals(otherMatch.groups[1]?.value, true) == true) {
                return FileRecordMatch(
                  currentMatch.groups[1]?.value to otherMatch.groups[1]?.value,
                  size == other.size,
                  false
                )
              }
            }
          }
        }
        
        ComparisonMethod.MINECRAFT_MOD_ID -> {
          if (minecraftModId != null && minecraftModId == other.minecraftModId) {
            return FileRecordMatch(null, size == other.size, crc == other.crc, true)
          }
        }
      }
      
      return FileRecordMatch()
    }
  }
  
  class FileRecordMatch(
    val phrases: Pair<String?, String?>? = null,
    val sameSize: Boolean = false,
    val sameHash: Boolean = false,
    val sameId: Boolean = false,
  ) {
    
    val status: FileRecordCmpStatus = let {
      if (sameHash)
        return@let FileRecordCmpStatus.IDENTICAL
      
      if (sameId)
        return@let FileRecordCmpStatus.MATCHED
      
      if (phrases?.first?.equals(phrases.second, true) == true)
        FileRecordCmpStatus.MATCHED
      else
        FileRecordCmpStatus.NOT_MATCHED
    }
    
  }
  
  enum class FileRecordCmpStatus(val hasMatch: Boolean) {
    IDENTICAL(true),
    MATCHED(true),
    NOT_MATCHED(false)
  }
  
  
  class FileTransferable(private val listOfFiles: List<File>) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> {
      return arrayOf(DataFlavor.javaFileListFlavor)
    }
    
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
      return DataFlavor.javaFileListFlavor.equals(flavor)
    }
    
    @Throws(UnsupportedFlavorException::class, IOException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
      return listOfFiles
    }
  }
  
  enum class ComparisonMethod(val label: String) {
    PATTERN("Pattern"),
    MINECRAFT_MOD_ID("Minecraft mod ID");
    
    override fun toString(): String = label
  }
}
