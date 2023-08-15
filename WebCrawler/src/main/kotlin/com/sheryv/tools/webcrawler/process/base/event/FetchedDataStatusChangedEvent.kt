package com.sheryv.tools.webcrawler.process.base.event

import com.sheryv.util.event.AsyncEvent

data class FetchedDataStatusChangedEvent(val statusText: String) : AsyncEvent
