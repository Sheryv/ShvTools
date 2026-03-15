package com.sheryv.tools.videoconverter.mergetomkv

import com.sheryv.util.fx.core.Styles
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.core.view.runActionInBackground
import com.sheryv.util.fx.lib.*
import com.sheryv.util.logging.log
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.converter.DoubleStringConverter
import java.nio.charset.Charset
import java.nio.file.Path

class MergeStreamsToSingleMkvView(val settings: ConverterSettings) : SimpleView() {
  
  private val selected = FXCollections.observableArrayList<ConvertVideo>()
  private val statusProperty = stringProperty("")
  private val processingInProgressProperty = booleanProperty(false)
  private val exampleMatchingResult = stringProperty("")
  private val detailedVideoProperty = mutableProperty<ConvertVideo?>(null)
  private lateinit var processor: ConversionProcessor
  private lateinit var sourcesPane: Pane
  private lateinit var filesTable: TableView<ConvertVideo>
  
  private fun findFiles() {
    val previous: List<ConvertVideo> = filesTable.items
    val frozenSettings = settings.copy()
    
    runActionInBackground("Scanning files", processingInProgressProperty.asEditable(), {
      processor.scanFilesystem(frozenSettings, previous)
    }, {
      filesTable.items.setAll(it)
    })
  }
  
  override val root: Parent by lazy {
    createRoot(1600.0, 900.0) {
      paddingAll = 0.0
      spacing = 0.0
      
      vbox {
        vgrow = Priority.ALWAYS
        paddingAll = 10.0
        
        
        // main
        splitpane {
          vbox0 {
            hbox {
              paddingAll = 5.0
              alignment = Pos.TOP_RIGHT
              
              button("Remove selected") {
                disableProperty().bind(selected.sizeProperty.map { it == 0 })
                
                setOnAction {
                  filesTable.items.removeAll(selected)
                }
              }
            }
            filesTable = TableView<ConvertVideo>().attachTo(this).apply {
              this.selectionModel.selectionMode = SelectionMode.MULTIPLE
              Bindings.bindContent(selected, selectionModel.selectedItems);
              styleClass.add(Styles.CLASS_MONO)
              vgrow = Priority.ALWAYS
              isFillWidth = true
              columnBound("#", 30) { it.numberProperty }
              columnBound("Video", 350) { it.targetVideoProperty }
              columnBound("State") { it.stateProperty }
              columnBound("Conversion Progress", 170) { it.progressProperty.mapObservable { it.formatted } }
              columnBound("Sources summary", 140) {
                val dependencies = it.sources.filterNotNull().map { it.timeOffsetProperty }
                stringBinding(it.sourcesProperty, *dependencies.toTypedArray()) {
                  filterNotNull().joinToString(", ") { "${it.type.code} (${it.timeOffset})" }
                }
              }
              column("Output", 300) { tryFindRelativePath(processor.calculateOutput(it.targetVideo)) }
              
            }.grow()
          }.grow()
//          vbox0 {
//            paddingAll = 0.0
//            vbox {
//              paddingAll = 10.0
//              maxWidth = Double.MAX_VALUE
//              label("Select any record to see details").growH()
//              managedProperty().bind(detailedVideoProperty.map { it == null })
//              visibleProperty().bind(managedProperty())
//            }.grow()
//
          vbox0 {
            paddingAll = 5.0
            label("Details of selected video") {
              alignment = Pos.TOP_LEFT
              styleClass.add("text-size-b2")
            }
            textarea(detailedVideoProperty.flatMap { it!!.metadataProperty }.map { it?.format().orEmpty() }) {
              styleClass.addAll("text-size-b1", Styles.CLASS_MONO, "no-scroll-bar")
              isEditable = false
              prefRowCount = 2
              minHeight = 50.0
              maxHeight = 50.0
              prefHeight = 50.0
              isFocusTraversable = false
            }
            spacer(Priority.NEVER) {
              minHeight = 5.0
            }
            
            TableView<Pair<Int, ConvertAdditionalSource?>>().attachTo(this).apply {
              styleClass.add(Styles.CLASS_MONO)
              isEditable = true
              columnBound("#", 30) { staticProperty(it.first + 1) }
              columnBound("Type") { it.second?.typeProperty ?: staticProperty("") }
              columnBound("Path", 280) { it.second?.pathProperty ?: staticProperty("") }
              columnBound("Offset", 60, true) { it.second?.timeOffsetProperty ?: mutableProperty(0.0) }.apply {
                cellFactory = TextFieldTableCell.forTableColumn(DoubleStringConverter())
                isEditable = true
              }
              itemsProperty().bind(detailedVideoProperty.flatMap {
                staticProperty(it?.sourcesProperty?.mapIndexed { i, v -> i to v }?.asObservable() ?: FXCollections.emptyObservableList())
              })
            }.growV()
            visibleProperty().bind(detailedVideoProperty.map { it != null })
          }
//          }
          
          this.setDividerPosition(0, 0.7)
        }.growV()
        // settings
        hbox {
          vbox {
            hgrow = Priority.ALWAYS
            
            labelWrap("Working directory", hbox {
              textfield(settings.workingDirProperty).grow()
              button("...") {
                setOnAction {
                  factory.dialogs.openDirectoryDialog(stage, initialDir = settings.workingDir.toString())
                    ?.apply { settings.workingDirProperty.value = this.toAbsolutePath().toString() }
                }
              }
            })
            labelWrap("Output directory (can be absolute)", textfield(settings.outputDirProperty))
            labelWrap("Target video search pattern", textfield(settings.videoPathPatternProperty) {
              styleClass.add(Styles.CLASS_MONO)
            })
            labelWrap("Example video path for pattern testing", textfield(settings.examplePathProperty))
            labelWrap("Results of matching with example value", textarea(exampleMatchingResult) {
              isEditable = false
              styleClass.add(Styles.CLASS_MONO)
              this.prefRowCount = 3
              this.prefHeight = 100.0
            })
          }
          vbox {
            hgrow = Priority.ALWAYS
            hbox {
              label("Sources:").growH()
            }
            hbox {
              label("#")
              label("Pattern").growH().strech()
              label("Time offset")
              label("Text encoding") {
                prefWidth = 90.0
              }
              label("Language code")
            }
            sourcesPane = vbox()
          }
          vbox {
            button("Scan filesystem") {
              disableProperty().bind(processingInProgressProperty)
            }.strech().action {
              findFiles()
            }
            
            button("Save settings / Reload").strech().setOnAction {
              settings.save()
//              table.refresh()
            }
            button("Convert subtitles encoding") {
              disableProperty().bind(processingInProgressProperty)
              setOnAction {
                convertSubtitlesEncoding()
              }
            }.strech()
            spacer()
            button("Cancel process") {
              disableProperty().bind(processingInProgressProperty.not())
              setOnAction {
                processor.cancelProcessing()
              }
            }.strech()
            button("Start conversion") {
              disableProperty().bind(processingInProgressProperty)
              styleClass.add("btn-info")
              setOnAction {
                val records = filesTable.items.toList()
                
                runActionInBackground("Video conversion", processingInProgressProperty.asEditable(), {
                  processor.start(records)
                }, {
                
                })
              }
            }.strech()
          }
        }
      }
      separator()
      // status bar
      hbox {
        paddingHorizontal = 20.0
        paddingVertical = 5.0
        label(statusProperty)
      }
    }
  }
  
  override fun onViewCreated(stage: Stage) {
    super.onViewCreated(stage)
    detailedVideoProperty.set(null)
    filesTable.selectionModel.selectedItemProperty().onChange {
      detailedVideoProperty.set(it)
    }
    
    processor = ConversionProcessor(settings, {
      statusProperty.set(it)
    }) {
      if (it == null) {
//      factory.dialogs.messageDialog("Conversion completed successfully")
      } else {
        factory.dialogs.exceptionDialog("Conversion failed", it)
      }
      processingInProgressProperty.value = false
    }
    
    settings.sources.forEachIndexed { i, source ->
      
      sourcesPane.hbox {
        background = Styles.background(Color.TRANSPARENT)
        label((i + 1).toString())
        textfield(source.pathPatternProperty) {
          promptText = "(disabled - fill pattern to enable)"
          styleClass.add(Styles.CLASS_MONO)
        }.growH()
        textfield(source.defaultTimeOffsetProperty) {
          maxWidth = 50.0
        }.stripNonNumeric()
        combo(
          Charset.availableCharsets().values.map { it.name() }.filter { !it.startsWith("x-") }.toList().asObservable(),
          source.textFileEncodingProperty
        ) {
          prefWidth = 120.0
        }
        textfield(source.languageProperty) {
          maxWidth = 50.0
        }
      }
    }
    
    exampleMatchingResult.bind(
      settings.examplePathProperty.stringBinding(
        settings.workingDirProperty,
        settings.videoPathPatternProperty, *settings.sources.map { it.pathPatternProperty }.toTypedArray()
      ) {
        try {
          val groups = Regex(settings.videoPathPattern).find(settings.examplePath)?.groupValues ?: emptyList()
          val patterns = processor.buildRegexForAdditionalSourcesPaths(groups)
          
          groups.joinToString("|") + "\n" + patterns.joinToString("\n") { it.first.orEmpty() }
        } catch (e: Exception) {
          log.warn("Exception while parsing pattern: ${e.message}")
          "=== Incorrect pattern ===\n" + e.message
        }
      })
  }
  
  
  private fun convertSubtitlesEncoding() {
    val records = filesTable.items.toList()
    
    runActionInBackground("Encoding conversion", processingInProgressProperty.asEditable(), {
      processor.convertSubtitlesEncoding(records)
    })
  }
  
  private fun tryFindRelativePath(path: Path): Path {
    return if (path.isAbsolute) {
      if (path.startsWith(settings.workingDir))
        settings.workingDir.relativize(path)
      else
        path
    } else {
      path
    }
  }
  
  override fun onViewDestroy() {
    super.onViewDestroy()
    processor.close()
  }
}
