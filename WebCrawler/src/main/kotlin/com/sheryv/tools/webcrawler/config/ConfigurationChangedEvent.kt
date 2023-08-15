package com.sheryv.tools.webcrawler.config

import com.sheryv.util.event.AsyncEvent

class ConfigurationChangedEvent(val configuration: Configuration) : AsyncEvent {
}
