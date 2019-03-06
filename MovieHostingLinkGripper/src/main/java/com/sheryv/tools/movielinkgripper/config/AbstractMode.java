package com.sheryv.tools.movielinkgripper.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

@Getter
//@JsonTypeInfo(
//        use = JsonTypeInfo.Id.NAME,
//        include = JsonTypeInfo.As.PROPERTY,
//        com.sheryv.tools.utils.property = "name")
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = RunMode.class, name = RunMode.NAME),
//        @JsonSubTypes.Type(value = ReplaceMode.class, name = ReplaceMode.NAME),
//        @JsonSubTypes.Type(value = AddMode.class, name = AddMode.NAME)
//})
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "clazz")
public abstract class AbstractMode {

    protected final String name;
    protected final String modeCommandLineName;
    //    protected final String clazz;
    protected String filePathWithEpisodesList = "";


    public AbstractMode(String name, String modeCommandLineName) {
        this.name = name;
//        this.clazz = this.getClass().getName();
        this.modeCommandLineName = modeCommandLineName;
    }

    public abstract void execute(Configuration configuration) throws Exception;

}
