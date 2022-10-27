package com.sheryv.util;

public interface ChangeListener<T> extends java.util.EventListener {
  void onChange(T oldValue, T newValue);
}
