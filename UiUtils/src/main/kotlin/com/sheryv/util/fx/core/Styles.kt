package com.sheryv.util.fx.core

import javafx.geometry.Insets
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color

object Styles {
  
  fun background(color: Color) = Background(BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY))
}
