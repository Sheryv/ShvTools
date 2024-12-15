package com.sheryv.tools.webcrawler.view.search

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.base.event.FetchedDataExternalChangeEvent
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.DirectUrl
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Episode
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.M3U8Url
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.service.streamingwebsite.generator.MetadataGenerator
import com.sheryv.tools.webcrawler.service.videosearch.SearchItem
import com.sheryv.tools.webcrawler.service.videosearch.TmdbApi
import com.sheryv.tools.webcrawler.service.videosearch.TmdbEpisode
import com.sheryv.tools.webcrawler.utils.DialogUtils
import com.sheryv.tools.webcrawler.utils.Utils
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.emitEvent
import com.sheryv.util.fx.core.Styles
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.lib.*
import com.sheryv.util.inBackground
import com.sheryv.util.io.HttpSupport
import com.sheryv.util.logging.log
import com.sheryv.util.singleAssign
import com.sheryv.util.unit.BinarySize
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

class SearchView(override val config: Configuration) : SimpleView() {
  
  private val name = stringProperty("")
  private val season = objectProperty(1)
  private val inProgress = booleanProperty(false)
  private val api = TmdbApi()
  private val settings = GlobalState.currentCrawler.findSettings(config) as StreamingWebsiteSettings
  
  private val results = FXCollections.observableArrayList<SearchItem>()
  private val selectedItem = objectProperty<SearchItem?>()
  private var sourceSeries: Series? = null
  private val series = objectProperty<Series?>(null)
  private val episodes = series.toList { it?.episodes }
  private val selectedEpisodes = FXCollections.observableArrayList<Episode>()
  
  override val root: Parent by lazy {
    createRoot {
      paddingAll = 5.0
      hbox {
        disableProperty().bind(inProgress)
        alignment = Pos.CENTER_LEFT
        isFillWidth = true
        spacing = 10.0
        label("Search name")
        textfield(name) {
          hgrow = Priority.ALWAYS
        }
        label("Season")
        Spinner<Int>(1, 100, 1, 1).attachTo(this).bind(season)
        button("Search") {
          styleClass.add("btn-success")
          setOnAction {
            inProgress.set(true)
            results.setAll(api.searchTv(name.value))
            selectedItem.set(results.first())
            inProgress.set(false)
          }
        }
      }
      hbox {
        disableProperty().bind(inProgress)
        alignment = Pos.CENTER_LEFT
        spacing = 10.0
        isFillWidth = true
        ChoiceBox(results).attachTo(this).apply {
          hgrow = Priority.ALWAYS
          maxWidth = Double.MAX_VALUE
          valueProperty().bindBidirectional(selectedItem)
          styleClass.add("mono")
        }
        button("Offset +").setOnAction {
          changeOffset(1)
        }
        button("Offset -").setOnAction {
          changeOffset(-1)
        }
      }
      hbox {
        disableProperty().bind(inProgress)
        alignment = Pos.CENTER_LEFT
        spacing = 10.0
        isFillWidth = true
        
        button("Remove selected") {
          disableProperty().bind(selectedEpisodes.sizeProperty.mapObservable { it == 0 })
          setOnAction {
            updateEpisodes(series.value!!.episodes.filterNot { selectedEpisodes.contains(it) })
          }
        }
        pane {
          hgrow = Priority.ALWAYS
        }
        button("Find file size").setOnAction {
          inBackground(inProgress.asEditable()) {
            val http = HttpSupport()
            episodes.forEach { e ->
              e.downloadUrl?.let {
                
                val size = if (it.isStreaming) {
                  Utils.getChunksOfM3U8Video(it.base).takeIf { it.isNotEmpty() }?.let { list ->
                    http.getContentSize(list.first())?.let { it * list.size }
                  }
                } else {
                  http.getContentSize(it.base)
                }
                
                e.lastSize.set(size ?: 0)
              }
            }
          }
        }
        button("Load from file").setOnAction {
          inBackground {
            val s = try {
              SerialisationUtils.jsonMapper.readValue(settings.outputPath.toFile(), Series::class.java)
            } catch (e: Exception) {
              log.error("Cannot load series from file", e)
              null
            }
            sourceSeries = s
            series.set(s)
            if (s != null) {
              name.set(s.title)
              season.set(s.season)
            }
          }
        }
        button("Edit as text").setOnAction {
          
          val text = series.value!!.episodes.joinToString("\n") { "%2d | %-40s | %s".format(it.number, it.title, it.url.value) }
          
          val dialog = DialogUtils.textAreaDialog(
            "Edit as text:",
            text,
            wrapText = false,
            editable = true,
            buttons = arrayOf(ButtonType.OK, ButtonType.CANCEL),
            type = Alert.AlertType.NONE
          )
          if (dialog.first == ButtonType.OK) {
            val mappedEpisodes = dialog.second.lines()
              .mapIndexed { index, l ->
                val parts = l.split('|').map { it.trim() }
                index to parts
              }
              .filter { it.second.size >= 3 }
              .map { (ind, parts) ->
                val existing = series.value!!.episodes.find { it.number == parts[0].toInt() }
                val url = parts[2].takeIf { it.isNotBlank() }?.let {
                  if (it.contains(".m3u8")) M3U8Url(it)
                  else DirectUrl(it)
                }
                
                if (existing != null) {
                  val old = existing.url
                    .takeIf { it.value.isNotBlank() }
                    ?.let {
                      if (it.value.contains(".m3u8")) M3U8Url(it.value)
                      else DirectUrl(it.value)
                    }
                    ?: existing.downloadUrl
                  
                  existing.copy(number = ind + 1, title = parts[1], downloadUrl = old?.let {
                    if (url?.base == old.base) old
                    else url
                  } ?: url)
                } else {
                  Episode(0, parts[1], ind + 1, url, "")
                }
              }
            
            val s = series.value!!.copy(episodes = mappedEpisodes)
            
            if (DialogUtils.textAreaDialog(
                "Confirm changes:",
                s.formattedString(),
                wrapText = false,
                buttons = arrayOf(ButtonType.OK, ButtonType.CANCEL),
                type = Alert.AlertType.NONE
              ).first == ButtonType.OK
            ) {
              updateEpisodes(mappedEpisodes)
            }
          }
        }
        button("Clear list") {
          disableProperty().bind(series.isNull)
          setOnAction {
            if (series.value != null) {
              series.set(series.value!!.copy(episodes = emptyList()))
            }
          }
        }
        button("Save to file").setOnAction {
          if (series.value != null) {
            inBackground(inProgress.asEditable()) {
              SerialisationUtils.jsonMapper.writeValue(settings.outputPath.toFile(), series.value)
              emitEvent(FetchedDataExternalChangeEvent())
            }
          }
        }
        button("Update file names").setOnAction {
          disableProperty().bind(series.isNull)
          
          if (series.value != null) {
            inBackground(inProgress.asEditable()) {
              val s = series.value!!
              val eps = s.episodes.toMutableList()
              val map = mutableMapOf<String, String>()
              for (e in s.episodes) {
                val old =
                  sourceSeries!!.episodes.find { it.id == e.id && it.id > 0 } ?: sourceSeries!!.episodes.find { it.number == e.number }
                if (old != null) {
                  map[old.generateFileName(sourceSeries!!, settings)] = e.generateFileName(s, settings)
                  eps.remove(e)
                }
              }
              val list = eps.map { it.generateFileName(s, settings) }
              val seriesDir = Path.of(settings.downloadDir).resolve(s.generateDirectoryPathForSeason())
              
              
              withContext(Dispatchers.Main) {
                if (DialogUtils.textAreaDialog(
                    "Confirm following changes:",
                    "Output: $seriesDir\nMapped files: \n${map.map { it.key + " -> " + it.value }.joinToString("\n")}\nNew files: \n" +
                        list.joinToString("\n"),
                    wrapText = false,
                    editable = true,
                    buttons = arrayOf(ButtonType.OK, ButtonType.CANCEL),
                    type = Alert.AlertType.NONE
                  ).first == ButtonType.OK
                ) {
                  withContext(Dispatchers.IO) {
                    for ((s, t) in map) {
                      val old = seriesDir.resolve(s)
                      Files.move(old, seriesDir.resolve(t))
                      val nfo = seriesDir.resolve(old.nameWithoutExtension + ".nfo")
                      if (Files.exists(nfo)) {
                        Files.move(nfo, seriesDir.resolve(seriesDir.resolve(t).nameWithoutExtension + ".nfo"))
                      }
                    }
                    for (c in list) {
                      Files.createFile(seriesDir.resolve(c))
                    }
                  }
                }
              }
            }
          }
        }
        button("Show file list") {
          disableProperty().bind(series.isNull)
          setOnAction {
            openFilesListDialog()
          }
        }
        button("Generate metadata") {
          disableProperty().bind(series.isNull)
          setOnAction {
            
            val s = series.value!!
            MetadataGenerator(settings).generateNfoMetadata(s)
            val seriesDir = Path.of(settings.downloadDir).resolve(s.generateDirectoryPathForSeason())
            
            if (seriesDir.parent.listDirectoryEntries().none { it.name == "poster.jpg" || it.name == "poster.png" }) {
              val http = HttpSupport()
              s.posterUrl?.also {
                val format = it.substringAfterLast('.', "jpg")
                http.stream(HttpSupport.getRequest(s.posterUrl)).body().use {
                  Files.copy(it, seriesDir.parent.resolve("poster.$format"), StandardCopyOption.REPLACE_EXISTING)
                }
              }
            }
            log.info("Metadata generated")
          }
        }
      }
      hbox {
        isFillWidth = true
        textfield(selectedItem.m().filter { it != null }.map { i ->
          String.format(
            "%-30s -> %s [%d, %s] %s | %.2f",
            i!!.name, i.originalName, i.id, i.originalLanguage, i.firstAirDate, i.popularity
          )
        }) {
          styleClass.add("mono")
          isEditable = false
          hgrow = Priority.ALWAYS
        }
      }
      TableView(episodes).attachTo(this).apply {
        this.selectionModel.selectionMode = SelectionMode.MULTIPLE
        Bindings.bindContent(selectedEpisodes, selectionModel.selectedItems);
        styleClass.add("mono")
        vgrow = Priority.ALWAYS
        isFillWidth = true
        isEditable = true
        column("#", 30) { it.number }
        columnBound("Size") { it.lastSize.mapObservable { if (it == 0L) "" else BinarySize.format(it) } }
        column("Name", 300) { it.title }
        columnBound("URL", 1600) { it.url }.apply {
          cellFactory = TextFieldTableCell.forTableColumn()
          isEditable = true
        }
      }
      hbox {
        progressbar(-1.0) {
          minWidth = 200.0
          visibleProperty().bind(inProgress)
        }
      }
    }
  }
  
  override fun onViewCreated(stage: Stage) {
    super.onViewCreated(stage)
    selectedItem.onChangeNotNull { i ->
      fetchEpisodes(i)
    }
    
    title = "Search and fill episodes links"
  }
  
  private fun changeOffset(offset: Int) {
    updateEpisodes(series.value!!.episodes.map {
      it.copy(number = it.number + offset)
    })
  }
  
  private fun updateEpisodes(episodes: List<Episode>) {
    series.set(
      series.value!!.copy(
        episodes = episodes
      )
    )
  }
  
  private fun fetchEpisodes(i: SearchItem) {
    inProgress.set(true)
    inBackground {
      try {
        val seasonNum = season.value
        val updatedEpisodes = api.getTvEpisodes(i.id, seasonNum).episodes.map { ep: TmdbEpisode ->
          
          val found = sourceSeries?.episodes?.firstOrNull { l -> l.number == ep.episodeNumber }
          if (found != null) {
            return@map found.copy(
              id = ep.id,
              title = ep.name,
              downloadUrl = found.url.takeIf { it.value.isNotBlank() }?.let {
                if (it.value == found.downloadUrl?.base) found.downloadUrl
                else if (it.value.contains(".m3u8")) M3U8Url(it.value)
                else DirectUrl(it.value)
              })
              .apply { lastSize.set(found.lastSize.value) }
          }
          Episode(ep.id, ep.name, ep.episodeNumber, null, "")
        }
        val imdb = api.getImdbId(i.id)
        
        series.set(
          Series(
            i.id,
            i.name,
            seasonNum,
            sourceSeries?.lang.orEmpty(),
            "",
            i.posterUrl(),
            i.id.toString(),
            imdb,
            i.firstAirDate?.let { LocalDate.parse(it) },
            updatedEpisodes,
          )
        )
      } catch (e: Exception) {
        log.error("Cannot fetch episodes", e)
      } finally {
        inProgress.set(false)
      }
    }
  }
  
  private fun openFilesListDialog() {
    val s = series.value!!
    val ext = stringProperty("ts")
    val list = ext.mapObservable { s.episodes.joinToString("\n") { it.generateFileName(s, settings, ext.value) } }
    
    val pane = createRoot(500.0, 350.0) {
      paddingAll = 5
      hbox {
        textfield("ts") {
          ext.bind(textProperty())
          promptText = "Extension"
          hgrow = Priority.ALWAYS
        }
        button("Generate") {
          setOnAction {
            DialogUtils.openDirectoryDialog(stage)?.run {
              val dir = toAbsolutePath().resolve(s.generateDirectoryPathForSeason())
              Files.createDirectories(dir)
              s.episodes
                .map { it.generateFileName(s, settings) }
                .forEach { Files.createFile(dir.resolve(it)) }
              
              log.info("Files generated in $dir")
            }
          }
        }
        
      }
      textarea(list) {
        isEditable = false
        isWrapText = false
        vgrow = Priority.ALWAYS
        styleClass.add("mono")
      }
    }
    
    
    DialogUtils.dialog(
      "",
      "Files",
      type = Alert.AlertType.NONE,
      components = pane,
      buttons = arrayOf(ButtonType.OK)
    )
  }
}
