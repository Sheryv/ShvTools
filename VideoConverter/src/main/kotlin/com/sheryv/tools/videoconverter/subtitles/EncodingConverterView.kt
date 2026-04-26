package com.sheryv.tools.videoconverter.subtitles

import com.sheryv.tools.videoconverter.video.MainSettings
import com.sheryv.util.fx.core.Styles
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.core.view.runActionInBackground
import com.sheryv.util.fx.lib.*
import com.sheryv.util.io.FileUtils
import com.sheryv.util.logging.log
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString


class EncodingConverterView(val settings: MainSettings) : SimpleView() {
  private val searchPathProperty = stringProperty(settings.workingDir.absolutePathString())
  private val searchPatternProperty = stringProperty("**.srt")
  private val processingInProgressProperty = booleanProperty(false)
  private val defaultCharset = StandardCharsets.UTF_8.name()
  private val charsets = Charset.availableCharsets().values.map { it.name() }.filter { !it.startsWith("x-") }.toList().asObservable()
  private lateinit var filesTable: TableView<SubtitleFile>
  
  val searchPath by searchPathProperty.map { Path.of(it) }
  
  
  override val root: Parent by lazy {
    filesTable = TableView()
    
    createRoot(1200.0, 600.0) {
//      paddingAll = 0.0
//      spacing = 0.0
      
      vbox {
        labelWrap("Search root path", hbox {
          textfield(searchPathProperty).grow().also {
            it.isEditable = false
          }
        })
        labelWrap("Search pattern (glob pattern)", hbox {
          textfield(searchPatternProperty).grow()
        })
        filesTable.attachTo(this).apply {
          this.selectionModel.selectionMode = SelectionMode.MULTIPLE
          styleClass.add(Styles.CLASS_MONO)
          isFillWidth = true
          column("Path", 700) { searchPath.relativize(it.path) }
          columnBoundToComboCell("Encoding", 200, modelValue = { it.encodingProperty }, default = charsets)

//            it.setCellFactory({ col ->
//              val c = TableCell<SubtitleFile, String>()
//              val comboBox = ComboBox(charsets)
//              c.itemProperty().addListener { observable, oldValue, newValue ->
//                if (oldValue != null) {
//                  comboBox.valueProperty().unbindBidirectional(c.itemProperty())
//                }
//                if (newValue != null) {
//                  comboBox.valueProperty().bindBidirectional(c.itemProperty())
//                }
//              }
//              c.graphicProperty().bind(Bindings.`when`(c.emptyProperty()).then(null as Node?).otherwise(comboBox))
//              c
//            })
        }.grow()
        hbox {
          button("Remove selected") {
            disableProperty().bind(filesTable.selectionModel.selectedIndices.sizeProperty.isEqualTo(0))
            setOnAction {
              val indices = filesTable.selectionModel.selectedIndices.toList()
              for (i in indices.reversed()) {
                filesTable.items.removeAt(i)
              }
            }
          }
          
          spacer()
          button("Search") {
            setOnAction {
              disableProperty().bind(processingInProgressProperty)
              factory.dialogs.openDirectoryDialog(stage, initialDir = searchPathProperty.get())?.apply {
                searchPathProperty.value = this.absolutePathString()
                searchFiles()
              }
            }
          }
          button("Start conversion") {
            disableProperty().bind(processingInProgressProperty.or(filesTable.items.sizeProperty.isEqualTo(0)))
            styleClass.add("btn-info")
            
            setOnAction {
              convert()
            }
          }
        }
      }.grow()
    }
  }
  
  
  fun searchFiles() {
    val base = Path.of(searchPathProperty.get())
    if (!Files.exists(base)) {
      factory.dialogs.messageDialog("Provided directory path is incorrect", Alert.AlertType.ERROR)
      return
    }
    
    runActionInBackground("Searching", processingInProgressProperty.asEditable(), {
      val matcher = FileSystems.getDefault().getPathMatcher("glob:${searchPatternProperty.get()}")
      Files.walk(base, 30)
        .filter { Files.isRegularFile(it) }
        .filter { matcher.matches(it) }
        .map { SubtitleFile(it.toAbsolutePath(), defaultCharset) }
        .toList()
    }, {
      filesTable.items.setAll(it)
    })
  }
  
  fun convert() {
    val items = filesTable.items.toList()
    runActionInBackground("Encoding conversion", processingInProgressProperty.asEditable(), {
      convertSubtitlesEncoding(items)
    })
  }
  
  
  private fun convertSubtitlesEncoding(records: List<SubtitleFile>) {
    for (file in records) {
      if (file.encoding != defaultCharset) {
        val targetEncoding = Charset.forName(file.encoding)
        val subtitlesContent = Files.readString(file.path, targetEncoding)
        FileUtils.renameAsBackup(file.path)
        Files.writeString(file.path, subtitlesContent, Charset.forName(defaultCharset))
        log.info("Converted encoding {} -> {} for {}", targetEncoding, defaultCharset, file.path)
      }
    }
  }
  
  private class SubtitleFile(val path: Path, encoding: String) {
    val encodingProperty = mutableProperty(encoding)
    var encoding by encodingProperty
  }
}


