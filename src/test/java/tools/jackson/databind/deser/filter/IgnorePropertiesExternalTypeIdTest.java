package tools.jackson.databind.deser.filter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for per-property {@code @JsonIgnoreProperties} / {@code @JsonIncludeProperties}
 * excluding a property with an external type id ({@code As.EXTERNAL_PROPERTY}):
 * such properties are only known to the {@code ExternalTypeHandler}, which must not
 * bind them either.
 */
public class IgnorePropertiesExternalTypeIdTest extends DatabindTestUtil
{
    static abstract class Animal {
        public String name;
    }

    static class Dog extends Animal { }

    // Property-based Creator, external-typed property NOT a Creator property
    static class CreatorBean {
        public final String id;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
        public Animal value;

        @JsonCreator
        public CreatorBean(@JsonProperty("id") String id) {
            this.id = id;
        }
    }

    // Same but with default constructor
    static class PlainBean {
        public String id;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
        public Animal value;
    }

    static class CreatorIgnoringWrapper {
        @JsonIgnoreProperties("value")
        public CreatorBean bean;
    }

    static class CreatorIncludingWrapper {
        @JsonIncludeProperties("id")
        public CreatorBean bean;
    }

    static class PlainIgnoringWrapper {
        @JsonIgnoreProperties("value")
        public PlainBean bean;
    }

    static class PlainIncludingWrapper {
        @JsonIncludeProperties("id")
        public PlainBean bean;
    }

    static class CreatorWrapper {
        public CreatorBean bean;
    }

    static class PlainWrapper {
        public PlainBean bean;
    }

    // External-typed Creator property
    static class CreatorParamBean {
        public final String id;
        public final Animal value;

        @JsonCreator
        public CreatorParamBean(@JsonProperty("id") String id,
                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
                @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
                @JsonProperty("value") Animal value) {
            this.id = id;
            this.value = value;
        }
    }

    static class CreatorParamIgnoringWrapper {
        @JsonIgnoreProperties("value")
        public CreatorParamBean bean;
    }

    // Bean with any-setter: type id of excluded property must not end up there
    static class AnySetterBean extends PlainBean {
        public Map<String, Object> other = new LinkedHashMap<>();

        @JsonAnySetter
        public void set(String name, Object value) {
            other.put(name, value);
        }
    }

    static class AnySetterIgnoringWrapper {
        @JsonIgnoreProperties("value")
        public AnySetterBean bean;
    }

    // Two external-typed properties, sharing type id or not
    static class TwoPropsBean {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
        public Animal value;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
        public Animal sameTypeId;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type2")
        @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
        public Animal otherTypeId;
    }

    static class TwoPropsIgnoringWrapper {
        @JsonIgnoreProperties({ "sameTypeId", "otherTypeId" })
        public TwoPropsBean bean;
    }

    // Builder-based
    @JsonDeserialize(builder = BuiltBean.Builder.class)
    static class BuiltBean {
        public final String id;
        public final Animal value;

        BuiltBean(String id, Animal value) {
            this.id = id;
            this.value = value;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            private String id;
            private Animal value;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
            @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
            public Builder value(Animal value) {
                this.value = value;
                return this;
            }

            public BuiltBean build() {
                return new BuiltBean(id, value);
            }
        }
    }

    static class BuiltIgnoringWrapper {
        @JsonIgnoreProperties("value")
        public BuiltBean bean;
    }

    // Value property included but not its type id name: type id must still be used
    static class CreatorValueIncludingWrapper {
        @JsonIncludeProperties({ "id", "value" })
        public CreatorBean bean;
    }

    static class PlainValueIncludingWrapper {
        @JsonIncludeProperties({ "id", "value" })
        public PlainBean bean;
    }

    static class BuiltValueIncludingWrapper {
        @JsonIncludeProperties({ "id", "value" })
        public BuiltBean bean;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#6312]: type id of excluded property must not be reported as unknown
    private final ObjectMapper STRICT_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final ObjectMapper[] MAPPERS = { MAPPER, STRICT_MAPPER };

    // Type id and value in either order; also external type id before Creator property
    private final static String[] DOCS = new String[] {
            """
            {"bean":{"id":"x","type":"dog","value":{"name":"Rex"}}}
            """,
            """
            {"bean":{"id":"x","value":{"name":"Rex"},"type":"dog"}}
            """,
            """
            {"bean":{"type":"dog","value":{"name":"Rex"},"id":"x"}}
            """
    };

    @Test
    public void ignoredExternalTypedPropertyWithCreator() throws Exception
    {
        for (ObjectMapper mapper : MAPPERS) {
            for (String doc : DOCS) {
                CreatorIgnoringWrapper ign = mapper.readValue(doc, CreatorIgnoringWrapper.class);
                assertEquals("x", ign.bean.id);
                assertNull(ign.bean.value);

                CreatorIncludingWrapper incl = mapper.readValue(doc, CreatorIncludingWrapper.class);
                assertEquals("x", incl.bean.id);
                assertNull(incl.bean.value);
            }
        }
    }

    @Test
    public void ignoredExternalTypedPropertyWithoutCreator() throws Exception
    {
        for (ObjectMapper mapper : MAPPERS) {
            for (String doc : DOCS) {
                PlainIgnoringWrapper ign = mapper.readValue(doc, PlainIgnoringWrapper.class);
                assertEquals("x", ign.bean.id);
                assertNull(ign.bean.value);

                PlainIncludingWrapper incl = mapper.readValue(doc, PlainIncludingWrapper.class);
                assertEquals("x", incl.bean.id);
                assertNull(incl.bean.value);
            }
        }
    }

    // Without exclusion, same documents must still bind the value
    @Test
    public void externalTypedPropertyNotIgnored() throws Exception
    {
        for (String doc : DOCS) {
            CreatorWrapper c = MAPPER.readValue(doc, CreatorWrapper.class);
            assertEquals("Rex", c.bean.value.name);
            PlainWrapper p = MAPPER.readValue(doc, PlainWrapper.class);
            assertEquals("Rex", p.bean.value.name);
        }
    }

    @Test
    public void ignoredExternalTypedCreatorProperty() throws Exception
    {
        for (ObjectMapper mapper : MAPPERS) {
            for (String doc : DOCS) {
                CreatorParamIgnoringWrapper ign = mapper.readValue(doc,
                        CreatorParamIgnoringWrapper.class);
                assertEquals("x", ign.bean.id);
                assertNull(ign.bean.value);
            }
        }
    }

    @Test
    public void ignoredExternalTypeIdNotPassedToAnySetter() throws Exception
    {
        for (String doc : DOCS) {
            AnySetterIgnoringWrapper ign = MAPPER.readValue(doc, AnySetterIgnoringWrapper.class);
            assertEquals("x", ign.bean.id);
            assertNull(ign.bean.value);
            assertEquals(Map.of(), ign.bean.other);
        }
    }

    // Only some external-typed properties excluded: remaining ones still bound,
    // type id still in use kept, type id no longer in use ignored
    @Test
    public void partiallyIgnoredExternalTypedProperties() throws Exception
    {
        final String doc = """
                {"bean":{"type":"dog","value":{"name":"Rex"},
                  "sameTypeId":{"name":"Fido"},
                  "type2":"dog","otherTypeId":{"name":"Spot"}}}
                """;
        for (ObjectMapper mapper : MAPPERS) {
            TwoPropsIgnoringWrapper ign = mapper.readValue(doc, TwoPropsIgnoringWrapper.class);
            assertNotNull(ign.bean.value);
            assertEquals("Rex", ign.bean.value.name);
            assertNull(ign.bean.sameTypeId);
            assertNull(ign.bean.otherTypeId);
        }
    }

    @Test
    public void ignoredExternalTypedPropertyWithBuilder() throws Exception
    {
        for (ObjectMapper mapper : MAPPERS) {
            for (String doc : DOCS) {
                BuiltIgnoringWrapper ign = mapper.readValue(doc, BuiltIgnoringWrapper.class);
                assertEquals("x", ign.bean.id);
                assertNull(ign.bean.value);
            }
        }
    }

    // Included value property must be bound even if its type id name is not
    // explicitly included
    @Test
    public void includedExternalTypedPropertyWithoutTypeIdName() throws Exception
    {
        for (ObjectMapper mapper : MAPPERS) {
            for (String doc : DOCS) {
                CreatorValueIncludingWrapper c = mapper.readValue(doc,
                        CreatorValueIncludingWrapper.class);
                assertEquals("x", c.bean.id);
                assertEquals("Rex", c.bean.value.name);

                PlainValueIncludingWrapper pl = mapper.readValue(doc,
                        PlainValueIncludingWrapper.class);
                assertEquals("x", pl.bean.id);
                assertEquals("Rex", pl.bean.value.name);

                BuiltValueIncludingWrapper b = mapper.readValue(doc,
                        BuiltValueIncludingWrapper.class);
                assertEquals("x", b.bean.id);
                assertEquals("Rex", b.bean.value.name);
            }
        }
    }
}
