package com.sheryv.util.fx.core.view

import com.sheryv.util.fx.core.Dialogs
import com.sheryv.util.fx.core.Styles
import com.sheryv.util.fx.core.app.AppConfiguration
import com.sheryv.util.fx.lib.onChange
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import org.koin.core.Koin
import kotlin.reflect.KClass

class ViewFactory(private val koin: Koin) {
  private val configuration = koin.get<AppConfiguration>()
  val dialogs = koin.get<Dialogs>()
  
  inline fun <reified T : BaseView> createWindow(
    width: Double = 1000.0,
    height: Double = 600.0,
    stage: Stage = Stage(),
  ) = createWindow(T::class, width, height, stage)
  
  fun <T : BaseView> createWindow(
    view: KClass<T>,
    width: Double = 0.0,
    height: Double = 0.0,
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
    
    var w = if (width > 0) width else 1000.0
    var h = if (height > 0) height else 600.0
    if (root is Pane) {
      if (root.prefWidth > 0) {
        w = root.prefWidth
      }
      if (root.prefHeight > 0) {
        h = root.prefHeight
      }
    }
    
    val scene = Scene(root, w, h)
    stage.scene = scene
    Styles.appendStyleSheets(stage.scene, configuration)
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
}
