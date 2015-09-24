
package com.fasterxml.jackson.databind.module;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.customenumkey.KeyEnum;
import com.fasterxml.jackson.databind.module.customenumkey.TestEnum;
import com.fasterxml.jackson.databind.module.customenumkey.TestEnumModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestCustomKeyDeserializer {
    @Test
    public void troubleWithKeys() throws Exception {
        ObjectMapper plainObjectMapper = new ObjectMapper();
        JsonNode tree = plainObjectMapper.readTree(Resources.getResource("data/enum-custom-key-test.json"));
        ObjectMapper fancyObjectMapper = TestEnumModule.setupObjectMapper(new ObjectMapper());
        // this line is might throw with Jackson 2.6.2.
        Map<TestEnum, Set<String>> map = fancyObjectMapper.convertValue(tree, new TypeReference<Map<TestEnum, Set<String>>>() {
        });
        assertNotNull(map);
    }

    @Ignore("issue 749, more or less")
    @Test
    public void tree() throws Exception {

        Map<KeyEnum, Object> inputMap = Maps.newHashMap();
        Map<TestEnum, Map<String, String>> replacements = Maps.newHashMap();
        Map<String, String> reps = Maps.newHashMap();
        reps.put("1", "one");
        replacements.put(TestEnum.GREEN, reps);
        inputMap.put(KeyEnum.replacements, replacements);
        ObjectMapper mapper = TestEnumModule.setupObjectMapper(new ObjectMapper());
        JsonNode tree = mapper.valueToTree(inputMap);
        ObjectNode ob = (ObjectNode) tree;
        JsonNode inner = ob.get("replacements");
        String firstFieldName = inner.fieldNames().next();
        assertEquals("green", firstFieldName);
    }
}
