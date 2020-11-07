package com.sheryv.tools.filematcher.view

import com.sheryv.tools.filematcher.utils.eventsAttach
import com.sheryv.tools.filematcher.utils.eventsDetach
import com.sheryv.tools.lasso.util.OnChangeScheduledExecutor
import javafx.fxml.FXML
import javafx.stage.Stage
import org.greenrobot.eventbus.Subscribe

abstract class BaseView {
  lateinit var stage: Stage
  
  
  fun initStage(stage: Stage) {
    this.stage = stage
    this.stage.setOnCloseRequest {
      eventsDetach(this)
      onCloseStage()
    }
    eventsAttach(this)
    this.onCreateStage(stage)
  }
  
  open fun onCreateStage(stage: Stage) {
  }
  
  @FXML
  open fun initialize() {
  
  }
  
  open fun onCloseStage() {
  }
  
  //hack that makes eventbus to not throw exception
  @Subscribe
  fun emptyEventy(e: BaseView) {
  }
}