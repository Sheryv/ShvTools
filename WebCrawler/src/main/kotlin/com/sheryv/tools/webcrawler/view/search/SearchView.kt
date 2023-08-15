package com.sheryv.tools.webcrawler.view.search

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.DirectUrl
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Episode
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.tools.webcrawler.service.videosearch.SearchItem
import com.sheryv.tools.webcrawler.service.videosearch.TmdbApi
import com.sheryv.tools.webcrawler.service.videosearch.TmdbEpisode
import com.sheryv.tools.webcrawler.utils.DialogUtils
import com.sheryv.util.BinarySize
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.fx.core.view.SimpleView
import com.sheryv.util.fx.lib.*
import com.sheryv.util.inBackground
import com.sheryv.util.logging.log
import com.sheryv.util.singleAssign
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.Priority
import javafx.stage.Stage
import kotlinx.coroutines.delay
import java.nio.file.Files
import java.time.LocalDate

class SearchView(override val config: Configuration) : SimpleView() {
  
  private val name = stringProperty("")
  private val season = objectProperty(1)
  private val inProgress = booleanProperty(false)
  private val api = TmdbApi()
  private val settings = GlobalState.currentCrawler.findSettings(config) as StreamingWebsiteSettings
  
  private var results = FXCollections.observableArrayList<SearchItem>()
  private var selectedItem = objectProperty<SearchItem?>()
  private var sourceSeries: Series? by singleAssign()
  private var series = objectProperty<Series?>(null)
  private var episodes = series.toList { it?.episodes }
  
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
        button("Find file size").setOnAction {
          log.debug("Search")
        }
        button("Clear list").setOnAction {
          log.debug("Search")
        }
        button("Save to file").setOnAction {
          inProgress.set(true)
          inBackground {
            delay(6000)
            inProgress.set(false)
          }
        }
        button("Show file list") {
          disableProperty().bind(series.map { it == null })
          setOnAction {
            val s = series.value!!
            val list = s.episodes.joinToString("\n") { it.generateFileName(s, settings) }
            if (DialogUtils.textAreaDialog(
                "Files",
                list,
                wrapText = false,
                buttons = arrayOf(ButtonType.OK, ButtonType.CANCEL),
                type = Alert.AlertType.NONE
              ) == ButtonType.OK
            ) {
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
        styleClass.add("mono")
        vgrow = Priority.ALWAYS
        isFillWidth = true
        isEditable = true
        column("#", 30) { it.number }
        columnBound("Size") { it.lastSize.map { if (it == 0L) "" else BinarySize.format(it) } }
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
    selectedItem.onChangeNotNull { i ->
      fetchEpisodes(i)
    }
    
    title = "Search and fill episodes links"
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
              title = ep.name,
              downloadUrl = found.url.value?.takeIf { it.isNotBlank() }?.let { DirectUrl(found.url.value) })
              .apply { lastSize.set(found.lastSize.value) }
          }
          Episode(ep.name, ep.episodeNumber, null, "")
        }
        val imdb = api.getImdbId(i.id)
        
        series.set(
          Series(
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
}
