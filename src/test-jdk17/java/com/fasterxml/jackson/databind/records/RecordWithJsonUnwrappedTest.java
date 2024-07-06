package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RecordWithJsonUnwrappedTest extends BaseMapTest {
    record RecordWithJsonUnwrapped(String unrelated, @JsonUnwrapped Inner inner) {
    }

    record Inner(String property1, String property2) {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, JsonUnwrapped
    /**********************************************************************
     */

    public void testUnwrappedWithRecord() throws Exception
    {
        RecordWithJsonUnwrapped initial = new RecordWithJsonUnwrapped("unrelatedValue", new Inner("value1", "value2"));

        ObjectNode tree = MAPPER.valueToTree(initial);

        assertEquals("unrelatedValue", tree.get("unrelated").textValue());
        assertEquals("value1", tree.get("property1").textValue());
        assertEquals("value2", tree.get("property2").textValue());

        RecordWithJsonUnwrapped outer = MAPPER.treeToValue(tree, RecordWithJsonUnwrapped.class);

        assertEquals("unrelatedValue", outer.unrelated());
        assertEquals("value1", outer.inner().property1());
        assertEquals("value2", outer.inner().property2());
    }
}
