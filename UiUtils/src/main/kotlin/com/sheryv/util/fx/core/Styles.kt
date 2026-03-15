package com.sheryv.util.fx.core

import com.sheryv.util.fx.core.app.AppConfiguration
import com.sheryv.util.fx.core.app.AppConfiguration.Theme
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color
import javafx.stage.Stage

object Styles {
  
  const val CLASS_MONO = "mono"
  
  fun background(color: Color) = Background(BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY))
  
  
  @JvmStatic
  fun appendStyleSheets(scene: Scene, config: AppConfiguration) {
    (scene.window as? Stage)?.also {
      val icon = javaClass.classLoader.getResourceAsStream(config.iconPath)
      if (icon != null) {
        it.icons.add(Image(icon))
      }
    }
    if (scene.stylesheets.isEmpty()) {
      for (path in config.stylesPaths.common) {
        scene.stylesheets.add(javaClass.classLoader.getResource(path)?.toExternalForm())
      }
      if (config.theme == Theme.DARK) {
        for (path in config.stylesPaths.dark) {
          scene.stylesheets.add(javaClass.classLoader.getResource(path)?.toExternalForm())
        }
      } else {
        for (path in config.stylesPaths.light) {
          scene.stylesheets.add(javaClass.classLoader.getResource(path)?.toExternalForm())
        }
      }
    }
  }
}
