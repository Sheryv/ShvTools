package com.sheryv.tools.websitescraper

import com.sheryv.tools.websitescraper.process.base.ScraperDef
import com.sheryv.tools.websitescraper.view.ViewActionsProvider
import com.sheryv.util.NotNullObservable

object GlobalState {
  lateinit var currentScrapper: ScraperDef
  lateinit var view: ViewActionsProvider
  var processingState: NotNullObservable<ProcessingStates> = NotNullObservable(ProcessingStates.IDLE)
}

enum class ProcessingStates {
  IDLE, RUNNING, PAUSED
}
