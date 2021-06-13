@file:JvmName("ExecuteCopyWithVerifyMainKt")

package com.sheryv.tools.copywithverifyhandler

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.MatchResult
import java.util.regex.Pattern
import java.util.stream.Collectors

private val PATTERN = Pattern.compile(" +")
private val DIR_PATTERN = Pattern.compile("""Directory: ([\S ]+)\n""")
private val LINE_PATTERN = Pattern.compile("""[a-z-]+\s\d+\.\d+\.\d+\s\d+:\d+\s(\d+\s)?([\S ]+)\n""")
private const val TEMP_FILE_PREFIX = "copy_with_verify_file_list_"
private const val USAGE = "USAGE: command.exe <output_directory>"

fun main(args: Array<String>) {
  if (args.size != 1 || args[0].isBlank()) {
    println(USAGE)
    return
  }
  
  val exec = Runtime.getRuntime().exec(arrayOf("powershell", "Get-Clipboard", "-Format", "FileDropList"))
  exec.waitFor()
  val string = PATTERN.matcher(String(exec.inputStream.readBytes())).replaceAll(" ").replace("\r", "")
  exec.destroy()
  
  if (string.isBlank()) {
    println("Cannot copy files. No data found in clipboard")
  } else {
    val output = Paths.get(args[0])
    
    val tempFile = Files.createTempFile(TEMP_FILE_PREFIX, ".txt")
    println("Temporary file list will be saved in \"${tempFile.toAbsolutePath()}\"")
    
    var files: List<Path>? = null
    try {
      val dir = matchAll(DIR_PATTERN, string)[0].group(1).trim()
      files = matchAll(LINE_PATTERN, string)
        .map { Paths.get(dir, it.group(it.groupCount()).trim()) }
      
      println("Output directory: $output")
      println("Files/Directories to copy: \n\t" + files.joinToString("\n\t"))
      
      Files.write(tempFile, files.joinToString("\n").toByteArray())
      teracopyCall(tempFile.toAbsolutePath(), output)
    } catch (e: Exception) {
      msg("Cannot copy files. Ex: ${e.message}\n\n-> Files:\n${files?.joinToString("\n")}")
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }
}

private fun msg(msg: String) {
  Runtime.getRuntime().exec(arrayOf("cmd", "/k", "msg", "%username%", msg))
}

private const val TERA_COPY_PATH = "C:\\Program Files\\TeraCopy\\TeraCopy.exe"

private fun teracopyCall(input: Path, output: Path) {
  Runtime.getRuntime()
    .exec(
      arrayOf(
        TERA_COPY_PATH,
        "copy",
        "*${input.toAbsolutePath()}",
        output.toAbsolutePath().toString(),
        "/NoClose"
      )
    )
  Thread.sleep(400)
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
