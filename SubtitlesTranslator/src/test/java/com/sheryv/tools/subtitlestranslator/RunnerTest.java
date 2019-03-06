package com.sheryv.tools.subtitlestranslator;

import com.sheryv.tools.subtitlestranslator.subsdownload.Options;
import com.sheryv.tools.subtitlestranslator.subsdownload.Runner;

public class RunnerTest {

    public void start() {
        Configuration.init(Configuration.DEFAULT);
        Runner runner = new Runner();
        runner.start(Options.getDefault()
                .setDownloadDirectory("C:\\temp\\dest")
                .setSeries("Arrow")
                .setSeriesId("137932"));
    }

    public void gen() {
        Main.main(new String[0]);
    }
}
