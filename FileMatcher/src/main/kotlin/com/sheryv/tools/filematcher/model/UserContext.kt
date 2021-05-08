package com.sheryv.tools.filematcher.model

import com.sheryv.tools.filematcher.utils.BundleUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class UserContext(var repo: Repository? = null,
                  var bundle: String? = null,
                  var version: Long? = null,
                  var basePath: File? = null
) {
  
  fun isFilled() = repo != null && bundle != null && version != null && basePath != null
  
  fun getBundle(): Bundle {
    return repo!!.bundles.first { it.id == bundle }
  }
  
  fun getBundleOrNull(): Bundle? {
    return repo?.bundles?.firstOrNull { it.id == bundle }
  }
  
  fun getVersion(): BundleVersion {
    return getBundle().versions.first { it.versionId == version }
  }
  
  fun getEntries(): List<Entry> {
    val bundl = getBundle()
    val result = bundl.versions.first { it.versionId == version }.entries.toMutableList()
    if (bundl.versioningMode == BundleMode.AGGREGATE_OLD) {
      val previous = bundl.versions.filter { it.versionId < version!! }.sortedBy { it.versionId }
      previous.forEach { v ->
        v.entries.forEach { e ->
          if (result.none { it.id == e.id }) {
            result.add(e)
          }
        }
      }
    }
    return result
  }
  
  fun getParentEntry(parentId: String): Entry {
    return getEntries().firstOrNull { it.id == parentId }
        ?: throw IllegalArgumentException("Parent entry not found in context: ${parentId}")
  }
  
  fun buildDirPathForEntry(entry: Entry): Path {
    val entries = getEntries()
    val found = entries.firstOrNull { it.id == entry.id }
        ?: throw IllegalArgumentException("Entry not found in context: ${entry.id}, ${entry.name}")
  
    if (found.parent != null) {
      return Paths.get(basePath!!.absolutePath, *BundleUtils.getParents(found.parent, entries).mapNotNull { it.target.directory?.findPath() }.reversed().toTypedArray())
    }
    return basePath!!.toPath()
  }
  
}
