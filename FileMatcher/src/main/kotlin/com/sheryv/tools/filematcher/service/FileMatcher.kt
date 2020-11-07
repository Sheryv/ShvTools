package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.model.event.ItemStateChangedEvent
import com.sheryv.tools.filematcher.utils.BundleUtils
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.lg
import com.sheryv.tools.filematcher.utils.postEvent
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.Paths

class FileMatcher(private val context: UserContext, onFinish: ((ProcessResult<Unit, FileSynchronizer>) -> Unit)? = null)
  : Process<Unit>(onFinish as ((ProcessResult<Unit, out Process<Unit>>) -> Unit)?) {
  
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
        throw ValidationError(ValidationResult("Target path '${entry.target.path}' does not point to " +
            "directory for item [name=${entry.name}, id=${entry.id}]. Problematic file: ${dir.absolutePath}"))
      }
      lg().info("Item state verification '${entry.name}' [id=${entry.id}]")
      val file = dir.resolve(entry.name)
      if (file.exists()) {
        if (entry.hashes != null && entry.hashes.hasAny()) {
          lg().debug("Calculating hash '${entry.name}' [id=${entry.id}], file: ${file.absolutePath}")
          val match = entry.hashes.getCorrespondingHasherAndCompare().invoke(file)
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
      lg().info("Item does not exists: ${entry.name} [id=${entry.id}]")
      ItemState.NOT_EXISTS
    }
    
  }
  
  fun getEntryDir(entry: Entry): Path {
    return if (entry.target.absolute) {
      Paths.get(entry.target.path!!.findPath()!!)
    } else {
      context.buildDirPathForEntry(entry).resolve(entry.target.path?.findPath() ?: "")
    }
  }
  
}