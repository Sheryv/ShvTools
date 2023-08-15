package com.sheryv.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.Path

object SerialisationUtils {
  
  val yamlMapper: ObjectMapper by lazy {
    createJsonMapper(factory = YAMLFactory())
  }
  
  val jsonMapper: ObjectMapper by lazy {
    createJsonMapper()
  }
  
  @JvmStatic
  fun toYaml(o: Any): String {
    return yamlMapper.writeValueAsString(o)
  }
  
  @JvmStatic
  fun <T> fromYaml(s: String, reference: TypeReference<T>): T {
    return yamlMapper.readValue(s, reference)
  }
  
  @JvmStatic
  fun <T> fromYaml(s: String, clazz: Class<T>): T {
    return yamlMapper.readValue(s, clazz)
  }
  
  @JvmStatic
  fun toJson(f: File, o: Any) {
    jsonMapper.writeValue(f, o)
  }
  
  @JvmStatic
  fun toJson(o: Any): String {
    return jsonMapper.writeValueAsString(o)
  }
  
  @JvmStatic
  fun toJsonPretty(o: Any): String {
    return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o)
  }
  
  @JvmStatic
  fun <T> fromJson(s: String, reference: TypeReference<T>): T {
    return jsonMapper.readValue(s, reference)
  }
  
  @JvmStatic
  fun <T> fromJson(s: String, clazz: Class<T>): T {
    return jsonMapper.readValue(s, clazz)
  }
  
  @JvmStatic
  inline fun <reified T> fromJson(s: String): T {
    return jsonMapper.readValue(s)
  }
  
  @JvmStatic
  fun <T> fromJson(f: File, clazz: Class<T>): T {
    return jsonMapper.readValue(f, clazz)
  }
  
  inline fun <reified T> type(): TypeReference<T> {
    return object : TypeReference<T>() {}
  }
  
  inline fun <reified T> deserializeList(list: List<Map<String, *>>?): List<T>? {
    if (list == null) return null
    val type = object : TypeReference<List<T>>() {}
    return jsonMapper.convertValue(list, type)
  }
  
  
  @JvmStatic
  fun createJsonMapper(types: Map<String, Class<*>> = emptyMap(), factory: JsonFactory? = null): ObjectMapper {
    val map = ObjectMapper(factory)
    map.configure(SerializationFeature.INDENT_OUTPUT, true)
    map.registerModule(KotlinModule.Builder().build())
    map.registerModule(JavaTimeModule())
    map.dateFormat = StdDateFormat()
    map.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    map.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    map.registerSubtypes(*types.map { NamedType(it.value, it.key) }.toTypedArray())
    val mod = SimpleModule()
    mod.addSerializer(Path::class.java, ToStringSerializer())
    map.registerModule(mod)
    return map
  }
  
}
