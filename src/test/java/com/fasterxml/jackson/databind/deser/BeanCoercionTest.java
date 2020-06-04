package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

public class BeanCoercionTest extends BaseMapTest
{
    static class Bean {
        public String a;
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final String JSON_EMPTY = quote("");

    private final String JSON_BLANK = quote("    ");

    public void testPOJOFromEmptyStringLegacy() throws Exception
    {
        // first, verify default settings which do not accept empty String:
        assertFalse(MAPPER.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT));

        // should be ok to enable dynamically
        _verifyFromEmptyPass(MAPPER.reader()
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT),
                JSON_EMPTY);

    }

    public void testPOJOFromEmptyGlobalConfig() throws Exception
    {
        _testPOJOFromEmptyGlobalConfig(CoercionInputShape.EmptyString, JSON_EMPTY);
    }

    private void _testPOJOFromEmptyGlobalConfig(final CoercionInputShape shape, final String json)
            throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = newJsonMapper();
        mapper.coercionConfigDefaults().setCoercion(shape,
                CoercionAction.AsNull);
        assertNull(_verifyFromEmptyPass(mapper, JSON_EMPTY));

        // Then coerce as empty
        mapper = newJsonMapper();
        mapper.coercionConfigDefaults().setCoercion(shape,
                CoercionAction.AsEmpty);
        Bean b = _verifyFromEmptyPass(mapper, JSON_EMPTY);
        assertNotNull(b);

        // and finally, "try convert", which aliases to 'null'
        mapper = newJsonMapper();
        mapper.coercionConfigDefaults().setCoercion(shape,
                CoercionAction.TryConvert);
        assertNull(_verifyFromEmptyPass(mapper, JSON_EMPTY));
    }

    public void testPOJOFromEmptyLogicalTypeConfig() throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = newJsonMapper();
        mapper.coercionConfigFor(LogicalType.POJO).setCoercion(CoercionInputShape.EmptyString,
                CoercionAction.AsNull);
        assertNull(_verifyFromEmptyPass(mapper, JSON_EMPTY));

        // Then coerce as empty
        mapper = newJsonMapper();
        mapper.coercionConfigFor(LogicalType.POJO).setCoercion(CoercionInputShape.EmptyString,
                CoercionAction.AsEmpty);
        Bean b = _verifyFromEmptyPass(mapper, JSON_EMPTY);
        assertNotNull(b);

        // But also make fail again with 2-level settings
        mapper = newJsonMapper();
        mapper.coercionConfigDefaults().setCoercion(CoercionInputShape.EmptyString,
                CoercionAction.AsNull);
        mapper.coercionConfigFor(LogicalType.POJO).setCoercion(CoercionInputShape.EmptyString,
                CoercionAction.Fail);
        _verifyFromEmptyFail(mapper, quote(""));
    }

    public void testPOJOFromEmptyPhysicalTypeConfig() throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = newJsonMapper();
        mapper.coercionConfigFor(Bean.class).setCoercion(CoercionInputShape.EmptyString,
                CoercionAction.AsNull);
        assertNull(_verifyFromEmptyPass(mapper, JSON_EMPTY));

        // Then coerce as empty
        mapper = newJsonMapper();
        mapper.coercionConfigFor(Bean.class).setCoercion(CoercionInputShape.EmptyString,
                CoercionAction.AsEmpty);
        Bean b = _verifyFromEmptyPass(mapper, JSON_EMPTY);
        assertNotNull(b);

        // But also make fail again with 2-level settings, with physical having precedence
        mapper = newJsonMapper();
        mapper.coercionConfigFor(LogicalType.POJO).setCoercion(CoercionInputShape.EmptyString,
                CoercionAction.AsEmpty);
        mapper.coercionConfigFor(Bean.class).setCoercion(CoercionInputShape.EmptyString,
                CoercionAction.Fail);
        _verifyFromEmptyFail(mapper, quote(""));
    }

    private Bean _verifyFromEmptyPass(ObjectMapper m, String json) throws Exception {
        return _verifyFromEmptyPass(m.reader(), json);
    }

    private Bean _verifyFromEmptyPass(ObjectReader r, String json) throws Exception
    {
        return r.forType(Bean.class)
                .readValue(json);
    }

    private void _verifyFromEmptyFail(ObjectMapper m, String json) throws Exception
    {
        try {
            m.readValue(json, Bean.class);
            fail("Should not accept Empty/Blank String for POJO with passed settings");
        } catch (JsonProcessingException e) {
            _verifyFailMessage(e);
        }
    }

    private void _verifyFailMessage(JsonProcessingException e)
    {
        verifyException(e, "Cannot deserialize value of type ");
        verifyException(e, " from empty String ");
        assertValidLocation(e.getLocation());
    }
}
