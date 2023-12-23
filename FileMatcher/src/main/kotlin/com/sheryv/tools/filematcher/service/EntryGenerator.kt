package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.DevContext
import com.sheryv.tools.filematcher.model.Entry
import com.sheryv.tools.filematcher.model.Hash
import com.sheryv.tools.filematcher.model.ProcessResult
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.Hashing
import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.Utils
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class EntryGenerator(
  private val dir: File,
  private val context: DevContext,
  private val mode: Mode = Mode.REPLACE,
  onFinish: ((ProcessResult<List<Entry>, EntryGenerator>) -> Unit)? = null
) : Process<List<Entry>>(onFinish as ((ProcessResult<List<Entry>, out Process<List<Entry>>>) -> Unit)?, false) {
  
  
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
    
    if (mode != Mode.REPLACE) {
      
      
      val copyVersion = context.repoCopy
        .bundles.first { context.version.bundle.id == it.id }
        .versions.first { it.versionId == context.version.versionId }
      val copy = copyVersion.entries.toMutableList()
      
      val currentEntries = copy.associate {
        it.id to copyVersion.relativePathWithParents(it).run { if (!it.group) resolve(it.name) else this }
      }
      val newPaths = entries.associateBy {
        copyVersion.relativePathWithParents(it, entries).run { if (!it.group) resolve(it.name) else this }
      }
      
      copyVersion.entries.filter { it.group }.forEach { e ->
        val newParent = newPaths[currentEntries[e.id]!!]
        if (newParent != null) {
          entries[entries.indexOf(newParent)] = newParent.copy(id = e.id)
          entries.replaceAll {
            if (newParent.id == it.parent) {
              it.copy(parent = e.id, updateDate = e.updateDate ?: Utils.now())
            } else
              it
          }
        }
      }
      val result = copy.mapNotNull { e ->
        
        val path = currentEntries[e.id]!!
        if (newPaths[path] != null) {
          if (mode == Mode.MERGE_SEMI)
            return@mapNotNull null
          
          val nn = newPaths[path]!!
          entries.removeAt(entries.indexOfFirst { it.id == e.id || it.id == nn.id })
          
          val additional = e.additionalFields.toMutableMap()
          nn.additionalFields.forEach { additional.putIfAbsent(it.key, it.value) }
          val date = if (e.group) {
            null
          } else if (e.fileSize != nn.fileSize || e.hashes?.hasAnySameHash(nn.hashes) != true || e.additionalFields != additional) {
            nn.updateDate
          } else {
            e.updateDate
          }
          
          return@mapNotNull e.copy(
//            id = nn.id,
//            parent = nn.parent,
            fileSize = nn.fileSize,
            hashes = nn.hashes,
            updateDate = date,
            additionalFields = additional
          )
        }
//        if (e.parent == null) {
//          return@mapNotNull e
//        }
//
        e
      }.toMutableList()
      result.addAll(entries)
      
      return result
    }
    return entries
  }
  
  private fun loadEntriesFromFiles(dir: File, rootDir: File, parent: Entry? = null): MutableList<Entry> {
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
    
    val map = mapOf("createdAt" to Utils.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    var id: String?
    do {
      id = BundleUtils.idForEntry()
    } while (alreadyCreated.any { it.id == id })
    
    val fileDate = OffsetDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneOffset.UTC).withNano(0)
    return if (file.isDirectory) {
      BundleUtils.createGroup(id!!, file.name, parent = parent?.id, updateDate = Utils.now(), itemDate = fileDate, additionalFields = map)
    } else {
      val md5 = Hashing.md5(file.toPath())
      val fileSize = file.length().let { if (it == 0L) null else it }
      Entry(
        id!!,
        file.name,
        src,
        null,
        parent = parent?.id,
        itemDate = fileDate,
        updateDate = Utils.now(),
        hashes = Hash(md5),
        additionalFields = map,
        fileSize = fileSize
      )
    }
  }
  
  enum class Mode {
    REPLACE,
    MERGE_SEMI,
    MERGE_FULL
  }
}
