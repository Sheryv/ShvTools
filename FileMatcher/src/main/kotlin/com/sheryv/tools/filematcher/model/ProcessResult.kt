package com.sheryv.tools.filematcher.model

import com.sheryv.tools.filematcher.service.Process
import java.lang.Exception

class ProcessResult<Output, P : Process<Output>>(val type: ResultType, val process: P, val error: Exception? = null, val data: Output? = null) {
}