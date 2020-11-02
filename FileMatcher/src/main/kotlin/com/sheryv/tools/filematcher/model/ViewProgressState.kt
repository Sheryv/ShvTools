package com.sheryv.tools.filematcher.model

import javafx.beans.property.*
import javafx.scene.control.ProgressIndicator

class ViewProgressState(
    val messageProp: StringProperty = SimpleStringProperty("Ready"),
    val progress: DoubleProperty = SimpleDoubleProperty(0.0),
    val inProgress: BooleanProperty = SimpleBooleanProperty(false)
) {
  
  fun onChange(listener: (inProgress: Boolean, progress: Double) -> Unit) {
    inProgress.addListener { _, _, newValue -> listener.invoke(newValue, progress.get()) }
    progress.addListener { _, _, newValue -> listener.invoke(inProgress.get(), newValue.toDouble()) }
  }
  
  fun setMessage(msg: String? = null) {
    inProgress.set(msg != null)
    messageProp.set(msg ?: "Ready")
  }
  
  fun stop() {
    setMessage(null)
    progress.set(0.0)
  }
  
  fun progessIndeterminate() {
    progress.set(ProgressIndicator.INDETERMINATE_PROGRESS)
  }
}