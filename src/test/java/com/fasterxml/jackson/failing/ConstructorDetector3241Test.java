package com.fasterxml.jackson.failing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.fail;

// Tests for [databind#1498] (Jackson 2.12)
class ConstructorDetector3241Test extends DatabindTestUtil {
    // Helper annotation to work around lack of implicit name access with Jackson 2.x
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ImplicitName {
        String value();
    }

    // And annotation introspector to make use of it
    @SuppressWarnings("serial")
    static class CtorNameIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public String findImplicitPropertyName(//MapperConfig<?> config,
                                               AnnotatedMember member) {
            final ImplicitName ann = member.getAnnotation(ImplicitName.class);
            return (ann == null) ? null : ann.value();
        }
    }

    // [databind#3241]
    static class Input3241 {
        private final Boolean field;

        // @JsonCreator gone!
        public Input3241(@ImplicitName("field") Boolean field) {
            if (field == null) {
                throw new NullPointerException("Field should not remain null!");
            }
            this.field = field;
        }

        public Boolean field() {
            return field;
        }
    }

    // [databind#3241]
    @Test
    void nullHandlingCreator3241() throws Exception {
        ObjectMapper mapper = mapperBuilder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED) // new!
                .defaultSetterInfo(JsonSetter.Value.construct(Nulls.FAIL, Nulls.FAIL))
                .build();

        try {
            mapper.readValue("{ \"field\": null }", Input3241.class);
            fail("InvalidNullException expected");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered");
        }
    }

    private JsonMapper.Builder mapperBuilder() {
        return jsonMapperBuilder()
                .annotationIntrospector(new CtorNameIntrospector());
    }
}
