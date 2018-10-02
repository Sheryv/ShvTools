package com.sheryv.tools.movielinkgripper;

import com.sheryv.common.property.PropertyUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class GripperTest {

    @Test
    public void test() {
        Bootstrapper.main(new String[]{"c", "s"});
    }

    @Test
    public void generateDefault() {
        Bootstrapper.main(new String[]{"c"});
    }

    @Test
    public void replace() {
        Bootstrapper.main(new String[]{"c", "r"});
    }

    @Test
    public void printDoc() {
        PropertyUtils.initLogging();
        Bootstrapper.main(new String[]{});
        Logger logger = LoggerFactory.getLogger(getClass());
        logger.info("Lo&1&gger&0& test");
        logger.debug("Lo&1&gger2&0& test");
    }

    @Test
    public void toIDM() {
        Bootstrapper.main(new String[]{"c", "m"});
    }

    @Test
    public void test2() {
        Bootstrapper.main(new String[]{"ins", "G:\\_list_Arrow.json", "G:\\links.csv"});
    }

}
