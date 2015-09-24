
package com.fasterxml.jackson.databind.module.customenumkey;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

/**
 *
 */
public class TestEnumKeyDeserializer extends KeyDeserializer {
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        try {
            return TestEnum.lookup(key);
        } catch (IllegalArgumentException e) {
            throw ctxt.weirdKeyException(TestEnum.class, key, "Unknown code");
        }
    }
}
