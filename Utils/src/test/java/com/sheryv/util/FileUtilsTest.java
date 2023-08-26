package com.sheryv.util;


import com.sheryv.util.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileUtilsTest {
  @Test
  public void sizeString() {
    String s = FileUtils.sizeString(5443880);
    Assertions.assertEquals("5 MB", s);
    s = FileUtils.sizeString(235443880);
    Assertions.assertEquals("225 MB", s);
    s = FileUtils.sizeString(5235443880L);
    Assertions.assertEquals("4,88 GB", s);
  }
}
