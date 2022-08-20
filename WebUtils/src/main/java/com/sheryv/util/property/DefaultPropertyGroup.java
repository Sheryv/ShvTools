package com.sheryv.util.property;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class DefaultPropertyGroup extends PropertyGroup {
    public static final String CHARSET = "UTF-8";
    private static final Map<String, String> STARTUP_SYSTEM_PROPS;


    @Comment("Charset name used to read and write files")
    private String charset = CHARSET;

    @Comment("Contains keys and values which are loaded as system properties during app initialisation")
    private Map<String, String> startupSystemProperties = STARTUP_SYSTEM_PROPS;

    public DefaultPropertyGroup(String id, String category) {
        super(id, category);
    }

//    public DefaultPropertyGroup(@JsonProperty("id") String id, @JsonProperty("category")  String category) {
//        super(id, category);
//    }

    static {
        Map<String, String> map = new HashMap<>();
        map.put("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT.%1$tL %4$-5s %2$25s: %5$s%n");
        map.put("com.sheryv.util.logging.ShFormatter.format", "%t %L %n: %m");
        map.put("com.sheryv.util.logging.ShFormatter.colorize", "true");
        map.put("com.sheryv.util.logging.ShFormatter.date.format", "yyyy-MM-dd HH:mm:ss.SSS");
        map.put("handlers", "java.util.logging.ConsoleHandler");
        map.put("java.util.logging.ConsoleHandler.formatter", "com.sheryv.util.logging.ShFormatter");
        map.put(".level", "FINEST");
        map.put("java.util.logging.ConsoleHandler", "FINEST");
        map.put("java.util.logging.FileHandler.formatter", "com.sheryv.util.logging.ShFormatter");
        STARTUP_SYSTEM_PROPS = Collections.unmodifiableMap(map);
    }
}
