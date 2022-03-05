@file:JvmName("ExecuteCopyWithVerifyMainKt")

package com.sheryv.tools.copywithverifyhandler

import picocli.CommandLine
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.nio.file.Paths
import java.util.regex.MatchResult
import java.util.regex.Pattern
import kotlin.system.exitProcess

private val PATTERN = Pattern.compile(" +")
private val DIR_PATTERN = Pattern.compile("""Directory: ([\S ]+)\n""")
private val LINE_PATTERN = Pattern.compile("""[a-z-]+\s\d+\.\d+\.\d+\s\d+:\d+\s(\d+\s)?([\S ]+)\n""")

fun main(args: Array<String>) {
  val res = CopyHandlerCommandManager.parse(args) {
    
    var files: List<File> = emptyList()
    try {
//      files = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.javaFileListFlavor) as List<File>
      files = fallbackClipbaordReader()
    } catch (e: Exception) {
    
    }
    
    if (files.isNullOrEmpty()) {
      println("Cannot copy files. No data found in clipboard")
      if (!it.noDialog) {
        CopyHandlerCommandManager.msg("Cannot copy files. No data found in clipboard")
      }
    } else {
      println("Output directory: ${it.target}")
      println("Files/Directories to copy: \n\t" + files.joinToString("\n\t"))
      
      CopyHandlerCommandManager.TOOLS[it.tool]?.call(files, it)
        ?: throw CommandLine.PicocliException("\"${it.tool}\" is not supported tool name. Available are: ${CopyHandlerCommandManager.TOOLS.values.joinToString { it.name() }}")
    }
  }
  exitProcess(if (res) 0 else 1)
}


private fun matchAll(pattern: Pattern, input: String): List<MatchResult> {
  val res = mutableListOf<MatchResult>()
  val matcher = pattern.matcher(input)
  do {
    val find = matcher.find()
    if (find)
      res.add(matcher.toMatchResult())
  } while (!matcher.hitEnd())
  return res
}

private fun fallbackClipbaordReader(): List<File> {
  val exec = Runtime.getRuntime().exec(arrayOf("powershell", "Get-Clipboard", "-Format", "FileDropList"))
  exec.waitFor()
  val string = PATTERN.matcher(String(exec.inputStream.readBytes())).replaceAll(" ").replace("\r", "")
  exec.destroy()
  
  if (string.isNotBlank()) {
    val dir = matchAll(DIR_PATTERN, string)[0].group(1).trim()
    return matchAll(LINE_PATTERN, string).map { Paths.get(dir, it.group(it.groupCount()).trim()).toFile() }
  }
  return emptyList()
}
