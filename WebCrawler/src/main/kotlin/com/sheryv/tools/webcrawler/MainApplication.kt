package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.view.MainView
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

class MainApplication {
  
//  override fun createModules(): List<Module> = listOf(
//    module {
//      factoryOf(::MainView)
//    }
//  )
  
//  override fun start(primaryStage: Stage) {
//
//    println("App init at ${ManagementFactory.getRuntimeMXBean().uptime}")
//    createWindow<MainView>("view/crawler-main.fxml", ViewUtils.TITLE, primaryStage)
//    println("App visible at ${ManagementFactory.getRuntimeMXBean().uptime}")
//    log.info("App started at {}", ManagementFactory.getRuntimeMXBean().uptime)
//  }
  
  companion object {
//    @JvmStatic
//    fun <T : BaseView> createWindow(fxml: String, title: String, stage: Stage = Stage()): T {
//      val loader = FXMLLoader(javaClass.classLoader.getResource(fxml))
////      stage.icons.add(Image(javaClass.classLoader.getResourceAsStream("icons/app.png")))
//      val root: Parent = loader.load()
//      val controller = loader.getController<T>()
//      controller.stage = stage
//      stage.title = title
////      stage.icons.add(Image(javaClass.classLoader.getResourceAsStream("icons/app.png")))
//      val scene = Scene(root, 1000.0, 600.0)
//      stage.scene = scene
//      appendStyleSheets(stage.scene)
//      controller.onViewCreated()
//      stage.show()
//      return controller
//    }
//
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
  }
}

