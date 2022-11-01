package com.sheryv.tools.webcrawler

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.ScraperDef
import com.sheryv.tools.webcrawler.process.base.SeleniumScraper
import com.sheryv.tools.webcrawler.view.ViewActionsProvider
import com.sheryv.util.NotNullObservable
import com.sheryv.util.Observable

object GlobalState {
  lateinit var currentScrapper: ScraperDef
  lateinit var view: ViewActionsProvider
  var processingState: NotNullObservable<ProcessingStates> = NotNullObservable(ProcessingStates.IDLE)
  var runningProcess: Observable<SeleniumScraper<SettingsBase>> = Observable()
  var pauseOnNextStep: Boolean = false
  
  fun settingsForCurrentScraper(): SettingsBase {
    return currentScrapper.findSettings(Configuration.get())
  }
}

enum class ProcessingStates {
  IDLE, RUNNING, PAUSED, PAUSING, STOPPING
}
