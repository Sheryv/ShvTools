@file:JvmName("MainLauncher")

package com.sheryv.tools.filematcher

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.utils.ViewUtils
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

fun main(args: Array<String>) {
  Application.launch(Main::class.java, *args)
}

class Main : Application() {
  override fun start(stage: Stage) {
    Configuration.get()
    val root: Parent = FXMLLoader.load(javaClass.classLoader.getResource("main.fxml"))
    stage.title = "ShvFileMatcher"
    val scene = Scene(root, 900.0, 600.0)
    stage.scene = scene
    ViewUtils.appendStyleSheets(stage.scene)
    stage.show()
  }
  
  
}