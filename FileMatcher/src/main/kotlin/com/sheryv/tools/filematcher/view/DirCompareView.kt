package com.sheryv.tools.filematcher.view


import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.ViewUtils
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.lib.*
import com.sheryv.util.logging.log
import com.sheryv.util.unit.BinarySize
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleSetProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.TableView.TableViewSelectionModel
import javafx.scene.layout.Priority
import javafx.stage.Stage
import org.fxmisc.easybind.EasyBind
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries


class DirCompareView(override val config: Configuration) : SimpleView() {
  
  private val pattern = stringProperty("([ +A-Za-z\\d_-]+)([ ._-]\\w*\\d*)*.*")
  private val left = DirPane("Left")
  private val right = DirPane("Right")
  private val trigger = triggerObs()
  
  fun dirPane(data: DirPane, parent: Parent) = parent.hbox(10) {
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
    
    data.path.onChangeNotNull {
      updateMatchers()
    }
    
    vbox(alignment = Pos.CENTER_LEFT) {
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
      }
      
      TableView(data.files).attachTo(this).apply {
        this.selectionModel.selectionMode = SelectionMode.MULTIPLE
        
        data.selectionModel = selectionModel
        styleClass.add("mono")
        vgrow = Priority.ALWAYS
        
        column("Name", 450) { it.path.fileName }
        column("Size", alignRight = true) { BinarySize.format(it.size) }
        columnBound("Status", 100) { it.status }.also {
          it.cellFactory = ViewUtils.tableCellFactoryWithCustomCss<FileRecord, FileRecordCmpSatus>(
            setOf(
//              "bg-purple",
//              "bg-yellow",
              "bg-green",
//              "bg-red",
              "bg-grey",
//              "bg-blue",
              "bg-orange"
            )
          ) {
            listOf(
              when (it.status.value) {
                FileRecordCmpSatus.MATCHED -> "bg-green"
                FileRecordCmpSatus.NOT_MATCHED -> "bg-orange"
                else -> "bg-grey"
              }
            )
          }
        }
        columnBound("Matched fragments") { f ->
          setProperty(f.phrases).mapObservable { it.joinToString("|") }
        }
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
            val matched = data.files.count { it.status.value == FileRecordCmpSatus.MATCHED }
            "Selected $selected/$count, Matched $matched, Total $size"
          }, data.files, data.selectedFiles))
        }
        MenuButton(
          "", null,
          item("Select all") { data.selectAll() },
          item("Select none") { data.selectNone() },
          item("Select matched") { data.select { it.status.value == FileRecordCmpSatus.MATCHED } },
          item("Select not matched") { data.select { it.status.value == FileRecordCmpSatus.NOT_MATCHED } },
          item("Invert selection") { data.invertSelect() },
          SeparatorMenuItem(),
          item("Copy selected to clipboard") {
            val selected = data.selectedFiles
            Toolkit.getDefaultToolkit().systemClipboard.setContents(
              FileTransferable(selected.map { it.path.toFile() })
            ) { _, _ -> }
            log.info("Copied {}:\n{}", selected.size, selected.joinToString("\n\t") { it.path.fileName.toString() })
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
      hbox(10) {
        vgrow = Priority.ALWAYS
        dirPane(left, this)
        dirPane(right, this)
      }
      vbox(5) {
        label("Pattern")
        hbox {
          hgrow = Priority.ALWAYS
          
          textfield(pattern) {
            hgrow = Priority.ALWAYS
            styleClass.add("mono")
          }
          button("Update").setOnAction {
          
          }
        }
      }
      vbox(5) {
        label("Actions")
        hbox {
          button("").setOnAction {
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
  }
  
  private fun updateMatchers() {
    if (left.path.value == null || right.path.value == null) {
      return
    }
    for (l in left.files) {
      l.status.value = FileRecordCmpSatus.NOT_MATCHED
    }
    for (r in right.files) {
      r.status.value = FileRecordCmpSatus.NOT_MATCHED
    }
    
    val regex = Regex(pattern.value)
    for (l in left.files) {
      val resR = regex.matchEntire(l.path.fileName.toString())
      if (resR?.groups?.isNotEmpty() == true) {
        for (r in right.files) {
          val resL = regex.matchEntire(r.path.fileName.toString())
          if (resL?.groups?.isNotEmpty() == true) {
            if (resR.groups[1]?.value?.equals(resL.groups[1]?.value, true) == true) {
              r.status.value = FileRecordCmpSatus.MATCHED
              l.status.value = FileRecordCmpSatus.MATCHED
              r.phrases.add(resR.groups[1]?.value)
              l.phrases.add(resL.groups[1]?.value)
            }
          }
        }
      }
    }
    trigger.fire()
  }
  
  
  private fun item(text: String, action: EventHandler<ActionEvent>): MenuItem {
    return MenuItem(text).also { it.onAction = action }
  }
}


class DirPane(
  val name: String,
  pathArg: Path? = null
) {
  val path: StringProperty = stringProperty(pathArg?.toAbsolutePath()?.toString())
  
  var files: ObservableList<FileRecord> = path.toList {
    if (Path.of(it).isDirectory()) {
      Path.of(it).listDirectoryEntries().filter { it.isRegularFile() }
        .map { FileRecord(it, Files.size(it)) }
    } else {
      null
    }
  }
  
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
}

data class FileRecord(val path: Path, val size: Long) {
  //  val selected = booleanProperty()
  val status = objectProperty(FileRecordCmpSatus.NOT_MATCHED)
  val phrases = FXCollections.observableSet<String>()
}

enum class FileRecordCmpSatus {
  MATCHED,
  NOT_MATCHED
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

