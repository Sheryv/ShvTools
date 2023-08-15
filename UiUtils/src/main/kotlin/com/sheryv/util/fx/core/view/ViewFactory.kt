package com.sheryv.util.fx.core.view

import com.sheryv.util.fx.lib.onChange
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import org.koin.core.Koin
import kotlin.reflect.KClass

class ViewFactory(private val koin: Koin) {
  
  inline fun <reified T : BaseView> createWindow(
    width: Double = 1000.0,
    height: Double = 600.0,
    stage: Stage = Stage(),
  ) = createWindow(T::class, width, height, stage)
  
  fun <T : BaseView> createWindow(
    view: KClass<T>,
    width: Double = 1000.0,
    height: Double = 600.0,
    stage: Stage = Stage(),
  ): () -> T {
    val v = koin.get<T>(view)
    
    val root = when (v) {
      is SimpleView -> v.root
      is FxmlView -> {
        val loader = FXMLLoader(javaClass.classLoader.getResource(v.fxmlPath))
        loader.setController(v)
        loader.load()
      }
      
      else -> throw IllegalArgumentException("Not recognized view type for $v")
    }
    
    val scene = Scene(root, width, height)
    stage.scene = scene
    appendStyleSheets(stage.scene)
    stage.titleProperty().bind(v.titleProperty)
    v.iconProperty.onChange {
      stage.icons.clear()
      stage.icons.add(it)
    }
    v.onViewCreated(stage)
    v.onViewReady()
    
    
    return {
      stage.show()
      v.onViewShown()
      v
    }
  }
  
//  fun <T : BaseView> createPrimaryWindow(view: KClass<T>): () -> T {
//    return createWindow(view, stage = context.primaryStage).let {
//      {
//        val v = it()
//        context.primaryView = v
//        v
//      }
//    }
//  }
  
  fun appendStyleSheets(scene: Scene) {
    if (scene.stylesheets.isEmpty()) {
      scene.stylesheets.add(javaClass.classLoader.getResource("style.css")?.toExternalForm())
      scene.stylesheets.add(javaClass.classLoader.getResource("dark.css")?.toExternalForm())
    }
  }
}
