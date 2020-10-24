package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

public class CoerceEnumTest extends BaseMapTest
{
    protected enum EnumCoerce {
        A, B, C,

        // since 2.12 defines Enum "empty" to be default value (if defined),
        // or `null` (if not defined), define default...
        @JsonEnumDefaultValue
        DEFAULT
        ;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final String JSON_EMPTY = quote("");
    private final String JSON_BLANK = quote("    ");

    private final EnumCoerce ENUM_DEFAULT = EnumCoerce.DEFAULT;
    
    /*
    /********************************************************
    /* Test methods, from empty String
    /********************************************************
     */

    public void testLegacyDefaults() throws Exception
    {
        // first, verify default settings which do not accept empty String:
        assertFalse(MAPPER.isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS));
    }

    public void testEnumFromEmptyGlobalConfig() throws Exception {
        _testEnumFromEmptyGlobalConfig(CoercionInputShape.EmptyString, JSON_EMPTY, null);
    }
    
    public void testEnumFromEmptyLogicalTypeConfig() throws Exception {
        _testEnumFromEmptyLogicalTypeConfig(CoercionInputShape.EmptyString, JSON_EMPTY, null);
    }

    public void testEnumFromEmptyPhysicalTypeConfig() throws Exception {
        _testEnumFromEmptyPhysicalTypeConfig(CoercionInputShape.EmptyString, JSON_EMPTY, null);
    }

    /*
    /********************************************************
    /* Test methods, from blank String
    /********************************************************
     */

    public void testEnumFromBlankGlobalConfig() throws Exception {
        _testEnumFromEmptyGlobalConfig(CoercionInputShape.EmptyString, JSON_BLANK, Boolean.TRUE);
    }

    public void testEnumFromBlankLogicalTypeConfig() throws Exception {
        _testEnumFromEmptyLogicalTypeConfig(CoercionInputShape.EmptyString, JSON_BLANK, Boolean.TRUE);
    }

    public void testEnumFromBlankPhysicalTypeConfig() throws Exception {
        _testEnumFromEmptyPhysicalTypeConfig(CoercionInputShape.EmptyString, JSON_BLANK, Boolean.TRUE);
    }

    /*
    /********************************************************
    /* Test methods, from numeric index
    /********************************************************
     */

    /*
    /********************************************************
    /* Second-level helper methods
    /********************************************************
     */

    private void _testEnumFromEmptyGlobalConfig(final CoercionInputShape shape, final String json,
            Boolean allowEmpty)
        throws Exception
    {
        ObjectMapper mapper;

        // First, coerce to null
        mapper = _globMapper(shape, CoercionAction.AsNull, allowEmpty);
        assertNull(_verifyFromEmptyPass(mapper, json));

        // Then coerce as empty
        mapper = _globMapper(shape, CoercionAction.AsEmpty, allowEmpty);
        EnumCoerce b = _verifyFromEmptyPass(mapper, json);
        assertEquals(ENUM_DEFAULT, b);

        // and finally, "try convert", which for Enums is same as "empty" (default)
        mapper = _globMapper(shape, CoercionAction.TryConvert, allowEmpty);
        assertEquals(ENUM_DEFAULT, _verifyFromEmptyPass(mapper, json));
    }

    private void _testEnumFromEmptyLogicalTypeConfig(final CoercionInputShape shape, final String json,
            Boolean allowEmpty)
        throws Exception
    {
        ObjectMapper mapper;
        EnumCoerce b;

        // First, coerce to null
        mapper = _logMapper(LogicalType.Enum, shape, CoercionAction.AsNull, allowEmpty);
        b = _verifyFromEmptyPass(mapper, json);
        assertNull(b);

        // Then coerce as empty
        mapper = _logMapper(LogicalType.Enum, shape, CoercionAction.AsEmpty, allowEmpty);
        b = _verifyFromEmptyPass(mapper, json);
        assertEquals(ENUM_DEFAULT, b);

        // and with TryConvert (for enums same as empty)
        mapper = _logMapper(LogicalType.Enum, shape, CoercionAction.TryConvert, allowEmpty);
        b = _verifyFromEmptyPass(mapper, json);
        assertEquals(ENUM_DEFAULT, b);

        // But also make fail again with 2-level settings
        mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(h -> {
                    h.setCoercion(shape, CoercionAction.AsNull)
                        .setAcceptBlankAsEmpty(allowEmpty);
                })
                .withCoercionConfig(LogicalType.Enum,
                        h -> h.setCoercion(shape, CoercionAction.Fail))
                .build();
        _verifyFromEmptyFail(mapper, json);
    }

    private void _testEnumFromEmptyPhysicalTypeConfig(final CoercionInputShape shape, final String json,
            Boolean allowEmpty)
        throws Exception
    {
        ObjectMapper mapper;
        EnumCoerce b;

        // First, coerce to null
        mapper = _physMapper(EnumCoerce.class, shape, CoercionAction.AsNull, allowEmpty);
        b = _verifyFromEmptyPass(mapper, json);
        assertNull(b);

        // Then coerce as empty
        mapper = _physMapper(EnumCoerce.class, shape, CoercionAction.AsEmpty, allowEmpty);
        b = _verifyFromEmptyPass(mapper, json);
        assertEquals(ENUM_DEFAULT, b);

        mapper = _physMapper(EnumCoerce.class, shape, CoercionAction.TryConvert, allowEmpty);
        b = _verifyFromEmptyPass(mapper, json);
        assertEquals(ENUM_DEFAULT, b);

        // But also make fail again with 2-level settings, with physical having precedence
        mapper = jsonMapperBuilder()
                .withCoercionConfig(LogicalType.Enum,
                        h -> h.setCoercion(shape, CoercionAction.AsEmpty)
                            .setAcceptBlankAsEmpty(allowEmpty)
                )
                .withCoercionConfig(EnumCoerce.class,
                        h -> h.setCoercion(shape, CoercionAction.Fail)
                )
                .build();
        _verifyFromEmptyFail(mapper, json);
    }

    /*
    /********************************************************
    /* Mapper construction helpers
    /********************************************************
     */

    private ObjectMapper _globMapper(CoercionInputShape shape, CoercionAction act,
            Boolean allowEmpty)
    {
        return jsonMapperBuilder()
                .withCoercionConfigDefaults(h -> {
                    h.setCoercion(shape, act)
                        .setAcceptBlankAsEmpty(allowEmpty);
                })
                .build();
    }

    private ObjectMapper _logMapper(LogicalType type, CoercionInputShape shape, CoercionAction act,
            Boolean allowEmpty)
    {
        return jsonMapperBuilder()
                .withCoercionConfig(type,
                        h -> h.setCoercion(shape, act)
                            .setAcceptBlankAsEmpty(allowEmpty)
                )
                .build();
    }

    private ObjectMapper _physMapper(Class<?> type, CoercionInputShape shape, CoercionAction act,
            Boolean allowEmpty)
    {
        return jsonMapperBuilder()
                .withCoercionConfig(type,
                        h -> h.setCoercion(shape, act)
                            .setAcceptBlankAsEmpty(allowEmpty)
                )
                .build();
    }

    /*
    /********************************************************
    /* Verification helper methods
    /********************************************************
     */

    private EnumCoerce _verifyFromEmptyPass(ObjectMapper m, String json) throws Exception {
        return _verifyFromEmptyPass(m.reader(), json);
    }

    private EnumCoerce _verifyFromEmptyPass(ObjectReader r, String json) throws Exception
    {
        return r.forType(EnumCoerce.class)
                .readValue(json);
    }

    private void _verifyFromEmptyFail(ObjectMapper m, String json) throws Exception
    {
        try {
            m.readValue(json, EnumCoerce.class);
            fail("Should not accept Empty/Blank String for Enum with passed settings");
        } catch (MismatchedInputException e) {
            _verifyFailMessage(e);
        }
    }

    private void _verifyFailMessage(JsonProcessingException e)
    {
        verifyException(e, "Cannot coerce ");
        verifyException(e, " empty String ", " blank String ");
        assertValidLocation(e.getLocation());
    }
}
