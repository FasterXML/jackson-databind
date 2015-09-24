
package com.fasterxml.jackson.databind.module;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.customenumkey.TestEnum;
import com.fasterxml.jackson.databind.module.customenumkey.TestEnumModule;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertNotNull;

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
        Map<TestEnum, Set<String>> map = fancyObjectMapper.convertValue(tree, new TypeReference<Map<TestEnum, Set<String>>>() {});
        assertNotNull(map);
    }
}
