@file:JvmName("MainLauncher")

package com.sheryv.tools.filematcher

import com.sheryv.tools.filematcher.config.Configuration
import com.sheryv.tools.filematcher.utils.ViewUtils
import com.sheryv.tools.filematcher.view.BaseView
import com.sheryv.tools.filematcher.view.MainView
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
    ViewUtils.createWindow<MainView>("main.fxml", ViewUtils.title, stage)
  }
}
