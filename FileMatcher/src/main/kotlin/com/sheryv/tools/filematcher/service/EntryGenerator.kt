package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.Hashing
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.Utils
import com.sheryv.util.Strings
import java.io.File
import java.nio.file.Path
import java.time.format.DateTimeFormatter

class EntryGenerator(
  private val dir: File,
  private val context: DevContext,
  private val replaceCurrentList: Boolean = true,
  onFinish: ((ProcessResult<List<Entry>, EntryGenerator>) -> Unit)? = null
) : Process<List<Entry>>(onFinish as ((ProcessResult<List<Entry>, out Process<List<Entry>>>) -> Unit)?, false) {
  
  private lateinit var currentEntries: Map<String, Path>
  
  override suspend fun process(): List<Entry> {
    return generateFromLocalDir()
  }
  
  private fun generateFromLocalDir(): List<Entry> {
    val file = if (!dir.isDirectory) {
      dir.parentFile
    } else {
      dir
    }
    
    val entries = loadEntriesFromFiles(file!!, file)
    
    if (!replaceCurrentList) {
      currentEntries = context.version.entries.associate { it.id to context.version.relativePathWithParents(it).resolve(it.name) }
      
      val newPaths = entries.associateBy { context.version.relativePathWithParents(it, entries).resolve(it.name) }
      
      val copy = context.version.entries.toMutableList()
      
      context.version.entries.filter { it.group }.forEach { e ->
        val newParent = newPaths[currentEntries[e.id]]
        if (newParent != null) {
          copy.replaceAll {
            if (e.id == it.parent) {
              it.copy(parent = newParent.id)
            } else
              it
          }
        }
      }
      val result = copy.mapNotNull { e ->
        
        val path = currentEntries[e.id]
        if (newPaths[path] != null) {
          return@mapNotNull null
        }
        if (e.parent == null) {
          return@mapNotNull e
        }
        
        e
      }.toMutableList()
      result.addAll(entries)
      
      return result
    }
    return entries
  }
  
  private fun loadEntriesFromFiles(dir: File, rootDir: File, parent: Entry? = null): List<Entry> {
    val list = mutableListOf<Entry>()
    for (child in dir.listFiles()!!) {
      val entry = createEntry(child, rootDir, parent, list)
      if (child.isDirectory) {
        
        if (child.listFiles()?.size ?: 0 > 0) {
          list.add(entry)
          list.addAll(loadEntriesFromFiles(child, rootDir, entry))
        }
        
      } else if (child.isFile) {
        list.add(entry)
      }
    }
    return list
  }
  
  private fun createEntry(file: File, rootDir: File, parent: Entry?, alreadyCreated: List<Entry>): Entry {
//    val path = file.toRelativeString(rootDir).replace('\\', '/')
    val src =
      file.relativeTo(rootDir).toPath().joinToString("/") { SystemUtils.encodeNameForWeb(it.fileName.toString()) }
    
    val map = mapOf("generatedAt" to Utils.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    var id: String?
    do {
      id = BundleUtils.idForEntry()
    } while (alreadyCreated.any { it.id == id })
    
    return if (file.isDirectory) {
      BundleUtils.createGroup(id!!, file.name, parent = parent?.id, itemDate = Utils.now(), additionalFields = map)
    } else {
      val md5 = Hashing.md5(file.toPath())
      val fileSize = file.length().let { if (it == 0L) null else it }
      Entry(
        id!!,
        file.name,
        src,
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
