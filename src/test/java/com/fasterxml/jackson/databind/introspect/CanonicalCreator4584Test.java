package com.fasterxml.jackson.databind.introspect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.Assert.assertEquals;

// Tests for [databind#4584]: extension point for discovering "Canonical"
// Creator (primary Creator, usually constructor, used in case no creator
// explicitly annotated)
//
// @since 2.18
public class CanonicalCreator4584Test extends DatabindTestUtil
{
    static class POJO4584 {
        final String value;

        POJO4584(@ImplicitName("v") String v, @ImplicitName("bogus") int bogus) {
            value = v;
        }

        public POJO4584(@ImplicitName("list") List<Object> list) {
            value = "List["+((list == null) ? -1 : list.size())+"]";
        }

        public POJO4584(@ImplicitName("array") Object[] array) {
            value = "Array["+((array == null) ? -1 : array.length)+"]";
        }

        public static POJO4584 factoryInt(@ImplicitName("i") int i) {
            return new POJO4584("int["+i+"]", 0);
        }

        public static POJO4584 factoryString(@ImplicitName("v") String v) {
            return new POJO4584(v, 0);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof POJO4584) && Objects.equals(((POJO4584) o).value, value);
        }

        @Override
        public String toString() {
            return "'"+value+"'";
        }
    }

    static class PrimaryCreatorFindingIntrospector extends ImplicitNameIntrospector
    {
        private static final long serialVersionUID = 1L;

        private final Class<?>[] _argTypes;

        private JsonCreator.Mode _mode;

        public PrimaryCreatorFindingIntrospector(JsonCreator.Mode mode,
                Class<?>... argTypes) {
            _mode = mode;
            _argTypes = argTypes;
        }

        @Override
        public PotentialCreator findPrimaryCreator(MapperConfig<?> config,
                AnnotatedClass valueClass,
                List<PotentialCreator> declaredConstructors,
                List<PotentialCreator> declaredFactories)
        {
            if (valueClass.getRawType() != POJO4584.class) {
                return null;
            }
            List<PotentialCreator> combo = new ArrayList<>(declaredConstructors);
            combo.addAll(declaredFactories);
            final int argCount = _argTypes.length;
            for (PotentialCreator ctor : combo) {
                if (ctor.paramCount() == argCount) {
                    int i = 0;
                    for (; i < argCount; ++i) {
                        if (_argTypes[i] != ctor.param(i).getRawType()) {
                            break;
                        }
                    }
                    if (i == argCount) {
                        ctor.overrideMode(_mode);
                        return ctor;
                    }
                }
            }
            return null;
        }
    }

    /*
    /**********************************************************************
    /* Test methods; properties-based Creators
    /**********************************************************************
     */

    @Test
    public void testCanonicalConstructor1ArgPropertiesCreator() throws Exception
    {
        // Instead of delegating, try denoting List-taking 1-arg one:
        assertEquals(POJO4584.factoryString("List[2]"),
                readerWith(new PrimaryCreatorFindingIntrospector(JsonCreator.Mode.PROPERTIES,
                        List.class))
                    .readValue(a2q("{'list':[ 1, 2]}")));
    }

    @Test
    public void testCanonicalConstructor2ArgPropertiesCreator() throws Exception
    {
        // Mark the "true" canonical
        assertEquals(POJO4584.factoryString("abc"),
                readerWith(new PrimaryCreatorFindingIntrospector(JsonCreator.Mode.PROPERTIES,
                        String.class, Integer.TYPE))
                    .readValue(a2q("{'bogus':12, 'v':'abc' }")));
    }

    /*
    /**********************************************************************
    /* Test methods; delegation-based Creators
    /**********************************************************************
     */

    @Test
    public void testCanonicalConstructorDelegatingIntCreator() throws Exception
    {
        assertEquals(POJO4584.factoryString("int[42]"),
                readerWith(new PrimaryCreatorFindingIntrospector(JsonCreator.Mode.DELEGATING,
                        Integer.TYPE))
                    .readValue(a2q("42")));
    }
    
    @Test
    public void testCanonicalConstructorDelegatingListCreator() throws Exception
    {
        assertEquals(POJO4584.factoryString("List[3]"),
                readerWith(new PrimaryCreatorFindingIntrospector(JsonCreator.Mode.DELEGATING,
                        List.class))
                    .readValue(a2q("[1, 2, 3]")));
    }

    @Test
    public void testCanonicalConstructorDelegatingArrayCreator() throws Exception
    {
        assertEquals(POJO4584.factoryString("Array[1]"),
                readerWith(new PrimaryCreatorFindingIntrospector(JsonCreator.Mode.DELEGATING,
                        Object[].class))
                    .readValue(a2q("[true]")));
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private ObjectReader readerWith(AnnotationIntrospector intr) {
        return mapperWith(intr).readerFor(POJO4584.class);
    }

    private ObjectMapper mapperWith(AnnotationIntrospector intr) {
        return JsonMapper.builder()
                .annotationIntrospector(intr)
                .build();
    }
}
