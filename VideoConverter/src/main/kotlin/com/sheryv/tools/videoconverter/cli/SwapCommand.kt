package com.sheryv.tools.videoconverter.cli


import ch.qos.logback.classic.Level
import com.sheryv.tools.videoconverter.Context
import com.sheryv.tools.videoconverter.VideoConverterMain
import com.sheryv.tools.videoconverter.video.MainSettings
import com.sheryv.tools.videoconverter.video.SourceSettings
import com.sheryv.tools.videoconverter.video.SourceType
import com.sheryv.tools.videoconverter.video.process.ConversionProcessor
import com.sheryv.util.CoreUtils
import com.sheryv.util.inBackground
import com.sheryv.util.logging.LoggingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.milliseconds


@Command(
  name = "swap",
  description = ["Copies input streams to a new mkv container without transcoding."]
)
class SwapCommand : Callable<Int> {
  
  @Option(names = ["-w", "--working-dir"], description = ["Base directory to search for videos (default: current)"])
  var workingDir: Path = Path.of("").toAbsolutePath()
  
  @Option(names = ["-o", "--output-dir"], description = ["Directory where muxed files will be saved. Relative to working dir or absolute"])
  var outputDir: Path = Path.of("")
  
  @Option(names = ["-p", "--pattern"], description = ["Regex pattern to match video files"], defaultValue = ".*")
  var videoPathPattern: Pattern = Pattern.compile(".*")
  
  @Option(names = ["--depth"], description = ["How many levels deep to search for files"])
  var directorySearchDepth: Int = 4
  
  @Option(
    names = ["-y", "--auto-accept"],
    description = ["Automatically continue conversion for whatever was found"],
    defaultValue = "false"
  )
  var autoAccept: Boolean = false
  
  @Option(names = ["--parallel"], description = ["Number of concurrent mkvmerge processes"])
  var parallelProcessing: Int = 2
  
  @Option(names = ["--extension"], description = ["Output file extension"])
  var outputExtension: String = "mkv"
  
  @Option(names = ["-l", "--languages"], split = ",", description = ["Comma-separated list of language codes to keep (e.g., eng,jpn)"])
  var languageFilter: List<String> = emptyList()
  
  @OptIn(InternalCoroutinesApi::class)
  override fun call(): Int {
    LoggingUtils.setGlobalLevel(Level.ERROR)
    println("${VideoConverterMain.NAME}: Running command swap")
    println("Scanning filesystem in path '$workingDir'")
    
    val context = Context(toSettings(), Dispatchers.IO)
    val processor = ConversionProcessor(context, {
    
    }, {
      if (it != null) {
      
      }
    })
    val videos = runBlocking {
      processor.scanFilesystem(context.settings, emptyList())
    }
    
    val list = videos.joinToString("\n") { v ->
      "${
        v.number.toString().padStart(2, ' ')
      } > ${v.targetVideo}, Streams: ${v.metadata?.streams?.size}"
    }
    
    if (videos.isEmpty()) {
      println("No files found")
      return 1
    }
    
    println("Found files list:")
    println(list)
    println()
    
    if (!autoAccept) {
      print("Continue conversion for listed files? [y/n]: ")
      val scanner = Scanner(System.`in`)
      val text = scanner.nextLine()
      if (!CoreUtils.parseBoolean(text)) {
        println("Operation cancelled")
        return -1
      }
    }
    
    val progress = inBackground {
      while (true) {
        val percent = videos.map { it.progress.percentage }.average()
        val done = videos.count { it.progress.percentage > 99.0 }
        
        print("\rProgress: %2.1f%% %d/%d".format(percent, done, videos.size))
        delay(1000.milliseconds)
      }
    }
    
    print("Progress:  0%")
    
    val processJob = inBackground {
      processor.start(videos)
    }
    processJob.invokeOnCompletion {
      progress.cancel()
    }
    runBlocking {
      processJob.join()
      progress.join()
    }
    
    return 0
  }
  
  fun toSettings(): MainSettings {
    return MainSettings(
      workingDir = this.workingDir,
      outputDir = this.outputDir,
      directorySearchDepth = this.directorySearchDepth,
      parallelProcessing = this.parallelProcessing,
      outputExtension = this.outputExtension,
      languageFilter = this.languageFilter
    ).also {
      it.sources.setAll(SourceSettings(main = true, type = SourceType.ALL, pathPattern = this.videoPathPattern.pattern()))
    }
  }
}
