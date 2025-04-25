package tools.jackson.databind.ext.javatime.misc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.jsonFormatVisitors.*;

import static org.junit.jupiter.api.Assertions.*;

public class DateTimeSchemasTest extends DateTimeTestBase
{
    static class VisitorWrapper implements JsonFormatVisitorWrapper {
        SerializationContext serializationContext;
        final String baseName;
        final Map<String, String> traversedProperties;

        public VisitorWrapper(SerializationContext ctxt, String baseName, Map<String, String> traversedProperties) {
            this.serializationContext = ctxt;
            this.baseName = baseName;
            this.traversedProperties = traversedProperties;
        }

        VisitorWrapper createSubtraverser(String bn) {
            return new VisitorWrapper(getContext(), bn, traversedProperties);
        }

        public Map<String, String> getTraversedProperties() {
            return traversedProperties;
        }

        @Override
        public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
            return new JsonObjectFormatVisitor.Base(serializationContext) {
                @Override
                public void property(BeanProperty prop) {
                    anyProperty(prop);
                }

                @Override
                public void optionalProperty(BeanProperty prop) {
                    anyProperty(prop);
                }

                private void anyProperty(BeanProperty prop) {
                    final String propertyName = prop.getFullName().toString();
                    traversedProperties.put(baseName + propertyName, "");
                    serializationContext.findPrimaryPropertySerializer(prop.getType(), prop)
                            .acceptJsonFormatVisitor(createSubtraverser(baseName + propertyName + "."), prop.getType());
                }
            };
        }

        @Override
        public JsonArrayFormatVisitor expectArrayFormat(JavaType type) {
            traversedProperties.put(baseName, "ARRAY/"+type.getGenericSignature());
            return null;
        }

        @Override
        public JsonStringFormatVisitor expectStringFormat(JavaType type) {
            return new JsonStringFormatVisitor.Base() {
                @Override
                public void format(JsonValueFormat format) {
                    traversedProperties.put(baseName, "STRING/"+format.name());
                }
            };
        }

        @Override
        public JsonNumberFormatVisitor expectNumberFormat(JavaType type) {
            return new JsonNumberFormatVisitor.Base() {
                @Override
                public void numberType(JsonParser.NumberType format) {
                    traversedProperties.put(baseName, "NUMBER/"+format.name());
                }
            };
        }

        @Override
        public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) {
            return new JsonIntegerFormatVisitor.Base() {
                @Override
                public void numberType(JsonParser.NumberType numberType) {
                    traversedProperties.put(baseName + "numberType", "INTEGER/" + numberType.name());
                }

                @Override
                public void format(JsonValueFormat format) {
                    traversedProperties.put(baseName + "format", "INTEGER/" + format.name());
                }
            };
        }

        @Override
        public JsonBooleanFormatVisitor expectBooleanFormat(JavaType type) {
            traversedProperties.put(baseName, "BOOLEAN");
            return new JsonBooleanFormatVisitor.Base();
        }

        @Override
        public JsonNullFormatVisitor expectNullFormat(JavaType type) {
            return new JsonNullFormatVisitor.Base();
        }

        @Override
        public JsonAnyFormatVisitor expectAnyFormat(JavaType type) {
            traversedProperties.put(baseName, "ANY");
            return new JsonAnyFormatVisitor.Base();
        }

        @Override
        public JsonMapFormatVisitor expectMapFormat(JavaType type) {
            traversedProperties.put(baseName, "MAP");
            return new JsonMapFormatVisitor.Base(serializationContext);
        }

        @Override
        public SerializationContext getContext() {
            return serializationContext;
        }

        @Override
        public void setContext(SerializationContext ctxt) {
            this.serializationContext = ctxt;
        }
    }

    // 05-Feb-2025, tatu: Change defaults to Jackson 2.x wrt serialization
    //    shape (as Timestamps vs Strings)
    private final ObjectMapper MAPPER = mapperBuilder()
            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    // // // Local date/time types

    // [modules-java8#105]
    @Test
    public void testLocalTimeSchema() throws Exception
    {
        VisitorWrapper wrapper = new VisitorWrapper(null, "", new HashMap<String, String>());
        MAPPER.writer().acceptJsonFormatVisitor(LocalTime.class, wrapper);
        Map<String, String> properties = wrapper.getTraversedProperties();

        // By default, serialized as an int array, so:
        assertEquals(1, properties.size());
        _verifyIntArrayType(properties.get(""));

        // but becomes date/time
        wrapper = new VisitorWrapper(null, "", new HashMap<String, String>());
        MAPPER.writer().without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .acceptJsonFormatVisitor(LocalTime.class, wrapper);
        properties = wrapper.getTraversedProperties();
        _verifyTimeType(properties.get(""));
    }

    @Test
    public void testLocalDateSchema() throws Exception
    {
        VisitorWrapper wrapper = new VisitorWrapper(null, "", new HashMap<String, String>());
        MAPPER.writer().acceptJsonFormatVisitor(LocalDate.class, wrapper);
        Map<String, String> properties = wrapper.getTraversedProperties();

        // By default, serialized as an int array, so:
        assertEquals(1, properties.size());
        _verifyIntArrayType(properties.get(""));

        // but becomes date/time
        wrapper = new VisitorWrapper(null, "", new HashMap<String, String>());
        MAPPER.writer().without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .acceptJsonFormatVisitor(LocalDate.class, wrapper);
        properties = wrapper.getTraversedProperties();
        _verifyDateType(properties.get(""));
    }

    // // // Zoned date/time types

    @Test
    public void testDateTimeSchema() throws Exception
    {
        VisitorWrapper wrapper = new VisitorWrapper(null, "", new HashMap<String, String>());
        MAPPER.writer().acceptJsonFormatVisitor(ZonedDateTime.class, wrapper);
        Map<String, String> properties = wrapper.getTraversedProperties();

        // By default, serialized as an int array, so:
        assertEquals(1, properties.size());
        _verifyBigDecimalType(properties.get(""));

        // but becomes long
        wrapper = new VisitorWrapper(null, "", new HashMap<String, String>());
        MAPPER.writer()
                .without(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .acceptJsonFormatVisitor(ZonedDateTime.class, wrapper);
        properties = wrapper.getTraversedProperties();
        _verifyLongType(properties.get("numberType"));
        _verifyLongFormat(properties.get("format"));

        // but becomes date/time
        wrapper = new VisitorWrapper(null, "", new HashMap<String, String>());
        MAPPER.writer().without(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .acceptJsonFormatVisitor(ZonedDateTime.class, wrapper);
        properties = wrapper.getTraversedProperties();
        _verifyDateTimeType(properties.get(""));
    }

    private void _verifyIntArrayType(String desc) {
        assertEquals("ARRAY/Ljava/util/List<Ljava/lang/Integer;>;", desc);
    }

    private void _verifyTimeType(String desc) {
        assertEquals("STRING/TIME", desc);
    }

    private void _verifyDateType(String desc) {
        assertEquals("STRING/DATE", desc);
    }

    private void _verifyDateTimeType(String desc) {
        assertEquals("STRING/DATE_TIME", desc);
    }

    private void _verifyBigDecimalType(String desc) {
        assertEquals("NUMBER/BIG_DECIMAL", desc);
    }

    private void _verifyLongType(String desc) {
        assertEquals("INTEGER/LONG", desc);
    }

    private void _verifyLongFormat(String desc) {
        assertEquals("INTEGER/UTC_MILLISEC", desc);
    }
}
