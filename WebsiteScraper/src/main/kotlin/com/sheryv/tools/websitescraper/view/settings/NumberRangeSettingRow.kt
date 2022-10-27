package com.sheryv.tools.websitescraper.view.settings

import com.sheryv.tools.websitescraper.utils.ViewUtils
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region

class NumberRangeSettingRow(
  name: String,
  val value: Int,
  val min: Int,
  val max: Int,
  val step: Int = 1,
  val showSlider: Boolean = (max - min) / step <= 100
) : SettingsViewRow<Int>(name) {
  private lateinit var spinner: Spinner<Long>
  
  override fun buildPart(): Region {
    require(max >= min)
    val slider = Slider(min.toDouble(), max.toDouble(), value.toDouble()).apply {
      maxWidth = Double.MAX_VALUE
      isSnapToTicks = (max - min) / step <= 10
      isShowTickLabels = true
      isShowTickMarks = true
      HBox.setHgrow(this, Priority.ALWAYS)
    }
    slider.blockIncrement = ((max - min) / 5.0) / 4
    slider.majorTickUnit = (max - min) / 5.0
    slider.minorTickCount = 3
    
    spinner = Spinner<Long>(min, max, value)
    spinner.isEditable = true
    spinner.editor.textProperty().addListener { _, oldValue, newValue ->
      val spinnerValue = newValue.toIntOrNull()
      if (spinnerValue == null) {
        if (newValue.isBlank()) {
          spinner.editor.text = ""
        }
        
        spinner.editor.text = oldValue
      } else if (showSlider) {
        slider.value = spinnerValue.toDouble()
      }
    }
    
    if (showSlider) {
      slider.valueProperty().addListener { _, _, newValue ->
        spinner.editor.text = newValue.toInt().toString()
        spinner.commitValue()
      }
    } else {
      spinner.maxWidth = Double.MAX_VALUE
      HBox.setHgrow(spinner, Priority.ALWAYS)
    }
    
    
    val menu = ContextMenu(
      MenuItem("Copy").apply { setOnAction { ViewUtils.saveToClipboard(spinner.value.toString()) } },
      MenuItem("Paste").apply {
        setOnAction {
          ViewUtils.loadFromClipboard()?.toIntOrNull()?.also {
            spinner.editor.text = it.toString()
            spinner.commitValue()
          }
        }
      },
      MenuItem("Reset").apply { setOnAction { spinner.editor.text = value.toString(); spinner.commitValue() } },
    )
    val btn = Button()
    btn.graphic = Region()
    btn.styleClass.add("ic-more-vert")
    btn.tooltip = Tooltip("Click to show Menu")
    btn.setOnMouseClicked { menu.show((it.source as Control).parent, it.screenX, it.screenY) }
    
    val hBox = if (showSlider) HBox(spinner, slider, btn) else HBox(spinner, btn)
    hBox.spacing = 10.0
    return hBox
  }
  
  override fun readValue(): Int {
    return spinner.value.toInt()
  }
}
