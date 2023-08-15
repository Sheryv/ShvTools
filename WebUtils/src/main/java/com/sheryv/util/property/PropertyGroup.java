package com.sheryv.util.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS
    , include = JsonTypeInfo.As.EXISTING_PROPERTY
    , property = "type"
)
public abstract class PropertyGroup {
  /**
   * Unique id for each {@link PropertyGroup} entry in configuration file.
   */
  private final String id;
  
  private String type;
  
  /**
   * String used as header for splitting {@link PropertyGroup} entries in configuration file.
   */
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private final String category;
  
  private String description = "";
  
  
  public PropertyGroup(@JsonProperty("id") @NotNull String id, @JsonProperty("category") @NotNull String category) {
    this.id = id;
    if (category == null)
      this.category = Categories.DEFAULT;
    else
      this.category = category;
    
    type = getClass().getName();
  }
  
  public PropertyGroup() {
    this(Integer.toHexString(ThreadLocalRandom.current().nextInt()), null);
  }
  
  static class KeyValueGroup extends PropertyGroup {
    
    @JsonProperty("key_value_pairs")
    Map<String, Object> keyValuePairs = new HashMap<>();
    
    public KeyValueGroup() {
      super(Categories.OTHERS, Categories.OTHERS);
    }
  }
  
  public String getId() {
    return id;
  }
  
  public String getType() {
    return type;
  }
  
  public void setType(String type) {
    this.type = type;
  }
  
  public String getCategory() {
    return category;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
}
