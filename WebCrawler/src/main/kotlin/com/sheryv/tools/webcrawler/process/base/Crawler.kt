package com.sheryv.tools.webcrawler.process.base

import com.sheryv.tools.webcrawler.GlobalState
import com.sheryv.tools.webcrawler.ProcessingStates
import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.SettingsBase
import com.sheryv.tools.webcrawler.process.base.model.ProcessParams
import com.sheryv.tools.webcrawler.process.base.model.SDriver
import com.sheryv.tools.webcrawler.process.base.model.Step
import com.sheryv.tools.webcrawler.process.base.model.TerminationException
import com.sheryv.tools.webcrawler.utils.lg
import kotlinx.coroutines.delay

abstract class Crawler<T : SDriver, S : SettingsBase>(
  protected val configuration: Configuration,
  val browser: BrowserConfig,
  val def: CrawlerDefinition<T, S>,
  val driver: T,
  val params: ProcessParams
) {
  abstract fun getSteps(): List<Step<out Any, out Any>>
  
  fun logText(text: String, vararg params: Any) {
    lg(javaClass).debug("[${def.id()}|${browser.type.name}] " + text, *params)
  }
  
  override fun toString(): String = "${def.attributes.name} [${def.id()}]"
  
  val settings: S = def.findSettings(configuration)
  
  internal suspend fun waitIfPaused() {
    val state = GlobalState.processingState
    if (state.value == ProcessingStates.STOPPING) {
      throw TerminationException()
    }
    if (state.value == ProcessingStates.PAUSING) {
      state.value = ProcessingStates.PAUSED
    }
    
    while (state.value == ProcessingStates.PAUSED) {
      delay(250)
    }
    if (state.value == ProcessingStates.STOPPING) {
      throw TerminationException()
    }
    if (GlobalState.pauseOnNextStep) {
      state.value = ProcessingStates.PAUSING
    }
  }
}
