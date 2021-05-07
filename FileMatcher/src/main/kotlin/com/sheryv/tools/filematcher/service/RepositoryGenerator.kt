package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.Hashing
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.Utils
import com.sheryv.util.Strings
import java.io.File
import java.net.URLEncoder
import java.time.format.DateTimeFormatter

class RepositoryGenerator(
  private val dir: File,
  onFinish: ((ProcessResult<Repository, RepositoryGenerator>) -> Unit)? = null
) : Process<Repository>(onFinish as ((ProcessResult<Repository, out Process<Repository>>) -> Unit)?, false) {
  
  
  override suspend fun process(): Repository {
    return generateFromLocalDir(dir)
  }
  
  
  private fun generateFromLocalDir(dir: File): Repository {
    val file = if (!dir.isDirectory) {
      dir.parentFile
    } else {
      dir
    }
    val entries = loadEntriesFromFiles(file!!, file)
    
    val bundles = listOf(
      Bundle(
        "bundle1", "Example bundle", preferredBasePath = BasePath(Configuration.get().devTools.bundlePreferredPath),
        versions = listOf(
          BundleVersion(
            versionId = 1,
            versionName = "0.1.0",
            entries = entries
          )
        )
      )
    )
    
    return Repository(
      "http://localhost/", "codeName", "1.0", 1,
      "http://localhost/",
      "Example title", "", mapOf("param1" to 123.toString()), bundles = bundles
    )
  }
  
  private fun loadEntriesFromFiles(dir: File, rootDir: File, parent: Entry? = null): List<Entry> {
    val list = mutableListOf<Entry>()
    for (child in dir.listFiles()!!) {
      if (child.isDirectory) {
        
        if (child.listFiles()?.size ?: 0 > 0) {
          val entry = createEntry(child, rootDir, parent, list)
          list.add(entry)
          list.addAll(loadEntriesFromFiles(child, rootDir, entry))
        }
        
      } else if (child.isFile) {
        list.add(createEntry(child, rootDir, parent, list))
      }
    }
    return list
  }
  
  private fun createEntry(file: File, rootDir: File, parent: Entry?, alreadyCreated: List<Entry>): Entry {
    val path = file.toRelativeString(rootDir).replace('\\', '/')
    val p = file.relativeTo(rootDir).toPath().joinToString("/") { SystemUtils.encodeNameForWeb(it.fileName.toString()) }
    
    val map = mapOf("generatedAt" to Utils.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    var id: String?
    do {
      id = Strings.generateId(4)
    } while (alreadyCreated.any { it.id == id })
    
    return if (file.isDirectory) {
      BundleUtils.createGroup(id!!, file.name, parent = parent?.id, itemDate = Utils.now(), additionalFields = map)
    } else {
      val md5 = Hashing.md5(file.toPath())
      val fileSize = file.length().let { if (it == 0L) null else it }
      Entry(
        id!!,
        file.name,
        p,
        null,
        parent = parent?.id,
        itemDate = Utils.now(),
        hashes = Hash(md5),
        additionalFields = map,
        fileSize = fileSize
      )
    }
  }
}
