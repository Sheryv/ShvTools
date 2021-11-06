package com.sheryv.tools.filematcher.model

import com.sheryv.tools.filematcher.utils.BundleUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class UserContext(
  var repo: Repository? = null,
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
    return getVersion().getAggregatedEntries()
  }
  
  fun buildDirPathForEntry(entry: Entry): Path {
    return getVersion().buildDirPathForEntry(entry, basePath)
  }
  
}
