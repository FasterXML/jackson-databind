package com.fasterxml.jackson.databind.introspect;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.Assert.assertEquals;

// Tests for [databind#4620]: 
//
// @since 2.18
public class DefaultCreatorResolution4620Test extends DatabindTestUtil
{
    static class POJO4620 {
        String value;

        public POJO4620(@JsonProperty("int") int i) {
            throw new RuntimeException("Should not get called");
        }

        public POJO4620(@JsonProperty("str") String str, @JsonProperty("int") int v) {
            value = str + "/" + v;
        }

        public POJO4620(@JsonProperty("str") String str,
                @JsonProperty("int") int v,
                @JsonProperty("long") long l) {
            throw new RuntimeException("Should not get called");
        }
    }

    static class PrimaryConstructorFindingIntrospector extends ImplicitNameIntrospector
    {
        private static final long serialVersionUID = 1L;

        private final Class<?>[] _argTypes;

        public PrimaryConstructorFindingIntrospector(Class<?>... argTypes) {
            _argTypes = argTypes;
        }

        @Override
        public PotentialCreator findDefaultCreator(MapperConfig<?> config,
                AnnotatedClass valueClass,
                List<PotentialCreator> declaredConstructors,
                List<PotentialCreator> declaredFactories)
        {
            // Apply to all test POJOs here but nothing else
            if (!valueClass.getRawType().toString().contains("4620")) {
                return null;
            }

            final int argCount = _argTypes.length;
            for (PotentialCreator ctor : declaredConstructors) {
                if (ctor.paramCount() == argCount) {
                    int i = 0;
                    for (; i < argCount; ++i) {
                        if (_argTypes[i] != ctor.param(i).getRawType()) {
                            break;
                        }
                    }
                    if (i == argCount) {
                        ctor.overrideMode(JsonCreator.Mode.PROPERTIES);
                        return ctor;
                    }
                }
            }
            return null;
        }
    }

    /*
    /**********************************************************************
    /* Test methods; simple properties-based Creators
    /**********************************************************************
     */

    @Test
    public void testCanonicalConstructor1ArgPropertiesCreator() throws Exception
    {
        // Select the "middle one"
        POJO4620 result = readerWith(new PrimaryConstructorFindingIntrospector(
                String.class, Integer.TYPE))
                .readValue(a2q("{'str':'value', 'int':42}"));
        assertEquals("value/42", result.value);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private ObjectReader readerWith(AnnotationIntrospector intr) {
        return mapperWith(intr).readerFor(POJO4620.class);
    }

    private ObjectMapper mapperWith(AnnotationIntrospector intr) {
        return JsonMapper.builder()
                .annotationIntrospector(intr)
                .build();
    }
}
