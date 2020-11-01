package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.Repository
import com.sheryv.tools.filematcher.model.UserContext
import com.sheryv.tools.filematcher.utils.DialogUtils
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import java.io.File

class FileSynchronizer(val context: UserContext) {
  
  fun synchronize() {
    if (!context.isFilled()) {
      DialogUtils.dialog("", "Bundle or version is not selected", Alert.AlertType.ERROR, ButtonType.OK)
      return
    }
    
  }
}