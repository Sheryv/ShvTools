package com.sheryv.tools

import picocli.CommandLine
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

val RESET = "\u001b[0m" // Text Reset
val BLACK = "\u001b[0;30m" // BLACK
val RED = "\u001b[0;31m" // RED
val GREEN = "\u001b[0;32m" // GREEN
val YELLOW = "\u001b[0;33m" // BLUE
val BLUE = "\u001b[0;34m" // BLUE
val PURPLE = "\u001b[0;35m" // BLUE
val CYAN = "\u001b[0;36m" // CYAN
val WHITE = "\u001b[0;37m" // WHITE
val BLACK_BRIGHT = "\u001b[0;90m" // BLACK
val RED_BRIGHT = "\u001b[0;91m" // RED
val GREEN_BRIGHT = "\u001b[0;92m" // GREEN
val YELLOW_BRIGHT = "\u001b[0;93m" // YELLOW
val BLUE_BRIGHT = "\u001b[0;94m" // BLUE
val PURPLE_BRIGHT = "\u001b[0;95m" // PURPLE
val CYAN_BRIGHT = "\u001b[0;96m" // CYAN
val WHITE_BRIGHT = "\u001b[0;97m" // WHITE
val COLORS = listOf(
  GREEN,
  BLUE,
  CYAN,
  YELLOW,
  PURPLE,
  RED,
  BLACK_BRIGHT,
  GREEN_BRIGHT,
  YELLOW_BRIGHT,
  BLUE_BRIGHT,
  PURPLE_BRIGHT,
  CYAN_BRIGHT,
  RED_BRIGHT,
)


companion object MKVMergeVars {
  const val PROGRAM_PATH = "F:\\__Programs\\mkvtoolnix\\mkvmerge.exe"
  
  private fun escapeQuotes(data: String): String {
    return data
//    return data.replace("'", """\'""")
  }
  
  //      |--display-dimensions 0:1920x1080
  private val TEMPLATE =
    """$PROGRAM_PATH
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
      | --track-order 0:0,1:0,0:1,1:1,0:2""".trimMargin().replace("\n", "")
  
  private val TEMPLATE_WITHOUT_SUBTITLES =
    """$PROGRAM_PATH
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
      | --track-order 0:0,1:0,0:1,1:1""".trimMargin().replace("\n", "")
  
  fun fillTemplate(
    output: String,
    inputVideo: String,
    inputAudio: String,
    delay: Int,
    priority: String,
    lang: String,
    hasSubtitles: Boolean,
    videoFlags: String ,
    audioFlags: String ,
    audioTrackId: Int = 0,
  ): String {
    var audioId = audioTrackId;
    if (inputAudio.endsWith(".mp4") || inputAudio.endsWith(".ts") || inputAudio.endsWith(".mkv")){
      audioId = 1;
    }
    
    return if (hasSubtitles)
      String.format(TEMPLATE, priority, escapeQuotes(output), videoFlags, escapeQuotes(inputVideo), audioId, delay, audioId, lang, audioFlags, escapeQuotes(inputAudio))
    else
      String.format(TEMPLATE_WITHOUT_SUBTITLES, priority, escapeQuotes(output), videoFlags, escapeQuotes(inputVideo), audioId, delay, audioId, lang, audioFlags, escapeQuotes(inputAudio))
  }
  
}

fun time(): String {
  return LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME).take(12)
}
fun convert(params: MKVMergeCommand) {
  println("Starting conversion. Params: ${args.toList()}")
  println(params)
  
  val files = params.videoDir.listFiles()!!.filter { !it.isDirectory && it.isFile }
  val audioFiles = params.audioDir.listFiles()!!.filter { !it.isDirectory && it.isFile }
  val regex = params.createRegex()
  val toConvert = files.associateWith { regex.find(it.name) }.filter { it.value != null }.map {
    val foundPhrase = it.value!!.groups[0]!!.value
    Pair(it.key, audioFiles.first { it.name.contains(foundPhrase, true) })
  }.toMap()
  
  val width = (toConvert.maxOfOrNull { it.key.name.length } ?: 20).toInt()
  
  val toRun = toConvert.map {
    val outputDir = params.getOutput()
    outputDir.mkdirs()
    MKVMergeVars.fillTemplate(
      outputDir.resolve(it.key.name).toString(),
      it.key.absolutePath, it.value.absolutePath, params.delay, params.priority, params.language, params.addSubtitles,
      params.videoInputOptions, params.audioInputOptions
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


var commandLine: CommandLine? = null
try {
  val c = MKVMergeCommand()
  commandLine = CommandLine(c)
  commandLine!!.parseArgs(*args)
  convert(c)
} catch (e: CommandLine.ParameterException) {
  commandLine!!.usage(System.out)
} catch (e: Exception) {
  e.printStackTrace()
}

println()


@CommandLine.Command(
  name = "<command>",
  mixinStandardHelpOptions = true,
  version = [MKVMergeCommand.VERSION],
  description = ["", "Adds audio track from separate file to MKV video. Version: " + MKVMergeCommand.VERSION, ""]
)
class MKVMergeCommand {
  companion object {
    const val PRIORITY_HIGH = "higher"
    const val VERSION = "0.1"
    
    @JvmStatic
    val PRIORITIES = listOf("lowest", "lower", "normal", PRIORITY_HIGH, "highest")
  }
  
  
  @CommandLine.Parameters(
    paramLabel = "PATTERN",
    description = [" regex pattern in file name to match, may be double quoted; it also used to match video and audio", "Example: when audio and video names contains \"Exx\" phrase eg. E01 then pattern \"[eE]\\d\\d\" can be used"]
  )
  lateinit var pattern: String
  
  @CommandLine.Parameters(paramLabel = "VIDEO_DIR", description = ["Path to dir with video files, can be empty \"\"."])
  lateinit var videoDir: File
  
  @CommandLine.Parameters(paramLabel = "AUDIO_DIR", description = ["Path to dir with audio files."])
  lateinit var audioDir: File
  
  @CommandLine.Option(
    names = ["-s", "--copy-subtitles"],
    description = ["Whether to copy subtitles to target file. By default they are not copied"]
  )
  var addSubtitles = false
  
  @CommandLine.Option(
    names = ["-p", "--priority"],
    description = ["Priority of process used by operating system; Valid values are \"lowest\", \"lower\", \"normal\", \"higher\", \"highest\".", "Default is higher"]
  )
  var priority: String = PRIORITY_HIGH
  
  @CommandLine.Option(
    names = ["-d", "--delay"],
    description = ["Delay of audio track in milliseconds, can be negative.", "Default is 0"]
  )
  var delay: Int = 0
  
  @CommandLine.Option(
    names = ["-t", "--threads"],
    description = ["Number of parallel threads to use.", "Default is 4"]
  )
  var parallelThreads: Int = 4
  
  @CommandLine.Option(
    names = ["-o", "--output"],
    description = ["Directory for output files, relative to VIDEO_DIR path. Absolute paths can be used.", "Default is \"output\"."]
  )
  var output: String = "output"
  
  @CommandLine.Option(
    names = ["--language"],
    description = ["Language of added audio track. Consists of 2 chars. Ex. \"en\". Default is \"pl\"."]
  )
  var language: String = "pl"
  
  @CommandLine.Option(
    names = ["-ao"],
    description = ["Additional options for video input file.","Default is \"\"."]
  )
  var videoInputOptions: String = ""
  
  @CommandLine.Option(
    names = ["-vo"],
    description = ["Additional options for audio input file.", "Default is \"-D\" what means skip video track from input file"]
  )
  var audioInputOptions: String = "-D"
  
  fun createRegex() = Regex(pattern)
  
  fun getOutput(): File {
    val o = File(output)
    if (o.isAbsolute) {
      return o
    }
    return videoDir.resolve(o)
  }
  
  
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
}
