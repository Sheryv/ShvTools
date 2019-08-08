package com.sheryv.tools.movielinkgripper.config;

import com.sheryv.tools.movielinkgripper.Series;
import com.sheryv.tools.movielinkgripper.Transformer;
import com.sheryv.util.FileUtils;
import com.sheryv.util.Strings;
import lombok.Getter;

@Getter
public class SendToManagerMode extends AbstractMode {
    public static final String NAME = "SendToManager";

    public SendToManagerMode() {
        super(NAME, "m");
    }

    @Override
    public void execute(Configuration configuration) throws Exception {
        String path = filePathWithEpisodesList;
        if (Strings.isNullOrEmpty(path))
            path = configuration.getDefaultFilePathWithEpisodesList();
        String json = FileUtils.readFileInMemory(path);
        Series series = Transformer.loadSeries(json);
        Transformer.sendToIDM(series, configuration);
    }
}
