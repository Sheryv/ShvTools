package com.sheryv.tools.filematcher.model

import com.sheryv.tools.filematcher.service.Process
import java.lang.Exception

class ProcessResult<P : Process>(val type: ResultType, val process: P, val error: Exception? = null, val data: Any? = null) {
}