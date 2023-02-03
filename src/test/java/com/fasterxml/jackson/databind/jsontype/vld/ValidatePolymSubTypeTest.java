package com.fasterxml.jackson.databind.jsontype.vld;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Tests to verify working of customizable {@PolymorphicTypeValidator},
 * see [databind#2195], regarding verification of subtype instances.
 *
 * @since 2.10
 */
public class ValidatePolymSubTypeTest extends BaseMapTest
{
    // // // Value types

    static abstract class BaseValue {
        public int x = 3;

        @Override
        public boolean equals(Object other) {
            return (other != null)
                    && (getClass() == other.getClass())
                    && ((BaseValue) other).x == this.x
                    ;
        }
    }

    static class BadValue extends BaseValue { }
    static class GoodValue extends BaseValue { }
    static class MehValue extends BaseValue { }

    // // // Wrapper types

    static class AnnotatedWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public BaseValue value;

        protected AnnotatedWrapper() { }
        public AnnotatedWrapper(BaseValue v) { value = v; }
    }

    static class AnnotatedMinimalWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
        public BaseValue value;

        protected AnnotatedMinimalWrapper() { }
        public AnnotatedMinimalWrapper(BaseValue v) { value = v; }
    }

    static class DefTypeWrapper {
        public BaseValue value;

        protected DefTypeWrapper() { }
        public DefTypeWrapper(BaseValue v) { value = v; }
    }

    // // // Validator implementations

    static class SimpleNameBasedValidator extends PolymorphicTypeValidator {
        private static final long serialVersionUID = 1L;

        // No categoric determination, depends on subtype
        @Override
        public Validity validateBaseType(MapperConfig<?> ctxt, JavaType baseType) {
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubClassName(MapperConfig<?> ctxt, JavaType baseType, String subClassName) {
            if (subClassName.equals(BadValue.class.getName())) {
                return Validity.DENIED;
            }
            if (subClassName.equals(GoodValue.class.getName())) {
                return Validity.ALLOWED;
            }
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubType(MapperConfig<?> ctxt, JavaType baseType, JavaType subType) {
            return Validity.DENIED;
        }
    }

    static class SimpleClassBasedValidator extends PolymorphicTypeValidator {
        private static final long serialVersionUID = 1L;

        @Override
        public Validity validateBaseType(MapperConfig<?> ctxt, JavaType baseType) {
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubClassName(MapperConfig<?> ctxt, JavaType baseType, String subClassName) {
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubType(MapperConfig<?> ctxt, JavaType baseType, JavaType subType) {
            if (subType.hasRawClass(BadValue.class)) {
                return Validity.DENIED;
            }
            if (subType.hasRawClass(GoodValue.class)) {
                return Validity.ALLOWED;
            }
            // defaults to denied, then:
            return Validity.INDETERMINATE;        }
    }

    // // // Mappers with Default Typing

    private final ObjectMapper MAPPER_DEF_TYPING_NAME_CHECK = jsonMapperBuilder()
            .activateDefaultTyping(new SimpleNameBasedValidator())
            .build();

    private final ObjectMapper MAPPER_DEF_TYPING_CLASS_CHECK = jsonMapperBuilder()
            .activateDefaultTyping(new SimpleClassBasedValidator())
            .build();

    // // // Mappers without Default Typing (explicit annotation needed)

    private final ObjectMapper MAPPER_EXPLICIT_NAME_CHECK = jsonMapperBuilder()
            .polymorphicTypeValidator(new SimpleNameBasedValidator())
            .build();

    private final ObjectMapper MAPPER_EXPLICIT_CLASS_CHECK = jsonMapperBuilder()
            .polymorphicTypeValidator(new SimpleClassBasedValidator())
            .build();

    /*
    /**********************************************************************
    /* Test methods: default typing
    /**********************************************************************
     */

    // // With Name check

    public void testWithDefaultTypingNameAccept() throws Exception
    {
        final BaseValue inputValue = new GoodValue();
        DefTypeWrapper result = _roundTripDefault(MAPPER_DEF_TYPING_NAME_CHECK, inputValue);
        assertEquals(inputValue, result.value);
    }

    public void testWithDefaultTypingNameDenyExplicit() throws Exception
    {
        _verifyBadDefaultValue(MAPPER_DEF_TYPING_NAME_CHECK);
    }

    public void testWithDefaultTypingNameDenyDefault() throws Exception
    {
        _verifyMehDefaultValue(MAPPER_DEF_TYPING_NAME_CHECK);
    }

    // // With Class check

    public void testWithDefaultTypingClassAccept() throws Exception
    {
        final BaseValue inputValue = new GoodValue();
        DefTypeWrapper result = _roundTripDefault(MAPPER_DEF_TYPING_CLASS_CHECK, inputValue);
        assertEquals(inputValue, result.value);
    }

    public void testWithDefaultTypingClassDenyExplicit() throws Exception
    {
        _verifyBadDefaultValue(MAPPER_DEF_TYPING_CLASS_CHECK);
    }

    public void testWithDefaultTypingClassDenyDefault() throws Exception
    {
        _verifyMehDefaultValue(MAPPER_DEF_TYPING_CLASS_CHECK);
    }

    /*
    /**********************************************************************
    /* Test methods, annotated typing, full class
    /**********************************************************************
     */

    // // With Name

    public void testWithAnnotationNameAccept() throws Exception
    {
        final BaseValue inputValue = new GoodValue();
        AnnotatedWrapper result = _roundTripAnnotated(MAPPER_EXPLICIT_NAME_CHECK, inputValue);
        assertEquals(inputValue, result.value);
    }

    public void testWithAnnotationNameDenyExplicit() throws Exception
    {
        _verifyBadAnnotatedValue(MAPPER_EXPLICIT_NAME_CHECK);
    }

    public void testWithAnnotationNameDenyDefault() throws Exception
    {
        _verifyMehAnnotatedValue(MAPPER_EXPLICIT_NAME_CHECK);
    }

    // // With Class

    public void testWithAnnotationClassAccept() throws Exception
    {
        final BaseValue inputValue = new GoodValue();
        AnnotatedWrapper result = _roundTripAnnotated(MAPPER_EXPLICIT_CLASS_CHECK, inputValue);
        assertEquals(inputValue, result.value);
    }

    public void testWithAnnotationClassDenyExplicit() throws Exception
    {
        _verifyBadAnnotatedValue(MAPPER_EXPLICIT_CLASS_CHECK);
    }

    public void testWithAnnotationClassDenyDefault() throws Exception
    {
        _verifyMehAnnotatedValue(MAPPER_EXPLICIT_CLASS_CHECK);
    }

    /*
    /**********************************************************************
    /* Test methods, annotated typing, minimal class name
    /**********************************************************************
     */

    // // With Name

    public void testWithAnnotationMinClassNameAccept() throws Exception
    {
        final BaseValue inputValue = new GoodValue();
        AnnotatedMinimalWrapper result = _roundTripAnnotatedMinimal(MAPPER_EXPLICIT_NAME_CHECK, inputValue);
        assertEquals(inputValue, result.value);
    }

    public void testWithAnnotationMinClassNameDenyExplicit() throws Exception
    {
        _verifyBadAnnotatedMinValue(MAPPER_EXPLICIT_NAME_CHECK);
    }

    public void testWithAnnotationMinClassNameDenyDefault() throws Exception
    {
        _verifyMehAnnotatedMinValue(MAPPER_EXPLICIT_NAME_CHECK);
    }

    // // With Class

    public void testWithAnnotationMinClassClassAccept() throws Exception
    {
        final BaseValue inputValue = new GoodValue();
        AnnotatedMinimalWrapper result = _roundTripAnnotatedMinimal(MAPPER_EXPLICIT_CLASS_CHECK, inputValue);
        assertEquals(inputValue, result.value);
    }

    public void testWithAnnotationMinClassClassDenyExplicit() throws Exception
    {
        _verifyBadAnnotatedMinValue(MAPPER_EXPLICIT_CLASS_CHECK);
    }

    public void testWithAnnotationMinClassClassDenyDefault() throws Exception
    {
        _verifyMehAnnotatedMinValue(MAPPER_EXPLICIT_CLASS_CHECK);
    }

    /*
    /**********************************************************************
    /* Helper methods, round-trip (ok case)
    /**********************************************************************
     */

    private DefTypeWrapper _roundTripDefault(ObjectMapper mapper, BaseValue input) throws Exception {
        final String json = mapper.writeValueAsString(new DefTypeWrapper(input));
        return mapper.readValue(json, DefTypeWrapper.class);
    }

    private AnnotatedWrapper _roundTripAnnotated(ObjectMapper mapper, BaseValue input) throws Exception {
        final String json = mapper.writeValueAsString(new AnnotatedWrapper(input));
        return mapper.readValue(json, AnnotatedWrapper.class);
    }

    private AnnotatedMinimalWrapper _roundTripAnnotatedMinimal(ObjectMapper mapper, BaseValue input) throws Exception {
        final String json = mapper.writeValueAsString(new AnnotatedMinimalWrapper(input));
        return mapper.readValue(json, AnnotatedMinimalWrapper.class);
    }

    /*
    /**********************************************************************
    /* Helper methods, failing deser verification
    /**********************************************************************
     */

    private void _verifyBadDefaultValue(ObjectMapper mapper) throws Exception {
        final String json = mapper.writeValueAsString(new DefTypeWrapper(new BadValue()));
        _verifyBadValue(mapper, json, DefTypeWrapper.class);
    }

    private void _verifyMehDefaultValue(ObjectMapper mapper) throws Exception {
        final String json = mapper.writeValueAsString(new DefTypeWrapper(new MehValue()));
        _verifyMehValue(mapper, json, DefTypeWrapper.class);
    }

    private void _verifyBadAnnotatedValue(ObjectMapper mapper) throws Exception {
        final String json = mapper.writeValueAsString(new AnnotatedWrapper(new BadValue()));
        _verifyBadValue(mapper, json, AnnotatedWrapper.class);
    }

    private void _verifyMehAnnotatedValue(ObjectMapper mapper) throws Exception {
        final String json = mapper.writeValueAsString(new AnnotatedWrapper(new MehValue()));
        _verifyMehValue(mapper, json, AnnotatedWrapper.class);
    }

    private void _verifyBadAnnotatedMinValue(ObjectMapper mapper) throws Exception {
        final String json = mapper.writeValueAsString(new AnnotatedMinimalWrapper(new BadValue()));
        _verifyBadValue(mapper, json, AnnotatedMinimalWrapper.class);
    }

    private void _verifyMehAnnotatedMinValue(ObjectMapper mapper) throws Exception {
        final String json = mapper.writeValueAsString(new AnnotatedMinimalWrapper(new MehValue()));
        _verifyMehValue(mapper, json, AnnotatedMinimalWrapper.class);
    }

    private void _verifyBadValue(ObjectMapper mapper, String json, Class<?> type) throws Exception {
        try {
            mapper.readValue(json, type);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id");
            verifyException(e, "`PolymorphicTypeValidator`");
            verifyException(e, "denied resolution");
        }
    }

    private void _verifyMehValue(ObjectMapper mapper, String json, Class<?> type) throws Exception {
        try {
            mapper.readValue(json, type);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id");
            verifyException(e, "`PolymorphicTypeValidator`");
            verifyException(e, "denied resolution");
        }
    }
}
