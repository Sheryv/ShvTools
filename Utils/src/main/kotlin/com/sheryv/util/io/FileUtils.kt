package com.sheryv.util.io

import org.apache.commons.lang3.StringUtils
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

object FileUtils {
  private val FILE_NAME_FORBIDDEN_CHARS_PATTER = Pattern.compile("[\\\\/:*?\"<>|]")
  const val CHARSET = "UTF-8"
  
  @JvmStatic
  fun readFileInMemory(path: Path): String {
    return String(Files.readAllBytes(path), charset(CHARSET))
  }
  
  @JvmStatic
  fun readFileInMemory(path: String): String {
    return String(Files.readAllBytes(Path.of(path)), charset(CHARSET))
  }
  
  @JvmStatic
  fun readFileInMemorySilently(path: String): String? {
    return try {
      val p: Path = Path.of(path)
      readFileInMemory(p)
    } catch (e: IOException) {
//            log.e(e);
      null
    }
  }
  
  @JvmStatic
  fun readFileInMemorySilently(path: Path): String? {
    return try {
      readFileInMemory(path)
    } catch (e: IOException) {
//            log.e(e);
      null
    }
  }
  
  @JvmStatic
  fun readFileStream(path: String): BufferedReader {
    return readFileStream(Path.of(path))
  }
  
  @JvmStatic
  fun readFileStream(path: Path): BufferedReader {
    return BufferedReader(FileReader(path.toFile()))
  }
  
  @JvmStatic
  fun writeFileStream(path: Path): BufferedWriter {
    return BufferedWriter(FileWriter(path.toFile()))
  }
  
  @JvmStatic
  fun saveFile(text: String, file: Path): Boolean {
    try {
      Files.write(file, text.toByteArray(charset(CHARSET)))
      return true
    } catch (e: IOException) {
//            log.e(e);
    }
    return false
  }
  
  @JvmStatic
  fun mergeFilesFromDirToSingle(parts: List<Path>, output: Path): Path {
    try {
      Files.createDirectories(output.getParent())
      Files.createFile(output)
    } catch (e: IOException) {
      throw RuntimeException(e)
    }
    try {
      FileOutputStream(output.toFile()).use { stream ->
        for (part in parts) {
          Files.copy(part, stream)
        }
        return output
      }
    } catch (e: IOException) {
      throw RuntimeException(e)
    }
  }
  
  @JvmStatic
  fun sizeString(bytes: Long): String {
    var mb = bytes / 1024.0 / 1024.0
    var label = "MB"
    var precision = 0
    if (mb > 1024) {
      label = "GB"
      mb = mb / 1024.0
      precision = 2
    }
    return String.format("%." + precision + "f %s", mb, label)
  }
  
  @JvmStatic
  fun fixFileName(fileName: String?): String {
    return FILE_NAME_FORBIDDEN_CHARS_PATTER.matcher(fileName).replaceAll("")
  }
  
  @JvmStatic
  fun fixFileNameWithColonSupport(fileName: String?): String {
    var s: String = StringUtils.replace(fileName, " : ", " - ")
    s = StringUtils.replace(s, " :", " - ")
    s = StringUtils.replace(s, ": ", " - ")
    s = StringUtils.replace(s, ":", "-")
    return fixFileName(s)
  }
  
}
