package tools.jackson.databind.jsontype.deftyping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [databind#3505] causing a NPE when setting a DefaultTypeResolverBuilder
// and registering subtypes through ObjectMapper (no annotations)
public class DefaultTypeResolver3505Test
    extends DatabindTestUtil
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
        extends DefaultTypeResolverBuilder
    {
        public AssertingTypeResolverBuilder() {
            super(BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(Parent.class).build(),
                    DefaultTyping.NON_CONCRETE_AND_ARRAYS,
                    JsonTypeInfo.As.PROPERTY);
        }

        @Override
        public TypeDeserializer buildTypeDeserializer(DeserializationContext ctxt,
                JavaType baseType, Collection<NamedType> subtypes) {
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

            return super.buildTypeDeserializer(ctxt, baseType, subtypes);
        }
    }

    @Test
    public void testDeductionWithDefaultTypeResolverBuilder() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .registerSubtypes(Parent.ChildOne.class, Parent.ChildTwo.class)
                .setDefaultTyping(new AssertingTypeResolverBuilder()
                        .init(JsonTypeInfo.Value.construct(JsonTypeInfo.Id.DEDUCTION,
                                JsonTypeInfo.As.PROPERTY, "prop", null, false, null),
                                null))
                .build();

        final Parent firstRead = mapper.readValue("{ \"one\": \"Hello World\" }", Parent.class);
        assertNotNull(firstRead);
        assertTrue(firstRead instanceof Parent.ChildOne);
        assertEquals("Hello World", ((Parent.ChildOne) firstRead).one);
        final Parent secondRead = mapper.readValue("{ \"two\": \"Hello World\" }", Parent.class);
        assertNotNull(secondRead);
        assertTrue(secondRead instanceof Parent.ChildTwo);
        assertEquals("Hello World", ((Parent.ChildTwo) secondRead).two);
    }
}
