package com.sheryv.util;

import lombok.Getter;

import java.time.Instant;

public class Version {
  private final String version;
  private final long buildTimeMs;
  
  public Version(String version, long buildTimeMs) {
    this.version = version;
    this.buildTimeMs = buildTimeMs;
  }
  
  public Instant toTimestamp() {
    return Instant.ofEpochMilli(buildTimeMs);
  }
  
  public String getVersion() {
    return version;
  }
  
  public long getBuildTimeMs() {
    return buildTimeMs;
  }
  
  @Override
  public String toString() {
    return version + " (Built at "+ toTimestamp() + ')';
  }
}
