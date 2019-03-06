package com.sheryv.common.property;

import org.junit.Ignore;


@Ignore
public class SerializationTest {
/*
    @Test
    public void parsing() throws IOException {
        PropertyManager propertyManager = PropertyManager.init()
                .registerGroup(new DefaultPropertyGroup("id_masn1", Categories.DEFAULT))
                .registerGroup(new DefaultPropertyGroup("idDS", "CAT"))
                .registerGroup(new DefaultPropertyGroup("id_masn3", Categories.DEFAULT));
        propertyManager.put("keyy", 2);
        Optional<String> serialised = propertyManager.writeAsString();
        Map<String, ArrayList<PropertyGroup>> map = propertyManager.getMapper()
                .readValue(serialised.get(), new TypeReference<Map<String, ArrayList<PropertyGroup>>>() {
                });
        Assert.assertEquals("idDS", map.get("CAT").get(0).getId());
        Assert.assertTrue(map.get(Categories.OTHERS).get(0) instanceof PropertyGroup.KeyValueGroup);
        Assert.assertEquals(2, ((PropertyGroup.KeyValueGroup) map.get(Categories.OTHERS).get(0)).keyValuePairs.get("keyy"));
    }*/
}
