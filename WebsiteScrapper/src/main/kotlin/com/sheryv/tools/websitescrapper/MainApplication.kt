package com.sheryv.tools.websitescrapper

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class MainApplication : Application() {
  override fun start(primaryStage: Stage?) {
    createWindow<MainView>("scrapper-main.fxml", "ShvWebsiteScrapper")
  }
  
  companion object {
    @JvmStatic
    fun <T : BaseView> createWindow(fxml: String, title: String, stage: Stage = Stage()): T {
      val loader = FXMLLoader(javaClass.classLoader.getResource(fxml))
//      stage.icons.add(Image(javaClass.classLoader.getResourceAsStream("icons/app.png")))
      val root: Parent = loader.load()
      val controller = loader.getController<T>()
      controller.stage = stage
      stage.title = title
      val scene = Scene(root, 650.0, 500.0)
      stage.scene = scene
      appendStyleSheets(stage.scene)
      stage.show()
      return controller
    }
    
    @JvmStatic
    fun appendStyleSheets(scene: Scene) {
      if (scene.stylesheets.isEmpty()) {
        scene.stylesheets.add(javaClass.classLoader.getResource("style.css")?.toExternalForm())
        scene.stylesheets.add(javaClass.classLoader.getResource("dark.css")?.toExternalForm())
      }
    }
    
    @JvmStatic
    fun start(args: Array<String>) {
      launch(MainApplication::class.java, *args)
    }
  }
}

abstract class BaseView {
  lateinit var stage: Stage
  abstract fun initialize()
}
