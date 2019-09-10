package com.fasterxml.jackson.databind.jsonschema;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;

public class SchemaWithUUIDTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    public void testUUIDSchema() throws Exception
    {
        final AtomicReference<JsonValueFormat> format = new AtomicReference<>();

        MAPPER.acceptJsonFormatVisitor(UUID.class, new JsonFormatVisitorWrapper.Base() {
            @Override
            public JsonStringFormatVisitor expectStringFormat(JavaType type) {
                return new JsonStringFormatVisitor() {
                    @Override
                    public void enumTypes(Set<String> enums) { }

                    @Override
                    public void format(JsonValueFormat f) {
                        format.set(f);
                    }
                };
            }
        });
        assertEquals(JsonValueFormat.UUID, format.get());
    }
}
