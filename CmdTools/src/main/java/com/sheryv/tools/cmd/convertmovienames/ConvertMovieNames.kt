package com.sheryv.tools.cmd.convertmovienames

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sheryv.tools.cmd.CMD_OPTIONS_LABEL
import com.sheryv.tools.cmd.CMD_PARAMETERS_LABEL
import com.sheryv.tools.cmd.Main
import com.sheryv.tools.cmd.convertmovienames.videosearch.TmdbApi
import com.sheryv.tools.cmd.videomerge.VIDEO_FORMATS
import picocli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.stream.Collectors
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.UIManager
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo


var VERBOSE = false

fun main() {
  ConvertMovieNames().start()
}

@CommandLine.Command(
  name = "convert-movie-names",
  aliases = ["cmn"],
  showDefaultValues = true,
  parameterListHeading = CMD_PARAMETERS_LABEL,
  optionListHeading = CMD_OPTIONS_LABEL,
  description = ["%nConverts videos in selected directory to be compatible with TinyMediaManager"]
)
class ConvertMovieNames : Callable<Int> {
  private val removeSpacesPattern = Regex("""\s+""")
  private val replaceSeparatorCharsPattern = Regex("""[\-_+.]""")
  private val replaceSpecialCharsPattern = Regex("""['",]""")
  
  @CommandLine.Option(
    names = ["-v"],
    description = ["Log requests"]
  )
  var verbose = false
  
  fun start() {
//    FlatDarkLaf.setup()
    VERBOSE = verbose
    val chooser = JFileChooser(Path.of(".").toFile())
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    chooser.isAcceptAllFileFilterUsed = false
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      val rootDir = chooser.selectedFile.toPath()
      val input = findInputFiles(rootDir)
      if (input.isNotEmpty() && JOptionPane.showOptionDialog(
          null,
          "Found ${input.size} files. Continue?",
          "ConvertMovieNames",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE,
          null, null, null
        ) != JOptionPane.YES_OPTION
      ) {
        return
      }
      
      val results = input.map {
        val phrase = replaceSpecialCharsPattern.replace(it.nameWithoutExtension.lowercase(), "")
        val simplified = removeSpacesPattern.replace(replaceSeparatorCharsPattern.replace(phrase, " "), " ").trim()
        val firstPart = simplified.takeWhile { Character.isLetterOrDigit(it) || it == ' ' }.trim()
        val firstPartNoDigits = simplified.takeWhile { Character.isLetter(it) || it == ' ' }.trim()
        
        println("Searching for '$firstPart'")
        val items = try {
          TmdbApi.searchMovie(firstPart).takeIf { it.isNotEmpty() } ?: TmdbApi.searchMovie(firstPartNoDigits)
        } catch (e: Exception) {
          println("Error searching '$firstPart': " + e.message)
          emptyList()
        }
        
        it.relativeTo(rootDir) to items
      }.toMap()
      
      MainWindow(config, results, rootDir).associate()
    } else {
      println("No input dir")
    }
  }
  
  private fun findInputFiles(dir: Path): List<Path> {
    return Files.walk(dir, 3)
      .filter { it.isRegularFile() && (VIDEO_FORMATS.contains(it.extension)) }
      .collect(Collectors.toList())
  }
  
  
  override fun call(): Int {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    if (loadConfig()) {
      start()
      return 0
    }
    return 1
  }
  
  companion object {
    
    lateinit var config: Config
    
    private fun loadConfig(): Boolean {
      val c = Main.configPath.resolve("config_convert-movie-names.json")
      if (!Files.exists(c)) {
        mapper.writeValue(c.toFile(), Config())
        println("Cannot find config file. Created empty at ${c.toAbsolutePath()}")
        println("App will exit now")
        return false
      }
      config = mapper.readValue(c.toFile(), Config::class.java)
      
      try {
        config.validate()
      } catch (e: Exception) {
        println("Config file is incorrect at ${c.toAbsolutePath()}\n${e.message}\n")
        throw e
      }
      return true
    }
    
    val mapper = run {
      val map = ObjectMapper()
      map.configure(SerializationFeature.INDENT_OUTPUT, true)
      map.registerModule(KotlinModule.Builder().build())
      map.registerModule(JavaTimeModule())
      map.dateFormat = StdDateFormat()
      map.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      map.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      val mod = SimpleModule()
      mod.addSerializer(Path::class.java, ToStringSerializer())
      map.registerModule(mod)
      map
    }
  }
  
}
