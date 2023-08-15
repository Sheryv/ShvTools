package com.sheryv.tools.webcrawler

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.MenuBar
import javafx.stage.Stage
//import net.yetihafen.javafx.customcaption.CaptionConfiguration
//import net.yetihafen.javafx.customcaption.CustomCaption


class HelloApplication : Application() {
  override fun start(stage: Stage) {
    
    // initialize FXMLLoader
    val loader = FXMLLoader(javaClass.classLoader.getResource("complex-application.fxml"))
    // load the contents of the fxml file
    val root = loader.load<Parent>()
    
    // create scene with loaded contents
    val scene = Scene(root)
    
    // start stage with specified scene
    stage.setScene(scene)
    stage.setTitle("customcaption-demo")
    stage.show()
    
    
    // get MenuBar to supply as DragRegion
    val bar: MenuBar? = root.lookup("#menu") as MenuBar
    
    // apply customizations
//    CustomCaption.useForStage(
//      stage, CaptionConfiguration()
////        .setIconColor(Color.BLACK) // set the icon/foreground color to black
//        .setCaptionDragRegion(bar) // set the MenuBar as DragRegion to exclude the
//        .setCaptionHeight(20)
//      // buttons automatically
//    )
  }
}
