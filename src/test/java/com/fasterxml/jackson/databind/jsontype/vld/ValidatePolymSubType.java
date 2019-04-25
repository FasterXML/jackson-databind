package com.fasterxml.jackson.databind.jsontype.vld;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Tests to verify working of customizable {@PolymorphicTypeValidator}
 *
 * @since 2.10
 */
public class ValidatePolymSubType extends BaseMapTest
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

    static class DefTypeMinimalWrapper {
        public BaseValue value;

        protected DefTypeMinimalWrapper() { }
        public DefTypeMinimalWrapper(BaseValue v) { value = v; }
    }
    
    // // // Validator implementations
    
    static class SimpleNameBasedValidator extends PolymorphicTypeValidator {
        private static final long serialVersionUID = 1L;

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
            return Validity.INDETERMINATE;
        }
    }

    // // // Mappers with Default Typing
    
    private final ObjectMapper MAPPER_DEF_TYPING_NAME_CHECK = jsonMapperBuilder()
            .polymorphicTypeValidator(new SimpleNameBasedValidator())
            .enableDefaultTyping()
            .build();

    private final ObjectMapper MAPPER_DEF_TYPING_CLASS_CHECK = jsonMapperBuilder()
            .polymorphicTypeValidator(new SimpleClassBasedValidator())
            .enableDefaultTyping()
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
        
    }

    public void testWithDefaultTypingNameDenyDefault() throws Exception
    {
        
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
        
    }

    public void testWithDefaultTypingClassDenyDefault() throws Exception
    {
        
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
    }

    public void testWithAnnotationNameDenyDefault() throws Exception
    {
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
    }

    public void testWithAnnotationClassDenyDefault() throws Exception
    {
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
        
    }

    public void testWithAnnotationMinClassNameDenyDefault() throws Exception
    {
        
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
        
    }

    public void testWithAnnotationMinClassClassDenyDefault() throws Exception
    {
        
    }
    
    /*
    /**********************************************************************
    /* Helper methods
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
}
