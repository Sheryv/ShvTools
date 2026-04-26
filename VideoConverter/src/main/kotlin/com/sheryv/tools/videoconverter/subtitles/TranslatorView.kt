package com.sheryv.tools.videoconverter.subtitles

import com.sheryv.tools.videoconverter.video.MainSettings
import com.sheryv.util.fx.core.Styles
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.core.view.ViewUtils
import com.sheryv.util.fx.core.view.runActionInBackground
import com.sheryv.util.fx.lib.*
import com.sheryv.util.inMainContext
import com.sheryv.util.io.FileUtils
import com.sheryv.util.logging.log
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.ChoiceBox
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.stage.Stage
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class TranslatorView(val settings: MainSettings) : SimpleView() {
  private val suffix = "-[ENG]"
  private val defaultApiKeyFilePath = Path.of("secrets.properties")
  private val apiKeyProperty = stringProperty()
  private val searchPathProperty = stringProperty(settings.workingDir.absolutePathString())
  private val searchPatternProperty = stringProperty("**.srt")
  private val mediaTypeProperty = stringProperty(SubtitlesTranslator.MediaType.TV_SHOW.label)
  private val languageProperty = stringProperty("Polish")
  private val processingInProgressProperty = booleanProperty(false)
  private var translator: SubtitlesTranslator? = null
  private lateinit var filesTable: TableView<SubtitleFile>
  
  val searchPath by searchPathProperty.map { Path.of(it) }
  
  
  override val root: Parent by lazy {
    filesTable = TableView()
    
    createRoot(1200.0, 600.0) {
//      paddingAll = 0.0
//      spacing = 0.0
      
      vbox {
        labelWrap("Search root path", hbox {
          textfield(searchPathProperty).grow()
          button("...") {
            setOnAction {
              factory.dialogs.openDirectoryDialog(stage, initialDir = searchPathProperty.get())
                ?.apply { searchPathProperty.value = this.absolutePathString() }
            }
          }
          button("Search") {
            disableProperty().bind(processingInProgressProperty)
            setOnAction {
              searchFiles()
            }
          }
        })
        labelWrap("Search pattern (glob pattern)", hbox {
          textfield(searchPatternProperty).grow()
        })
        labelWrap(
          "Type of media subtitles are referring",
          ChoiceBox(SubtitlesTranslator.MediaType.entries.map { it.label }.asObservable()).attachTo(this).apply {
            maxWidth = Double.MAX_VALUE
            valueProperty().bindBidirectional(mediaTypeProperty)
          }).growH()
        labelWrap("Language (English name)", textfield(languageProperty))
        hbox {
          label(apiKeyProperty.map { if (it.isNotEmpty()) "Google AI Studio API key is set" else "Google AI Studio API key is not set" })
          hyperlink("(Where to get)").action {
            ViewUtils.openWebpage("https://aistudio.google.com/api-keys")
          }
          button {
            textProperty().bind(apiKeyProperty.map { if (it.isNotEmpty()) "Update key" else "Set key" })
            setOnAction {
              val results =
                factory.dialogs.inputDialog("API Key", "Provide API Key from Google AI Studio", listOf("Key" to apiKeyProperty.get()))
              if (results.isNotEmpty() && results.first().isNotBlank()) {
                apiKeyProperty.set(results.first())
                Files.writeString(defaultApiKeyFilePath, "ai_key=${results.first()}")
              }
            }
          }
        }
        filesTable.attachTo(this).apply {
          this.selectionModel.selectionMode = SelectionMode.MULTIPLE
          styleClass.add(Styles.CLASS_MONO)
          isFillWidth = true
          column("Path", 700) { searchPath.relativize(it.path) }
          columnBound("Status", 110) { it.stateProperty.map { it.label } }
          columnBound("Records", 100) { f ->
            f.subtitlesProperty.map {
              if (f.processed.value >= 0) {
                it?.getStatements()?.size?.let { f.processed.value.toString() + " / " + it } ?: "-"
              } else {
                it?.getStatements()?.size ?: "-"
              }
            }
          }
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
          button("Revert renaming") {
            disableProperty().bind(processingInProgressProperty.or(filesTable.items.sizeProperty.isEqualTo(0)))
            setOnAction {
              
              val items = filesTable.items.toList()
              runActionInBackground("Reverting", processingInProgressProperty.asEditable(), {
                for (file in items) {
                  if (file.path.nameWithoutExtension.endsWith(suffix)) {
                    val target =
                      file.path.resolveSibling(file.path.nameWithoutExtension.dropLast(suffix.length) + "." + file.path.extension)
                    
                    if (Files.exists(target)) {
                      FileUtils.renameAsBackup(target)
                    }
                    Files.move(file.path, target)
                  }
                }
              }, {
                searchFiles()
              })
            }
          }
          
          spacer()
          button("Start translation") {
            disableProperty().bind(processingInProgressProperty.or(filesTable.items.sizeProperty.isEqualTo(0)))
            styleClass.add("btn-info")
            
            setOnAction {
              translate()
            }
          }
        }
      }
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
        .map {
          val item = SubtitleFile(it)
          item.subtitles = Subtitles.parse(it)
          item
        }
        .toList()
    }, {
      filesTable.items.setAll(it)
    })
  }
  
  fun translate() {
    if (apiKeyProperty.get().isBlank()) {
      factory.dialogs.messageDialog("API key is not set!", Alert.AlertType.ERROR)
      return
    }
    
    val mediaType = SubtitlesTranslator.MediaType.entries.first { it.label == mediaTypeProperty.get() }
    val language = languageProperty.get()
    val items = filesTable.items.toList()
    runActionInBackground("Translating", processingInProgressProperty.asEditable(), {
      for (file in items) {
        try {
          inMainContext {
            file.state = TranslationState.PROCESSING
          }
          
          val result = translator!!.translateWithGemini(file.path, "", mediaType, null, language)
          if (result != null) {
            val oldFile = file.path.resolveSibling(file.path.nameWithoutExtension + "$suffix." + file.path.extension)
            if (oldFile.exists()) {
              FileUtils.renameAsBackup(oldFile)
            }
            Files.move(file.path, oldFile)
            Files.newBufferedWriter(file.path).use {
              result.translated.render(it)
            }
          }
          
          inMainContext {
            if (result != null) {
              file.subtitles = result.translated
              file.processed.value = result.translated.getStatements().size - result.missingEntries.size
            } else {
              file.processed.value = 0
            }
            file.state = TranslationState.COMPLETED
          }
        } catch (e: Exception) {
          log.error("Error while translating", e)
          inMainContext {
            file.processed.value = 0
            file.state = TranslationState.COMPLETED
          }
        }
      }
    })
  }
  
  override fun onViewCreated(stage: Stage) {
    super.onViewCreated(stage)
    
    apiKeyProperty.onChangeNotNull { apiKey ->
      if (apiKey.isNotBlank()) {
        translator = SubtitlesTranslator(apiKey)
      }
    }
    if (Files.exists(defaultApiKeyFilePath)) {
      val props = Properties()
      props.load(Files.newBufferedReader(defaultApiKeyFilePath))
      apiKeyProperty.set(props.getProperty("ai_key"))
    } else {
      apiKeyProperty.set("")
    }
  }
}

private enum class TranslationState(val label: String) {
  IN_QUEUE("In Queue"),
  PROCESSING("Processing"),
  COMPLETED("Completed"),
}

private class SubtitleFile(val path: Path, state: TranslationState = TranslationState.IN_QUEUE) {
  val stateProperty = mutableProperty(state)
  var state by stateProperty
  
  val subtitlesProperty = mutableProperty<Subtitles?>(null)
  var subtitles by subtitlesProperty
  
  val processed = mutableProperty(-1)
}
