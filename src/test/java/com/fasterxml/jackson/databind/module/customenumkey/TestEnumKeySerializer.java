
package com.fasterxml.jackson.databind.module.customenumkey;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Jackson serializer for LanguageCode used as a key.
 */
public class TestEnumKeySerializer extends JsonSerializer<TestEnum> {
    @Override
    public void serialize(TestEnum test, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeFieldName(test.code());
    }

    @Override
    public Class<TestEnum> handledType() {
        return TestEnum.class;
    }
}
