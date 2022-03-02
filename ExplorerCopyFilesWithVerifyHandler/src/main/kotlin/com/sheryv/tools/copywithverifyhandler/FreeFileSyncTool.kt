package com.sheryv.tools.copywithverifyhandler

import java.io.File

object FreeFileSyncTool : Tool {
  const val NAME = "freefilesync"
  
  
  override fun call(inputFiles: List<File>, cmd: CopyHandlerCommand) {
  
  
  }
  
  override fun name() = NAME
  
  override fun defaultPath() = "C:\\Program Files\\FreeFileSync\\FreeFileSync.exe"
}
