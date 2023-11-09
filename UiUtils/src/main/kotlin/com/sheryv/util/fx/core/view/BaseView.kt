package com.sheryv.util.fx.core.view

import com.sheryv.util.fx.core.app.AppConfiguration
import com.sheryv.util.fx.lib.getValue
import com.sheryv.util.fx.lib.setValue
import com.sheryv.util.unsubscribeAllEvents
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.Parent
import javafx.scene.control.MenuBar
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.koin.core.component.KoinComponent
import org.koin.core.component.get


abstract class SimpleView : BaseView() {
  abstract val root: Parent
  
  fun createRoot(prefWidth: Double = 1200.0, prefHeight: Double = 700.0, b: VBox.() -> Unit) = VBox().apply {
    setPrefSize(prefWidth, prefHeight)
    
    spacing = 10.0
    isFillWidth = true
    menuBar()?.also { this.children.add(it) }
    b()
  }
  
  fun menuBar(): MenuBar? = null
  
}

abstract class FxmlView(val fxmlPath: String) : BaseView() {

}

abstract class BaseView : KoinComponent {
  protected open val config: AppConfiguration by lazy { get() }
  protected lateinit var stage: Stage
  
  open fun onViewCreated(stage: Stage) {
    this.stage = stage
    if (config.iconPath.isNotBlank())
      icon = Image(javaClass.classLoader.getResourceAsStream(config.iconPath))
    
    title = config.name
    
    stage.setOnCloseRequest { onViewDestroy() }
  }
  
  open fun onViewReady() {
  }
  
  open fun onViewShown() {
  
  }
  
  open fun onViewDestroy() {
    unsubscribeAllEvents()
  }
  
  
  open val titleProperty: StringProperty = SimpleStringProperty()
  var title: String
    get() = titleProperty.get() ?: ""
    set(value) {
      if (titleProperty.isBound)
        titleProperty.unbind()
      titleProperty.set(value)
    }
  
  val iconProperty: ObjectProperty<Image> = SimpleObjectProperty(null)
  var icon by iconProperty
  
}
