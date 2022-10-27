package com.sheryv.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotNullObservable<T> {
  
  private final List<ChangeListener<T>> listeners = Collections.synchronizedList(new ArrayList<>());
  
  public NotNullObservable(T initial) {
    this.value = initial;
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
  
  @Nonnull
  public T getValue() {
    if (this.value == null) throw new IllegalStateException("Value is null");
    return this.value;
  }
  
  public void set(@Nonnull T newValue) {
    if (this.value == null) throw new IllegalStateException("Value cannot be null null");
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

