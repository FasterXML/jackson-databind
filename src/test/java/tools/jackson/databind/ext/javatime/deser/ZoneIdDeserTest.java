package tools.jackson.databind.ext.javatime.deser;

import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;
import tools.jackson.databind.ext.javatime.MockObjectConfiguration;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

import static org.junit.jupiter.api.Assertions.*;

public class ZoneIdDeserTest extends DateTimeTestBase
{
    private final ObjectMapper MAPPER = newMapper();
    private final TypeReference<Map<String, ZoneId>> MAP_TYPE_REF = new TypeReference<Map<String, ZoneId>>() { };

    private final ObjectMapper MOCK_OBJECT_MIXIN_MAPPER = mapperBuilder()
            .addMixIn(ZoneId.class, MockObjectConfiguration.class)
            .build();

    @Test
    public void testSimpleZoneIdDeser()
    {
        assertEquals(ZoneId.of("America/Chicago"),
                MAPPER.readValue("\"America/Chicago\"", ZoneId.class));
        assertEquals(ZoneId.of("America/Anchorage"),
                MAPPER.readValue("\"America/Anchorage\"", ZoneId.class));
    }

    @Test
    public void testPolymorphicZoneIdDeser()
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(ZoneId.class, MockObjectConfiguration.class)
                .build();
        ZoneId value = mapper.readValue("[\"" + ZoneId.class.getName() + "\",\"America/Denver\"]", ZoneId.class);
        assertEquals(ZoneId.of("America/Denver"), value);
    }

    @Test
    public void testDeserialization01()
    {
        assertEquals(ZoneId.of("America/Chicago"),
                MAPPER.readValue("\"America/Chicago\"", ZoneId.class));
    }

    @Test
    public void testDeserialization02()
    {
        assertEquals(ZoneId.of("America/Anchorage"),
                MAPPER.readValue("\"America/Anchorage\"", ZoneId.class));
    }

    @Test
    public void testDeserializationWithTypeInfo02()
    {
        ZoneId value = MOCK_OBJECT_MIXIN_MAPPER.readValue("[\"" + ZoneId.class.getName() + "\",\"America/Denver\"]", ZoneId.class);
        assertEquals(ZoneId.of("America/Denver"), value);
    }

    /*
    /**********************************************************
    /* Tests for empty string handling
    /**********************************************************
     */

    @Test
    public void testLenientDeserializeFromEmptyString()
    {

        String key = "zoneId";
        ObjectMapper mapper = newMapper();
        ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, null));
        Map<String, ZoneId> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        ZoneId actualDateFromNullStr = actualMapFromNullStr.get(key);
        assertNull(actualDateFromNullStr);

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, ""));
        Map<String, ZoneId> actualMapFromEmptyStr = objectReader.readValue(valueFromEmptyStr);
        ZoneId actualDateFromEmptyStr = actualMapFromEmptyStr.get(key);
        assertEquals(null, actualDateFromEmptyStr,
                "empty string failed to deserialize to null with lenient setting");
    }

    public void testStrictDeserializeFromEmptyString()
    {

        final String key = "zoneId";
        final ObjectMapper mapper = mapperBuilder()
                .withCoercionConfig(LogicalType.DateTime,
                        cfg -> cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .build();
        final ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, null));
        Map<String, ZoneId> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        assertNull(actualMapFromNullStr.get(key));

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, ""));
        try {
            objectReader.readValue(valueFromEmptyStr);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce empty String");
            verifyException(e, ZoneId.class.getName());
        }
    }

    // Test to verify that JavaTimeInitializer's ValueInstantiators.Base
    // handles ZoneId subtypes (like the internal ZoneRegion class) via
    // the `modifyValueInstantiator` callback -- specifically the
    // `ZoneId.class.isAssignableFrom(raw)` branch.
    // This exercises the code path when polymorphic type id resolves to
    // the concrete ZoneId subtype (not ZoneId itself), bypassing the
    // registered custom deserializer (which is keyed on ZoneId.class).
    @Test
    public void testPolymorphicZoneIdConcreteSubtypeDeser()
    {
        // ZoneId.of() returns a concrete subtype (java.time.ZoneRegion),
        // not ZoneId itself
        Class<?> concreteZoneIdClass = ZoneId.of("America/Denver").getClass();
        assertNotEquals(ZoneId.class, concreteZoneIdClass,
                "Expected concrete ZoneId subtype, not ZoneId itself");

        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(ZoneId.class, MockObjectConfiguration.class)
                .build();

        // Use the concrete class name as the type id in the wrapper array
        ZoneId value = mapper.readValue(
                "[\"" + concreteZoneIdClass.getName() + "\",\"America/Denver\"]",
                ZoneId.class);
        assertEquals(ZoneId.of("America/Denver"), value);
    }

    // [module-java8#68]
    @Test
    public void testZoneIdDeserFromEmpty()
    {
        // by default, should be fine
        assertNull(MAPPER.readValue(q("  "), ZoneId.class));
        // but fail if coercion illegal
        final ObjectMapper mapper = mapperBuilder()
                .withCoercionConfig(LogicalType.DateTime,
                        cfg -> cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .build();
        try {
            mapper.readValue(q(" "), ZoneId.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce empty String");
            verifyException(e, ZoneId.class.getName());
        }
    }
}
