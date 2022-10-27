package com.sheryv.tools.websitescraper.view.settings

import com.sheryv.tools.websitescraper.utils.ViewUtils
import javafx.event.ActionEvent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.*

abstract class SettingsViewRow<R>(val name: String, private val addLabel: Boolean = true) {
  
  protected abstract fun buildPart(): Region
  
  fun build(): Pane {
    val p = buildPart()
    p.styleClass.add(javaClass.simpleName)
    val content = HBox(p)
    content.spacing = 10.0
    HBox.setHgrow(p, Priority.ALWAYS)
    return if (addLabel)
      VBox(Label(name), content).apply {
        isFillWidth = true
      }
    else
      content
  }
  
  open fun readValue(): R {
    throw UnsupportedOperationException("This row type doses not provide any value")
  }
  
  protected fun copyButton(block: (ActionEvent) -> String): Region {
    val btn = Button()
    btn.graphic = Region()
    btn.styleClass.add("ic-copy")
    btn.tooltip = Tooltip("Click to Copy")
    btn.setOnAction { ViewUtils.saveToClipboard(block(it)) }
    return btn
  }
}

