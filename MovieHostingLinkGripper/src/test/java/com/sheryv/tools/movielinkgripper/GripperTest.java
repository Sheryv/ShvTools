package com.sheryv.tools.movielinkgripper;

import com.sheryv.common.property.PropertyUtils;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.tools.movielinkgripper.provider.AlltubeProvider;
import com.sheryv.tools.movielinkgripper.provider.Hosting;
import com.sheryv.tools.movielinkgripper.provider.Item;
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


    @Test
    public void getDlUrl() {
        Hosting hosting = new Hosting("Openload", EpisodesTypes.LECTOR, "",
                "https://alltube.tv/link/ZWlkPTM0OTY0Jmhvc3Rpbmc9b3BlbmxvYWQmaWQ9VW5RQW9oNENHYjQmbG9naW49cnl6ZHU=");
        AlltubeProvider provider = new AlltubeProvider("Orphan", 3, "");

        Configuration.init(Configuration.DEFAULT);
        try (Gripper ignored = Gripper.create(new Gripper.Options(), provider)) {
            Item orp = new Item("", "orp", 1);
            orp.updateHosting(hosting);
            provider.openVideoPage(orp, hosting.getVideoLink());
            String loadedVideoDownloadUrl = provider.findLoadedVideoDownloadUrl(orp);
            System.out.println("loadedVideoDownloadUrl = " + loadedVideoDownloadUrl);
        }

    }
}
