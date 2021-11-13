package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.Hashing
import com.sheryv.tools.filematcher.utils.lg
import java.io.File
import java.nio.file.Paths

class EntryHashUpdater(
  private val context: UserContext,
  onFinish: ((ProcessResult<List<Entry>, EntryHashUpdater>) -> Unit)? = null
) : Process<List<Entry>>(onFinish as ((ProcessResult<List<Entry>, out Process<List<Entry>>>) -> Unit)?, false) {
  
  
  override suspend fun process(): List<Entry> {
    return updateHashes()
  }
  
  
  private fun updateHashes(): List<Entry> {
    
    val entries = context.getVersion().entries
    entries.forEach { e ->
      val entryDir = Paths.get(getEntryDir(context, e))
      val file = entryDir.resolve(e.name)
      if (!e.group) {
        if (file.toFile().exists()) {
          val md5 = Hashing.md5(file)
          e.hashes = e.hashes?.copy(md5 = md5) ?: Hash(md5)
        } else {
          lg().info("File does not exist: {}", file.toAbsolutePath())
        }
      }
    }
    return entries
  }
  
  private fun getEntryDir(context: UserContext, entry: Entry): String {
    return if (entry.target.absolute) {
      entry.target.directory!!.findPath()!!
    } else {
      context.buildDirPathForEntry(entry).resolve(entry.target.directory?.findPath() ?: "").toAbsolutePath().toString()
    }
  }
}
