package com.sheryv.tools.videoconverter.video.encode

import com.sheryv.tools.videoconverter.core.Context
import com.sheryv.tools.videoconverter.core.MainSettings
import com.sheryv.tools.videoconverter.video.convert.DEFAULT_DECIMAL_FORMATTER
import com.sheryv.util.fx.core.Styles
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.core.view.runActionInBackground
import com.sheryv.util.fx.lib.*
import javafx.scene.Parent
import javafx.scene.control.*
import java.nio.file.Files
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

class VideoReEncodingView(val settings: MainSettings, val context: Context) : SimpleView() {
  private val processingInProgressProperty = booleanProperty(false)
  private lateinit var filesTable: TableView<VideoToEncodeFile>
  private val exampleCommand = stringProperty("")
  private val qualityMax = 50
  private val processor = VideoReEncodingProcessor(
    context, {}, {}
  )
  
  
  override val root: Parent by lazy {
    filesTable = TableView()
    
    if (settings.videoEncodingSettings.baseSearchPath.toString().isEmpty()) {
      settings.videoEncodingSettings.baseSearchPathProperty.value = settings.workingDir.absolute()
    }
    
    createRoot(1300.0, 700.0) {
//      paddingAll = 0.0
//      spacing = 0.0
      
      vbox {
        labelWrap("Search root path", hbox {
          pathField(settings.videoEncodingSettings.baseSearchPathProperty).grow()
        })
        labelWrap("Output path (can be absolute)", hbox {
          pathField(settings.videoEncodingSettings.outputPathProperty).grow()
        })
        hbox {
          labelWrap(
            "Search pattern (glob pattern)",
            textfield(settings.videoEncodingSettings.globPathFilterProperty).grow()
          ).grow()
          labelWrap(
            "Handbrake CLI path",
            pathField(settings.videoEncodingSettings.cliPathProperty).grow()
          ).grow()
        }
        filesTable.attachTo(this).apply {
          this.selectionModel.selectionMode = SelectionMode.MULTIPLE
          styleClass.add(Styles.CLASS_MONO)
          isFillWidth = true
          columnBound("Path", 650) { it.pathProperty }
          columnBound("State") { it.stateProperty }
          columnBound("Progress", 100) { it.progressProperty }.customCell(onUpdate = { i, r, n ->
            var pr = r?.progressProperty?.value ?: 0.0
            pr = Math.clamp(pr, 0.0, 100.0)
            n.progress = pr / 100.0
          }) {
            ProgressBar(0.0).also { it.maxWidth = Double.MAX_VALUE }
          }
          columnBound("%", 50, alignRight = true) { it.progressProperty.mapObservable { DEFAULT_DECIMAL_FORMATTER.format(it) } }
          columnBoundToCustomCell(
            "Quality", 200,
            modelValue = { it.qualityProperty },
            builder = { Slider(0.0, qualityMax.toDouble(), it.item?.toDouble() ?: 0.0) },
            controlValue = { it.valueProperty().mapped({ qualityMax - it!!.toInt() }, { qualityMax - it!!.toInt() }) }
          )
          columnBound("", 50) { it.qualityProperty }
          
        }.grow()
        hbox {
          labelWrap(
            "Example command",
            textfield(exampleCommand) {
              styleClass.add(Styles.CLASS_MONO)
              isEditable = false
            }.grow()
          ).grow()
        }
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
              val path = if (settings.videoEncodingSettings.baseSearchPath.toString().isEmpty()) {
                factory.dialogs.openDirectoryDialog(stage, initialDir = settings.workingDir.absolutePathString())
              } else {
                settings.videoEncodingSettings.baseSearchPath
              } ?: settings.videoEncodingSettings.baseSearchPath
              settings.videoEncodingSettings.baseSearchPathProperty.value = path
              
              searchFiles()
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
    val base = settings.videoEncodingSettings.baseSearchPath
    if (!Files.exists(base)) {
      factory.dialogs.messageDialog("Provided directory path is incorrect", Alert.AlertType.ERROR)
      return
    }
    
    runActionInBackground("Searching", processingInProgressProperty.asEditable(), {
      processor.search()
    }, {
      it.firstOrNull()?.let {
        exampleCommand.value = processor.commandFor(it).joinToString(" ")
      }
      filesTable.items.setAll(it)
    })
  }
  
  fun convert() {
    val items = filesTable.items.toList()
    runActionInBackground("Encoding", processingInProgressProperty.asEditable(), {
      processor.encode(items)
    })
  }
}
