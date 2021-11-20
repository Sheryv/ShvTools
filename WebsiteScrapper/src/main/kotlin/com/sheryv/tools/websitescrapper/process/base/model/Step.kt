package com.sheryv.tools.websitescrapper.process.base.model

open class Step<R>(val name: String, val runBlock: (R?) -> R, val initialValue: R? = null) {
}
