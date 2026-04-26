package com.sheryv.tools.videoconverter

import com.sheryv.tools.videoconverter.subtitles.EncodingConverterView
import com.sheryv.tools.videoconverter.subtitles.TranslatorView
import com.sheryv.tools.videoconverter.video.*
import com.sheryv.tools.videoconverter.video.process.ConversionProcessor
import com.sheryv.util.fx.core.Styles
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.core.view.ViewUtils
import com.sheryv.util.fx.core.view.runActionInBackground
import com.sheryv.util.fx.lib.*
import com.sheryv.util.logging.log
import contextMenuPerRow
import item
import javafx.beans.binding.Binding
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.stage.Stage
import javafx.util.converter.DoubleStringConverter
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class VideoConverterView(val settings: MainSettings) : SimpleView() {
  
  private val selected = FXCollections.observableArrayList<ConvertVideo>()
  private val statusProperty = stringProperty("")
  private val processingInProgressProperty = booleanProperty(false)
  private val exampleMatchingResult = stringProperty("")
  private val detailedVideoProperty = mutableProperty<ConvertVideo?>(null)
  private lateinit var processor: ConversionProcessor
  private lateinit var sourcesPane: Pane
  private lateinit var sourcesTableView: TableView<SourceSettings>
  private lateinit var filesTable: TableView<ConvertVideo>
  
  private val bindings = mutableListOf<Binding<*>>()
  
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
      
      filesTable = TableView<ConvertVideo>().apply {
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
        columnBound("Command", 2000) {
          stringBinding(it, *it.commandDependencies.toTypedArray()) { processor.generateCommand(it).joinToString(" ") }
        }
        contextMenuPerRow {
          item("Copy command").action {
            ViewUtils.saveToClipboard(processor.generateCommand(it.item).joinToString(" "))
          }
        }
      }
      
      toolbar(
        buttonIcon("\ue2c8", tooltip = "Select working directory").action {
          factory.dialogs.openDirectoryDialog(stage, initialDir = settings.workingDir.toString())
            ?.apply { settings.workingDirProperty.value = this.toAbsolutePath().toString() }
        },
        buttonIcon("\ue161", tooltip = "Save settings / Reload").action {
          settings.save()
        },
        separator(orientation = Orientation.VERTICAL).growV(),
        buttonIcon("\uEEF8", "One-off", tooltip = "Run for single movie") {
          setOnAction {
            factory.dialogs.openFileDialog(stage, initialFile = settings.workingDir.absolutePathString())?.let {
              settings.workingDirProperty.value = it.parent.absolutePathString()
              val exp = Pattern.compile(it.nameWithoutExtension, Pattern.LITERAL).pattern()
              settings.videoPathPatternProperty.value = "^" + exp.split(' ').first() + ".*\\." + it.extension
              settings.sources.first().run {
                this.pathPatternProperty.value = "^" + exp + ".*\\." + SourceType.AUDIO.fileExtensions.joinToString("|", "(", ")")
                this.defaultTimeOffsetProperty.value = 0.0
              }
              settings.sources[1].run {
                this.pathPatternProperty.value = "^" + exp + ".*\\." + SourceType.SUBTITLES.fileExtensions.joinToString("|", "(", ")")
                this.defaultTimeOffsetProperty.value = 0.0
              }
              settings.outputDirProperty.value = ""
              findFiles()
            }
          }
        },
        buttonIcon("\ue01c", "Subs to UTF-8", tooltip = "Convert encoding of matching subtitles files to UTF-8") {
          setOnAction {
            factory.createWindow(EncodingConverterView::class)()
          }
        },
        buttonIcon(
          "\ue8e2",
          "Subs translate",
          tooltip = "Translate matching SRT subtitles from English to selected language using Gemini."
        ) {
          setOnAction {
            factory.createWindow(TranslatorView::class)()
          }
        },
        separator(orientation = Orientation.VERTICAL).growV(),
        buttonIcon("\ue872", tooltip = "Remove selected items from the list") {
          disableProperty().bind(selected.sizeProperty.map { it == 0 })
          styleClass.add("btn-warn")
          
          setOnAction {
            filesTable.items.removeAll(selected)
          }
        },
        Region().growH(),
        buttonIcon("\uf385", "Scan filesystem") {
          visibleProperty().bind(processingInProgressProperty.not())
          setOnAction {
            findFiles()
          }
        },
        buttonIcon("\ue037", "Start conversion", tooltip = "Start conversion for all items on the list") {
          visibleProperty().bind(processingInProgressProperty.not())
          disableProperty().bind(filesTable.items.sizeProperty.isEqualTo(0))
          managedProperty().bind(visibleProperty())
          styleClass.add("btn-info")
          setOnAction {
            val records = filesTable.items.toList()
            
            runActionInBackground("Video conversion", processingInProgressProperty.asEditable(), {
              processor.start(records)
            }, {
            
            })
          }
        },
        buttonIcon("\uef71", "Cancel process") {
          visibleProperty().bind(processingInProgressProperty)
          managedProperty().bind(visibleProperty())
          styleClass.add("btn-danger")
          setOnAction {
            processor.cancelProcessing()
          }
        },
      )
      
      vbox {
        vgrow = Priority.ALWAYS
        paddingAll = 10.0
        
        
        // main
        splitpane {
          vbox0 {
            filesTable.attachTo(this).grow()
          }.grow()
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
            hgrow = Priority.SOMETIMES
            
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
            hgrow = Priority.SOMETIMES
//            hbox {
//              label("Sources:").growH()
//            }
//            hbox {
//              label("#")
//              label("Pattern").growH().strech()
//              label("Time offset")
//              label("Text encoding") {
//                prefWidth = 90.0
//              }
//              label("Language code")
//            }
//            sourcesPane = vbox()
            
            sourcesTableView = TableView(FXCollections.observableArrayList<SourceSettings>()).attachTo(this).apply {
              this.selectionModel.selectionMode = SelectionMode.SINGLE
              styleClass.add(Styles.CLASS_MONO)
              isEditable = true
              vgrow = Priority.ALWAYS
              isFillWidth = true
              bindings.addAll(settings.sources.map {
                Bindings.createStringBinding(
                  { it.toString() },
                  it.typeProperty,
                  it.audioTypeProperty,
                  it.languageProperty,
                  it.defaultTimeOffsetProperty,
                  it.pathPatternProperty
                ).apply {
                  onChange { s ->
                    log.debug("Setting source changed: {}", s)
                  }
                }
              })
              
              columnBound("#", 30) { it.indexProperty }
              columnBoundToComboCell(
                "Type",
                modelValue = { it.typeProperty },
                default = listOf(SourceType.AUDIO, SourceType.SUBTITLES).asObservable(),
              )
              columnBoundToTextFieldCell(
                "Pattern",
                500,
                modelValue = { it.pathPatternProperty }
              )
              columnBoundToTextFieldFormattedCell(
                "Offset",
                default = 0.0,
                modelValue = { it.defaultTimeOffsetProperty },
                builder = { TextField().stripNonNumeric() }
              )
              columnBoundToTextFieldCell("Language code", modelValue = { it.languageProperty })
              columnBoundToComboCell(
                "Audio type",
                modelValue = { it.audioTypeProperty },
                default = SourceAudioType.entries.asObservable(),
                onUpdate = { item, row, combo ->
                  if (item != null && row != null) {
                    combo?.disableProperty()?.bind(row.typeProperty.mapObservable { it != SourceType.AUDIO })
                  } else {
                    combo?.disableProperty()?.unbind()
                  }
                })
              
              
            }
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


//    settings.sources.forEachIndexed { i, source ->
//
//      sourcesPane.hbox {
//        background = Styles.background(Color.TRANSPARENT)
//        label((i + 1).toString())
//        textfield(source.pathPatternProperty) {
//          promptText = "(disabled - fill pattern to enable)"
//          styleClass.add(Styles.CLASS_MONO)
//        }.growH()
//        textfield(source.defaultTimeOffsetProperty) {
//          maxWidth = 50.0
//        }.stripNonNumeric()
//        combo(
//          Charset.availableCharsets().values.map { it.name() }.filter { !it.startsWith("x-") }.toList().asObservable(),
//          source.textFileEncodingProperty
//        ) {
//          prefWidth = 120.0
//        }
//        textfield(source.languageProperty) {
//          maxWidth = 50.0
//        }
//      }
//    }
    
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
    
    log.debug("SETTINGS: \n{}", settings.sources.joinToString("\n"))
    sourcesTableView.items = settings.sources
    sourcesTableView.refresh()
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
