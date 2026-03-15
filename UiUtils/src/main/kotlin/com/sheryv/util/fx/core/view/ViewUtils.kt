package com.sheryv.util.fx.core.view

import com.sheryv.util.EditableValue
import com.sheryv.util.inBackground
import com.sheryv.util.inMainContext
import com.sheryv.util.logging.log
import javafx.scene.control.Alert
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent

fun <T> BaseView.withErrorHandler(block: () -> T): T? {
  try {
    return block()
  } catch (e: Exception) {
    log.error("Error in action", e)
    this.factory.dialogs.textAreaDialog(
      "Details",
      e.stackTraceToString(),
      "Cannot process action",
      type = Alert.AlertType.ERROR,
    )
  }
  return null
}

fun <T> BaseView.runActionInBackground(
  title: String,
  toUpdate: EditableValue<Boolean>? = null,
  action: suspend () -> T,
  onSuccess: suspend (T) -> Unit = {},
  onError: suspend (Exception) -> Boolean = { true }
) {
  inBackground(toUpdate) {
    try {
      val result = action()
      inMainContext {
        onSuccess(result)
      }
    } catch (e: Exception) {
      inMainContext {
        if (onError(e)) {
          log.error("Action failed: $title", e)
          factory.dialogs.exceptionDialog("$title failed", e)
        }
      }
    }
  }
}

object ViewUtils {
  fun saveToClipboard(text: String) {
    val content = ClipboardContent()
    content.putString(text)
    Clipboard.getSystemClipboard().setContent(content)
  }
  
  fun loadFromClipboard(): String? = Clipboard.getSystemClipboard().takeIf { it.hasString() }?.string
  
}
