package com.sheryv.tools.movielinkgripper;

import com.sheryv.tools.movielinkgripper.config.HostingConfig;
import com.sheryv.util.Strings;
import com.sheryv.util.property.PropertyUtils;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.tools.movielinkgripper.provider.AlltubeProvider;
import com.sheryv.tools.movielinkgripper.provider.Hosting;
import com.sheryv.tools.movielinkgripper.provider.Item;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;

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
    public void gui() {
        Bootstrapper.main(new String[]{"i"});
    }

    @Test
    public void test2() {
        Bootstrapper.main(new String[]{"ins", "G:\\_list_Arrow.json", "G:\\links.csv"});
    }


    @Test
    public void getDlUrl() {
        Hosting hosting = new Hosting("Openload", EpisodesTypes.LECTOR, null, 1,
                "https://alltube.tv/link/ZWlkPTM0OTY0Jmhvc3Rpbmc9b3BlbmxvYWQmaWQ9VW5RQW9oNENHYjQmbG9naW49cnl6ZHU=");
        AlltubeProvider provider = new AlltubeProvider("Orphan", 3, "");

        Configuration.init(Configuration.getDefault());
        try (Gripper ignored = Gripper.create(new Gripper.Options(), provider)) {
            Item orp = new Item("", "orp", 1);
            orp.updateHosting(hosting);
            provider.openVideoPage(orp, hosting);
            String loadedVideoDownloadUrl = provider.findLoadedVideoDownloadUrl(orp, hosting);
            System.out.println("loadedVideoDownloadUrl = " + loadedVideoDownloadUrl);
        }

    }

    @Test
    public void tempalte() {
        String s = "{{name}} S{{season}}E{{episode_number}} {{name}} ";
        var a = new LinkedHashMap<String, Object>();
        a.put("name", "nazwa");
        a.put("season", String.format("%02d", 3));
        a.put("episode_number", "12");
        System.out.println(Strings.fillTemplate(s, a));
    }

    @Test
    public void priorities() {
        Configuration init = Configuration.init(Configuration.getDefault());
        Gripper gripper = Gripper.create(new Gripper.Options(), Transformer.createProvider("alltube", "Arrow", 1, "alltube.org"));
        List<HostingConfig> z = gripper.getPriorities(0);
        List<HostingConfig> f = gripper.getPriorities(1);
        List<HostingConfig> s = gripper.getPriorities(2);
        System.out.println();
    }
}
