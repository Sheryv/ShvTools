package com.sheryv.tools.webcrawler.process.base.event

import com.sheryv.util.InternalEvent

data class FetchedDataStatusChangedEvent(val statusText: String) : InternalEvent
