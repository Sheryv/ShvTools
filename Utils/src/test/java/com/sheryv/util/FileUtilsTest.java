package com.sheryv.util;

import org.junit.Assert;
import org.junit.Test;

public class FileUtilsTest {
    @Test
    public void sizeString() {
        String s = FileUtils.sizeString(5443880);
        Assert.assertEquals("5 MB", s);
        s = FileUtils.sizeString(235443880);
        Assert.assertEquals("225 MB", s);
        s = FileUtils.sizeString(5235443880L);
        Assert.assertEquals("4,88 GB", s);
    }
}
