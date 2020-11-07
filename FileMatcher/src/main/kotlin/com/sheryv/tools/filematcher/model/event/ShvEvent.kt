package com.sheryv.tools.filematcher.model.event

import com.sheryv.tools.filematcher.model.Entry

interface ShvEvent

class AbortEvent : ShvEvent

class ItemStateChangedEvent(e: Entry) : ShvEvent