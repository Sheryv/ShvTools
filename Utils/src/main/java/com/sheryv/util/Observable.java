package com.sheryv.util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Observable<T> {
  
  private final List<ChangeListener<T>> listeners = Collections.synchronizedList(new ArrayList<>());
  
  public Observable(T initial) {
    this.value = initial;
  }
  
  public Observable() {
  
  }
  
  public void addListener(ChangeListener<T> listener) {
    this.listeners.add(listener);
  }
  
  public synchronized void removeListener(ChangeListener<T> listener) {
    try {
      this.listeners.remove(listener);
    } catch (Exception e) {
      return;
    }
  }
  
  private T value;
  
  @Nullable
  public T getValue() {
    return this.value;
  }
  
  public void set(@Nullable T newValue) {
    T oldValue = this.value;
    this.value = newValue;
    firePropertyChange(oldValue, newValue);
  }
  
  public void firePropertyChange(T oldValue, T newValue) {
    List<ChangeListener<T>> copy = null;
    boolean run = false;
    
    synchronized (this) {
      if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
        copy = new ArrayList<>(listeners);
        run = true;
      }
    }
    
    if (run) {
      for (ChangeListener<T> changeListener : copy) {
        changeListener.onChange(oldValue, newValue);
      }
    }
  }
}

