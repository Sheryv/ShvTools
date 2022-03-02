package com.sheryv.tools.copywithverifyhandler

import java.io.File
import java.nio.file.Files

object TeraCopyTool : Tool {
  private const val TEMP_FILE_PREFIX = "copy_with_verify_file_list_"
  const val NAME = "teracopy"
  
  override fun call(inputFiles: List<File>, cmd: CopyHandlerCommand) {
    val tempFile = Files.createTempFile(TEMP_FILE_PREFIX, ".txt")
    try {
      println("Temporary file list will be saved in \"${tempFile.toAbsolutePath()}\"")
      Files.write(tempFile, inputFiles.joinToString("\n").toByteArray())
      
      val params = arrayOf(
        cmd.toolPath?.absolutePath ?: defaultPath(),
        "copy",
        "*${tempFile.toAbsolutePath()}",
        cmd.target.absolutePath.toString(),
        "/NoClose"
      )
      println("Executing: ${params.joinToString(" ")}")
      Runtime.getRuntime().exec(params)
      Thread.sleep(400)
      
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }
  
  override fun name() = NAME
  
  override fun defaultPath() = "C:\\Program Files\\TeraCopy\\TeraCopy.exe"
}
