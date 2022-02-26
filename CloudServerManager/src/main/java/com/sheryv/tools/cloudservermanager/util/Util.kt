package com.sheryv.tools.cloudservermanager.util

import com.sheryv.util.logging.LoggingUtils
import org.slf4j.Logger

object Util {

}

inline fun <reified T> T.lg(clazz: Class<*> = T::class.java): Logger {
  return LoggingUtils.getLogger(clazz)
}
