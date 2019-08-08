package com.sheryv.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;

public class SerialisationUtils {
    private SerialisationUtils() {
    }

    private static ObjectMapper yamlMapper;
    private static ObjectMapper jsonMapper;

    public static String toYaml(Object o) throws JsonProcessingException {
        ObjectMapper mapper = getYamlMapper();
        return mapper.writeValueAsString(o);
    }

    public static <T> T fromYaml(String s, TypeReference<T> reference) throws IOException {
        ObjectMapper mapper = getYamlMapper();
        return mapper.readValue(s, reference);
    }

    public static <T> T fromYaml(String s, Class<T> clazz) throws IOException {
        ObjectMapper mapper = getYamlMapper();
        return mapper.readValue(s, clazz);
    }


    public static <T> TypeReference<T> create() {
        return new TypeReference<T>() {
        };
    }

    public static ObjectMapper getYamlMapper() {
        if (yamlMapper == null) {
            YAMLFactory jf = new YAMLFactory();
            yamlMapper = new ObjectMapper(jf);
        }
        return yamlMapper;
    }


    public static ObjectMapper getJsonMapper() {
        if (jsonMapper == null) {
            jsonMapper = new ObjectMapper();
        }
        return jsonMapper;
    }

    public static String toJson(Object o) throws JsonProcessingException {
        return getJsonMapper().writeValueAsString(o);
    }

    public static String toJsonPretty(Object o) throws JsonProcessingException {
        ObjectMapper mapper = getJsonMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
    }

    public static <T> T fromJson(String s, TypeReference<T> reference) throws IOException {
        return getJsonMapper().readValue(s, reference);
    }

    public static <T> T fromJson(String s, Class<T> clazz) throws IOException {
        return getJsonMapper().readValue(s, clazz);
    }
}
