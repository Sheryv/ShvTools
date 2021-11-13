package com.sheryv.tools.filematcher.model.event

import com.sheryv.tools.filematcher.model.Entry

interface ShvEvent

class AbortEvent : ShvEvent

class ItemStateChangedEvent(val e: Entry) : ShvEvent

class ItemEnableChangedEvent(val e: Entry) : ShvEvent
