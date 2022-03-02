package com.sheryv.tools.copywithverifyhandler

import picocli.CommandLine
import java.io.File

private const val TOOLS_DESC = "${TeraCopyTool.NAME}, ${FreeFileSyncTool.NAME}"

object CopyHandlerCommandManager {
  val TOOLS = mapOf(FreeFileSyncTool.NAME to FreeFileSyncTool, TeraCopyTool.NAME to TeraCopyTool)
  
  fun parse(args: Array<String>, onSuccess: (CopyHandlerCommand) -> Unit): Boolean {
    var commandLine: CommandLine? = null
    val c = CopyHandlerCommand()
    try {
      commandLine = CommandLine(c)
      commandLine.parseArgs(*args)
      onSuccess(c)
      return true
    } catch (e: CommandLine.PicocliException) {
      println("Error: " + e.message + "\n\n")
      commandLine!!.usage(System.out)
      if (!c.noDialog) {
        msg(commandLine.usageMessage)
      }
    } catch (e: Exception) {
      println(e.stackTraceToString())
      if (!c.noDialog) {
        msg("Error executing command: ${args.joinToString(" ")}\n\n${e.stackTraceToString()}")
      }
    }
    return false
  }
  
  fun msg(msg: String) {
    Runtime.getRuntime().exec(arrayOf("cmd", "/k", "msg", "%username%", msg))
  }
}

interface Tool {
  fun call(inputFiles: List<File>, cmd: CopyHandlerCommand)
  
  fun defaultPath(): String
  
  fun name(): String
}


private const val VERSION = "0.2"

@CommandLine.Command(
  name = "<command>",
  mixinStandardHelpOptions = true,
  version = [VERSION],
  description = ["", "Start external tool to copy files/dirs from clipboard to provided dir. Version: $VERSION", ""]
)
class CopyHandlerCommand {
  @CommandLine.Parameters(
    paramLabel = "TOOL",
    description = ["Name of tool to use to execute copy process", "Available: $TOOLS_DESC"],
  )
  lateinit var tool: String
  
  @CommandLine.Parameters(paramLabel = "TARGET_DIRECTORY", description = ["Path to target directory"])
  lateinit var target: File
  
  @CommandLine.Option(
    names = ["-e", "--exec-path"],
    description = ["Path to executable for specific tool"],
    paramLabel = "<path>"
  )
  var toolPath: File? = null
  
  @CommandLine.Option(
    names = ["--no-dialog"],
    description = ["Do not show error dialogs"]
  )
  var noDialog: Boolean = false

//  @CommandLine.Option(
//    names = ["--freefilesync"],
//    description = ["Path to FreeFileSync main executable. Default: \"${FREEFILESYNC_PATH}\""]
//  )
//  var ffsPath: String = FREEFILESYNC_PATH
  
}

