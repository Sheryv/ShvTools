package com.sheryv.tools.cmd

import com.sheryv.tools.cmd.adbuninstaller.AdbUninstallCommand
import com.sheryv.tools.cmd.convertmovienames.ConvertMovieNames
import com.sheryv.tools.cmd.videomerge.AddAudioToMKVWithMkvtoolnix
import com.sheryv.util.VersionUtils
import picocli.CommandLine
import java.nio.file.Path

const val CMD_PARAMETERS_LABEL = "%nParameters:%n"
const val CMD_OPTIONS_LABEL = "%nOptions:%n"
const val CMD_SUB_COMMANDS_LABEL = "%nSubcommands:%n"

@CommandLine.Command(
  name = "shv-tools",
  mixinStandardHelpOptions = true,
  sortOptions = false,
  usageHelpWidth = 120,
  showDefaultValues = true,
  parameterListHeading = CMD_PARAMETERS_LABEL,
  optionListHeading = CMD_OPTIONS_LABEL,
  commandListHeading = CMD_SUB_COMMANDS_LABEL,
  description = ["%nThis application provides various tools - see any subcommand help for details"],
  subcommands = [
    AddAudioToMKVWithMkvtoolnix::class,
    AdbUninstallCommand::class,
    ConvertMovieNames::class
  ], footer = ["%nRun 'shv-tools [COMMAND] -h' to see help for each individual command"]
)
class MainCommand {
}


object Main {
  const val PARAM_CONFIG_DIR = "configuration.directory-path"
  
  @JvmStatic
  lateinit var configPath: Path
  
  @JvmStatic
  fun main(args: Array<String>) {
    configPath = Path.of(System.getProperty(PARAM_CONFIG_DIR, ""))
    val version = VersionUtils.loadVersionByModuleName("cmd-tools-version")
    val commandLine = CommandLine(MainCommand())
    commandLine.commandSpec.version(version.toString())
    
    System.exit(commandLine.execute(*args))
  }
  
}
