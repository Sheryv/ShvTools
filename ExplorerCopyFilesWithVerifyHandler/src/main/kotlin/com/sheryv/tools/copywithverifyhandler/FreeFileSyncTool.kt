package com.sheryv.tools.copywithverifyhandler

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

private const val LEFT_PATTERN = "****LEFT****"
private const val RIGHT_PATTERN = "****RIGHT****"
private const val TEMP_FILE_PREFIX = "copy_with_verify_file_list_"

object FreeFileSyncTool : Tool {
  const val NAME = "freefilesync"
  private val LEFT_REGEX = Regex(Regex.escape(LEFT_PATTERN))
  private val RIGHT_REGEX = Regex(Regex.escape(RIGHT_PATTERN))
  
  override fun call(inputFiles: List<File>, cmd: CopyHandlerCommand) {
    val source = inputFiles[0]
    if (inputFiles.size > 1 || !source.isDirectory) {
      val msg = "ERROR: FreeFileSync supports only single directory comparison"
      println(msg)
      if (!cmd.noDialog) {
        CopyHandlerCommandManager.msg(msg)
      }
      return
    }
    
    val target = cmd.target.toPath().resolve(source.name).toFile()
  
    val resource = javaClass.classLoader.getResource("ExplorerHandler_SyncSettings.ffs_gui")
    var xml = resource!!.readText(StandardCharsets.UTF_8)
    xml = LEFT_REGEX.replace(xml, Regex.escapeReplacement(source.absolutePath))
    xml = RIGHT_REGEX.replace(xml, Regex.escapeReplacement(target.absolutePath))
  
    val tempFile = Files.createTempFile(TEMP_FILE_PREFIX, ".ffs_gui")
    try {
      println("Temporary file list will be saved in \"${tempFile.toAbsolutePath()}\"")
      Files.writeString(tempFile, xml)
    
      val params = arrayOf(
        cmd.toolPath?.absolutePath ?: defaultPath(),
        tempFile.toAbsolutePath().toString(),
      )
      println("Executing: ${params.joinToString(" ")}")
      Runtime.getRuntime().exec(params)
      Thread.sleep(400)
    
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }
  
  override fun name() = NAME
  
  override fun defaultPath() = "C:\\Program Files\\FreeFileSync\\FreeFileSync.exe"
}
