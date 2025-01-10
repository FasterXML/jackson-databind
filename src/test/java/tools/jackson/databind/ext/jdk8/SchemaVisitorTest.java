package tools.jackson.databind.ext.jdk8;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonParser.NumberType;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// trivial tests visitor used (mostly) for JSON Schema generation
public class SchemaVisitorTest
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // for [datatype-jdk8#25]
    public void testOptionalInteger() throws Exception
    {
        final AtomicReference<Object> result = new AtomicReference<>();
        MAPPER.acceptJsonFormatVisitor(OptionalInt.class,
                new JsonFormatVisitorWrapper.Base() {
            @Override
            public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) {
                return new JsonIntegerFormatVisitor.Base() {
                    @Override
                    public void numberType(NumberType t) {
                        result.set(t);
                    }
                };
            }
        });
        assertEquals(JsonParser.NumberType.INT, result.get());
    }

    // for [datatype-jdk8#25]
    @Test
    public void testOptionalLong() throws Exception
    {
        final AtomicReference<Object> result = new AtomicReference<>();
        MAPPER.acceptJsonFormatVisitor(OptionalLong.class,
                new JsonFormatVisitorWrapper.Base() {
            @Override
            public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) {
                return new JsonIntegerFormatVisitor.Base() {
                    @Override
                    public void numberType(NumberType t) {
                        result.set(t);
                    }
                };
            }
        });
        assertEquals(JsonParser.NumberType.LONG, result.get());
    }

    // for [datatype-jdk8#25]
    @Test
    public void testOptionalDouble() throws Exception
    {
        final AtomicReference<Object> result = new AtomicReference<>();
        MAPPER.acceptJsonFormatVisitor(OptionalDouble.class,
                new JsonFormatVisitorWrapper.Base() {
            @Override
            public JsonNumberFormatVisitor expectNumberFormat(JavaType type) {
                return new JsonNumberFormatVisitor.Base() {
                    @Override
                    public void numberType(NumberType t) {
                        result.set(t);
                    }
                };
            }
        });
        assertEquals(JsonParser.NumberType.DOUBLE, result.get());
    }
}
