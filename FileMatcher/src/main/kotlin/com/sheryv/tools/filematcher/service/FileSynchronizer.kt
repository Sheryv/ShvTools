package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.*
import com.sheryv.tools.filematcher.model.event.AbortEvent
import com.sheryv.tools.filematcher.utils.DataUtils
import com.sheryv.tools.filematcher.utils.DialogUtils
import com.sheryv.tools.filematcher.utils.lg
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.net.URLEncoder
import java.nio.charset.Charset

class FileSynchronizer(
  private val context: UserContext,
  private val state: ViewProgressState,
  private val deleteOldFiles: Boolean,
  onFinish: ((ProcessResult<Unit, FileSynchronizer>) -> Unit)? = null
) : Process<Unit>(onFinish as ((ProcessResult<Unit, out Process<Unit>>) -> Unit)?, true) {
  
  override fun preValidation(): Boolean {
    if (!context.isFilled()) {
      DialogUtils.dialog("", "Bundle or version is not selected", Alert.AlertType.ERROR, ButtonType.OK)
      return false
    }
    return true
  }
  
  override suspend fun process() {
    val matcher = FileMatcher(context)
    val entries = context.getEntries().filter { !it.group }
    val all = entries.size
    var processed = 0
    for (entry in entries) {
      
      matcher.updateEntryState(entry)
      
      if (entry.selected && !entry.group && entry.type == ItemType.ITEM) {
        val fileName =
          if (entry.state == ItemState.MODIFIED || entry.state == ItemState.NEEDS_UPDATE || entry.state == ItemState.NOT_EXISTS) {
            
            withContext(Dispatchers.Main) {
              state.progress.set(processed.toDouble() / all)
            }
            
            entry.state = ItemState.DOWNLOADING
            val downloadFile = downloadEntry(entry, matcher)
            
            processed++
            matcher.updateEntryState(entry)
            downloadFile
          } else {
            matcher.getEntryDir(entry).resolve(entry.name).toFile()
          }
        if (deleteOldFiles) {
          entry.target.matching.lastMatches.filter { it != fileName }.forEach { it.delete() }
        }
        entry.target.matching.lastMatches = emptyList()
      }
    }
  }
  
  private suspend fun downloadEntry(entry: Entry, matcher: FileMatcher): File? {
    val dir = matcher.getEntryDir(entry)
    dir.toFile().mkdirs()
    val file = dir.resolve(entry.name).toFile()
    val url = entry.getSrcUrl(context)
    return try {
      DataUtils.downloadFile(url, file) { !isActive() }
    } catch (e: Exception) {
      matcher.updateEntryState(entry)
      throw IllegalStateException("Unable to download '${entry.name}' from '$url'", e)
    }
  }
  
  @Subscribe
  fun eventAbort(e: AbortEvent) {
    stop()
  }
}
