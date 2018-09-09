package com.sheryv.utils;

import com.sheryv.common.property.PropertyManager;
import com.sheryv.common.property.PropertyUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;


@Ignore
@Slf4j
public class LoggingTest {

    static Logger logger = LoggerFactory.getLogger(LoggingTest.class.getName());

    @Test
    public void test() throws IOException {
        PropertyManager.init();
        PropertyUtils.initLogging();
        logger.info("Normal");
        log.info("Global");
        Optional<String> s = PropertyManager.getInstance().writeAsString();
        log();
    }

    public static void log() {
        logger.info("other");
    }
}
