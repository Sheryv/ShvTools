package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.DialogUtils
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import java.nio.file.Paths

class FileMatcher(val context: UserContext) {
  
  fun verifyLocal() {
    if (!context.isFilled()) {
      DialogUtils.dialog("", "Bundle or version is not selected", Alert.AlertType.ERROR, ButtonType.OK)
      return
    }
    
    val p = context.basePath!!
    
    BundleUtils.forEachEntry(context.getEntries()) {
      verifyEntryState(it)
    }
  }
  
  fun verifyEntryState(entry: Entry) {
    entry.state = ItemState.VERIFICATION
    val path = if (entry.target.absolute) {
      Paths.get(entry.target.path!!.findPath()!!)
    } else {
      context.buildDirPathForEntry(entry).resolve(entry.target.path?.findPath() ?: "")
    }
//    val dir = context.buildDirPathForEntry(entry)
    val dir = path.toFile()
    if (dir.exists()) {
      if (!dir.isDirectory) {
        throw ValidationError(ValidationResult("Target path '${entry.target.path}' does not point to " +
            "directory for item [name=${entry.name}, id=${entry.id}]. Problematic file: ${dir.absolutePath}"))
      }
      val file = dir.resolve(entry.name)
      if (file.exists()) {
        if (entry.hashes != null && entry.hashes.hasAny()) {
          val match = entry.hashes.getCorrespondingHasherAndCompare().invoke(file)
          if (match) {
            entry.state = ItemState.SYNCED
          } else if (entry.target.override) {
            entry.state = ItemState.MODIFIED
          } else {
            entry.state = ItemState.SKIPPED
          }
        } else if (entry.target.override) {
          entry.state = ItemState.NEEDS_UPDATE
        } else {
          entry.state = ItemState.SKIPPED
        }
      } else {
        entry.state = ItemState.NOT_EXISTS
      }
    } else {
      entry.state = ItemState.NOT_EXISTS
    }
    
  }
  
  
}