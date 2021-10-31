package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.model.event.ItemStateChangedEvent
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.lg
import com.sheryv.tools.filematcher.utils.postEvent
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class FileMatcher(
  private val context: UserContext,
  onFinish: ((ProcessResult<Unit, FileSynchronizer>) -> Unit)? = null
) : Process<Unit>(onFinish as ((ProcessResult<Unit, out Process<Unit>>) -> Unit)?) {
  
  override fun preValidation(): Boolean {
    if (!context.isFilled()) {
      DialogUtils.dialog("", "Bundle or version is not selected", Alert.AlertType.ERROR, ButtonType.OK)
      return false
    }
    return true
  }
  
  override suspend fun process() {
    context.getEntries().filter { !it.group }.forEach {
      updateEntryState(it)
    }
  }
  
  suspend fun updateEntryState(it: Entry) {
    withContext(Dispatchers.Main) {
      it.state = ItemState.VERIFICATION
    }
    val state = verifyEntryState(it)
    withContext(Dispatchers.Main) {
      it.state = state
      postEvent(ItemStateChangedEvent(it))
    }
  }
  
  fun verifyEntryState(entry: Entry): ItemState {
    val dir = getEntryDir(entry).toFile()
    return if (dir.exists()) {
      if (!dir.isDirectory) {
        throw ValidationError(
          ValidationResult(
            "Target path '${entry.target.directory}' does not point to " +
                "directory for item [name=${entry.name}, id=${entry.id}]. Problematic file: ${dir.absolutePath}"
          )
        )
      }
      lg().info("Item state verification '${entry.name}' [id=${entry.id}]")
      val matchingFiles = findMatchingFiles(entry, dir)
      val file = matchingFiles.firstOrNull { it.name == entry.name }
      if (file != null && file.exists()) {
        if (entry.hashes != null && entry.hashes!!.hasAny()) {
          lg().debug("Calculating hash '${entry.name}' [id=${entry.id}], file: ${file.absolutePath}")
          val match = entry.hashes!!.getCorrespondingHasherAndCompare().invoke(file)
          if (match) {
            ItemState.SYNCED
          } else if (entry.target.override) {
            ItemState.MODIFIED
          } else {
            ItemState.SKIPPED
          }
        } else if (entry.target.override) {
          ItemState.NEEDS_UPDATE
        } else {
          ItemState.SKIPPED
        }
      } else {
        ItemState.NOT_EXISTS
      }
    } else {
      lg().info("Directory does not exists: '${dir.absolutePath}' for '${entry.name}' [id=${entry.id}]")
      ItemState.NOT_EXISTS
    }
    
  }
  
  private fun findMatchingFiles(entry: Entry, dir: File): List<File> {
    val matching = entry.target.matching
    if (!matching.isConfigured()) {
      val file = dir.resolve(entry.name)
      if (file.exists()) {
        matching.lastMatches = listOf(file)
      }
      return matching.lastMatches
    } else {
      matching.lastMatches = dir.listFiles()!!.filter { !it.isDirectory }.filter {
        matching.matches(it.name)
      }
      return matching.lastMatches
    }
  }
  
  fun getEntryDir(entry: Entry): Path {
    return if (entry.target.absolute) {
      Paths.get(entry.target.directory!!.findPath()!!)
    } else {
      context.buildDirPathForEntry(entry).resolve(entry.target.directory?.findPath() ?: "")
    }
  }
  
}
