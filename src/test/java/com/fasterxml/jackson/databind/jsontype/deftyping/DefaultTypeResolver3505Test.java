package com.fasterxml.jackson.databind.jsontype.deftyping;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// Tests for [databind#3505] causing a NPE when setting a DefaultTypeResolverBuilder
// and registering subtypes through ObjectMapper (no annotations)
public class DefaultTypeResolver3505Test
    extends BaseMapTest
{
    interface Parent {
        class ChildOne implements Parent {
            public String one;
        }

        class ChildTwo implements Parent {
            public String two;
        }
    }

    // This class is technically not needed for the test to fail without the fix
    // (AsDeductionTypeDeserializer will crash in #buildFingerprints), but was
    // added to have more assertions on the subtypes values
    @SuppressWarnings("serial")
    static final class AssertingTypeResolverBuilder
        extends ObjectMapper.DefaultTypeResolverBuilder
    {
        public AssertingTypeResolverBuilder() {
            super(ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS,
                    BasicPolymorphicTypeValidator.builder().allowIfSubType(Parent.class).build());
        }

        @Override
        public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
            // this also gets called for String, not sure if that's expected with PTV
            // this behaviour is outside the scope of this test
            if (baseType.isAbstract()) {
                assertNotNull(subtypes);
                assertEquals(2, subtypes.size());
                final List<NamedType> expected = new ArrayList<>(2);
                expected.add(new NamedType(Parent.ChildOne.class));
                expected.add(new NamedType(Parent.ChildTwo.class));
                assertTrue(subtypes.containsAll(expected));
            }

            return super.buildTypeDeserializer(config, baseType, subtypes);
        }
    }

    public void testDeductionWithDefaultTypeResolverBuilder() {
        final ObjectMapper mapper = jsonMapperBuilder()
                .registerSubtypes(Parent.ChildOne.class, Parent.ChildTwo.class)
                .setDefaultTyping(new AssertingTypeResolverBuilder()
                        .init(JsonTypeInfo.Id.DEDUCTION, null))
                .build();

        try {
            final Parent firstRead = mapper.readValue("{ \"one\": \"Hello World\" }", Parent.class);
            assertNotNull(firstRead);
            assertTrue(firstRead instanceof Parent.ChildOne);
            assertEquals("Hello World", ((Parent.ChildOne) firstRead).one);
            final Parent secondRead = mapper.readValue("{ \"two\": \"Hello World\" }", Parent.class);
            assertNotNull(secondRead);
            assertTrue(secondRead instanceof Parent.ChildTwo);
            assertEquals("Hello World", ((Parent.ChildTwo) secondRead).two);
        } catch (Exception e) {
            fail("This call should not throw");
        }
    }
}
