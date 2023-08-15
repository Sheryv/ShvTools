package com.sheryv.tools.cmd.adbuninstaller

import com.sheryv.tools.cmd.CMD_OPTIONS_LABEL
import com.sheryv.tools.cmd.CMD_PARAMETERS_LABEL
import com.sheryv.util.Version
import com.sheryv.util.VersionUtils
import picocli.CommandLine
import picocli.CommandLine.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  try {
    val commandLine = CommandLine(AdbUninstallCommand())
    exitProcess(commandLine!!.execute(*args))
  } catch (e: IllegalStateException) {
    println("Cannot execute operation: " + e.message)
  }
}

object UninstallAndroidApp {
  
  private fun fileNameWithoutExt(fileName: String): String {
    val pos = fileName.lastIndexOf(".")
    return if (pos == -1) fileName else fileName.substring(0, pos)
  }
  
  fun validate() {
    val listFile = Paths.get("list.txt")
    if (!listFile.toFile().exists()) {
      throw IllegalStateException("list.txt file not found")
    }
  }
  
  fun uninstall() {
    validate()
    val listFile = Files.readAllLines(Paths.get("list.txt"))
    
    listPackages(listFile).forEach { app ->
      println("Uninstalling " + app.packageName)
      val exec = Runtime.getRuntime().exec("adb shell pm uninstall -k --user 0 " + app.packageName)
      println("Removed " + app.packageName + " -> " + app.fileName)
    }
  }
  
  fun backup() {
    validate()
    val listFile = Files.readAllLines(Paths.get("list.txt"))
    val dir = File("backups")
    dir.mkdirs()
    println("Backups will be saved in " + dir.absolutePath)
    
    listPackages(listFile).forEach { app ->
      println("Backing up " + app.packageName)
      val exec = Runtime.getRuntime().exec("adb pull " + app.path + " " + app.buildFileName(), null, dir)
      println("Pulled " + app.fileName + "_" + app.packageName + ".apk")
    }
  }
  
  fun list(
    filterText: String? = null,
    disabled: Boolean = false,
    enabled: Boolean = false,
    system: Boolean = false,
    thirdParty: Boolean = false
  ) {
    val listFile = if (Files.exists(Paths.get("list.txt"))) {
      Files.readAllLines(Paths.get("list.txt"))
    } else {
      emptyList()
    }
  
    println("List of matched packages that are present on device and are provided in list.txt file. \n" +
        "If list.txt file is empty it returns all packages from device. Filters provided in params apply.\n")
    println("FORMAT:\npackage id : file path on device : optional file name (for backup command)\n")
    listPackages(listFile, filterText, disabled, enabled, system, thirdParty).forEach {
      System.out.printf("%-50s : %s \t: %s%n", it.packageName, it.path, it.buildFileName())
    }
    
  }
  
  private fun listPackages(
    filterLines: List<String> = emptyList(),
    filterText: String? = null,
    disabled: Boolean = false,
    enabled: Boolean = false,
    system: Boolean = false,
    thirdParty: Boolean = false
  ): List<App> {
    var params = ""
    if (disabled) {
      params += " -d"
    }
    if (enabled) {
      params += " -e"
    }
    if (system) {
      params += " -s"
    }
    if (thirdParty) {
      params += " -3"
    }
    if (!filterText.isNullOrBlank()) {
      params += " $filterText"
    }
    
    val exec = Runtime.getRuntime().exec("adb shell pm list packages -f$params")
    val devicePackages = BufferedReader(InputStreamReader(exec.inputStream)).useLines { lines ->
      lines
        .filter { it.contains('=') }
        .map {
          val line = it.substring(8)
          val index = line.lastIndexOf('=')
          val path = line.substring(0, index)
          val fileName = if (path.endsWith("base.apk")) {
            ""
          } else {
            Paths.get(path).fileName.toString()
          }
          App(path, fileName, line.substring(index + 1))
        }
        .sortedBy { it.packageName + it.path }
        .toList()
    }
    
    if (filterLines.isEmpty()) {
      return devicePackages
    }
    
    val toBackupPaths: MutableList<App> = ArrayList()
    for (s in filterLines) {
      val read = s.trim()
      if (read.startsWith("#") || "" == read) continue
      var path = ""
      var packageId = read
      if (read.contains(':')) {
        val split = read.split(':')
        packageId = split[0].trim()
        path = split[1].trim()
      }
      if (path.isBlank()) {
        val found = devicePackages.firstOrNull { it.packageName == packageId }
        if (found != null) {
          path = found.path
        } else {
          continue
        }
      }
      val fileName = if (path.endsWith("base.apk")) {
        ""
      } else {
        Paths.get(path).fileName.toString()
      }
      toBackupPaths.add(App(path, fileName, packageId))
    }
    toBackupPaths.removeAll { fromFile -> devicePackages.none { fromFile.packageName == it.packageName } }
    return toBackupPaths
  }
  
  private fun getPathFromADB(packageId: String): String {
    val exec = Runtime.getRuntime().exec("adb shell pm path $packageId")
    return BufferedReader(InputStreamReader(exec.inputStream)).use {
      val line = it.readLine()?.trim()
      if (line == null || "" == line) {
        throw IllegalArgumentException("Cannot get path from ADB for package \"$packageId\"")
      }
      line.split(':')[1]
    }
  }
  
  private class App constructor(val path: String, val fileName: String, val packageName: String) {
    fun buildFileName(): String {
      return if (fileName.isBlank()) {
        packageName + ".apk"
      } else {
        packageName + "__" + fileName
      }
    }
  }
}


@CommandLine.Command(
  name = "adb-uninstaller",
  aliases = ["adbu"],
  description = [AdbUninstallCommand.DESCRIPTION],
  parameterListHeading = CMD_PARAMETERS_LABEL,
  optionListHeading = CMD_OPTIONS_LABEL,
)
class AdbUninstallCommand : Callable<Int> {
  companion object {
    private var VER: Version? = null
    const val DESCRIPTION = "%nUninstalls applications on Android device connected to local machine. " +
        "Apps to uninstall have to be written in separate lines in list.txt file in the same directory. " +
        "Each line should contain application id [package] i.e: com.google.chrome. " +
        "Character # can be used to skip lines.%n"
    
    fun version(): Version {
      if (VER == null)
        VER = VersionUtils.loadVersionByModuleName("android-adb-app-uninstaller-version")!!
      return VER!!
    }
    
  }
  
  @Option(names = ["-h", " --help"], description = ["Show this help message and exit."], usageHelp = true)
  private var help: Boolean = false
  
  @CommandLine.Spec
  private lateinit var spec: CommandLine.Model.CommandSpec
  
  @Command(name = "list", aliases = ["l"],
    description = ["Lists packages with paths (use 'list --help' to show options)"],
    mixinStandardHelpOptions = true
  )
  fun list(
    @Parameters(paramLabel = "<FILTER>", description = ["Filter packages by provided text"], defaultValue = "") filterText: String?,
    @Option(names = ["-d"], description = ["Filter to only show disabled packages"]) disabled: Boolean,
    @Option(names = ["-e"], description = ["Filter to only show enabled packages"]) enabled: Boolean,
    @Option(names = ["-s"], description = ["Filter to only show system packages"]) system: Boolean,
    @Option(names = ["-3"], description = ["Filter to only show third party packages"]) thirdParty: Boolean,
  ) {
    UninstallAndroidApp.list(filterText, disabled, enabled, system, thirdParty)
  }
  
  @Command(name = "backup", aliases = ["b"], description = ["Save packages to local machine as backup"])
  fun backup() {
    UninstallAndroidApp.backup()
  }
  
  @Command(name = "uninstall", aliases = ["u"], description = ["Uninstall packages from android device"])
  fun uninstall() {
    println(" --------- uninstall ------------")
    UninstallAndroidApp.uninstall()
    
  }
  
  override fun call(): Int {
    throw CommandLine.ParameterException(spec.commandLine(), "Specify a subcommand")
  }
}

