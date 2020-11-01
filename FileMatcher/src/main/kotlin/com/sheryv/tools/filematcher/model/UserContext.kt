package com.sheryv.tools.filematcher.model

import com.sheryv.tools.filematcher.utils.BundleUtils
import java.io.File
import java.nio.file.Path

class UserContext(var repo: Repository? = null,
                  var bundle: String? = null,
                  var version: Long? = null,
                  var basePath: File? = null
) {
  
  fun isFilled() = repo != null && bundle != null && version != null && basePath != null
  
  fun getBundle(): Bundle {
    return repo!!.bundles.first { it.id == bundle }
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
        BundleUtils.forEachEntry(v.entries) { e ->
          if (result.none { it.id == e.id }) {
            result.add(e)
          }
        }
      }
    }
    return result
  }
  
  fun buildDirPathForEntry(entry: Entry): Path {
    val entries = getEntries()
    return fillPath(entries, basePath!!.toPath()) { it.id == entry.id }
        ?: throw IllegalArgumentException("Entry not found in context: ${entry.id}, ${entry.name}")
  }
  
  private fun fillPath(entries: List<Entry>, basePath: Path, matcher: (Entry) -> Boolean): Path? {
    for (entry in entries) {
      if (matcher.invoke(entry)) {
        return basePath
      }
      if (entry.isGroup()) {
        val newPath = entry.target.path?.findPath()?.let { basePath.resolve(it) } ?: basePath
        val path = fillPath((entry as Group).entries, newPath, matcher)
        if (path != null)
          return path
      }
    }
    return null
  }
  
}