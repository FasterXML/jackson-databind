
package com.fasterxml.jackson.databind.module.customenumkey;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;

/**
 *
 */
public class TestEnumDeserializer extends StdDeserializer<TestEnum> {

    public TestEnumDeserializer() {
        super(TestEnum.class);
    }

    @Override
    public TestEnum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String code = jp.getText();
        try {
            return TestEnum.lookup(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidFormatException("Undefined ISO-639 language code", jp.getCurrentLocation(), code, TestEnum.class);
        }
    }
}
