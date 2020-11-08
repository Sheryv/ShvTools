package com.sheryv.util.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;

public abstract class LoggerMessageDecorator implements Logger {
  public abstract Logger getLogger();
  
  protected abstract String transformMessage(String message);
  
  protected String transformFormat(String format) {
    return transformMessage(format);
  }
  
  
  @Override
  public String getName() {
    return getLogger().getName();
  }
  
  @Override
  public boolean isTraceEnabled() {
    return getLogger().isTraceEnabled();
  }
  
  @Override
  public void trace(String msg) {
    getLogger().trace(transformMessage(msg));
  }
  
  @Override
  public void trace(String format, Object arg) {
    getLogger().trace(transformFormat(format), arg);
  }
  
  @Override
  public void trace(String format, Object arg1, Object arg2) {
    getLogger().trace(transformFormat(format), arg1, arg2);
  }
  
  @Override
  public void trace(String format, Object... arguments) {
    getLogger().trace(transformFormat(format), arguments);
  }
  
  @Override
  public void trace(String msg, Throwable t) {
    getLogger().trace(transformMessage(msg), t);
  }
  
  @Override
  public boolean isTraceEnabled(Marker marker) {
    return getLogger().isTraceEnabled(marker);
  }
  
  @Override
  public void trace(Marker marker, String msg) {
    getLogger().trace(marker, transformMessage(msg));
  }
  
  @Override
  public void trace(Marker marker, String format, Object arg) {
    getLogger().trace(marker, transformFormat(format), arg);
  }
  
  @Override
  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().trace(marker, transformFormat(format), arg1, arg2);
  }
  
  @Override
  public void trace(Marker marker, String format, Object... argArray) {
    getLogger().trace(marker, transformFormat(format), argArray);
  }
  
  @Override
  public void trace(Marker marker, String msg, Throwable t) {
    getLogger().trace(marker, transformMessage(msg), t);
  }
  
  @Override
  public boolean isDebugEnabled() {
    return getLogger().isDebugEnabled();
  }
  
  @Override
  public void debug(String msg) {
    getLogger().debug(transformMessage(msg));
  }
  
  @Override
  public void debug(String format, Object arg) {
    getLogger().debug(transformFormat(format), arg);
  }
  
  @Override
  public void debug(String format, Object arg1, Object arg2) {
    getLogger().debug(transformFormat(format), arg1, arg2);
  }
  
  @Override
  public void debug(String format, Object... arguments) {
    getLogger().debug(transformFormat(format), arguments);
  }
  
  @Override
  public void debug(String msg, Throwable t) {
    getLogger().debug(transformMessage(msg), t);
  }
  
  @Override
  public boolean isDebugEnabled(Marker marker) {
    return getLogger().isDebugEnabled(marker);
  }
  
  @Override
  public void debug(Marker marker, String msg) {
    getLogger().debug(marker, transformMessage(msg));
  }
  
  @Override
  public void debug(Marker marker, String format, Object arg) {
    getLogger().debug(marker, transformFormat(format), arg);
  }
  
  @Override
  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().debug(marker, transformFormat(format), arg1, arg2);
  }
  
  @Override
  public void debug(Marker marker, String format, Object... arguments) {
    getLogger().debug(marker, transformFormat(format), arguments);
  }
  
  @Override
  public void debug(Marker marker, String msg, Throwable t) {
    getLogger().debug(marker, transformMessage(msg), t);
  }
  
  @Override
  public boolean isInfoEnabled() {
    return getLogger().isInfoEnabled();
  }
  
  @Override
  public void info(String msg) {
    getLogger().info(transformMessage(msg));
  }
  
  @Override
  public void info(String format, Object arg) {
    getLogger().info(transformFormat(format), arg);
  }
  
  @Override
  public void info(String format, Object arg1, Object arg2) {
    getLogger().info(transformFormat(format), arg1, arg2);
  }
  
  @Override
  public void info(String format, Object... arguments) {
    getLogger().info(transformFormat(format), arguments);
  }
  
  @Override
  public void info(String msg, Throwable t) {
    getLogger().info(transformMessage(msg), t);
  }
  
  @Override
  public boolean isInfoEnabled(Marker marker) {
    return getLogger().isTraceEnabled(marker);
  }
  
  @Override
  public void info(Marker marker, String msg) {
    getLogger().info(marker, transformMessage(msg));
  }
  
  @Override
  public void info(Marker marker, String format, Object arg) {
    getLogger().info(marker, transformFormat(format), arg);
  }
  
  @Override
  public void info(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().info(marker, transformFormat(format), arg1, arg2);
    
  }
  
  @Override
  public void info(Marker marker, String format, Object... arguments) {
    getLogger().info(marker, transformFormat(format), arguments);
  }
  
  @Override
  public void info(Marker marker, String msg, Throwable t) {
    getLogger().info(marker, transformMessage(msg), t);
  }
  
  @Override
  public boolean isWarnEnabled() {
    return getLogger().isWarnEnabled();
  }
  
  @Override
  public void warn(String msg) {
    getLogger().warn(transformMessage(msg));
  }
  
  @Override
  public void warn(String format, Object arg) {
    getLogger().warn(transformFormat(format), arg);
    
  }
  
  @Override
  public void warn(String format, Object... arguments) {
    getLogger().warn(transformFormat(format), arguments);
    
  }
  
  @Override
  public void warn(String format, Object arg1, Object arg2) {
    getLogger().warn(transformFormat(format), arg1, arg2);
    
  }
  
  @Override
  public void warn(String msg, Throwable t) {
    getLogger().warn(transformMessage(msg), t);
  }
  
  @Override
  public boolean isWarnEnabled(Marker marker) {
    return getLogger().isWarnEnabled(marker);
  }
  
  @Override
  public void warn(Marker marker, String msg) {
    getLogger().warn(marker, transformMessage(msg));
    
  }
  
  @Override
  public void warn(Marker marker, String format, Object arg) {
    getLogger().warn(marker, transformFormat(format), arg);
  }
  
  @Override
  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().warn(marker, transformFormat(format), arg1, arg2);
  }
  
  @Override
  public void warn(Marker marker, String format, Object... arguments) {
    getLogger().warn(marker, transformFormat(format), arguments);
  }
  
  @Override
  public void warn(Marker marker, String msg, Throwable t) {
    getLogger().warn(marker, transformMessage(msg), t);
  }
  
  @Override
  public boolean isErrorEnabled() {
    return getLogger().isErrorEnabled();
  }
  
  @Override
  public void error(String msg) {
    getLogger().error(transformMessage(msg));
  }
  
  @Override
  public void error(String format, Object arg) {
    getLogger().error(transformFormat(format), arg);
  }
  
  @Override
  public void error(String format, Object arg1, Object arg2) {
    getLogger().error(transformFormat(format), arg1, arg2);
  }
  
  @Override
  public void error(String format, Object... arguments) {
    getLogger().error(transformFormat(format), arguments);
  }
  
  @Override
  public void error(String msg, Throwable t) {
    getLogger().error(transformMessage(msg), t);
  }
  
  @Override
  public boolean isErrorEnabled(Marker marker) {
    return getLogger().isErrorEnabled(marker);
  }
  
  @Override
  public void error(Marker marker, String msg) {
    getLogger().error(marker, transformMessage(msg), msg);
  }
  
  @Override
  public void error(Marker marker, String format, Object arg) {
    getLogger().error(marker, transformFormat(format), arg);
  }
  
  @Override
  public void error(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().error(marker, transformFormat(format), arg1, arg2);
  }
  
  @Override
  public void error(Marker marker, String format, Object... arguments) {
    getLogger().error(marker, transformFormat(format), arguments);
    
  }
  
  @Override
  public void error(Marker marker, String msg, Throwable t) {
    getLogger().error(marker, transformMessage(msg), t);
  }
}
