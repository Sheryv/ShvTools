package com.sheryv.tools.cmd.videomerge

import com.sheryv.tools.cmd.CMD_OPTIONS_LABEL
import com.sheryv.tools.cmd.CMD_PARAMETERS_LABEL
import com.sheryv.tools.cmd.Colors.COLORS
import com.sheryv.tools.cmd.Colors.RESET
import picocli.CommandLine
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.Executors


val VIDEO_FORMATS = listOf("mkv", "mp4", "m4v", "ts", "avi")
val AUDIO_FORMATS = listOf("mp3", "aac")

object MKVMergeCommandRunner {
  
  const val PROGRAM_PATH = "F:\\__Programs\\mkvtoolnix\\mkvmerge.exe"
  
  private fun escapeQuotes(data: String): String {
    return data
//    return data.replace("'", """\'""")
  }
  
  //      |--display-dimensions 0:1920x1080
  private val TEMPLATE =
    """%s
      | --ui-language en
      | --priority %s
      | --output "%s"
      | --no-global-tags
      | --no-chapters
      | --language 0:und
      | --language 1:en
      | --language 2:en
      | %s
      | "(" "%s" ")"
      | --sync %d:%d
      | --language %d:%s
      | %s
      | "(" "%s" ")"
      | --track-order 0:0,1:1,1:0,0:1,0:2""".trimMargin().replace("\n", "")
  
  private val TEMPLATE_WITHOUT_SUBTITLES =
    """%s
      | --ui-language en
      | --priority %s
      | --output "%s"
      | --no-global-tags
      | --no-chapters
      | --language 0:und
      | --language 1:en
      | %s
      | "(" "%s" ")"
      | --sync %d:%d
      | --language %d:%s
      | %s
      | "(" "%s" ")"
      | --track-order 0:0,1:1,1:0,0:1""".trimMargin().replace("\n", "")
  
  private fun fillTemplate(
    output: String,
    inputVideo: String,
    inputAudio: String,
    delay: Int,
    priority: String,
    lang: String,
    hasSubtitles: Boolean,
    videoFlags: String,
    audioFlags: String,
    programPath: String,
    audioTrackId: Int = 0,
  ): String {
    var audioId = audioTrackId;
    if (inputAudio.endsWith(".mp4") || inputAudio.endsWith(".ts") || inputAudio.endsWith(".mkv")) {
      audioId = 1;
    }
    
    return if (hasSubtitles)
      String.format(
        TEMPLATE,
        programPath.takeIf { it.isNotBlank() } ?: PROGRAM_PATH,
        priority,
        escapeQuotes(output),
        videoFlags,
        escapeQuotes(inputVideo),
        audioId,
        delay,
        audioId,
        lang,
        audioFlags,
        escapeQuotes(inputAudio)
      )
    else
      String.format(
        TEMPLATE_WITHOUT_SUBTITLES,
        programPath.takeIf { it.isNotBlank() } ?: PROGRAM_PATH,
        priority,
        escapeQuotes(output),
        videoFlags,
        escapeQuotes(inputVideo),
        audioId,
        delay,
        audioId,
        lang,
        audioFlags,
        escapeQuotes(inputAudio)
      )
  }
  
  
  private fun time(): String {
    return LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME).take(12)
  }
  
  private fun convert(params: AddAudioToMKVWithMkvtoolnix) {
    println("Starting conversion")
    println(params)
    
    val files = params.videoDir.listFiles()?.filter { !it.isDirectory && it.isFile && VIDEO_FORMATS.contains(it.extension) }
      ?: throw IllegalArgumentException("Path ${params.videoDir.toPath().toAbsolutePath()} is incorrect")
    val audioFiles = params.audioDir.listFiles()?.filter { !it.isDirectory && it.isFile && AUDIO_FORMATS.contains(it.extension) }
      ?: throw IllegalArgumentException("Path ${params.audioDir.toPath().toAbsolutePath()} is incorrect")
    val regex = params.createRegex()
    val toConvert = files.associateWith { regex.find(it.name) }.filter { it.value != null }.mapNotNull {
      val foundPhrase = it.value!!.groups[0]!!.value
      val second = audioFiles.firstOrNull { it.name.contains(foundPhrase, true) }
      if (second != null) {
        Pair(it.key, second)
      } else {
        println("No matching audio file found using phrase '$foundPhrase' for video '${it.key.name}'")
        null
      }
    }.toMap()
    
    require(toConvert.isNotEmpty()) { "No matching files to convert" }
    
    val width = (toConvert.maxOfOrNull { it.key.name.length } ?: 20).toInt()
    
    val toRun = toConvert.map {
      val outputDir = params.getOutput()
      outputDir.mkdirs()
      fillTemplate(
        outputDir.resolve(it.key.name).toString(),
        it.key.absolutePath, it.value.absolutePath, params.delay, params.priority, params.language, params.addSubtitles,
        params.videoInputOptions, params.audioInputOptions, params.mkvMergePath,
      )
    }
    
    
    println(
      "Matched ${toConvert.size}/${files.size} files: \n\t${
        toConvert.map { "${it.key.name.padEnd(width)} + ${it.value.name}" }.joinToString("\n\t")
      }\n\n"
    )
    
    
    val parts = mutableListOf<MutableList<String>>()
    for (i in 1..params.parallelThreads) {
      parts.add(mutableListOf())
    }
    
    toRun.forEachIndexed { i, cmd ->
      val list = parts[i % params.parallelThreads]
      list.add(cmd)
    }
    
    val threadPool = Executors.newFixedThreadPool(params.parallelThreads)
    
    val results = parts.filter { it.isNotEmpty() }.mapIndexed { partNum, list ->
      val color = COLORS[partNum % COLORS.size]
      println(
        String.format(
          "%s[%-12s] Initialised thread %02d%s for converting %d items",
          color,
          time(),
          partNum + 1,
          RESET,
          list.size
        )
      )
      return@mapIndexed threadPool.submit {
        list.forEachIndexed { i, cmd ->
//        val process = ProcessBuilder(MKVMergeVars.PROGRAM_PATH, cmd)
//          .directory(Paths.get("").toAbsolutePath().toFile())
//          .redirectOutput(ProcessBuilder.Redirect.PIPE)
//          .redirectError(ProcessBuilder.Redirect.PIPE)
//          .start()
          println(String.format("%s[%02d-%02d]%s Running: %s", color, partNum + 1, i + 1, RESET, cmd))
          val process = Runtime.getRuntime().exec(cmd)
          val reader = process.inputStream.bufferedReader()
          var line = reader.readLine()
          while (line != null) {
            println(String.format("%s%-12s [%02d-%02d]%s: %s", color, time(), partNum + 1, i + 1, RESET, line))
            line = reader.readLine()
          }
          reader.close()
          process.waitFor()
        }
        println(String.format("[%-12s] Processing for thread %s%02d%s finished", time(), color, partNum + 1, RESET))
      }
    }
    
    results.forEach { it.get() }
    threadPool.shutdownNow()
  }
  
  
  fun start(cmd: AddAudioToMKVWithMkvtoolnix) {
    convert(cmd)
    println()
  }
}


@CommandLine.Command(
  name = "merge-mkv-streams",
  aliases = ["mms"],
  showDefaultValues = true,
  parameterListHeading = CMD_PARAMETERS_LABEL,
  optionListHeading = CMD_OPTIONS_LABEL,
  description = ["%nAdds audio track from separate file to MKV video."]
)
class AddAudioToMKVWithMkvtoolnix : Callable<Int> {
  companion object {
    const val PRIORITY_HIGH = "higher"
    
    @JvmStatic
    val PRIORITIES = listOf("lowest", "lower", "normal", PRIORITY_HIGH, "highest")
  }
  
  
  @CommandLine.Parameters(
    paramLabel = "<PATTERN>",
    description = ["Regex pattern in file name to match, may be double quoted; it also used to match video and audio. It doesn't have to match entire name",
      "Example: when audio and video names contains \"Exx\" phrase eg. E01 then pattern \"[eE]\\d\\d\" can be used",
      "If names of video and audio contains phrase \"Star Wars\" and there is only these 2 files in directory, then it is enough to provide just \"Star Wars\""]
  )
  lateinit var pattern: String
  
  @CommandLine.Parameters(paramLabel = "<VIDEO_DIR>", description = ["Path to dir with video files, can be empty \"\"."])
  lateinit var videoDir: File
  
  @CommandLine.Parameters(paramLabel = "<AUDIO_DIR>", description = ["Path to dir with audio files."])
  lateinit var audioDir: File
  
  @CommandLine.Option(names = ["-h", "--help"], description = ["Show this help message and exit."], usageHelp = true)
  private var help: Boolean = false
  
  @CommandLine.Option(
    names = ["-s", "--copy-subtitles"],
    defaultValue = "false",
    description = ["Whether to copy subtitles to target file. By default they are not copied"]
  )
  var addSubtitles = false
  
  @CommandLine.Option(
    names = ["-p", "--priority"],
    defaultValue = PRIORITY_HIGH,
    description = ["Priority of process used by operating system; Valid values are \"lowest\", \"lower\", \"normal\", \"higher\", \"highest\"."]
  )
  var priority: String = PRIORITY_HIGH
  
  @CommandLine.Option(
    names = ["-o", "--output"],
    defaultValue = "output",
    description = ["Directory for output files, relative to VIDEO_DIR path. Absolute paths can be used."]
  )
  var output: String = ""
  
  @CommandLine.Option(
    names = ["-d", "--delay"],
    defaultValue = "0",
    description = ["Delay of audio track in milliseconds, can be negative."]
  )
  var delay: Int = 0
  
  @CommandLine.Option(
    names = ["-t", "--threads"],
    defaultValue = "4",
    description = ["Number of parallel threads to use."]
  )
  var parallelThreads: Int = 0
  
  @CommandLine.Option(
    names = ["--language"],
    defaultValue = "pl",
    description = ["Language of added audio track. Consists of 2 chars. Ex. \"en\"."]
  )
  var language: String = ""
  
  @CommandLine.Option(
    names = ["-vo"],
    description = ["Additional options for video input file.", "See https://mkvtoolnix.download/doc/mkvmerge.html#d4e1970"]
  )
  var videoInputOptions: String = ""
  
  @CommandLine.Option(
    names = ["-ao"],
    defaultValue = "-D",
    description = ["Additional options for audio input file.", "See https://mkvtoolnix.download/doc/mkvmerge.html#d4e1970",
      "\"-D\" means skip video track from input file"]
  )
  var audioInputOptions: String = ""
  
  @CommandLine.Option(
    names = ["--exec"],
    description = ["Path to mkvmerge executable"]
  )
  var mkvMergePath: String = ""
  
  fun createRegex() = Regex(pattern)
  
  fun getOutput(): File {
    val o = File(output)
    if (o.isAbsolute) {
      return o
    }
    return videoDir.resolve(o)
  }
  
  
  @CommandLine.Spec
  internal lateinit var spec: CommandLine.Model.CommandSpec
  
  override fun toString(): String {
    return """
      |pattern=$pattern,
      |videoDir=$videoDir,
      |audioDir=$audioDir,
      |addSubtitles=$addSubtitles,
      |priority=$priority,
      |delay=$delay,
      |parallelThreads=$parallelThreads,
      |videoInputOptions=$videoInputOptions,
      |audioInputOptions=$audioInputOptions,
      |output=$output)""".trimMargin()
  }
  
  override fun call(): Int {
    MKVMergeCommandRunner.start(this)
    return 0;
  }
}
