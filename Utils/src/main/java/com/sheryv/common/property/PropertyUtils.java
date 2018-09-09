package com.sheryv.common.property;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class PropertyUtils {

    public static final String DEFAULT_PROPERTY_GROUP_ID = "DEFAULT";

    public static void traversePropertyGroupFields(@Nonnull Class<? extends PropertyGroup> classs,
                                                   @Nonnull BiConsumer<Field, Comment> consumer,
                                                   @Nullable Class<? extends Comment> annotation) {
        if (annotation == null)
            annotation = Comment.class;
        Class<?> c = classs;
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotation)) {
                    consumer.accept(field, field.getAnnotation(annotation));
                }
            }
            c = c.getSuperclass();
        }
    }

    public static DefaultPropertyGroup getDefaultProperty(boolean ignoreNotInitialized) {
        if (ignoreNotInitialized && !PropertyManager.isInitialised()) {
            return new DefaultPropertyGroup(DEFAULT_PROPERTY_GROUP_ID, null);
        }
        Optional<DefaultPropertyGroup> propertyGroup = PropertyManager.getInstance().byId(DEFAULT_PROPERTY_GROUP_ID);
        if (propertyGroup.isPresent()) {
            return propertyGroup.get();
        } else {
            DefaultPropertyGroup def = new DefaultPropertyGroup(DEFAULT_PROPERTY_GROUP_ID, null);
            PropertyManager.getInstance().registerGroup(def);
            return def;
        }
    }

    public static void initLogging() {
        LogManager logManager = LogManager.getLogManager();
        try {
            logManager.updateConfiguration(
                    s -> (s1, s2) -> PropertyUtils.getDefaultProperty(true)
                            .getStartupSystemProperties().get(s));

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            String level = logManager.getProperty(".level");
            Level parse = Level.parse(level);
//            logManager.getLogger("").setLevel(parse);
        } catch (Exception e) {
        }
    }

}
