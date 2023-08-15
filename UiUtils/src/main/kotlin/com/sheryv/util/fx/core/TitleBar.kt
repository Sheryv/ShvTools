package com.sheryv.util.fx.core

import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.scene.control.Button
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Screen
import javafx.stage.Stage
import kotlin.math.max


class WindowButtons(stage: Stage) : VBox(4.0) {
  private val stage: Stage
  private var backupWindowBounds: Rectangle2D? = null
  var isMaximized = false
    private set
  
  init {
    this.stage = stage
    // create buttons
    val buttonBox = VBox(4.0)
    val closeBtn = Button()
    closeBtn.id = "window-close"
    closeBtn.setOnAction {
      Platform.exit()
    }
    val minBtn = Button()
    minBtn.id = "window-min"
    minBtn.setOnAction {
      stage.setIconified(true)
    }
    val maxBtn = Button()
    maxBtn.id = "window-max"
    maxBtn.setOnAction {
      toogleMaximized()
    }
    children.addAll(closeBtn, minBtn, maxBtn)
  }
  
  fun toogleMaximized() {
    val screen: Screen = Screen.getScreensForRectangle(stage.x, stage.y, 1.0, 1.0).get(0)
    if (isMaximized) {
      isMaximized = false
      if (backupWindowBounds != null) {
        stage.x = backupWindowBounds!!.minX
        stage.y = backupWindowBounds!!.minY
        stage.setWidth(backupWindowBounds!!.width)
        stage.setHeight(backupWindowBounds!!.height)
      }
    } else {
      isMaximized = true
      backupWindowBounds = Rectangle2D(stage.x, stage.y, stage.width, stage.height)
      stage.x = screen.visualBounds.minX
      stage.y = screen.visualBounds.minY
      stage.setWidth(screen.visualBounds.width)
      stage.setHeight(screen.visualBounds.height)
    }
  }
}


class WindowResizeButton(stage: Stage, stageMinimumWidth: Double, stageMinimumHeight: Double) : Region() {
  private var dragOffsetX = 0.0
  private var dragOffsetY = 0.0
  
  init {
    id = "window-resize-button"
    setPrefSize(11.0, 11.0)
    setOnMousePressed { e ->
      dragOffsetX = stage.x + stage.width - e.screenX
      dragOffsetY = stage.y + stage.height - e.screenY
      e.consume()
    }
    setOnMouseDragged { e ->
      val screens = Screen.getScreensForRectangle(stage.x, stage.y, 1.0, 1.0)
      val screen: Screen
      screen = if (screens.size > 0) {
        Screen.getScreensForRectangle(stage.x, stage.y, 1.0, 1.0)[0]
      } else {
        Screen.getScreensForRectangle(0.0, 0.0, 1.0, 1.0)[0]
      }
      val visualBounds = screen.visualBounds
      val maxX = Math.min(visualBounds.maxX, e.screenX + dragOffsetX)
      val maxY = Math.min(visualBounds.maxY, e.screenY - dragOffsetY)
      stage.setWidth(max(stageMinimumWidth, maxX - stage.x))
      stage.setHeight(max(stageMinimumHeight, maxY - stage.y))
      e.consume()
    }
  }
}

