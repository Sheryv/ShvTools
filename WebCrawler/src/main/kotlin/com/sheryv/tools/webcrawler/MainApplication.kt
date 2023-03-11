package com.sheryv.tools.webcrawler

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.util.SwingUtils
import com.sheryv.tools.webcrawler.utils.ViewUtils
import com.sheryv.tools.webcrawler.view.MainView
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import javax.swing.SwingUtilities

class MainApplication : Application() {
  override fun start(primaryStage: Stage?) {
    createWindow<MainView>("view/crawler-main.fxml", ViewUtils.TITLE)
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
//      stage.icons.add(Image(javaClass.classLoader.getResourceAsStream("icons/app.png")))
      val scene = Scene(root, 1000.0, 600.0)
      stage.scene = scene
      appendStyleSheets(stage.scene)
      controller.onViewCreated()
      stage.show()
      return controller
    }
    
    @JvmStatic
    fun appendStyleSheets(scene: Scene) {
      (scene.window as? Stage)?.also {
        it.icons.add(Image(javaClass.classLoader.getResourceAsStream("icons/app.png")))
      }
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
  open fun initialize() {}
  open fun onViewCreated() {
  
  }
}
