package com.sheryv.util.property;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class PropertyManager {

    private static PropertyManager instance;
    private static final Object lock = new Object();
    private final ObjectMapper mapper;
    private final Map<String, ArrayList<PropertyGroup>> propertyGroups;
    //                 id         field     comment
    private final Map<String, Map<String, String>> comments;
    private final File file;

    private Map<String, Object> others;

    public static PropertyManager init() {
        return initWithFileSaving(null);
    }

    public static PropertyManager initWithFileSaving(File file) {
        if (instance != null) {
            throw new IllegalStateException("PropertyManager is already initialised!");
        }
        YAMLFactory factory = new YAMLFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        synchronized (lock) {
            instance = new PropertyManager(mapper, file);
            instance.readPropFile();
        }
        return instance;
    }

    public static boolean isInitialised() {
        return instance != null;
    }

    private PropertyManager(ObjectMapper mapper, File file) {
        this.mapper = mapper;
        this.file = file;
        propertyGroups = new HashMap<>();
        ArrayList<PropertyGroup> groups = new ArrayList<>();
        PropertyGroup.KeyValueGroup group = new PropertyGroup.KeyValueGroup();
        others = group.keyValuePairs;
        groups.add(group);
        propertyGroups.put(Categories.OTHERS, groups);
        comments = new HashMap<>();
    }

    @Nonnull
    public static PropertyManager getInstance() {
        synchronized (lock) {
            if (instance == null) {
                throw new IllegalStateException("PropertyManager is not initialised! Use init...() first.");
            }
            return instance;
        }
    }


    public PropertyManager registerGroup(PropertyGroup groupDefaultValues) throws IllegalArgumentException {
        if (exists(groupDefaultValues.getId()))
            throw new IllegalArgumentException("PropertyGroup with id \"" + groupDefaultValues.getId()
                    + "\" already exists! [passed type: " + groupDefaultValues.getClass().getName() + "]");
        String categoryName = groupDefaultValues.getCategory();
        if (Categories.OTHERS.equals(categoryName)) {
            throw new IllegalArgumentException("PropertyGroup instances cannot belong to others group ("
                    + Categories.OTHERS + ")");
        }
        List<PropertyGroup> category = this.propertyGroups.computeIfAbsent(categoryName, s -> new ArrayList<>());
        category.add(groupDefaultValues);
        Map<String, String> commentsForId = new HashMap<>();
        PropertyUtils.traversePropertyGroupFields(groupDefaultValues.getClass(), (field, annotation) -> {
            commentsForId.put(field.getName(), transformComment(annotation.value()));
        }, null);
        comments.put(groupDefaultValues.getId(), commentsForId);
        return this;
    }

    @Nonnull
    public List<PropertyGroup> getGroups() {
        List<PropertyGroup> accumulator = new ArrayList<>();
        for (List<PropertyGroup> groups : propertyGroups.values()) {
            accumulator.addAll(groups);
        }
        return accumulator;
    }


    public Optional<String> byKeyAsString(@Nonnull String key) {
        Object value = others.get(key);
        if (value == null)
            return Optional.empty();
        return Optional.of(String.valueOf(value));
    }

    public <T> Optional<T> byKey(@Nonnull String key) {
        Object value = others.get(key);
        if (value == null)
            return Optional.empty();
        try {
            @SuppressWarnings("unchecked")
            T res = (T) value;
            return Optional.of(res);
        } catch (Exception e) {
            throw new IllegalArgumentException("Generic parameter type does not match value saved in properties under key: "
                    + key + ". Saved type is " + value.getClass(), e);
        }
    }

    public Object put(@Nonnull String key, Object value) {
        return others.put(key, value);
    }

    public <T extends PropertyGroup> Optional<T> byId(@Nonnull String id) {
        for (Map.Entry<String, ArrayList<PropertyGroup>> categories : propertyGroups.entrySet()) {
            for (PropertyGroup propertyGroup : categories.getValue()) {
                if (propertyGroup.getId().equals(id)) {
                    try {
                        @SuppressWarnings("unchecked")
                        T t = (T) propertyGroup;
                        return Optional.of(t);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<String> writeAsString() {
        final String pattern = "\n- id:";

        Map<String, TreeMap<Integer, String>> indices = new TreeMap<>();
        try (StringWriter sw = new StringWriter()) {
            mapper.writeValue(sw, propertyGroups);
            StringBuffer buffer = sw.getBuffer();

            int next = buffer.indexOf(pattern, 0);
            while (next >= 0) {
                int end = buffer.indexOf(pattern, next + 8);
                int start = next;
                next = end;
                if (end == -1) {
                    end = buffer.length();
                }
                String group = buffer.substring(start, end);
                if (group.charAt(group.length() - 1) == ':') {
                    int cat = group.lastIndexOf('\n');
                    group = group.substring(0, cat);
                }
                int idEnd = buffer.indexOf("\"", start + 8);
                String id = buffer.substring(start + 8, idEnd);
                if (id.equals(Categories.OTHERS))
                    continue;
                Map<String, String> commentsForId = comments.get(id);
                if (commentsForId != null) {
                    TreeMap<Integer, String> mapping = new TreeMap<>();
                    indices.put(id, mapping);
                    for (Map.Entry<String, String> comment : commentsForId.entrySet()) {
                        int fieldIndex = group.indexOf("\n  " + comment.getKey() + ":");
                        if (fieldIndex != -1) {
                            mapping.put(fieldIndex + start + 1, comment.getValue());
                        }
                    }
                }
            }

            for (String id : ((TreeMap<String, TreeMap<Integer, String>>) indices).descendingKeySet()) {
                TreeMap<Integer, String> groupComments = indices.get(id);
                for (Integer index : groupComments.descendingKeySet()) {
                    buffer.insert(index, groupComments.get(index));
                }
            }
            return Optional.of(buffer.toString());
        } catch (
                IOException e)

        {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private void readPropFile() {
        //todo reading file from disk at start
    }

    private void writeCurrent() {
        synchronized (lock) {
            //todo writing file to disk
        }
    }

    private boolean exists(String id) {
        for (List<PropertyGroup> list : propertyGroups.values()) {
            for (PropertyGroup group : list) {
                if (group.getId().equals(id))
                    return true;
            }
        }
        return false;
    }

    private String transformComment(String com) {
        return "  # " + com.replace("\n", "\n  # ") + "\n";
    }


    public Map<String, ArrayList<PropertyGroup>> getPropertyGroups() {
        return propertyGroups;
    }

    ObjectMapper getMapper() {
        return mapper;
    }

    static {
        Map<String, String> startupSystemProperties = PropertyUtils.getDefaultProperty(true)
                .getStartupSystemProperties();
        startupSystemProperties.forEach(System::setProperty);

    }
}
